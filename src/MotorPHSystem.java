import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;


/**
 * MOTORPH PAYROLL SYSTEM
 * * Logic Highlights:
 * 1. WINDOWING: 08:00-17:00 cap (No unauthorized OT).
 * 2. LATE LOGIC: 10-min grace (08:01-08:10). Exact deduction starting 08:11.
 * 3. LUNCH: Mandatory 1-hour deduction per shift.
 * 4. SEMI-MONTHLY: 1st Cutoff (Gross), 2nd Cutoff (Gross & Net minus all Monthly Deductions).
 */
public class MotorPHSystem {

    // Database File Names
    static final String EMP_DB = "resource/MotorPH_Employee Database.csv";
    static final String ATT_DB = "resource/Attendance Record.csv";
    static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:mm:ss]");

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n========================================");
            System.out.println("      MOTOR PH MANAGEMENT SYSTEM       ");
            System.out.println("========================================");
            System.out.print("User ID / Username: "); String user = sc.nextLine();
            System.out.print("Password          : "); String pass = sc.nextLine();

            // Security Gate (Group Logic)            
            if (!pass.equals("12345")) {
                System.out.println("[!] Access Denied.");
                continue;
            }

            if (user.equalsIgnoreCase("payroll_staff")) {
                runAdminDashboard(sc);
            } else {
                String[] employeeRow = findEmployeeData(user);
                if (employeeRow != null) {
                    runEmployeeDashboard(employeeRow, sc);
                } else {
                    System.out.println("[!] ID not found.");
                }
            }
        }
    }

    /**
     * ADMIN DASHBOARD
     */
    static void runAdminDashboard(Scanner sc) {
        while (true) {
            System.out.println("\n--- [ADMIN] MASTER PAYROLL PORTAL ---");
            System.out.println("[1] Generate Payroll Report | [2] Logout");
            if (sc.nextLine().equals("2")) break;

            System.out.print("Enter Target ID (or 'ALL'): ");
            String id = sc.nextLine();
            
            if (id.equalsIgnoreCase("ALL")) {
                processEntirePayroll();
            } else {
                executePayrollCalculation(id);
            }
        }
    }
    
    /**
     * EMPLOYEE DASHBOARD
     */    
    static void runEmployeeDashboard(String[] data, Scanner sc) {
        while (true) {
            System.out.println("\n--- [EMPLOYEE] INFORMATION PORTAL ---");            
            System.out.println("[1] View Profile | [2] Logout");
            if (sc.nextLine().equals("2")) break;

            System.out.println("\n--- PERSONAL DETAILS ---");
            System.out.println("Name: " + data[2] + " " + data[1]);
            System.out.println("Employee ID: " + data[0]);
            System.out.println("Position   : " + data[11]);
            System.out.println("Birthday   : " + data[3]); // Now showing Birthday
        }
    }

/**
     * LOGIC: ATTENDANCE & LATE DEDUCTION
     * Formula: Total Hours = (Time Out - Time In) - 1.0 Lunch
     * Ruling: 08:00-08:10 (Grace), 08:11+ (Exact Deduction)
     */
    static double getPeriodHours(String id, int targetM, int targetY, int startD, int endD) {
        double accumulatedHours = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(ATT_DB))) {
            String line; br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] row = line.split(",");
                String[] date = row[3].split("/");
                int m = Integer.parseInt(date[0]), d = Integer.parseInt(date[1]), y = Integer.parseInt(date[2]);

                if (row[0].equals(id) && m == targetM && y == targetY && d >= startD && d <= endD) {
                    LocalTime in = LocalTime.parse(row[4], TF);
                    LocalTime out = LocalTime.parse(row[5], TF);
                    LocalTime shiftStart = LocalTime.of(8, 0), shiftEnd = LocalTime.of(17, 0);

                    // 1. Windowing: Cap to 8-5
                    if (in.isBefore(shiftStart)) in = shiftStart;
                    if (out.isAfter(shiftEnd)) out = shiftEnd;
                    
                    // 2. Grace Period vs Exact Late Deduction
                    // If arrived at 08:10:00 or earlier, set to 08:00:00
                    if (!in.isAfter(LocalTime.of(8, 10))) {
                        in = shiftStart;
                    } 
                    // If arrived at 08:11:00 or later, 'in' remains the actual time (exact deduction)
                    // 3. Calculation: Minutes to Hours minus 1hr Lunch
                    double dayTotal = (Duration.between(in, out).toMinutes() / 60.0) - 1.0;
                    accumulatedHours += Math.max(0, dayTotal);
                }
            }
        } catch (Exception e) {}
        return accumulatedHours;
    }

/**
     * LOGIC: PAYROLL PROCESSING
     * Gross = Hours * Rate | Net = Gross - Total Deductions
     */
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
            double h1 = getPeriodHours(id, m, 2024, 1, 15);
            double h2 = getPeriodHours(id, m, 2024, 16, lastDay);

            if (h1 + h2 == 0) continue;

            // Statutory Computations
            double sss = (basic < 3250) ? 135 : (basic >= 24750) ? 1125 : 157.5 + ((int)((basic-3250)/500)*22.5);
            double ph = (basic <= 10000) ? 150 : (basic >= 60000) ? 900 : (basic * 0.03) / 2;
            double pi = Math.min(100, (basic > 1500) ? 5000 * 0.02 : basic * 0.01);
            double taxable = basic - (sss + ph + pi);
            double tax = (taxable <= 20832) ? 0 : (taxable <= 33333) ? (taxable - 20833) * 0.20 : 2500 + (taxable - 33333) * 0.25;

            System.out.println("MONTHLY PERIOD: " + Month.of(m).name());
            
            // 1ST CUTOFF 
            System.out.println("   1st Cutoff (1-15)");
            System.out.printf("   Hours Worked: %.2f\n", h1);
            System.out.printf("   Gross/Net   : %,.2f\n", h1 * hourlyRate);
            
            // 2ND CUTOFF 
            double gross2 = h2 * hourlyRate;
            double totalDeductions = sss + ph + pi + tax;
            double netSalary = gross2 - totalDeductions;

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
            System.out.printf("   [FINAL NET PAYOUT]  : %,.2f\n", netSalary);
            System.out.println("--------------------------------------------------------------");
        }
    }
    
    /**
     * DATA PARSING HELPERS
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

    static void processEntirePayroll() {
        try (BufferedReader br = new BufferedReader(new FileReader(EMP_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                executePayrollCalculation(line.split(",")[0]);
            }
        } catch (Exception e) {}
    }

    static double cleanValue(String val) {
        return Double.parseDouble(val.replaceAll("[\",]", ""));
    }

}
