MotorPH Payroll Management System (Milestone 2)
A Java-based console application designed to automate payroll processing for MotorPH. This system integrates employee data and attendance records to calculate accurate gross and net wages while strictly adhering to Philippine statutory requirements and company-specific attendance policies.

🛠 Features
1. Employee Dashboard
Details Presentation: Displays comprehensive employee profiles including ID, Full Name, Birthday, Position, and Status.

Profile Verification: Ensures secure access to personal records via Employee ID validation.

2. HR Admin Dashboard
Bulk Processing: Automatically scans and calculates payroll for all employees in the database.

Individual Reports: Generates a month-by-month breakdown (June to December) for specific employees.

3. Payroll Logic Determination
The system implements complex business rules to ensure accuracy:

Hours Worked Calculation:

Grace Period: Logins between 8:00 AM and 8:10 AM are rounded back to 8:00 AM.

Late Logic: Logins at 8:11 AM or later are treated as late; the actual time is used for calculation, effectively deducting the late minutes from the gross pay.

Lunch Break: An automatic 1-hour deduction is applied to every workday.

Statutory Deductions (Government Tables):

SSS: Calculated based on the 2023-2024 contribution brackets.

PhilHealth: 3% premium rate shared equally (1.5% employee share).

Pag-IBIG: 1% or 2% contribution based on salary, capped at a ₱5,000 monthly compensation base.

Withholding Tax: Calculated using the Bureau of Internal Revenue (BIR) monthly tax table on taxable income.

📂 Project Structure
MotorPHSystem.java: The main executable class containing the UI and business logic.

MotorPH_Employee Database.csv: Contains employee master data (Basic Salary, Hourly Rate, etc.).

Attendance Record.csv: Contains clock-in/clock-out logs for all employees.

🚀 Getting Started
Prerequisites
Java Development Kit (JDK) 8 or higher.

A Java IDE (e.g., NetBeans, IntelliJ, or Eclipse).

Installation
Clone the repository:

Bash
git clone https://github.com/lrkferrer-dot/MotorPH-Payroll-System.git

Ensure the .csv database files are placed in the root directory of the project.

Compile and run MotorPHSystem.java.

Login Credentials (Default)
Admin: payroll_staff / 12345

Employee: employee / 12345
