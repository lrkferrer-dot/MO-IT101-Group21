import java.io.*;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * MotorPH Payroll System - Milestone 2
 * * LOGIC DETERMINATION:
 * 1. GROSS SALARY: (Total Worked Hours * Hourly Rate).
 * 2. TAXABLE INCOME: (Monthly Basic Salary) - (SSS + PhilHealth + Pag-IBIG).
 * 3. NET SALARY: (Gross Salary) - (Total Government Deductions + Withholding Tax).
 * 4. ATTENDANCE: 10-minute grace period (8:10 AM) and 1-hour lunch deduction.
 * 5. LOOPING: Returns to Login Screen upon logout.
 */
public class MotorPHSystem {

    private static final String EMPLOYEE_DB = "MotorPH_Employee Database.csv";
    private static final String ATTENDANCE_DB = "Attendance Record.csv";
    private static final String CSV_SPLIT = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:mm:ss]");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Loop back to login screen after logout
        while (true) {
            System.out.println("\n========================================");
            System.out.println("      MOTOR PH PAYROLL SYSTEM          ");
            System.out.println("========================================");
            System.out.print("Enter Username: ");
            String user = scanner.nextLine();
            System.out.print("Enter Password: ");
            String pass = scanner.nextLine();

            if (pass.equals("12345")) {
                if (user.equalsIgnoreCase("payroll_staff")) {
                    showAdminMenu(scanner);
                } else if (user.equalsIgnoreCase("employee")) {
                    showEmployeeMenu(scanner);
                } else {
                    System.out.println("[!] Role not recognized.");
                }
            } else {
                System.out.println("[!] Incorrect password. Please try again.");
            }
        }
    }

    // --- 1. EMPLOYEE DASHBOARD (PROFILE & LOGOUT ONLY) ---
    private static void showEmployeeMenu(Scanner scanner) {
        System.out.print("\nPlease enter your Employee # for verification: ");
        String empId = scanner.nextLine();
        if (findEmployee(empId) == null) return;

        boolean active = true;
        while (active) {
            System.out.println("\n--- EMPLOYEE DASHBOARD ---");
            System.out.println("1. View Personal Profile");
            System.out.println("2. Logout");
            System.out.print("Choice: ");
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                displayProfileOnly(empId);
            } else if (choice.equals("2")) {
                active = false; 
            }
        }
    }

    // --- 2. ADMIN DASHBOARD ---
    private static void showAdminMenu(Scanner scanner) {
        boolean active = true;
        while (active) {
            System.out.println("\n--- HR ADMIN DASHBOARD ---");
            System.out.println("1. Process Full Year Payroll");
            System.out.println("2. Logout");
            System.out.print("Choice: ");
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                System.out.print("Enter Employee # (or 'ALL'): ");
                String target = scanner.nextLine();
                if (target.equalsIgnoreCase("ALL")) {
                    processAllEmployees();
                } else {
                    processEmployeeAllMonths(target);
                }
            } else if (choice.equals("2")) {
                active = false;
            }
        }
    }

    private static void processEmployeeAllMonths(String empId) {
        String[] emp = findEmployee(empId);
        if (emp == null) {
            System.out.println("[!] ID not found.");
            return;
        }

        System.out.println("\n============================================================");
        System.out.println("PAYROLL REPORT FOR: " + emp[2].toUpperCase() + ", " + emp[1]);
        System.out.println("EMPLOYEE #: " + emp[0]);
        System.out.println("BIRTHDAY: " + emp[3]);
        System.out.println("============================================================");

        // Scan through all months automatically (June to December)
        for (int m = 6; m <= 12; m++) {
            calculateAndDisplayPayroll(emp, m);
        }
    }

    public static void calculateAndDisplayPayroll(String[] emp, int month) {
        String empId = emp[0];
        double basic = parseDouble(emp[13]);
        double rate = parseDouble(emp[18]);
        String monthName = java.time.Month.of(month).name();

        double hrs1 = getHours(empId, month, 1, 15);
        double hrs2 = getHours(empId, month, 16, 31);

        if (hrs1 == 0 && hrs2 == 0) return; // Skip months with no data

        System.out.println("\n>>> " + monthName + " 2024");
        
        // 1st Cutoff
        double gross1 = hrs1 * rate;
        System.out.printf("   1st Cutoff - %s 1 to %s 15:\n", monthName, monthName);
        System.out.printf("      Hours: %.2f | Gross: %.2f | Net Payout: %.2f\n", hrs1, gross1, gross1);

        // 2nd Cutoff (Including Net Salary Computation Logic)
        double gross2 = hrs2 * rate;
        double sss = calculateSSS(basic);
        double ph = calculatePH(basic);
        double pi = calculatePI(basic);
        
        // Logic: Net Salary Calculation Sequence
        double taxableIncome = basic - (sss + ph + pi);
        double tax = calculateTax(taxableIncome);
        double totalDeductions = sss + ph + pi + tax;
        double netSalary = gross2 - totalDeductions;

        System.out.printf("   2nd Cutoff - %s 16 to %s 31:\n", monthName, monthName);
        System.out.printf("      Hours: %.2f | Gross: %.2f\n", hrs2, gross2);
        System.out.printf("      Deductions: SSS:%.2f PH:%.2f PI:%.2f Tax:%.2f\n", sss, ph, pi, tax);
        System.out.printf("      Total Deductions: %.2f | Net Payout: %.2f\n", totalDeductions, netSalary);
    }

    /**
     * Logic: Attendance Calculation
     * 1. GRACE PERIOD: 08:10 AM threshold. Arriving <= 08:10 sets start to 08:00.
     * 2. LUNCH: 1.0 hour subtraction per daily entry.
     */
    public static double getHours(String id, int m, int start, int end) {
        double total = 0.0;
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_DB))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d[0].trim().equals(id.trim())) {
                    String[] dateParts = d[3].split("/");
                    int month = Integer.parseInt(dateParts[0]);
                    int day = Integer.parseInt(dateParts[1]);
                    
                    if (month == m && day >= start && day <= end) {
                        LocalTime logIn = LocalTime.parse(d[4], TIME_FORMAT);
                        LocalTime logOut = LocalTime.parse(d[5], TIME_FORMAT);
                        
                        if (!logIn.isAfter(LocalTime.of(8, 10))) {
                            logIn = LocalTime.of(8, 0); 
                        }
                        
                        double duration = (Duration.between(logIn, logOut).toMinutes() / 60.0) - 1.0;
                        total += Math.max(0, duration);
                    }
                }
            }
        } catch (Exception e) {}
        return total;
    }

    // --- CONTRIBUTION LOGICS ---

    private static double calculatePH(double salary) {
        if (salary <= 10000) return 150.00;
        if (salary >= 60000) return 900.00;
        return (salary * 0.03) / 2;
    }

    private static double calculatePI(double salary) {
        return Math.min(100.00, salary * (salary > 1500 ? 0.02 : 0.01));
    }

    private static double calculateSSS(double salary) {
        if (salary < 3250) return 135.00;
        if (salary >= 24750) return 1125.00;
        return 157.50 + ((int)((salary - 3250) / 500) * 22.50);
    }

    private static double calculateTax(double taxable) {
        if (taxable <= 20832) return 0;
        if (taxable <= 33333) return (taxable - 20833) * 0.20;
        if (taxable <= 66667) return 2500 + (taxable - 33333) * 0.25;
        if (taxable <= 166667) return 10833 + (taxable - 66667) * 0.30;
        return 40833 + (taxable - 166667) * 0.32;
    }

    // --- DATA UTILITIES ---

    public static String[] findEmployee(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_DB))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(CSV_SPLIT);
                if (d[0].trim().equals(id.trim())) return d;
            }
        } catch (Exception e) {}
        return null;
    }

    private static void displayProfileOnly(String id) {
        String[] d = findEmployee(id);
        System.out.println("\n--- PERSONAL PROFILE ---");
        System.out.println("ID: " + d[0] + " | Name: " + d[2] + " " + d[1]);
        System.out.println("Birthday: " + d[3]);
        System.out.println("Position: " + d[11] + " | Status: " + d[10]);
    }

    private static void processAllEmployees() {
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_DB))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                processEmployeeAllMonths(line.split(CSV_SPLIT)[0]);
            }
        } catch (Exception e) {}
    }

    private static double parseDouble(String val) {
        return Double.parseDouble(val.replace("\"", "").replace(",", ""));
    }
}