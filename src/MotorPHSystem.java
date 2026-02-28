import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
    * MotorPH Payroll System - Milestone 2
    * WINDOWED HOURS: Only counts time between 08:00 and 17:00.
    * GRACE PERIOD: 08:01 - 08:10 is NOT late (8 hours total).
    * LATE LOGIC: 08:11 onwards uses actual time (e.g., 08:30 to 17:00 = 7.5 hrs).
    * DEDUCTIONS: SSS, PhilHealth, Pag-IBIG, and Tax applied on 2nd cutoff.
 */
public class MotorPHSystem {

    static String EMP_DB = "MotorPH_Employee Database.csv";
    static String ATT_DB = "Attendance Record.csv";
    static DateTimeFormatter TF = DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:mm:ss]");

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n========================================");
            System.out.println("      MOTOR PH PAYROLL SYSTEM          ");
            System.out.println("========================================");
            System.out.print("Username: "); String user = sc.nextLine();
            System.out.print("Password: "); String pass = sc.nextLine();

            if (!pass.equals("12345")) {
                System.out.println("[!] Access Denied.");
                continue;
            }

            if (user.equalsIgnoreCase("payroll_staff")) {
                runAdminMenu(sc);
            } else {
                runEmployeeMenu(sc);
            }
        }
    }

    static void runEmployeeMenu(Scanner sc) {
        System.out.print("Enter Employee #: ");
        String id = sc.nextLine();
        String[] emp = findEmployee(id);
        if (emp == null) return;

        String fullName = emp[1] + ", " + emp[2];

        while (true) {
            System.out.println("\n[1] View Profile | [2] Logout");
            if (sc.nextLine().equals("2")) break;

            System.out.println("\n--- PERSONAL DETAILS ---");
            System.out.println("Employee #: " + emp[0]);
            System.out.println("Full Name : " + fullName);
            System.out.println("Birthday  : " + emp[3]);
            System.out.println("Position  : " + emp[11]);
        }
    }

    static void runAdminMenu(Scanner sc) {
        while (true) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("[1] Process Payroll | [2] Logout");
            String choice = sc.nextLine();
            if (choice.equals("2")) break;

            System.out.print("Enter ID (or 'ALL'): ");
            String id = sc.nextLine();
            if (id.equalsIgnoreCase("ALL")) {
                processBulk();
            } else {
                processPayrollReport(id);
            }
        }
    }

    static double getWindowedHours(String id, int month, int year, int start, int end) {
        double total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(ATT_DB))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                String[] dt = d[3].split("/");
                int m = Integer.parseInt(dt[0]);
                int day = Integer.parseInt(dt[1]);
                int y = Integer.parseInt(dt[2]);

                if (d[0].equals(id) && m == month && y == year && day >= start && day <= end) {
                    LocalTime in = LocalTime.parse(d[4], TF);
                    LocalTime out = LocalTime.parse(d[5], TF);

                    LocalTime workStart = LocalTime.of(8, 0);
                    LocalTime workEnd = LocalTime.of(17, 0);

                    if (in.isBefore(workStart)) in = workStart;
                    if (out.isAfter(workEnd)) out = workEnd;

                    if (!in.isAfter(LocalTime.of(8, 10))) in = workStart;

                    double duration = (Duration.between(in, out).toMinutes() / 60.0) - 1.0; 
                    total += Math.max(0, duration);
                }
            }
        } catch (Exception e) {}
        return total;
    }

    static void processPayrollReport(String id) {
        String[] emp = findEmployee(id);
        if (emp == null) { System.out.println("ID not found."); return; }

        String fullName = emp[1] + ", " + emp[2];
        double basic = parse(emp[13]);
        double hourlyRate = parse(emp[18]); 
        int payrollYear = 2024; 

        System.out.println("\n==============================================================");
        System.out.println("PAYROLL FOR: " + fullName.toUpperCase());
        System.out.println("ID: " + emp[0] + " | BDAY: " + emp[3]);
        System.out.println("==============================================================");

        for (int m = 6; m <= 12; m++) {
            String mName = Month.of(m).name();
            // Get last day of the specific month (e.g., 30 for June, 31 for July)
            int lastDay = YearMonth.of(payrollYear, m).lengthOfMonth();
            
            double h1 = getWindowedHours(id, m, payrollYear, 1, 15);
            double h2 = getWindowedHours(id, m, payrollYear, 16, lastDay);

            if (h1 + h2 == 0) continue;

            double g1 = h1 * hourlyRate;
            double g2 = h2 * hourlyRate;

            // Statutory Calculations
            double sss = (basic < 3250) ? 135 : (basic >= 24750) ? 1125 : 157.5 + ((int)((basic-3250)/500)*22.5);
            double ph = (basic <= 10000) ? 150 : (basic >= 60000) ? 900 : (basic * 0.03) / 2;
            double pi = Math.min(100, (basic > 1500) ? 5000 * 0.02 : basic * 0.01);
            double taxable = basic - (sss + ph + pi);
            double tax = (taxable <= 20832) ? 0 : (taxable <= 33333) ? (taxable - 20833) * 0.20 : 2500 + (taxable - 33333) * 0.25;
            double totalDeduc = sss + ph + pi + tax;

            System.out.println("PERIOD: " + mName + " " + payrollYear);
            System.out.println("HOURLY RATE: P" + hourlyRate);
            System.out.printf("   Cut-off [%s 1-15]:   Hrs: %.2f | Gross: %.2f | Net: %.2f\n", mName, h1, g1, g1);
            System.out.printf("   Cut-off [%s 16-%d]:  Hrs: %.2f | Gross: %.2f\n", mName, lastDay, h2, g2);
            System.out.printf("      [DEDUCTIONS] SSS:%.2f PH:%.2f PI:%.2f Tax:%.2f\n", sss, ph, pi, tax);
            System.out.printf("      [MONTHLY NET PAYOUT]: P%.2f\n", (g2 - totalDeduc));
            System.out.println("--------------------------------------------------------------");
        }
    }

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
                processPayrollReport(line.split(",")[0]);
            }
        } catch (Exception e) {}
    }

    static double parse(String v) { 
        return Double.parseDouble(v.replaceAll("[\",]", "")); 
    }
}
