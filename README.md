**TEAM DETAILS**

**Blue Aldridge Romulo** (Project Lead & Security): Responsible for the Authentication Gateway and the Dashboard Routing logic. Ensured the system distinguishes between payroll_staff and standard employees.

**Miguel Fabie** (Database & Data Parsing): Developed the Regex CSV Parser. Ensured that the program reads the employee and attendance files correctly, even when fields contain commas or quotes.

**Matilda Clarisse Tunguia** (Attendance Engine): Developed the Grace Period & 8-5 Windowing logic. Implemented the time-capping rules and the 10-minute arrival buffer to automate late deductions.

**Avie Kaille Pedrera** (Statutory Logic): Programmed the PH Statutory Tiers. Translated the 2024 SSS, PhilHealth, Pag-IBIG, and BIR Tax tables into functional Java code.

**Kelvine Ferrer** (Financial Reporter): Designed the 2-Cutoff Payout Logic and the Consolidated Payroll Report. Ensured that deductions are only taken from the second cutoff to maintain accurate net salary disbursements.

**PROJECT DETAILS**
**1. System Access & Authentication**
   
The program serves as a dual-purpose portal. Access is determined at the login gate:

**Administrative Access (payroll_staff / 12345):**

-Unlocks the Payroll Processing Engine.

-Allows for individual ID queries or Bulk Processing of the entire database.

-Generates multi-month reports including gross pay, statutory deductions, and net payouts.

**Employee Access (Employee ID / 12345):**

-Unlocks the Profile Viewer.

-Displays non-sensitive data: Full Name, Position, Birthday, and Hourly Rate.

-Prevents employees from viewing other staff records or payroll summaries.

**2. Core Logic: Attendance & Hourly Computation**
   
The system does not simply count the difference between "In" and "Out." it applies strict labor compliance rules to ensure accuracy:

**The 8-5 Work Window:**

-The system caps hours between 08:00 AM and 05:00 PM.

-Early Arrival: Logging in at 07:30 counts as 08:00.

-Late Stay: Logging out at 18:00 counts as 17:00.

**10-Minute Grace Period:**

-On-Time: 08:01 to 08:10 is automatically "rounded down" to 08:00.

-Late Arrival: 08:11 or later is treated as a late arrival; the actual time is used for the calculation, resulting in a decimal reduction of the day's pay.

**Mandatory Break Deduction:**

-Every shift automatically has 1.0 hour (60 minutes) subtracted to account for the unpaid lunch break, regardless of arrival time.

**3. Financial Logic: Net Salary & Statutory Deductions**
   
The payroll follows a Two-Cutoff Cycle (Semi-monthly), but statutory obligations are computed on a Monthly Basis and deducted in the second half of the month.

**A. Statutory Calculations (Philippine Standards)**

**-SSS:** Uses a tiered bracket. For every ₱500 increment in Basic Salary, the contribution increases. It is capped at a maximum of ₱1,125 for salaries ₱24,750 and above.

**-PhilHealth:** Calculated at 3% of the Monthly Basic Salary, then divided by 2 (Employee Share). It is capped at a maximum of ₱900.

**-Pag-IBIG:** Calculated as 2% of the salary, but strictly capped at a maximum of ₱100 per month.

**-Withholding Tax:** Calculated on Taxable Income (Basic Salary minus SSS, PhilHealth, and Pag-IBIG).

   **Exempt:** ₱20,832 and below.

   **Bracket 1 (20%):** ₱20,833 to ₱33,333.

   **Bracket 2 (25%):** ₱33,334 and above (Base tax of ₱2,500 + 25% of excess).

**B. The Payout Flow**

**-1st Cutoff (Days 1–15):** The system pays the Gross Amount (Hours x Hourly Rate). No government deductions are taken here to ensure the employee has maximum liquidity mid-month.

**-2nd Cutoff (Days 16–End):** The system calculates the gross pay for the remaining days. It then subtracts the entire month's SSS, PhilHealth, Pag-IBIG, and Tax from this specific payout.

**-Final Result:** The "Net Payout" displayed for the second cutoff is the take-home pay after all legal obligations are settled.

**PROJECT PLAN:**
https://docs.google.com/spreadsheets/d/1dX1KMGB9wlETWnv0iz9NB4PCXuAkhBlLnwz2LDf5m4M/edit?usp=sharing
