import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MotorPHSystem {

    // --- GLOBAL SETTINGS & CONSTANTS ---
    static final String EMP_DB = "resource/MotorPH_Employee Database.csv";
    static final String ATT_DB = "resource/Attendance Record.csv";
    static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:mm:ss]");
    
    // Business Rule Constants 
    static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    static final LocalTime GRACE_PERIOD_END = LocalTime.of(8, 10);
    static final LocalTime SHIFT_END = LocalTime.of(17, 0);
    static final double LUNCH_BREAK_HOUR = 1.0;

    public static void main(String[] args) {
        startSystem();
    }

    /**
     * Handles system login and user routing.
     */
    static void startSystem() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n========================================");
            System.out.println("      MOTOR PH MANAGEMENT SYSTEM       ");
            System.out.println("========================================");
            
            System.out.print("Username : "); String user = sc.nextLine().trim().toLowerCase();
            System.out.print("Password : "); String pass = sc.nextLine();

            if (!pass.equals("12345")) {
                System.out.println("[!] Access Denied: Invalid Password.");
                continue;
            }

            if (user.equals("payroll_staff")) {
                runAdminDashboard(sc);
            } else if (user.equals("employee")) {
                System.out.print("Enter Employee ID: ");
                String empId = sc.nextLine().trim();
                String[] row = findEmployeeData(empId);
                
                if (row != null) {
                    runEmployeeDashboard(row, sc);
                } else {
                    System.out.println("[!] Error: Employee ID not found.");
                }
            } else {
                System.out.println("[!] Invalid Username. Please use 'admin' or 'employee'.");
            }
        }
    }

    /**
     * Provides administrative functions.
     * PERFORMANCE FIX: Loads attendance ONCE before processing.
     */
    static void runAdminDashboard(Scanner sc) {
        while (true) {
            System.out.println("\n--- [ADMIN] MASTER PAYROLL PORTAL ---");
            System.out.println("[1] Generate Payroll Report | [2] Logout");
            System.out.print("Select: ");
            String choice = sc.nextLine().trim();
            
            if (choice.equals("2")) break;
            
            if (choice.equals("1")) {
                System.out.print("Enter Target ID (or 'ALL'): ");
                String id = sc.nextLine().trim();
                
                // Load data into memory once to resolve performance hit
                List<String[]> cachedAttendance = loadAttendanceToMemory();

                if (id.equalsIgnoreCase("ALL")) {
                    processEntirePayroll(cachedAttendance);
                } else {
                    if (findEmployeeData(id) != null) {
                        executePayrollCalculation(id, cachedAttendance);
                    } else {
                        System.out.println("[!] Invalid ID#, please try again.");
                    }
                }
            }
        }
    }

    /**
     * Displays employee profile information.
     */
    static void runEmployeeDashboard(String[] data, Scanner sc) {
        while (true) {
            System.out.println("\n--- [EMPLOYEE] INFORMATION PORTAL ---");
            System.out.println("[1] View Profile | [2] Logout");
            if (sc.nextLine().equals("2")) break;
            
            System.out.println("\n--- PERSONAL DETAILS ---");
            System.out.println("Employee ID: " + data[0]);
            System.out.println("Full Name  : " + data[1].toUpperCase() + ", " + data[2].toUpperCase());
            System.out.println("Position   : " + data[11]);
            System.out.println("Birthday   : " + data[3]);
        }
    }

    /**
     * Loads attendance CSV into memory 
     */
    static List<String[]> loadAttendanceToMemory() {
        List<String[]> allRecords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ATT_DB))) {
            String line; 
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                allRecords.add(line.split(","));
            }
        } catch (Exception e) {
            System.out.println("[!] Error: Attendance file not found in resource folder.");
        }
        return allRecords;
    }

    /**
     * Calculates hours worked using the in-memory attendance list.
     */
    static double computeHoursWorked(String id, int month, int year, int startDay, int endDay, List<String[]> attendanceData) {
        double totalHours = 0;
        for (String[] row : attendanceData) {
            String[] dParts = row[3].split("/");
            int m = Integer.parseInt(dParts[0]), d = Integer.parseInt(dParts[1]), y = Integer.parseInt(dParts[2]);

            if (row[0].equals(id) && m == month && y == year && d >= startDay && d <= endDay) {
                LocalTime in = LocalTime.parse(row[4], TF);
                LocalTime out = LocalTime.parse(row[5], TF);

                // Apply Shift Windowing & Grace Period
                if (in.isBefore(SHIFT_START)) in = SHIFT_START;
                if (out.isAfter(SHIFT_END)) out = SHIFT_END;
                if (!in.isAfter(GRACE_PERIOD_END)) in = SHIFT_START;

                double daily = (Duration.between(in, out).toMinutes() / 60.0) - LUNCH_BREAK_HOUR;
                totalHours += Math.max(0, daily);
            }
        }
        return totalHours;
    }

    /**
     * Payroll Execution.
     * 1st Cutoff = Gross. 2nd Cutoff = Gross minus itemized deductions.
     */
    static void executePayrollCalculation(String id, List<String[]> attendanceData) {
        String[] emp = findEmployeeData(id);
        if (emp == null) return;

        double basic = cleanValue(emp[13]);
        double hourlyRate = cleanValue(emp[18]);

        System.out.println("\n**************************************************************");
        System.out.println("PAYROLL SUMMARY: " + emp[1].toUpperCase() + ", " + emp[2].toUpperCase());
        System.out.printf("EMPLOYEE ID: %s | HOURLY RATE: %,.2f\n", emp[0], hourlyRate);
        System.out.println("**************************************************************");

        for (int m = 6; m <= 12; m++) {
            int lastDay = YearMonth.of(2024, m).lengthOfMonth();
            
            double h1 = computeHoursWorked(id, m, 2024, 1, 15, attendanceData);
            double h2 = computeHoursWorked(id, m, 2024, 16, lastDay, attendanceData);

            if (h1 + h2 == 0) continue;

            // Statutory Deduction Logic
            double sss = (basic < 3250) ? 135 : (basic >= 24750) ? 1125 : 157.5 + ((int)((basic - 3250) / 500) * 22.5);
            double ph = (basic <= 10000) ? 150 : (basic >= 60000) ? 900 : (basic * 0.03) / 2;
            double pi = Math.min(100, (basic > 1500) ? 5000 * 0.02 : basic * 0.01);
            
            double taxable = basic - (sss + ph + pi);
            double tax = (taxable <= 20832) ? 0 : (taxable <= 33333) ? (taxable - 20833) * 0.20 : 2500 + (taxable - 33333) * 0.25;
            double totalDeductions = sss + ph + pi + tax;

            System.out.println("MONTHLY PERIOD: " + Month.of(m).name());
            
            // 1st Cutoff
            System.out.println("   1st Cutoff (1-15)");
            System.out.printf("   Hours Worked: %.2f\n", h1);
            System.out.printf("   Gross Payout: %,.2f\n", h1 * hourlyRate);
            
            // 2nd Cutoff 
            double gross2 = h2 * hourlyRate;
            System.out.printf("\n   2nd Cutoff (16-%d) \n", lastDay);
            System.out.printf("   Hours Worked        : %.2f\n", h2);
            System.out.printf("   Gross Earnings      : %,.2f\n", gross2);
            System.out.println("   --- Statutory Deductions ---");
            System.out.printf("   SSS Contribution    : %,.2f\n", sss);
            System.out.printf("   PhilHealth Premium  : %,.2f\n", ph);
            System.out.printf("   Pag-IBIG Fund       : %,.2f\n", pi);
            System.out.printf("   Withholding Tax     : %,.2f\n", tax);
            System.out.println("   ----------------------------");
            System.out.printf("   Total Deductions    :  %,.2f\n", totalDeductions);
            System.out.printf("   [NET PAYOUT]        :  %,.2f\n", gross2 - totalDeductions);
            System.out.println("--------------------------------------------------------------");
        }
    }

    /**
     * Reads Employee CSV using Regex to safely handle commas within addresses.
     */
    static String[] findEmployeeData(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMP_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                // Regex: Splits by comma ONLY if comma is not inside double quotes
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (data[0].trim().equals(id.trim())) return data;
            }
        } catch (Exception e) {}
        return null;
    }

    /**
     * Batch process that uses the cached attendance data for high performance.
     */
    static void processEntirePayroll(List<String[]> cachedAttendance) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMP_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String empId = line.split(",")[0];
                executePayrollCalculation(empId, cachedAttendance);
            }
        } catch (Exception e) {}
    }

    /**
     * Sanitizes string values for calculation.
     */
    static double cleanValue(String val) { 
        return Double.parseDouble(val.replaceAll("[\",]", "")); 
    }
}