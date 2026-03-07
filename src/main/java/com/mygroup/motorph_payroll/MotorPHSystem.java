import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * MotorPH Payroll System - Milestone 2
 * Logic Implementation:
 * 1. WORK WINDOW: 08:00 to 17:00 strictly (No early/late overtime).
 * 2. GRACE PERIOD: 08:01-08:10 treated as 08:00. 08:11+ is LATE (Actual time used).
 * 3. LUNCH BREAK: Mandatory 1-hour deduction per day.
 * 4. NET SALARY LOGIC: 
 * - 1st Cutoff (1-15): Gross Payout.
 * - 2nd Cutoff (16-End): Gross Payout minus ALL Monthly Statutory Deductions.
 */
public class MotorPHSystem {

    static final String EMP_DB = "resource/MotorPH_Employee Database.csv";
    static final String ATT_DB = "resource/Attendance Record.csv";
    static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:mm:ss]");

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n========================================");
            System.out.println("      MOTOR PH MANAGEMENT SYSTEM       ");
            System.out.println("========================================");
            System.out.print("Username/ID: "); String user = sc.nextLine();
            System.out.print("Password   : "); String pass = sc.nextLine();

            // Universal testing password
            if (!pass.equals("12345")) {
                System.out.println("[!] Access Denied. Invalid Password.");
                continue;
            }

            if (user.equalsIgnoreCase("payroll_staff")) {
                runAdminDashboard(sc);
            } else {
                String[] emp = findEmployee(user);
                if (emp != null) {
                    runEmployeeDashboard(emp, sc);
                } else {
                    System.out.println("[!] ID not found in database.");
                }
            }
        }
    }

    /**
     * ADMIN DASHBOARD: Logic for bulk and individual payroll processing.
     */
    static void runAdminDashboard(Scanner sc) {
        while (true) {
            System.out.println("\n--- ADMIN: PAYROLL STAFF DASHBOARD ---");
            System.out.println("[1] Process Payroll Report");
            System.out.println("[2] Logout");
            System.out.print("Select: ");
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
     * EMPLOYEE DASHBOARD: View personal profile information.
     */
    static void runEmployeeDashboard(String[] emp, Scanner sc) {
        while (true) {
            System.out.println("\n--- EMPLOYEE DASHBOARD ---");
            System.out.println("Name: " + emp[1] + ", " + emp[2]);
            System.out.println("ID  : " + emp[0]);
            System.out.println("[1] View Full Profile | [2] Logout");
            System.out.print("Select: ");
            if (sc.nextLine().equals("2")) break;

            System.out.println("\n--- PERSONAL INFORMATION ---");
            System.out.println("Position: " + emp[11]);
            System.out.println("Birthday: " + emp[3]);
            System.out.println("Hourly Rate: P" + emp[18]);
        }
    }

    /**
     * ATTENDANCE LOGIC: Computes payable hours.
     * Incorporates: 8-5 Window, 10-min Grace Period, and 1-hour Lunch deduction.
     */
    static double calculateHours(String id, int month, int year, int start, int end) {
        double totalHours = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(ATT_DB))) {
            String line; br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                String[] dateParts = d[3].split("/");
                int m = Integer.parseInt(dateParts[0]), day = Integer.parseInt(dateParts[1]), y = Integer.parseInt(dateParts[2]);

                if (d[0].equals(id) && m == month && y == year && day >= start && day <= end) {
                    LocalTime in = LocalTime.parse(d[4], TF);
                    LocalTime out = LocalTime.parse(d[5], TF);
                    LocalTime workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0);

                    // 1. WINDOW LOGIC: Cap early/late arrival
                    if (in.isBefore(workStart)) in = workStart;
                    if (out.isAfter(workEnd)) out = workEnd;

                    // 2. LATE/GRACE LOGIC: 8:01-8:10 is grace, 8:11+ is actual late
                    if (!in.isAfter(LocalTime.of(8, 10))) in = workStart;

                    // 3. DURATION LOGIC: Subtract 1 hour for lunch
                    double duration = (Duration.between(in, out).toMinutes() / 60.0) - 1.0;
                    totalHours += Math.max(0, duration);
                }
            }
        } catch (Exception e) {}
        return totalHours;
    }

    /**
     * NET SALARY LOGIC: Generates the 2-Cutoff report for 2024.
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

            System.out.println("PERIOD: " + Month.of(m).name() + " " + year + " (Rate: P" + hourlyRate + ")");
            System.out.printf("   [1st Cutoff 01-15] Hrs: %.2f | Gross: %.2f | Net: %.2f\n", h1, h1*hourlyRate, h1*hourlyRate);
            System.out.printf("   [2nd Cutoff 16-%d] Hrs: %.2f | Gross: %.2f\n", lastDay, h2, h2*hourlyRate);
            System.out.printf("      DEDUCTIONS: SSS:%.2f PH:%.2f PI:%.2f TAX:%.2f\n", sss, ph, pi, tax);
            System.out.printf("      [NET SALARY PAYOUT]: P%.2f\n", (h2*hourlyRate) - (sss+ph+pi+tax));
            System.out.println("--------------------------------------------------------------");
        }
    }

    /**
     * HELPERS: CSV Parsing and Bulk Processing
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
            while ((line = br.readLine()) != null) generatePayrollReport(line.split(",")[0]);
        } catch (Exception e) {}
    }

    static double parse(String v) { return Double.parseDouble(v.replaceAll("[\",]", "")); }
}