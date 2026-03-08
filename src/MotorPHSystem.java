import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * MOTORPH PAYROLL SYSTEM - MILESTONE 2
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
            System.out.print("Password: "); String pass = sc.nextLine();

            // Security Gate (Group Logic)
            if (!pass.equals("12345")) {
                System.out.println("[!] Access Denied: Incorrect Password.");
                continue;
            }

            if (user.equalsIgnoreCase("payroll_staff")) {
                runAdminDashboard(sc);
            } else {
                String[] emp = findEmployee(user);
                if (emp != null) {
                    runEmployeeDashboard(emp, sc);
                } else {
                    System.out.println("[!] Error: Employee ID " + user + " not found.");
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
            System.out.println("[1] Process Payroll Report");
            System.out.println("[2] Logout");
            System.out.print("Selection: ");
            String choice = sc.nextLine();

            if (choice.equals("2")) break;

            System.out.print("Enter Employee ID (or 'ALL'): ");
            String id = sc.nextLine();
            if (id.equalsIgnoreCase("ALL")) {
                processBulk();
            } else {
                generatePayrollReport(id);
            }
        }
    }

    /**
     * EMPLOYEE DASHBOARD
     */
    static void runEmployeeDashboard(String[] emp, Scanner sc) {
        while (true) {
            System.out.println("\n--- [EMPLOYEE] PERSONAL PORTAL ---");
            System.out.println("[1] View Profile Information | [2] Logout");
            System.out.print("Selection: ");
            if (sc.nextLine().equals("2")) break;

            System.out.println("\n--- PROFILE METADATA ---");
            System.out.println("Name: " + emp[1] + ", " + emp[2]);
            System.out.println("Employee ID: " + emp[0]);
            System.out.println("Position   : " + emp[11]);
            System.out.println("Birthday   : " + emp[3]);
        }
    }

    /**
     * EXACT LATE DEDUCTION ENGINE
     * Handles 8-5 window, 10-min grace, and 1-hour lunch.
     */
    /**
     * EXACT LATE DEDUCTION LOGIC
     * 08:00 - 08:10 = Grace Period (No deduction)
     * 08:11 - onward = Late (Exact minutes deducted from total hours)
     */
    static double calculateHours(String id, int m, int y, int start, int end) {
        double totalHours = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(ATT_DB))) {
            String line; br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                String[] dt = d[3].split("/");
                int recM = Integer.parseInt(dt[0]), recD = Integer.parseInt(dt[1]), recY = Integer.parseInt(dt[2]);

                // Filter for ID, Month, Year, and specific Cutoff Days
                if (d[0].equals(id) && recM == m && recY == y && recD >= start && recD <= end) {
                    LocalTime in = LocalTime.parse(d[4], TF);
                    LocalTime out = LocalTime.parse(d[5], TF);
                    LocalTime workStart = LocalTime.of(8, 0);
                    LocalTime workEnd = LocalTime.of(17, 0);
                    LocalTime graceLimit = LocalTime.of(8, 10);

                    // 1. Cap Early Arrival / Late Stay (8-5 Window)
                    if (in.isBefore(workStart)) in = workStart;
                    if (out.isAfter(workEnd)) out = workEnd;

                    // 2. Late Deduction Logic
                    // If arrived within 8:01-8:10, reset to 8:00 (Grace Period)
                    if (!in.isAfter(graceLimit)) {
                        in = workStart;
                    } 
                    // If arrived at 8:11 or later, 'in' remains the actual late time.

                    // 3. Calculate Exact Duration & Subtract 1-hour Lunch
                    long minutesWorked = Duration.between(in, out).toMinutes();
                    double dailyHours = (minutesWorked / 60.0) - 1.0; 

                    totalHours += Math.max(0, dailyHours);
                }
            }
        } catch (Exception e) {
            System.out.println("[!] Error processing attendance: " + e.getMessage());
        }
        return totalHours;
    }

    /**
     * NET SALARY LOGIC: Generates the 2-Cutoff report.
     */
    static void generatePayrollReport(String id) {
        String[] emp = findEmployee(id);
        if (emp == null) return;

        double basic = parse(emp[13]), hourlyRate = parse(emp[18]);
        int year = 2024;

        System.out.println("\n**************************************************************");
        System.out.println("PAYROLL REPORT: " + emp[1].toUpperCase() + ", " + emp[2]);
        System.out.println("ID #: " + emp[0] + " | BIRTHDAY: " + emp[3]);
        System.out.println("**************************************************************");

        for (int m = 6; m <= 12; m++) {
            int lastDay = YearMonth.of(year, m).lengthOfMonth();
            double h1 = calculateHours(id, m, year, 1, 15);
            double h2 = calculateHours(id, m, year, 16, lastDay);

            if (h1 + h2 == 0) continue;

            // Monthly Statutory Tiers (Philippine Law)
            double sss = (basic < 3250) ? 135 : (basic >= 24750) ? 1125 : 157.5 + ((int)((basic-3250)/500)*22.5);
            double ph = (basic <= 10000) ? 150 : (basic >= 60000) ? 900 : (basic * 0.03) / 2;
            double pi = Math.min(100, (basic > 1500) ? 5000 * 0.02 : basic * 0.01);
            double taxable = basic - (sss + ph + pi);
            double tax = (taxable <= 20832) ? 0 : (taxable <= 33333) ? (taxable - 20833) * 0.20 : 2500 + (taxable - 33333) * 0.25;

            System.out.println("PERIOD: " + Month.of(m).name() + " " + year + " (Rate:" + hourlyRate + ")");
            System.out.printf("   [1st Cutoff 01-15] Hrs: %.2f | Gross: %.2f | Net: %.2f\n", h1, h1*hourlyRate, h1*hourlyRate);
            System.out.printf("   [2nd Cutoff 16-%d] Hrs: %.2f | Gross: %.2f\n", lastDay, h2, h2*hourlyRate);
            System.out.printf("      DEDUCTIONS: SSS:%.2f PH:%.2f PI:%.2f TAX:%.2f\n", sss, ph, pi, tax);
            System.out.printf("      [NET SALARY PAYOUT]: %.2f\n", (h2*hourlyRate) - (sss+ph+pi+tax));
            System.out.println("--------------------------------------------------------------");
        }
    }

    /**
     * DATA PARSING HELPERS
     */
    static String[] findEmployee(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMP_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (d[0].equals(id.trim())) return d;
            }
        } catch (Exception e) {}
        return null;
    }

    static void processBulk() {
        try (BufferedReader br = new BufferedReader(new FileReader(EMP_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                generatePayrollReport(line.split(",")[0]);
            }
        } catch (Exception e) {}
    }

    static double parse(String v) { 
        return Double.parseDouble(v.replaceAll("[\",]", "")); 
    }
}