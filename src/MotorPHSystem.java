import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MotorPHSystem {

    // --- GLOBAL SETTINGS & BUSINESS RULES ---
    static final String EMP_DB = "resource/MotorPH_Employee Database.csv";
    static final String ATT_DB = "resource/Attendance Record.csv";
    static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:mm:ss]");
    
    // Time-based constants for easier maintenance
    static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    static final LocalTime GRACE_PERIOD_END = LocalTime.of(8, 10);
    static final LocalTime SHIFT_END = LocalTime.of(17, 0);
    static final double LUNCH_BREAK_HOUR = 1.0;

    public static void main(String[] args) {
        startSystem();
    }

    /**
     * Entry point for the application. Handles user login and 
     * redirects to the appropriate dashboard.
     */
    static void startSystem() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n========================================");
            System.out.println("      MOTOR PH MANAGEMENT SYSTEM       ");
            System.out.println("========================================");
            
            // Collect and sanitize login credentials
            System.out.print("Username : "); String user = sc.nextLine().trim().toLowerCase();
            System.out.print("Password : "); String pass = sc.nextLine();

            if (!pass.equals("12345")) {
                System.out.println("[!] Access Denied: Invalid Password.");
                continue;
            }

            // Route user based on role
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
                System.out.println("[!] Invalid Username. Use 'payroll_staff' or 'employee'.");
            }
        }
    }

    /**
     * Provides administrative functions such as batch payroll generation.
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
                
                if (id.equalsIgnoreCase("ALL")) {
                    processEntirePayroll();
                } else {
                    if (findEmployeeData(id) != null) {
                        executePayrollCalculation(id);
                    } else {
                        System.out.println("[!] Invalid ID#, please try again.");
                    }
                }
            }
        }
    }

    /**
     * Displays personal data for the logged-in employee.
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
     * Scans the attendance database and calculates total hours for a date range.
     * Applies shift windowing, grace periods, and lunch break deductions.
     */
    static double computeHoursWorked(String id, int month, int year, int startDay, int endDay) {
        double totalHours = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(ATT_DB))) {
            String line; br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] row = line.split(",");
                String[] dParts = row[3].split("/");
                int m = Integer.parseInt(dParts[0]), d = Integer.parseInt(dParts[1]), y = Integer.parseInt(dParts[2]);

                // Filter for matching employee and date criteria
                if (row[0].equals(id) && m == month && y == year && d >= startDay && d <= endDay) {
                    LocalTime in = LocalTime.parse(row[4], TF);
                    LocalTime out = LocalTime.parse(row[5], TF);

                    // Standardize time within 8:00 AM - 5:00 PM shift
                    if (in.isBefore(SHIFT_START)) in = SHIFT_START;
                    if (out.isAfter(SHIFT_END)) out = SHIFT_END;
                    
                    // Apply 10-minute grace period at start of shift
                    if (!in.isAfter(GRACE_PERIOD_END)) in = SHIFT_START;

                    // Subtract 1 hour for lunch break
                    double daily = (Duration.between(in, out).toMinutes() / 60.0) - LUNCH_BREAK_HOUR;
                    totalHours += Math.max(0, daily);
                }
            }
        } catch (Exception e) {}
        return totalHours;
    }

    static void executePayrollCalculation(String id) {
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
            
            double h1 = computeHoursWorked(id, m, 2024, 1, 15);
            double h2 = computeHoursWorked(id, m, 2024, 16, lastDay);

            if (h1 + h2 == 0) continue;

            // Statutory Deduction Logic using Ternary Operators (Local Doubles)
            double sss = (basic < 3250) ? 135 : (basic >= 24750) ? 1125 : 157.5 + ((int)((basic - 3250) / 500) * 22.5);
            double ph = (basic <= 10000) ? 150 : (basic >= 60000) ? 900 : (basic * 0.03) / 2;
            double pi = Math.min(100, (basic > 1500) ? 5000 * 0.02 : basic * 0.01);
            
            // Taxable income and Withholding Tax calculation
            double taxable = basic - (sss + ph + pi);
            double tax = (taxable <= 20832) ? 0 : (taxable <= 33333) ? (taxable - 20833) * 0.20 : 2500 + (taxable - 33333) * 0.25;
            
            double totalDeductions = sss + ph + pi + tax;

            System.out.println("MONTHLY PERIOD: " + Month.of(m).name());
            
            // 1st Cutoff Display
            System.out.println("   1st Cutoff (1-15)");
            System.out.printf("   Hours Worked: %.2f\n", h1);
            System.out.printf("   Gross/Net   : %,.2f\n", h1 * hourlyRate);
            
            // 2nd Cutoff Display with Itemized Deductions
            double gross2 = h2 * hourlyRate;
            System.out.printf("\n   2nd Cutoff (16-%d) \n", lastDay);
            System.out.printf("   Hours Worked        : %.2f\n", h2);
            System.out.printf("   Gross Earnings      : %,.2f\n", gross2);
            System.out.println("   ------------------------------------");
            System.out.printf("   SSS Contribution    : %,.2f\n", sss);
            System.out.printf("   PhilHealth Premium  : %,.2f\n", ph);
            System.out.printf("   Pag-IBIG Fund       : %,.2f\n", pi);
            System.out.printf("   Withholding Tax     : %,.2f\n", tax);
            System.out.println("   ------------------------------------");
            System.out.printf("   Total Deductions    : %,.2f\n", totalDeductions);
            System.out.printf("   [FINAL NET PAYOUT]  : %,.2f\n", gross2 - totalDeductions);
            System.out.println("--------------------------------------------------------------");
        }
    }

    /**
     * Retrieves specific employee data from CSV using regex to handle complex fields.
     */
    static String[] findEmployeeData(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMP_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (data[0].equals(id.trim())) return data;
            }
        } catch (Exception e) {}
        return null;
    }

    /**
     * Loops through the entire database to generate a comprehensive company report.
     */
    static void processEntirePayroll() {
        // PERFORMANCE NOTE: For high-volume production, attendance data 
        // should be cached in memory to avoid redundant file I/O.
        try (BufferedReader br = new BufferedReader(new FileReader(EMP_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                executePayrollCalculation(line.split(",")[0]);
            }
        } catch (Exception e) {}
    }

    /**
     * Removes formatting characters (quotes, commas) from strings to allow parsing.
     */
    static double cleanValue(String val) { 
        return Double.parseDouble(val.replaceAll("[\",]", "")); 
    }
}
