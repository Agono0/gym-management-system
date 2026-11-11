package com.gym;

import com.gym.dao.*;

import com.gym.exception.*;
import com.gym.exception.ValidationException;
import com.gym.model.Administrator;
import com.gym.model.Attendance;
import com.gym.model.AvailabilityStatus;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.Equipment;
import com.gym.model.EquipmentCondition;
import com.gym.model.EquipmentStatus;
import com.gym.model.MedicalProfile;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Payment;
import com.gym.model.PaymentMethod;
import com.gym.model.PlanType;
import com.gym.model.ProgressRecord;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.StaffRole;
import com.gym.model.SubscriptionStatus;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;
import com.gym.model.TrainerSubscription;
import com.gym.model.WorkoutProgram;
import com.gym.util.DatabaseConnection;
import com.gym.util.SerializationUtil;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// ===== Main (entry point, was GymCliApp) =====

// Terminal UI entry point for Gym-OS: initializes the database, then shows login + dashboard
public class Main {

    private final Scanner scanner;
    private final CliInput input;
    private final CliSession session;
    private final GymOperations ops;

    public Main() {
        this.scanner = new Scanner(System.in);
        this.input = new CliInput(scanner);
        this.session = new CliSession();
        this.ops = new GymOperations();
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    public void start() {
        try {
            DatabaseConnection.initializeDatabase();
            SampleDataSeeder.seedIfEmpty();
        } catch (ValidationException e) {
            CliTheme.printValidationError("Startup seed failed: " + e.getMessage());
            return;
        } catch (DatabaseException e) {
            CliTheme.printDatabaseError("Database initialization failed: " + e.getMessage());
            return;
        }

        // Show the real database file location so data is never "lost" in another folder
        System.out.println("Using database file: " + new File("gym.db").getAbsolutePath());

        while (!session.isExitRequested()) {
            Administrator loggedIn = showLoginGate();
            if (loggedIn == null || session.isExitRequested()) {
                break;
            }
            session.setCurrentUser(loggedIn);
            session.clearTransientFlags();
            runMainDashboardLoop();
        }

        System.out.println("\nGoodbye! Closed Gym-OS Terminal Workflow Console cleanly.");
    }

    private Administrator showLoginGate() {
        while (!session.isExitRequested()) {
            CliTheme.printHeader("GYM-OS TERMINAL WORKFLOW CONSOLE",
                    "Not signed in", DatabaseConnection.getDbUrl(),
                    "Login Gate");

            System.out.println("  Seeded Accounts (admin / manager active, oldstaff inactive):");
            System.out.println("    1) admin    / admin123   [ADMIN]   (active = true)");
            System.out.println("    2) manager  / mgr123     [MANAGER] (active = true)");
            System.out.println("    3) staff    / staff123   [STAFF]   (active = true)");
            System.out.println("    4) oldstaff / old123     [STAFF]   (active = false -> login blocked!)");
            CliTheme.printSingleLine();
            System.out.println("  [1] Sign In with Username & Password");
            System.out.println("  [2] Quick Login as ADMIN   (admin)");
            System.out.println("  [3] Quick Login as MANAGER (manager)");
            System.out.println("  [4] Quick Login as STAFF   (staff)");
            System.out.println("  [0] Exit Application");
            CliTheme.printSingleLine();

            String choice = input.readLine("Select option > ");
            if (choice == null || "0".equals(choice)) {
                session.setExitRequested(true);
                return null;
            }

            try {
                if ("1".equals(choice)) {
                    String username = input.readRawString("Username: ");
                    String password = input.readRawString("Password: ");
                    Administrator user = ops.authenticate(username, password);
                    CliTheme.printSuccess("Welcome back, " + user.getFullName() + " [" + user.getRole() + "]!");
                    return user;
                } else if ("2".equals(choice)) {
                    return ops.authenticate("admin", "admin123");
                } else if ("3".equals(choice)) {
                    return ops.authenticate("manager", "mgr123");
                } else if ("4".equals(choice)) {
                    return ops.authenticate("staff", "staff123");
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
        return null;
    }

    private void runMainDashboardLoop() {
        MemberCliMenu memberMenu = new MemberCliMenu(input, session);
        TrainerCliMenu trainerMenu = new TrainerCliMenu(input, session);
        MembershipBillingCliMenu billingMenu = new MembershipBillingCliMenu(input, session);
        ScheduleWorkoutCliMenu scheduleMenu = new ScheduleWorkoutCliMenu(input, session);
        FacilityAdminCliMenu facilityMenu = new FacilityAdminCliMenu(input, session);
        ValidationSuiteRunner validationRunner = new ValidationSuiteRunner(input, session);

        while (!session.isLogoutRequested() && !session.isExitRequested()) {
            session.setReturnHome(false);
            CliTheme.printHeader("GYM-OS MAIN DASHBOARD",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu");

            printLiveKpiBanner();
            CliTheme.printSingleLine();
            System.out.println("  [1] Members, 360 Profiles, Medical & Progress");
            System.out.println("  [2] Coaches, Roster, Availability Slots & Subscriptions");
            System.out.println("  [3] Memberships & Plans (Sell / Freeze / Unfreeze / Renew / Cancel)");
            System.out.println("  [4] Training Schedule & Sessions (Book / Request / Reserve / Transitions)");
            System.out.println("  [5] Workout Programs & Live Workout HUD Simulator");
            System.out.println("  [6] Attendance Fast Check-In / Check-Out & Daily Report");
            System.out.println("  [7] Payments, Void/Replace Ledger & Outstanding Balance");
            System.out.println("  [8] Equipment Inventory & ASCII Facility Floor Map");
            System.out.println("  [9] Staff & Admin Controls [ADMIN Only]");
            System.out.println(" [10] Serialization (.ser Backup/Load) & Database Tools");
            System.out.println("  [V] Validation & Exception Test Suite");
            CliTheme.printSingleLine();
            System.out.println("  [L] Logout / Switch User    [0] Exit Application");
            CliTheme.printSingleLine();

            String choice = input.readLine("Select option > ");
            if (choice == null || "0".equals(choice)) {
                session.setExitRequested(true);
                return;
            }
            if ("L".equalsIgnoreCase(choice)) {
                session.setLogoutRequested(true);
                return;
            }

            // Safety net: no unexpected runtime error should ever kill the whole app
            try {
                if ("1".equals(choice)) {
                    memberMenu.openMenu();
                } else if ("2".equals(choice)) {
                    trainerMenu.openMenu();
                } else if ("3".equals(choice)) {
                    billingMenu.openMembershipsMenu();
                } else if ("4".equals(choice)) {
                    scheduleMenu.openScheduleMenu();
                } else if ("5".equals(choice)) {
                    scheduleMenu.openWorkoutsMenu();
                } else if ("6".equals(choice)) {
                    facilityMenu.openAttendanceMenu();
                } else if ("7".equals(choice)) {
                    billingMenu.openPaymentsMenu();
                } else if ("8".equals(choice)) {
                    facilityMenu.openEquipmentMenu();
                } else if ("9".equals(choice)) {
                    facilityMenu.openStaffAdminMenu();
                } else if ("10".equals(choice)) {
                    facilityMenu.openBackupAndDbMenu();
                } else if ("V".equalsIgnoreCase(choice)) {
                    validationRunner.openMenu();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (Exception e) {
                System.out.println("[!] Unexpected error: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                System.out.println("    The app recovered - you are back on the dashboard.");
                input.pause();
            }
        }
    }

    private void printLiveKpiBanner() {
        try {
            List<Customer> customers = new CustomerDao().findAll();
            int activeCustomers = 0;
            for (int i = 0; i < customers.size(); i++) {
                if (customers.get(i).getStatus() == CustomerStatus.ACTIVE) {
                    activeCustomers++;
                }
            }
            int coaches = new TrainerDao().findAll().size();

            String today = LocalDate.now().toString();
            List<Membership> plans = new MembershipDao().findAll();
            int activePlans = 0;
            for (int i = 0; i < plans.size(); i++) {
                if (ops.effectiveMembershipStatus(plans.get(i), today) == MembershipStatus.ACTIVE) {
                    activePlans++;
                }
            }

            List<Attendance> checkIns = new AttendanceDao().findAll();
            int todayCheckIns = 0;
            for (int i = 0; i < checkIns.size(); i++) {
                if (today.equals(checkIns.get(i).getDate())) {
                    todayCheckIns++;
                }
            }

            int sessionsCount = new SessionDao().findAll().size();

            List<Payment> payments = new PaymentDao().findAll();
            BigDecimal netRevenue = BigDecimal.ZERO;
            for (int i = 0; i < payments.size(); i++) {
                Payment p = payments.get(i);
                if (!p.isVoided() && p.getAmount() != null) {
                    netRevenue = netRevenue.add(p.getAmount());
                }
            }

            List<Equipment> gear = new EquipmentDao().findAll();
            int gearMaint = 0;
            for (int i = 0; i < gear.size(); i++) {
                if (gear.get(i).getStatus() == EquipmentStatus.IN_MAINTENANCE) {
                    gearMaint++;
                }
            }

            System.out.println("  LIVE KPIs: Members " + customers.size() + " (" + activeCustomers + " Active) | "
                    + "Coaches: " + coaches + " | Active Plans: " + activePlans);
            System.out.println("             Today Check-Ins: " + todayCheckIns + " | Sessions: " + sessionsCount
                    + " | Revenue: $" + netRevenue.toPlainString() + " | Gear Maint: " + gearMaint + "/" + gear.size());
        } catch (DatabaseException e) {
            System.out.println("  LIVE KPIs: [Unable to read KPI counters: " + e.getMessage() + "]");
        }
    }
}

// ===== CliTheme =====

// Pure ASCII formatting helpers for Windows CMD terminal screens
class CliTheme {

    public static final int LINE_WIDTH = 80;

    private CliTheme() {
    }

    public static void printDoubleLine() {
        System.out.println("+" + repeat('=', LINE_WIDTH - 2) + "+");
    }

    public static void printSingleLine() {
        System.out.println("+" + repeat('-', LINE_WIDTH - 2) + "+");
    }

    public static void printHeader(String title, String userLabel, String dbUrl, String breadcrumb) {
        System.out.println();
        printDoubleLine();
        String left = " " + safeText(title);
        String right = safeText(userLabel) + " | " + shortDbName(dbUrl) + " ";
        int gap = (LINE_WIDTH - 2) - left.length() - right.length();
        if (gap < 1) {
            String combined = truncate(left + " | " + right, LINE_WIDTH - 2);
            System.out.println("|" + padRight(combined, LINE_WIDTH - 2) + "|");
        } else {
            System.out.println("|" + left + repeat(' ', gap) + right + "|");
        }
        String pathLine = " Path: " + safeText(breadcrumb);
        System.out.println("|" + padRight(truncate(pathLine, LINE_WIDTH - 2), LINE_WIDTH - 2) + "|");
        printDoubleLine();
    }

    public static void printSectionTitle(String title) {
        System.out.println();
        System.out.println("--- " + safeText(title) + " ---");
    }

    public static void printFooterNav(boolean showHome) {
        printSingleLine();
        if (showHome) {
            System.out.println("  [0] Back    [H] Main Menu    [L] Logout/Switch User");
        } else {
            System.out.println("  [0] Exit");
        }
        printSingleLine();
    }

    public static void printSuccess(String message) {
        System.out.println();
        System.out.println("[OK] " + safeText(message));
    }

    public static void printValidationError(String message) {
        System.out.println();
        System.out.println("[! VALIDATION ERROR] " + safeText(message));
    }

    public static void printDatabaseError(String message) {
        System.out.println();
        System.out.println("[! DATABASE ERROR] " + safeText(message));
    }

    public static void printNotice(String message) {
        System.out.println("[INFO] " + safeText(message));
    }

    public static void printTable(String[] headers, List<String[]> rows, int[] widths) {
        String divider = buildTableDivider(widths);
        System.out.println(divider);
        System.out.println(formatRow(headers, widths));
        System.out.println(divider);
        if (rows == null || rows.isEmpty()) {
            System.out.println("| (No records found)");
            System.out.println(divider);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            System.out.println(formatRow(rows.get(i), widths));
        }
        System.out.println(divider);
    }

    private static String buildTableDivider(int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat('-', widths[i] + 2));
            sb.append("+");
        }
        return sb.toString();
    }

    private static String formatRow(String[] cols, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = i < cols.length ? safeText(cols[i]) : "";
            sb.append(" ");
            sb.append(padRight(truncate(cell, widths[i]), widths[i]));
            sb.append(" |");
        }
        return sb.toString();
    }

    public static String padRight(String text, int width) {
        String value = safeText(text);
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        return value + repeat(' ', width - value.length());
    }

    public static String truncate(String text, int maxLen) {
        String value = safeText(text);
        if (maxLen <= 3 || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen - 3) + "...";
    }

    public static String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    public static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String shortDbName(String dbUrl) {
        if (dbUrl == null || dbUrl.isEmpty()) {
            return "gym.db";
        }
        int prefix = dbUrl.lastIndexOf(':');
        if (prefix >= 0 && prefix < dbUrl.length() - 1) {
            return dbUrl.substring(prefix + 1);
        }
        return dbUrl;
    }
}

// ===== CliInput =====

// Safe console input helpers using java.util.Scanner
class CliInput {

    private final Scanner scanner;

    public CliInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }

    public String readRequiredString(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                return null;
            }
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("[!] Input cannot be empty (or enter a valid value).");
        }
    }

    // Allows submitting an empty line directly when deliberately testing validation errors
    public String readRawString(String prompt) {
        return readLine(prompt);
    }

    public String readOptionalString(String prompt, String defaultValue) {
        String suffix = defaultValue != null && !defaultValue.isEmpty() ? " [" + defaultValue + "]" : "";
        String value = readLine(prompt + suffix + ": ");
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public Integer readInt(String prompt, int minValue) {
        while (true) {
            String raw = readLine(prompt);
            if (raw == null) {
                return null;
            }
            try {
                int parsed = Integer.parseInt(raw);
                if (parsed < minValue) {
                    System.out.println("[!] Please enter a number >= " + minValue + ".");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid integer. Please enter digits only.");
            }
        }
    }

    public int readIntOrDefault(String prompt, int defaultValue) {
        while (true) {
            String raw = readLine(prompt + " [" + defaultValue + "]: ");
            if (raw == null || raw.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid integer. Try again or press Enter for " + defaultValue + ".");
            }
        }
    }

    public Double readDouble(String prompt, Double defaultValue) {
        while (true) {
            String suffix = defaultValue != null ? " [" + defaultValue + "]" : "";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A value is required. Example: 72.5");
                continue;
            }
            try {
                double value = Double.parseDouble(raw);
                // Java also accepts the words "NaN" and "Infinity" as numbers, so block them here
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    System.out.println("[!] Please enter a real number. Example: 72.5");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid decimal number. Example: 72.5");
            }
        }
    }

    public Double readOptionalDouble(String prompt, Double currentValue) {
        String suffix = currentValue != null ? " [" + currentValue + "]" : " [optional, Enter to skip]";
        while (true) {
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null || raw.isEmpty()) {
                return currentValue;
            }
            if ("-".equals(raw)) {
                return null;
            }
            try {
                double value = Double.parseDouble(raw);
                // Java also accepts the words "NaN" and "Infinity" as numbers, so block them here
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    System.out.println("[!] Please enter a real number. Example: 68.0 (or '-' to clear).");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid decimal number. Example: 68.0 (or '-' to clear).");
            }
        }
    }

    public BigDecimal readBigDecimal(String prompt, BigDecimal defaultValue) {
        while (true) {
            String suffix = defaultValue != null ? " [" + defaultValue.toPlainString() + "]" : "";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A value is required. Example: 45.00");
                continue;
            }
            try {
                return new BigDecimal(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid money amount. Example: 45.00");
            }
        }
    }

    public String readIsoDate(String prompt, String defaultDate) {
        while (true) {
            String suffix = defaultDate != null ? " [" + defaultDate + "]" : " (yyyy-MM-dd)";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultDate != null) {
                return defaultDate;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A date is required. Expected yyyy-MM-dd (e.g. 2026-11-09).");
                continue;
            }
            try {
                LocalDate.parse(raw);
                return raw;
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid date format. Expected yyyy-MM-dd (e.g. 2026-11-09).");
            }
        }
    }

    public String readIsoTime(String prompt, String defaultTime) {
        while (true) {
            String suffix = defaultTime != null ? " [" + defaultTime + "]" : " (HH:mm)";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultTime != null) {
                return defaultTime;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A time is required. Expected HH:mm (e.g. 14:30).");
                continue;
            }
            try {
                LocalTime.parse(raw);
                return raw;
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid time format. Expected HH:mm (e.g. 14:30).");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String raw = readLine(prompt + " (y/n): ");
            if (raw == null) {
                return false;
            }
            if ("y".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw)) {
                return true;
            }
            if ("n".equalsIgnoreCase(raw) || "no".equalsIgnoreCase(raw)) {
                return false;
            }
            System.out.println("[!] Enter y or n.");
        }
    }

    public void pause() {
        System.out.print("\nPress [ENTER] to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}

// ===== CliSession =====

// Holds the active signed-in staff user and simple terminal navigation flags
class CliSession {

    private Administrator currentUser;
    private boolean returnHome;
    private boolean logoutRequested;
    private boolean exitRequested;

    public CliSession() {
    }

    public Administrator getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Administrator currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isReturnHome() {
        return returnHome;
    }

    public void setReturnHome(boolean returnHome) {
        this.returnHome = returnHome;
    }

    public boolean isLogoutRequested() {
        return logoutRequested;
    }

    public void setLogoutRequested(boolean logoutRequested) {
        this.logoutRequested = logoutRequested;
    }

    public boolean isExitRequested() {
        return exitRequested;
    }

    public void setExitRequested(boolean exitRequested) {
        this.exitRequested = exitRequested;
    }

    public void clearTransientFlags() {
        this.returnHome = false;
        this.logoutRequested = false;
    }

    public String userLabel() {
        if (currentUser == null) {
            return "Guest";
        }
        return currentUser.getUsername() + " [" + currentUser.getRole() + "]";
    }

    public boolean shouldStopSubMenu() {
        return returnHome || logoutRequested || exitRequested;
    }
}

// ===== GymOperations =====

// Coordinates cross-entity operations using the 13 concrete DAOs
class GymOperations {

    private final AdministratorDao adminDao = new AdministratorDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final TrainerDao trainerDao = new TrainerDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final WorkoutProgramDao workoutDao = new WorkoutProgramDao();
    private final TrainerAvailabilityDao availabilityDao = new TrainerAvailabilityDao();
    private final TrainerSubscriptionDao subscriptionDao = new TrainerSubscriptionDao();
    private final SessionDao sessionDao = new SessionDao();
    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final ProgressRecordDao progressDao = new ProgressRecordDao();
    private final MedicalProfileDao medicalDao = new MedicalProfileDao();

    // Authenticate user and reject inactive accounts
    public Administrator authenticate(String username, String password) throws DatabaseException, ValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required");
        }
        Administrator admin = adminDao.findByUsername(username.trim());
        if (admin == null || admin.getPasswordHash() == null || !admin.getPasswordHash().equals(password)) {
            throw new ValidationException("Invalid username or password");
        }
        if (!admin.isActive()) {
            throw new ValidationException("Account '" + admin.getUsername() + "' is inactive and cannot log in");
        }
        return admin;
    }

    // Enforce ADMIN role for restricted actions
    public void requireAdmin(Administrator actor) throws ValidationException {
        if (actor == null || actor.getRole() != StaffRole.ADMIN) {
            String current = actor != null && actor.getRole() != null ? actor.getRole().name() : "NONE";
            throw new ValidationException("ADMIN role required for this action (current role: " + current + ")");
        }
    }

    // Soft delete customer sets INACTIVE (references are never touched)
    public void softDeleteCustomer(int customerId) throws DatabaseException, ValidationException {
        Customer c = customerDao.findById(customerId);
        if (c == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        c.setStatus(CustomerStatus.INACTIVE);
        customerDao.update(c);
    }

    // Admin-only hard delete refuses while child references exist
    public void hardDeleteCustomer(Administrator actor, int customerId) throws DatabaseException, ValidationException {
        requireAdmin(actor);
        Customer c = customerDao.findById(customerId);
        if (c == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        int refs = countReferences("memberships", "customer_id", customerId)
                + countReferences("sessions", "customer_id", customerId)
                + countReferences("payments", "customer_id", customerId)
                + countReferences("attendance", "customer_id", customerId)
                + countReferences("trainer_subscriptions", "customer_id", customerId)
                + countReferences("workout_programs", "customer_id", customerId)
                + countReferences("progress_records", "customer_id", customerId)
                + countReferences("medical_profiles", "customer_id", customerId);
        if (refs > 0) {
            throw new ValidationException("Hard delete refused - customer #" + customerId
                    + " still has " + refs + " referenced record(s). Use soft-delete (INACTIVE) instead.");
        }
        customerDao.delete(customerId);
    }

    // Admin-only trainer hard delete refuses while references exist
    public void hardDeleteTrainer(Administrator actor, int trainerId) throws DatabaseException, ValidationException {
        requireAdmin(actor);
        if (trainerDao.findById(trainerId) == null) {
            throw new ValidationException("Trainer not found with id: " + trainerId);
        }
        int refs = countReferences("customers", "trainer_id", trainerId)
                + countReferences("sessions", "trainer_id", trainerId)
                + countReferences("trainer_availability", "trainer_id", trainerId)
                + countReferences("trainer_subscriptions", "trainer_id", trainerId)
                + countReferences("workout_programs", "creator_trainer_id", trainerId);
        if (refs > 0) {
            throw new ValidationException("Hard delete refused - trainer #" + trainerId
                    + " still has " + refs + " linked record(s).");
        }
        trainerDao.delete(trainerId);
    }

    // Suspending a customer freezes all active memberships
    public int suspendCustomer(int customerId) throws DatabaseException, ValidationException {
        Customer c = customerDao.findById(customerId);
        if (c == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        c.setStatus(CustomerStatus.SUSPENDED);
        customerDao.update(c);

        int frozenCount = 0;
        List<Membership> list = membershipDao.findByCustomerId(customerId);
        for (int i = 0; i < list.size(); i++) {
            Membership m = list.get(i);
            if (m.getStatus() == MembershipStatus.ACTIVE) {
                m.setStatus(MembershipStatus.FROZEN);
                if (m.getFrozenDays() <= 0) {
                    m.setFrozenDays(7);
                }
                membershipDao.update(m);
                frozenCount++;
            }
        }
        return frozenCount;
    }

    // Only ACTIVE membership can be frozen
    public void freezeMembership(int membershipId, int frozenDays) throws DatabaseException, ValidationException {
        Membership m = membershipDao.findById(membershipId);
        if (m == null) {
            throw new ValidationException("Membership not found with id: " + membershipId);
        }
        if (m.getStatus() != MembershipStatus.ACTIVE) {
            throw new ValidationException("Only ACTIVE memberships can be frozen (current: " + m.getStatus() + ")");
        }
        if (frozenDays <= 0) {
            throw new ValidationException("Freeze days must be greater than zero");
        }
        m.setStatus(MembershipStatus.FROZEN);
        m.setFrozenDays(frozenDays);
        membershipDao.update(m);
    }

    // Safely parse an ISO date stored in the database so bad stored data shows a clean
    // validation message instead of crashing the whole app with DateTimeParseException
    private LocalDate parseStoredDate(String value, String what) throws ValidationException {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Stored " + what + " has an invalid date format: " + value);
        }
    }

    // Safely parse an ISO time stored in the database (same crash protection as dates)
    private LocalTime parseStoredTime(String value, String what) throws ValidationException {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Stored " + what + " has an invalid time format: " + value);
        }
    }

    // Unfreeze shifts endDate forward by frozenDays and clears frozenDays
    public Membership unfreezeMembership(int membershipId) throws DatabaseException, ValidationException {
        Membership m = membershipDao.findById(membershipId);
        if (m == null) {
            throw new ValidationException("Membership not found with id: " + membershipId);
        }
        if (m.getStatus() != MembershipStatus.FROZEN) {
            throw new ValidationException("Only FROZEN memberships can be unfrozen (current: " + m.getStatus() + ")");
        }
        if (m.getEndDate() == null) {
            throw new ValidationException("Membership #" + membershipId + " has no end date stored");
        }
        LocalDate newEnd = parseStoredDate(m.getEndDate(), "membership #" + membershipId + " end date")
                .plusDays(m.getFrozenDays());
        m.setEndDate(newEnd.toString());
        m.setFrozenDays(0);
        m.setStatus(MembershipStatus.ACTIVE);
        membershipDao.update(m);
        return m;
    }

    // Renew extends endDate for ACTIVE or EXPIRED plans; refuses CANCELLED
    public Membership renewMembership(int membershipId) throws DatabaseException, ValidationException {
        Membership m = membershipDao.findById(membershipId);
        if (m == null) {
            throw new ValidationException("Membership not found with id: " + membershipId);
        }
        if (m.getStatus() == MembershipStatus.CANCELLED) {
            throw new ValidationException("Renewing a CANCELLED membership is refused");
        }
        if (m.getStatus() == MembershipStatus.FROZEN) {
            throw new ValidationException("Membership #" + membershipId + " is FROZEN - unfreeze it first, then renew");
        }
        int days = daysForPlan(m.getPlan());
        if (m.getEndDate() == null) {
            throw new ValidationException("Membership #" + membershipId + " has no end date stored");
        }
        LocalDate baseDate = parseStoredDate(m.getEndDate(), "membership #" + membershipId + " end date");
        LocalDate today = LocalDate.now();
        if (baseDate.isBefore(today)) {
            baseDate = today;
        }
        m.setEndDate(baseDate.plusDays(days).toString());
        m.setStatus(MembershipStatus.ACTIVE);
        membershipDao.update(m);
        return m;
    }

    public int daysForPlan(PlanType plan) {
        if (plan == PlanType.QUARTERLY) {
            return 90;
        }
        if (plan == PlanType.YEARLY) {
            return 365;
        }
        return 30;
    }

    // Derived membership status (EXPIRED if endDate < today while stored ACTIVE)
    public MembershipStatus effectiveMembershipStatus(Membership m, String todayIso) {
        if (m.getStatus() == MembershipStatus.ACTIVE && m.getEndDate() != null && m.getEndDate().compareTo(todayIso) < 0) {
            return MembershipStatus.EXPIRED;
        }
        return m.getStatus();
    }

    // Occupancy counts CONFIRMED sessions only
    public int countConfirmedInSlot(int availabilityId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM sessions WHERE availability_id = ? AND status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, availabilityId);
            ps.setString(2, SessionStatus.CONFIRMED.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count confirmed sessions in slot: " + availabilityId, e);
        }
    }

    // Derived FULL status when CONFIRMED occupancy reaches maxCustomers
    public AvailabilityStatus effectiveSlotStatus(TrainerAvailability slot) throws DatabaseException {
        if (slot.getStatus() == AvailabilityStatus.CLOSED) {
            return AvailabilityStatus.CLOSED;
        }
        int booked = countConfirmedInSlot(slot.getId());
        if (booked >= slot.getMaxCustomers()) {
            return AvailabilityStatus.FULL;
        }
        return AvailabilityStatus.OPEN;
    }

    // Book a published OPEN availability slot directly as CONFIRMED
    public Session bookOpenSlot(int customerId, int availabilityId, int durationMin,
                                Integer subscriptionId, BigDecimal price, String notes)
            throws DatabaseException, ValidationException {
        Customer cust = customerDao.findById(customerId);
        if (cust == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        if (cust.getStatus() != CustomerStatus.ACTIVE) {
            throw new ValidationException("Booking denied because member status is " + cust.getStatus());
        }
        TrainerAvailability slot = availabilityDao.findById(availabilityId);
        if (slot == null) {
            throw new ValidationException("Availability slot not found with id: " + availabilityId);
        }
        if (slot.getStatus() == AvailabilityStatus.CLOSED) {
            throw new ValidationException("Cannot book a CLOSED slot");
        }
        if (slot.getDate() != null && parseStoredDate(slot.getDate(), "slot #" + availabilityId + " date")
                .isBefore(LocalDate.now())) {
            throw new ValidationException("Cannot book a slot in the past (" + slot.getDate() + ")");
        }
        int booked = countConfirmedInSlot(availabilityId);
        if (booked >= slot.getMaxCustomers()) {
            throw new ValidationException("Slot #" + availabilityId + " is FULL ("
                    + booked + "/" + slot.getMaxCustomers() + ")");
        }
        int finalDuration = durationMin > 0 ? durationMin : 60;
        BigDecimal finalPrice = subscriptionId != null ? BigDecimal.ZERO : (price != null ? price : BigDecimal.ZERO);

        Session s = new Session(0, customerId, slot.getTrainerId(), slot.getDate(),
                slot.getStartTime(), finalDuration, SessionStatus.CONFIRMED,
                availabilityId, subscriptionId, finalPrice, null, notes);
        return sessionDao.insert(s);
    }

    // Request session outside published slots -> REQUESTED (default 60m)
    public Session requestOutsideSlots(int customerId, int trainerId, String date, String startTime,
                                       int durationMin, Integer subscriptionId, BigDecimal price, String notes)
            throws DatabaseException, ValidationException {
        // Same member check as bookOpenSlot: only ACTIVE members may request sessions
        Customer cust = customerDao.findById(customerId);
        if (cust == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        if (cust.getStatus() != CustomerStatus.ACTIVE) {
            throw new ValidationException("Session request denied because member status is " + cust.getStatus());
        }
        int finalDuration = durationMin > 0 ? durationMin : 60;
        BigDecimal finalPrice = subscriptionId != null ? BigDecimal.ZERO : (price != null ? price : BigDecimal.ZERO);
        Session s = new Session(0, customerId, trainerId, date, startTime, finalDuration,
                SessionStatus.REQUESTED, null, subscriptionId, finalPrice, null, notes);
        return sessionDao.insert(s);
    }

    // Admin override booking ignores slot cap/status -> CONFIRMED (ADMIN only)
    public Session adminReserve(Administrator actor, int customerId, int trainerId, String date,
                                String startTime, int durationMin, Integer availabilityId,
                                BigDecimal price, String notes) throws DatabaseException, ValidationException {
        requireAdmin(actor);
        int finalDuration = durationMin > 0 ? durationMin : 60;
        BigDecimal finalPrice = price != null ? price : BigDecimal.ZERO;
        Session s = new Session(0, customerId, trainerId, date, startTime, finalDuration,
                SessionStatus.CONFIRMED, availabilityId, null, finalPrice, null, notes);
        return sessionDao.insert(s);
    }

    // Confirm a REQUESTED session (checking slot cap if attached to an availability slot)
    public void confirmSession(int sessionId) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        if (s.getStatus() != SessionStatus.REQUESTED) {
            throw new ValidationException("Only REQUESTED sessions can be confirmed (current: " + s.getStatus() + ")");
        }
        if (s.getAvailabilityId() != null) {
            TrainerAvailability slot = availabilityDao.findById(s.getAvailabilityId().intValue());
            if (slot != null && countConfirmedInSlot(slot.getId()) >= slot.getMaxCustomers()) {
                throw new ValidationException("Cannot confirm session - linked slot is FULL");
            }
        }
        s.setStatus(SessionStatus.CONFIRMED);
        sessionDao.update(s);
    }

    // Complete a CONFIRMED session
    public void completeSession(int sessionId) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        if (s.getStatus() != SessionStatus.CONFIRMED) {
            throw new ValidationException("Only CONFIRMED sessions can be completed (current: " + s.getStatus() + ")");
        }
        s.setStatus(SessionStatus.COMPLETED);
        sessionDao.update(s);
    }

    // Mark CONFIRMED session as NO_SHOW (counted in trainer stats)
    public void markNoShow(int sessionId) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        if (s.getStatus() != SessionStatus.CONFIRMED) {
            throw new ValidationException("Only CONFIRMED sessions can be marked NO_SHOW (current: " + s.getStatus() + ")");
        }
        s.setStatus(SessionStatus.NO_SHOW);
        sessionDao.update(s);
    }

    // Cancel CONFIRMED session, computing the 2h notice status
    public String cancelSession(int sessionId, String cancelReason) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        if (cancelReason == null || cancelReason.trim().isEmpty()) {
            throw new ValidationException("Cancel reason is required");
        }
        LocalDateTime startDateTime = LocalDateTime.of(
                parseStoredDate(s.getDate(), "session #" + sessionId + " date"),
                parseStoredTime(s.getStartTime(), "session #" + sessionId + " start time"));
        long minutesUntilStart = Duration.between(LocalDateTime.now(), startDateTime).toMinutes();

        s.setStatus(SessionStatus.CANCELLED);
        s.setCancelReason(cancelReason.trim());
        sessionDao.update(s);

        if (minutesUntilStart >= 120) {
            return "Free cancellation (>= 2h notice, " + minutesUntilStart + " min remaining).";
        }
        return "Late cancellation (< 2h before start). Marked CANCELLED with reason recorded.";
    }

    // Count trainer NO_SHOW sessions for trainer statistics
    public int countTrainerNoShows(int trainerId) throws DatabaseException {
        List<Session> sessions = sessionDao.findByTrainerId(trainerId);
        int count = 0;
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getStatus() == SessionStatus.NO_SHOW) {
                count++;
            }
        }
        return count;
    }

    // Check-in validates ACTIVE status + unexpired plan and reuses today's existing row
    public Attendance checkIn(int customerId, String dateIso, String timeIso) throws DatabaseException, ValidationException {
        Customer cust = customerDao.findById(customerId);
        if (cust == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        if (cust.getStatus() != CustomerStatus.ACTIVE) {
            throw new ValidationException("Check-in denied because member status is " + cust.getStatus());
        }
        boolean hasValidMembership = false;
        List<Membership> plans = membershipDao.findByCustomerId(customerId);
        for (int i = 0; i < plans.size(); i++) {
            Membership m = plans.get(i);
            if (m.getStatus() == MembershipStatus.ACTIVE && m.getEndDate() != null && m.getEndDate().compareTo(dateIso) >= 0) {
                hasValidMembership = true;
                break;
            }
        }
        if (!hasValidMembership) {
            throw new ValidationException("Check-in denied - no ACTIVE unexpired membership on " + dateIso);
        }

        List<Attendance> existingList = attendanceDao.findByCustomerId(customerId);
        for (int i = 0; i < existingList.size(); i++) {
            Attendance row = existingList.get(i);
            if (dateIso.equals(row.getDate())) {
                row.setCheckIn(timeIso);
                attendanceDao.update(row);
                return row;
            }
        }
        return attendanceDao.insert(new Attendance(0, customerId, dateIso, timeIso, null));
    }

    public Attendance checkOut(int customerId, String dateIso, String checkOutTime)
            throws DatabaseException, ValidationException {
        List<Attendance> existingList = attendanceDao.findByCustomerId(customerId);
        for (int i = 0; i < existingList.size(); i++) {
            Attendance row = existingList.get(i);
            if (dateIso.equals(row.getDate())) {
                row.setCheckOut(checkOutTime);
                attendanceDao.update(row);
                return row;
            }
        }
        throw new ValidationException("No check-in record found for customer #" + customerId + " on " + dateIso);
    }

    // Medical profile read check, allowed for the assigned trainer only
    public MedicalProfile getMedicalProfileForTrainer(int trainerId, int customerId)
            throws DatabaseException, ValidationException {
        Customer c = customerDao.findById(customerId);
        if (c == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        if (c.getTrainerId() == null || c.getTrainerId().intValue() != trainerId) {
            throw new ValidationException("Access denied - trainer #" + trainerId
                    + " is not the assigned trainer for customer #" + customerId);
        }
        return medicalDao.findByCustomerId(customerId);
    }

    // Outstanding unpaid balance across memberships, subscriptions, and non-zero ad-hoc sessions
    public BigDecimal computeOutstandingBalance(int customerId) throws DatabaseException {
        List<Payment> payments = paymentDao.findByCustomerId(customerId);
        BigDecimal total = BigDecimal.ZERO;

        List<Membership> memberships = membershipDao.findByCustomerId(customerId);
        for (int i = 0; i < memberships.size(); i++) {
            Membership m = memberships.get(i);
            if (m.getStatus() == MembershipStatus.CANCELLED || m.getPrice() == null) {
                continue;
            }
            BigDecimal paid = sumNonVoidedForMembership(payments, m.getId());
            BigDecimal owed = m.getPrice().subtract(paid);
            if (owed.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(owed);
            }
        }

        List<TrainerSubscription> subs = subscriptionDao.findByCustomerId(customerId);
        for (int i = 0; i < subs.size(); i++) {
            TrainerSubscription s = subs.get(i);
            if (s.getStatus() == SubscriptionStatus.CANCELLED || s.getRatePerPeriod() == null) {
                continue;
            }
            BigDecimal paid = sumNonVoidedForSubscription(payments, s.getId());
            BigDecimal owed = s.getRatePerPeriod().subtract(paid);
            if (owed.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(owed);
            }
        }

        List<Session> allSessions = sessionDao.findAll();
        for (int i = 0; i < allSessions.size(); i++) {
            Session ses = allSessions.get(i);
            if (ses.getCustomerId() != customerId || ses.getSubscriptionId() != null) {
                continue;
            }
            if (ses.getStatus() == SessionStatus.CANCELLED
                    || ses.getStatus() == SessionStatus.REQUESTED || ses.getPrice() == null) {
                continue;
            }
            if (ses.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal paid = sumNonVoidedForSession(payments, ses.getId());
            BigDecimal owed = ses.getPrice().subtract(paid);
            if (owed.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(owed);
            }
        }

        return total;
    }

    // Void an existing payment and optionally create a replacement payment
    public Payment voidAndReplacePayment(int paymentId, String voidReason, String todayIso,
                                         BigDecimal replacementAmount, PaymentMethod replacementMethod,
                                         String replacementReceiptNo) throws DatabaseException, ValidationException {
        Payment old = paymentDao.findById(paymentId);
        if (old == null) {
            throw new ValidationException("Payment not found with id: " + paymentId);
        }
        if (old.isVoided()) {
            throw new ValidationException("Payment #" + paymentId + " is already voided");
        }
        if (voidReason == null || voidReason.trim().isEmpty()) {
            throw new ValidationException("Void reason is required");
        }
        // Validate the replacement receipt BEFORE voiding the old payment, so a failed
        // replacement cannot leave the old payment voided with nothing to replace it
        boolean wantsReplacement = replacementReceiptNo != null && !replacementReceiptNo.trim().isEmpty();
        if (wantsReplacement) {
            if (replacementAmount == null || replacementAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Replacement amount must be greater than zero");
            }
            if (replacementMethod == null) {
                throw new ValidationException("Replacement payment method is required");
            }
            Payment receiptOwner = paymentDao.findByReceiptNo(replacementReceiptNo.trim());
            if (receiptOwner != null) {
                throw new ValidationException("Replacement receipt number already exists: "
                        + replacementReceiptNo.trim());
            }
        }
        old.setPreviousAmount(old.getAmount());
        old.setVoided(true);
        old.setVoidReason(voidReason.trim());
        old.setVoidedDate(todayIso);
        paymentDao.update(old);

        if (wantsReplacement) {
            Payment replacement = new Payment(0, old.getCustomerId(), old.getMembershipId(),
                    old.getSubscriptionId(), old.getSessionId(), replacementAmount,
                    replacementMethod, todayIso, replacementReceiptNo.trim(), false);
            return paymentDao.insert(replacement);
        }
        return old;
    }

    public GymBackupSnapshot exportSnapshotToFile(String filePath, String actorUsername) throws DatabaseException {
        GymBackupSnapshot snap = new GymBackupSnapshot();
        snap.setCreatedAt(LocalDate.now().toString());
        snap.setCreatedBy(actorUsername);
        snap.setAdministrators(new ArrayList<>(adminDao.findAll()));
        snap.setTrainers(new ArrayList<>(trainerDao.findAll()));
        snap.setCustomers(new ArrayList<>(customerDao.findAll()));
        snap.setMemberships(new ArrayList<>(membershipDao.findAll()));
        snap.setWorkoutPrograms(new ArrayList<>(workoutDao.findAll()));
        snap.setTrainerAvailabilities(new ArrayList<>(availabilityDao.findAll()));
        snap.setTrainerSubscriptions(new ArrayList<>(subscriptionDao.findAll()));
        snap.setSessions(new ArrayList<>(sessionDao.findAll()));
        snap.setEquipment(new ArrayList<>(equipmentDao.findAll()));
        snap.setAttendanceRecords(new ArrayList<>(attendanceDao.findAll()));
        snap.setPayments(new ArrayList<>(paymentDao.findAll()));
        snap.setProgressRecords(new ArrayList<>(progressDao.findAll()));
        snap.setMedicalProfiles(new ArrayList<>(medicalDao.findAll()));
        SerializationUtil.saveToFile(snap, filePath);
        return snap;
    }

    public GymBackupSnapshot loadSnapshotFromFile(String filePath) throws DatabaseException {
        Object loaded = SerializationUtil.loadFromFile(filePath);
        if (!(loaded instanceof GymBackupSnapshot)) {
            throw new DatabaseException("File does not contain a valid GymBackupSnapshot: " + filePath);
        }
        return (GymBackupSnapshot) loaded;
    }

    private BigDecimal sumNonVoidedForMembership(List<Payment> payments, int membershipId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < payments.size(); i++) {
            Payment p = payments.get(i);
            if (!p.isVoided() && p.getMembershipId() != null && p.getMembershipId().intValue() == membershipId && p.getAmount() != null) {
                sum = sum.add(p.getAmount());
            }
        }
        return sum;
    }

    private BigDecimal sumNonVoidedForSubscription(List<Payment> payments, int subscriptionId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < payments.size(); i++) {
            Payment p = payments.get(i);
            if (!p.isVoided() && p.getSubscriptionId() != null && p.getSubscriptionId().intValue() == subscriptionId && p.getAmount() != null) {
                sum = sum.add(p.getAmount());
            }
        }
        return sum;
    }

    private BigDecimal sumNonVoidedForSession(List<Payment> payments, int sessionId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < payments.size(); i++) {
            Payment p = payments.get(i);
            if (!p.isVoided() && p.getSessionId() != null && p.getSessionId().intValue() == sessionId && p.getAmount() != null) {
                sum = sum.add(p.getAmount());
            }
        }
        return sum;
    }

    private int countReferences(String table, String column, int targetId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed counting references in " + table, e);
        }
    }
}

// ===== GymBackupSnapshot =====

// Serializable snapshot of all 13 database tables for binary .ser backup and restore
class GymBackupSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String createdAt;
    private String createdBy;
    private ArrayList<Administrator> administrators;
    private ArrayList<Trainer> trainers;
    private ArrayList<Customer> customers;
    private ArrayList<Membership> memberships;
    private ArrayList<WorkoutProgram> workoutPrograms;
    private ArrayList<TrainerAvailability> trainerAvailabilities;
    private ArrayList<TrainerSubscription> trainerSubscriptions;
    private ArrayList<Session> sessions;
    private ArrayList<Equipment> equipment;
    private ArrayList<Attendance> attendanceRecords;
    private ArrayList<Payment> payments;
    private ArrayList<ProgressRecord> progressRecords;
    private ArrayList<MedicalProfile> medicalProfiles;

    public GymBackupSnapshot() {
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ArrayList<Administrator> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(ArrayList<Administrator> administrators) {
        this.administrators = administrators;
    }

    public ArrayList<Trainer> getTrainers() {
        return trainers;
    }

    public void setTrainers(ArrayList<Trainer> trainers) {
        this.trainers = trainers;
    }

    public ArrayList<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(ArrayList<Customer> customers) {
        this.customers = customers;
    }

    public ArrayList<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(ArrayList<Membership> memberships) {
        this.memberships = memberships;
    }

    public ArrayList<WorkoutProgram> getWorkoutPrograms() {
        return workoutPrograms;
    }

    public void setWorkoutPrograms(ArrayList<WorkoutProgram> workoutPrograms) {
        this.workoutPrograms = workoutPrograms;
    }

    public ArrayList<TrainerAvailability> getTrainerAvailabilities() {
        return trainerAvailabilities;
    }

    public void setTrainerAvailabilities(ArrayList<TrainerAvailability> trainerAvailabilities) {
        this.trainerAvailabilities = trainerAvailabilities;
    }

    public ArrayList<TrainerSubscription> getTrainerSubscriptions() {
        return trainerSubscriptions;
    }

    public void setTrainerSubscriptions(ArrayList<TrainerSubscription> trainerSubscriptions) {
        this.trainerSubscriptions = trainerSubscriptions;
    }

    public ArrayList<Session> getSessions() {
        return sessions;
    }

    public void setSessions(ArrayList<Session> sessions) {
        this.sessions = sessions;
    }

    public ArrayList<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(ArrayList<Equipment> equipment) {
        this.equipment = equipment;
    }

    public ArrayList<Attendance> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(ArrayList<Attendance> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public ArrayList<Payment> getPayments() {
        return payments;
    }

    public void setPayments(ArrayList<Payment> payments) {
        this.payments = payments;
    }

    public ArrayList<ProgressRecord> getProgressRecords() {
        return progressRecords;
    }

    public void setProgressRecords(ArrayList<ProgressRecord> progressRecords) {
        this.progressRecords = progressRecords;
    }

    public ArrayList<MedicalProfile> getMedicalProfiles() {
        return medicalProfiles;
    }

    public void setMedicalProfiles(ArrayList<MedicalProfile> medicalProfiles) {
        this.medicalProfiles = medicalProfiles;
    }

    public int totalRecordsCount() {
        int total = 0;
        if (administrators != null) total += administrators.size();
        if (trainers != null) total += trainers.size();
        if (customers != null) total += customers.size();
        if (memberships != null) total += memberships.size();
        if (workoutPrograms != null) total += workoutPrograms.size();
        if (trainerAvailabilities != null) total += trainerAvailabilities.size();
        if (trainerSubscriptions != null) total += trainerSubscriptions.size();
        if (sessions != null) total += sessions.size();
        if (equipment != null) total += equipment.size();
        if (attendanceRecords != null) total += attendanceRecords.size();
        if (payments != null) total += payments.size();
        if (progressRecords != null) total += progressRecords.size();
        if (medicalProfiles != null) total += medicalProfiles.size();
        return total;
    }
}

// ===== SampleDataSeeder =====

// Seeds repeatable sample gym data when the SQLite database starts empty
class SampleDataSeeder {

    private SampleDataSeeder() {
    }

    public static void seedIfEmpty() throws DatabaseException, ValidationException {
        AdministratorDao adminDao = new AdministratorDao();
        if (!adminDao.findAll().isEmpty()) {
            return;
        }
        seedRecords();
    }

    public static void resetAndSeed() throws DatabaseException, ValidationException {
        String[] tables = {
            "payments", "attendance", "sessions", "trainer_subscriptions",
            "trainer_availability", "workout_programs", "progress_records",
            "medical_profiles", "memberships", "equipment", "customers",
            "trainers", "administrators"
        };
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (int i = 0; i < tables.length; i++) {
                stmt.executeUpdate("DELETE FROM " + tables[i]);
                stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name = '" + tables[i] + "'");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to reset SQLite database tables", e);
        }
        seedRecords();
    }

    private static void seedRecords() throws DatabaseException, ValidationException {
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        String monthAgo = LocalDate.now().minusDays(30).toString();
        String nextMonth = LocalDate.now().plusDays(30).toString();
        String nextYear = LocalDate.now().plusDays(365).toString();

        // 1. Staff and administrators (includes active + inactive accounts)
        AdministratorDao adminDao = new AdministratorDao();
        adminDao.insert(new Administrator(0, "Alex Carter", "admin", "admin123",
                StaffRole.ADMIN, "01012345678", "admin@gymos.local", true, monthAgo));
        adminDao.insert(new Administrator(0, "Mona El-Sayed", "manager", "mgr123",
                StaffRole.MANAGER, "01023456789", "manager@gymos.local", true, monthAgo));
        adminDao.insert(new Administrator(0, "Youssef Ali", "staff", "staff123",
                StaffRole.STAFF, "01034567890", "staff@gymos.local", true, monthAgo));
        adminDao.insert(new Administrator(0, "Inactive Ex-Staff", "oldstaff", "old123",
                StaffRole.STAFF, "01045678901", "old@gymos.local", false, monthAgo));

        // 2. Trainers
        TrainerDao trainerDao = new TrainerDao();
        Trainer t1 = trainerDao.insert(new Trainer(0, "Coach Rami Nassar", "01111112222",
                "rami@gymos.local", "Strength & Conditioning", 7, "NSCA-CSCS, CPR",
                "2023-01-15", new BigDecimal("50.00")));
        Trainer t2 = trainerDao.insert(new Trainer(0, "Coach Sara Mansour", "01122223333",
                "sara@gymos.local", "HIIT & Mobility", 5, "NASM-CPT",
                "2024-03-01", new BigDecimal("45.00")));
        Trainer t3 = trainerDao.insert(new Trainer(0, "Coach Omar Fathy", "01133334444",
                "omar.f@gymos.local", "Olympic Lifting", 9, "USA-W L2",
                "2022-06-10", new BigDecimal("60.00")));

        // 3. Customers across statuses (ACTIVE, SUSPENDED, INACTIVE)
        CustomerDao customerDao = new CustomerDao();
        Customer c1 = customerDao.insert(new Customer(0, "Omar Hassan", "01211110001",
                "omar.h@mail.com", "Nasr City, Cairo", "1998-04-12", "Male",
                "Hany Hassan", "01299990001", CustomerStatus.ACTIVE, monthAgo, t1.getId()));
        Customer c2 = customerDao.insert(new Customer(0, "Nadia Youssef", "01211110002",
                "nadia.y@mail.com", "Maadi, Cairo", "1996-09-23", "Female",
                "Dalia Youssef", "01299990002", CustomerStatus.ACTIVE, monthAgo, t2.getId()));
        Customer c3 = customerDao.insert(new Customer(0, "Karim Adel", "01211110003",
                "karim.a@mail.com", "Heliopolis, Cairo", "2000-02-05", "Male",
                "Adel Samir", "01299990003", CustomerStatus.ACTIVE, monthAgo, t1.getId()));
        Customer c4 = customerDao.insert(new Customer(0, "Salma Tariq", "01211110004",
                "salma.t@mail.com", "Zamalek, Cairo", "1999-11-18", "Female",
                "Tariq Nabil", "01299990004", CustomerStatus.SUSPENDED, monthAgo, t3.getId()));
        Customer c5 = customerDao.insert(new Customer(0, "Tarek Nabil", "01211110005",
                "tarek.n@mail.com", "Dokki, Giza", "1995-07-30", "Male",
                "Nabil Saad", "01299990005", CustomerStatus.INACTIVE, monthAgo, null));

        // 4. Memberships (ACTIVE, EXPIRED, FROZEN, CANCELLED)
        MembershipDao memDao = new MembershipDao();
        Membership m1 = memDao.insert(new Membership(0, c1.getId(), PlanType.YEARLY,
                new BigDecimal("500.00"), monthAgo, nextYear, MembershipStatus.ACTIVE, 0));
        Membership m2 = memDao.insert(new Membership(0, c2.getId(), PlanType.MONTHLY,
                new BigDecimal("60.00"), today, nextMonth, MembershipStatus.ACTIVE, 0));
        memDao.insert(new Membership(0, c3.getId(), PlanType.MONTHLY,
                new BigDecimal("60.00"), LocalDate.now().minusDays(60).toString(),
                yesterday, MembershipStatus.ACTIVE, 0)); // past endDate -> expired on check-in/derived EXPIRED
        memDao.insert(new Membership(0, c4.getId(), PlanType.QUARTERLY,
                new BigDecimal("150.00"), monthAgo, nextMonth, MembershipStatus.FROZEN, 10));
        memDao.insert(new Membership(0, c5.getId(), PlanType.MONTHLY,
                new BigDecimal("60.00"), monthAgo, yesterday, MembershipStatus.CANCELLED, 0));

        // 5. Medical Profile (on-demand for c1; others have none yet)
        MedicalProfileDao medDao = new MedicalProfileDao();
        medDao.insert(new MedicalProfile(0, c1.getId(), "O+", "None",
                "Penicillin", "None", "Mild right shoulder strain (2024)",
                "Dr. Sherif Kamel", "01055556666", "Cleared for heavy compound lifts", today));

        // 6. Progress Records
        ProgressRecordDao progDao = new ProgressRecordDao();
        progDao.insert(new ProgressRecord(0, c1.getId(), monthAgo, 84.5, 19.2,
                "Chest 102cm, Waist 86cm", 78.0, "Baseline assessment"));
        progDao.insert(new ProgressRecord(0, c1.getId(), today, 82.1, 17.6,
                "Chest 103cm, Waist 83cm", 78.0, "Down 2.4 kg in 4 weeks"));
        progDao.insert(new ProgressRecord(0, c2.getId(), today, 61.0, 22.0,
                "Waist 70cm", 58.0, "Core endurance improving"));

        // 7. Workout Programs (v1 and v2 for c1)
        WorkoutProgramDao wpDao = new WorkoutProgramDao();
        wpDao.insert(new WorkoutProgram(0, c1.getId(), t1.getId(), "Strength Foundation",
                "Squat 4x8, Bench Press 4x8, Barbell Row 4x10", 1, monthAgo));
        wpDao.insert(new WorkoutProgram(0, c1.getId(), t1.getId(), "Hypertrophy Split",
                "Incline DB Press 4x10, Pull-Ups 4x8, RDL 3x10, Plank 3x60s", 2, today));

        // 8. Trainer Availability Slots
        TrainerAvailabilityDao availDao = new TrainerAvailabilityDao();
        TrainerAvailability slot1 = availDao.insert(new TrainerAvailability(0, t1.getId(), tomorrow,
                "10:00", "11:00", 2, AvailabilityStatus.OPEN));
        availDao.insert(new TrainerAvailability(0, t1.getId(), tomorrow,
                "12:00", "13:00", 1, AvailabilityStatus.OPEN));
        availDao.insert(new TrainerAvailability(0, t2.getId(), tomorrow,
                "14:00", "15:00", 3, AvailabilityStatus.OPEN));

        // 9. Trainer Subscriptions (matches c1.trainerId == t1.getId())
        TrainerSubscriptionDao subDao = new TrainerSubscriptionDao();
        TrainerSubscription sub1 = subDao.insert(new TrainerSubscription(0, c1.getId(), t1.getId(),
                today, nextMonth, new BigDecimal("120.00"), SubscriptionStatus.ACTIVE));

        // 10. Sessions (CONFIRMED covered by sub price=0, REQUESTED ad-hoc, COMPLETED, NO_SHOW)
        SessionDao sesDao = new SessionDao();
        sesDao.insert(new Session(0, c1.getId(), t1.getId(), tomorrow, "10:00", 60,
                SessionStatus.CONFIRMED, slot1.getId(), sub1.getId(), BigDecimal.ZERO,
                null, "Subscription strength check-in"));
        sesDao.insert(new Session(0, c2.getId(), t2.getId(), tomorrow, "16:00", 60,
                SessionStatus.REQUESTED, null, null, new BigDecimal("45.00"),
                null, "Requested outside regular slots"));
        sesDao.insert(new Session(0, c1.getId(), t1.getId(), yesterday, "09:00", 60,
                SessionStatus.COMPLETED, null, sub1.getId(), BigDecimal.ZERO,
                null, "Completed lower body block"));
        sesDao.insert(new Session(0, c2.getId(), t2.getId(), yesterday, "18:00", 60,
                SessionStatus.NO_SHOW, null, null, new BigDecimal("40.00"),
                null, "Member did not arrive"));

        // 11. Equipment
        EquipmentDao eqDao = new EquipmentDao();
        eqDao.insert(new Equipment(0, "TechnoGym Treadmill T1", "CARDIO", "2024-01-10",
                EquipmentCondition.GOOD, EquipmentStatus.AVAILABLE, monthAgo));
        eqDao.insert(new Equipment(0, "Concept2 Rower R4", "CARDIO", "2024-02-15",
                EquipmentCondition.NEW, EquipmentStatus.AVAILABLE, today));
        eqDao.insert(new Equipment(0, "Rogue Monster Squat Rack", "STRENGTH", "2023-05-20",
                EquipmentCondition.GOOD, EquipmentStatus.AVAILABLE, monthAgo));
        eqDao.insert(new Equipment(0, "Dual Cable Pulley", "STRENGTH", "2023-08-12",
                EquipmentCondition.FAIR, EquipmentStatus.IN_MAINTENANCE, today));
        eqDao.insert(new Equipment(0, "Hex Dumbbell Rack (5-40kg)", "FREE WEIGHTS", "2023-03-01",
                EquipmentCondition.GOOD, EquipmentStatus.AVAILABLE, monthAgo));
        eqDao.insert(new Equipment(0, "Mobility & Foam Zone", "RECOVERY", "2024-06-01",
                EquipmentCondition.NEW, EquipmentStatus.AVAILABLE, today));

        // 12. Attendance (c1 open today check-in; c2 yesterday visit)
        AttendanceDao attDao = new AttendanceDao();
        attDao.insert(new Attendance(0, c1.getId(), today, "08:30", null));
        attDao.insert(new Attendance(0, c2.getId(), yesterday, "17:15", "18:45"));

        // 13. Payments (includes a voided + replacement pair, and leaves c2 ad-hoc sessions unpaid)
        PaymentDao payDao = new PaymentDao();
        Payment voidedPay = new Payment(0, c1.getId(), m1.getId(), null, null,
                new BigDecimal("450.00"), PaymentMethod.CARD, monthAgo, "REC-1001", true);
        voidedPay.setVoidReason("Incorrect invoice tier");
        voidedPay.setVoidedDate(monthAgo);
        voidedPay.setPreviousAmount(new BigDecimal("450.00"));
        payDao.insert(voidedPay);
        payDao.insert(Payment.forMembership(c1.getId(), m1.getId(), new BigDecimal("500.00"),
                PaymentMethod.CARD, monthAgo, "REC-1002"));
        payDao.insert(Payment.forSubscription(c1.getId(), sub1.getId(), new BigDecimal("120.00"),
                PaymentMethod.TRANSFER, today, "REC-1003"));
        payDao.insert(Payment.forMembership(c2.getId(), m2.getId(), new BigDecimal("60.00"),
                PaymentMethod.CASH, today, "REC-1004"));
    }
}

// ===== MemberCliMenu =====

// Terminal screens for Customer List, Member 360 Profile, Medical Profile, and Progress Records
class MemberCliMenu {

    private final CliInput input;
    private final CliSession session;
    private final GymOperations ops = new GymOperations();
    private final CustomerDao customerDao = new CustomerDao();
    private final TrainerDao trainerDao = new TrainerDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final MedicalProfileDao medicalDao = new MedicalProfileDao();
    private final ProgressRecordDao progressDao = new ProgressRecordDao();
    private final WorkoutProgramDao workoutDao = new WorkoutProgramDao();

    public MemberCliMenu(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openMenu() {
        while (!session.shouldStopSubMenu()) {
            CliTheme.printHeader("MEMBERS, PROFILES, MEDICAL & PROGRESS",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu > Members");

            System.out.println("  [1] List All Members");
            System.out.println("  [2] Search Members (by Name, Phone, or ID)");
            System.out.println("  [3] Open Member 360 Profile (Edit / Medical / Progress / Suspend / Delete)");
            System.out.println("  [4] Register New Member");
            CliTheme.printFooterNav(true);

            String choice = input.readLine("Select option > ");
            if (choice == null || "0".equals(choice)) {
                return;
            }
            if ("H".equalsIgnoreCase(choice)) {
                session.setReturnHome(true);
                return;
            }
            if ("L".equalsIgnoreCase(choice)) {
                session.setLogoutRequested(true);
                return;
            }

            if ("1".equals(choice)) {
                listAllMembers();
                input.pause();
            } else if ("2".equals(choice)) {
                searchMembers();
                input.pause();
            } else if ("3".equals(choice)) {
                Integer id = input.readInt("Enter Member ID (or 0 to cancel): ", 0);
                if (id != null && id.intValue() > 0) {
                    openMemberProfile(id.intValue());
                }
            } else if ("4".equals(choice)) {
                registerMember();
                input.pause();
            } else {
                System.out.println("[!] Unknown option: " + choice);
            }
        }
    }

    private void listAllMembers() {
        try {
            List<Customer> list = customerDao.findAll();
            printCustomerTable(list);
        } catch (DatabaseException e) {
            CliTheme.printDatabaseError(e.getMessage());
        }
    }

    private void searchMembers() {
        String term = input.readLine("Enter search text (name, phone, or exact ID): ");
        if (term == null || term.isEmpty()) {
            System.out.println("[!] Search cancelled.");
            return;
        }
        String lower = term.toLowerCase();
        try {
            List<Customer> all = customerDao.findAll();
            List<Customer> matches = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                Customer c = all.get(i);
                String name = c.getFullName() != null ? c.getFullName().toLowerCase() : "";
                String phone = c.getPhone() != null ? c.getPhone().toLowerCase() : "";
                String idStr = String.valueOf(c.getId());
                if (name.contains(lower) || phone.contains(lower) || idStr.equals(term)) {
                    matches.add(c);
                }
            }
            CliTheme.printSectionTitle("Search Results for '" + term + "'");
            printCustomerTable(matches);
        } catch (DatabaseException e) {
            CliTheme.printDatabaseError(e.getMessage());
        }
    }

    private void registerMember() {
        CliTheme.printSectionTitle("Register New Member");
        String name = input.readRawString("Full Name (required): ");
        if (name == null) {
            return;
        }
        String phone = input.readRawString("Phone (min 7 digits, or blank): ");
        String email = input.readRawString("Email (example: user@mail.com, or blank): ");
        String address = input.readOptionalString("Address", "Cairo");
        String dob = input.readRawString("Date of Birth yyyy-MM-dd (or blank): ");
        String gender = input.readOptionalString("Gender", "Male");
        String emName = input.readOptionalString("Emergency Contact Name", "Parent");
        String emPhone = input.readRawString("Emergency Phone (min 7 digits, or blank): ");
        Integer trainerId = readOptionalTrainerId(null);

        Customer c = new Customer(0, name, phone, email, address, dob, gender,
                emName, emPhone, CustomerStatus.ACTIVE, LocalDate.now().toString(), trainerId);

        try {
            Customer saved = customerDao.insert(c);
            CliTheme.printSuccess("Registered Member #" + saved.getId() + ": " + saved.getFullName());
        } catch (ValidationException e) {
            CliTheme.printValidationError(e.getMessage());
        } catch (DatabaseException e) {
            CliTheme.printDatabaseError(e.getMessage());
        }
    }

    public void openMemberProfile(int customerId) {
        while (!session.shouldStopSubMenu()) {
            try {
                Customer c = customerDao.findById(customerId);
                if (c == null) {
                    CliTheme.printValidationError("Customer #" + customerId + " was not found.");
                    input.pause();
                    return;
                }
                Trainer assigned = c.getTrainerId() != null ? trainerDao.findById(c.getTrainerId().intValue()) : null;
                List<Membership> plans = membershipDao.findByCustomerId(customerId);
                MedicalProfile med = medicalDao.findByCustomerId(customerId);
                List<ProgressRecord> progress = progressDao.findByCustomerId(customerId);
                List<WorkoutProgram> workouts = workoutDao.findByCustomerId(customerId);
                BigDecimal owed = ops.computeOutstandingBalance(customerId);

                CliTheme.printHeader("MEMBER 360 PROFILE (#" + c.getId() + ")",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Members > Profile #" + c.getId());

                System.out.println("  Name     : " + c.getFullName() + "   | Status: " + c.getStatus());
                System.out.println("  Phone    : " + CliTheme.safeText(c.getPhone()) + " | Email: " + CliTheme.safeText(c.getEmail()));
                System.out.println("  DOB      : " + CliTheme.safeText(c.getDateOfBirth()) + " | Joined: " + CliTheme.safeText(c.getJoinDate()));
                System.out.println("  Trainer  : " + (assigned != null ? "#" + assigned.getId() + " " + assigned.getFullName() : "None"));
                System.out.println("  Balance  : $" + owed.toPlainString() + " outstanding");
                System.out.println("  Plans    : " + plans.size() + " | Workouts: " + workouts.size()
                        + " | Progress Logs: " + progress.size()
                        + " | Medical Profile: " + (med != null ? "Present (" + CliTheme.safeText(med.getBloodType()) + ")" : "Not created yet"));

                CliTheme.printSingleLine();
                System.out.println("  [1] Edit Member Details / Reassign Trainer");
                System.out.println("  [2] Soft-Delete Member -> Set INACTIVE");
                System.out.println("  [3] Hard-Delete Member [ADMIN Only + Ref Guard]");
                System.out.println("  [4] Suspend Member (Cascades Freeze to Active Plans) / Reactivate");
                System.out.println("  [5] Medical Profile Manager");
                System.out.println("  [6] Progress Records & ASCII Trend (add / view / delete)");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    editCustomer(c);
                    input.pause();
                } else if ("2".equals(choice)) {
                    if (input.readYesNo("Set member #" + c.getId() + " (" + c.getFullName() + ") to INACTIVE?")) {
                        ops.softDeleteCustomer(c.getId());
                        CliTheme.printSuccess("Member #" + c.getId() + " set to INACTIVE.");
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    if (input.readYesNo("PERMANENTLY delete member #" + c.getId() + " (" + c.getFullName() + ")?")) {
                        ops.hardDeleteCustomer(session.getCurrentUser(), c.getId());
                        CliTheme.printSuccess("Member #" + c.getId() + " permanently deleted.");
                        input.pause();
                        return;
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    toggleSuspendCustomer(c);
                    input.pause();
                } else if ("5".equals(choice)) {
                    openMedicalMenu(c);
                } else if ("6".equals(choice)) {
                    openProgressMenu(c);
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void editCustomer(Customer c) throws DatabaseException, ValidationException {
        CliTheme.printSectionTitle("Edit Member #" + c.getId());
        String name = input.readOptionalString("Full Name", c.getFullName());
        String phone = input.readOptionalString("Phone", c.getPhone());
        String email = input.readOptionalString("Email", c.getEmail());
        Integer trainerId = readOptionalTrainerId(c.getTrainerId());

        c.setFullName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setTrainerId(trainerId);
        customerDao.update(c);
        CliTheme.printSuccess("Member #" + c.getId() + " updated.");
    }

    private void toggleSuspendCustomer(Customer c) throws DatabaseException, ValidationException {
        if (c.getStatus() == CustomerStatus.INACTIVE) {
            throw new ValidationException("Member #" + c.getId()
                    + " is INACTIVE (soft-deleted). Only ACTIVE members can be suspended.");
        }
        if (c.getStatus() == CustomerStatus.SUSPENDED) {
            c.setStatus(CustomerStatus.ACTIVE);
            customerDao.update(c);
            CliTheme.printSuccess("Member #" + c.getId() + " reactivated to ACTIVE (frozen plans remain frozen until unfrozen).");
        } else {
            int frozenCount = ops.suspendCustomer(c.getId());
            CliTheme.printSuccess("Member #" + c.getId() + " set to SUSPENDED and "
                    + frozenCount + " active membership(s) frozen.");
        }
    }

    private void openMedicalMenu(Customer c) {
        while (!session.shouldStopSubMenu()) {
            try {
                MedicalProfile med = medicalDao.findByCustomerId(c.getId());
                CliTheme.printHeader("MEDICAL PROFILE - " + c.getFullName(),
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Members > Profile #" + c.getId() + " > Medical");

                if (med == null) {
                    System.out.println("  No medical profile on file yet (created on demand).");
                } else {
                    System.out.println("  ID: #" + med.getId() + " | Blood Type: " + med.getBloodType());
                    System.out.println("  Conditions : " + med.getConditions());
                    System.out.println("  Allergies  : " + med.getAllergies());
                    System.out.println("  Medications: " + med.getMedications());
                    System.out.println("  Injuries   : " + med.getInjuries());
                    System.out.println("  Doctor     : " + med.getDoctorName() + " (" + med.getDoctorPhone() + ")");
                    System.out.println("  Updated    : " + med.getUpdatedDate());
                }

                CliTheme.printSingleLine();
                System.out.println("  [1] Create On-Demand Medical Profile");
                System.out.println("  [2] Update Existing Medical Profile");
                System.out.println("  [3] Trainer Medical Read Check");
                System.out.println("  [4] Delete Medical Profile");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    String blood = input.readOptionalString("Blood Type", "A+");
                    String cond = input.readOptionalString("Conditions", "None");
                    String allergies = input.readOptionalString("Allergies", "None");
                    String meds = input.readOptionalString("Medications", "None");
                    String injuries = input.readOptionalString("Injuries", "None");
                    String doc = input.readOptionalString("Doctor Name", "Dr. Ahmed");
                    String docPhone = input.readOptionalString("Doctor Phone", "01000001111");
                    String notes = input.readOptionalString("Notes", "Cleared for exercise");
                    MedicalProfile created = new MedicalProfile(0, c.getId(), blood, cond,
                            allergies, meds, injuries, doc, docPhone, notes, LocalDate.now().toString());
                    medicalDao.insert(created);
                    CliTheme.printSuccess("Created MedicalProfile #" + created.getId());
                    input.pause();
                } else if ("2".equals(choice)) {
                    if (med == null) {
                        CliTheme.printValidationError("No medical profile exists to update. Create one first.");
                    } else {
                        med.setBloodType(input.readOptionalString("Blood Type", med.getBloodType()));
                        med.setConditions(input.readOptionalString("Conditions", med.getConditions()));
                        med.setAllergies(input.readOptionalString("Allergies", med.getAllergies()));
                        med.setInjuries(input.readOptionalString("Injuries", med.getInjuries()));
                        med.setUpdatedDate(LocalDate.now().toString());
                        medicalDao.update(med);
                        CliTheme.printSuccess("Updated MedicalProfile #" + med.getId());
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer tid = input.readInt("Enter Trainer ID requesting read access: ", 1);
                    if (tid != null) {
                        MedicalProfile access = ops.getMedicalProfileForTrainer(tid.intValue(), c.getId());
                        CliTheme.printSuccess("Access GRANTED for Trainer #" + tid + ": "
                                + (access != null ? "Blood " + access.getBloodType() + ", Notes: " + access.getNotes()
                                : "Access allowed (member has no medical profile yet)."));
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    if (med != null && input.readYesNo("Delete medical profile #" + med.getId() + "?")) {
                        medicalDao.delete(med.getId());
                        CliTheme.printSuccess("Deleted MedicalProfile #" + med.getId());
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void openProgressMenu(Customer c) {
        while (!session.shouldStopSubMenu()) {
            try {
                List<ProgressRecord> records = progressDao.findByCustomerId(c.getId());
                CliTheme.printHeader("PROGRESS RECORDS - " + c.getFullName(),
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Members > Profile #" + c.getId() + " > Progress");

                printProgressTableAndChart(records);

                CliTheme.printSingleLine();
                System.out.println("  [1] Add Progress Record");
                System.out.println("  [2] Delete Progress Record");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    String date = input.readIsoDate("Date", LocalDate.now().toString());
                    Double weight = input.readDouble("Weight kg (>0)", 75.0);
                    Double bodyFat = input.readDouble("Body Fat % (0-100)", 18.0);
                    Double target = input.readOptionalDouble("Target Weight kg", 72.0);
                    String meas = input.readOptionalString("Measurements", "Waist 82cm");
                    String notes = input.readOptionalString("Notes", "Steady progress");
                    if (date != null && weight != null && bodyFat != null) {
                        ProgressRecord pr = new ProgressRecord(0, c.getId(), date,
                                weight.doubleValue(), bodyFat.doubleValue(), meas, target, notes);
                        progressDao.insert(pr);
                        CliTheme.printSuccess("Added ProgressRecord #" + pr.getId());
                    }
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer pid = input.readInt("Progress Record ID to delete: ", 1);
                    if (pid != null) {
                        ProgressRecord target = progressDao.findById(pid.intValue());
                        if (target == null) {
                            CliTheme.printValidationError("Progress record not found: #" + pid);
                        } else if (target.getCustomerId() != c.getId()) {
                            CliTheme.printValidationError("Progress record #" + pid
                                    + " belongs to another member and cannot be deleted here.");
                        } else if (input.readYesNo("Delete progress record #" + pid + "?")) {
                            progressDao.delete(pid.intValue());
                            CliTheme.printSuccess("Deleted ProgressRecord #" + pid);
                        }
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void printProgressTableAndChart(List<ProgressRecord> records) {
        String[] headers = {"ID", "Date", "Weight(kg)", "BodyFat%", "Target", "Notes"};
        int[] widths = {4, 10, 10, 8, 8, 22};
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            ProgressRecord r = records.get(i);
            rows.add(new String[]{
                String.valueOf(r.getId()),
                r.getDate(),
                String.valueOf(r.getWeightKg()),
                String.valueOf(r.getBodyFatPct()),
                r.getTargetWeightKg() != null ? String.valueOf(r.getTargetWeightKg()) : "-",
                r.getNotes()
            });
        }
        CliTheme.printTable(headers, rows, widths);
        if (!records.isEmpty()) {
            CliTheme.printSectionTitle("Weight Trend (ASCII)");
            for (int i = records.size() - 1; i >= 0; i--) {
                ProgressRecord r = records.get(i);
                int bars = (int) Math.max(1, Math.min(35, Math.round(r.getWeightKg() / 3.0)));
                System.out.println("  " + r.getDate() + " | " + CliTheme.repeat('#', bars) + " " + r.getWeightKg() + " kg");
            }
        }
    }

    private void printCustomerTable(List<Customer> list) throws DatabaseException {
        String[] headers = {"ID", "Full Name", "Status", "Phone", "TrainerId", "Email"};
        int[] widths = {4, 18, 10, 12, 9, 18};
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Customer c = list.get(i);
            rows.add(new String[]{
                String.valueOf(c.getId()),
                c.getFullName(),
                c.getStatus() != null ? c.getStatus().name() : "",
                c.getPhone(),
                c.getTrainerId() != null ? "#" + c.getTrainerId() : "None",
                c.getEmail()
            });
        }
        CliTheme.printTable(headers, rows, widths);
    }

    private Integer readOptionalTrainerId(Integer currentTrainerId) {
        try {
            List<Trainer> trainers = trainerDao.findAll();
            if (!trainers.isEmpty()) {
                System.out.print("  Available Trainers: ");
                for (int i = 0; i < trainers.size(); i++) {
                    Trainer t = trainers.get(i);
                    System.out.print("#" + t.getId() + "=" + t.getFullName());
                    if (i < trainers.size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println();
            }
        } catch (DatabaseException ignored) {
        }
        String hint = currentTrainerId != null ? String.valueOf(currentTrainerId) : "none";
        while (true) {
            String raw = input.readLine("Assigned Trainer ID (Enter for " + hint + ", or '-' for none): ");
            if (raw == null || raw.isEmpty()) {
                return currentTrainerId;
            }
            if ("-".equals(raw)) {
                return null;
            }
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Enter a valid numeric trainer ID, '-' for none, or Enter.");
            }
        }
    }
}

// ===== TrainerCliMenu =====

// Terminal screens for Coaches, Coach Profile, Availability Slots, and Trainer Subscriptions
class TrainerCliMenu {

    private final CliInput input;
    private final CliSession session;
    private final GymOperations ops = new GymOperations();
    private final TrainerDao trainerDao = new TrainerDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final TrainerAvailabilityDao availDao = new TrainerAvailabilityDao();
    private final TrainerSubscriptionDao subDao = new TrainerSubscriptionDao();

    public TrainerCliMenu(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openMenu() {
        while (!session.shouldStopSubMenu()) {
            CliTheme.printHeader("COACHES, AVAILABILITY SLOTS & SUBSCRIPTIONS",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu > Coaches");

            System.out.println("  [1] List All Coaches (with Rate & NO_SHOW Count)");
            System.out.println("  [2] Open Coach Profile (Roster / Medical Clearance / Delete Guard)");
            System.out.println("  [3] Add New Coach");
            System.out.println("  [4] Trainer Availability Slots");
            System.out.println("  [5] Trainer Subscriptions");
            CliTheme.printFooterNav(true);

            String choice = input.readLine("Select option > ");
            if (choice == null || "0".equals(choice)) {
                return;
            }
            if ("H".equalsIgnoreCase(choice)) {
                session.setReturnHome(true);
                return;
            }
            if ("L".equalsIgnoreCase(choice)) {
                session.setLogoutRequested(true);
                return;
            }

            if ("1".equals(choice)) {
                listTrainers();
                input.pause();
            } else if ("2".equals(choice)) {
                Integer id = input.readInt("Enter Coach ID (or 0 to cancel): ", 0);
                if (id != null && id.intValue() > 0) {
                    openTrainerProfile(id.intValue());
                }
            } else if ("3".equals(choice)) {
                addTrainer();
                input.pause();
            } else if ("4".equals(choice)) {
                openAvailabilityMenu();
            } else if ("5".equals(choice)) {
                openSubscriptionMenu();
            } else {
                System.out.println("[!] Unknown option: " + choice);
            }
        }
    }

    private void listTrainers() {
        try {
            List<Trainer> list = trainerDao.findAll();
            String[] headers = {"ID", "Coach Name", "Specialty", "Exp", "Rate", "NO_SHOWs"};
            int[] widths = {4, 20, 22, 4, 8, 8};
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Trainer t = list.get(i);
                int noShows = ops.countTrainerNoShows(t.getId());
                rows.add(new String[]{
                    String.valueOf(t.getId()),
                    t.getFullName(),
                    t.getSpecialization(),
                    t.getExperienceYears() + "y",
                    "$" + t.getHourlyRate().toPlainString(),
                    String.valueOf(noShows)
                });
            }
            CliTheme.printTable(headers, rows, widths);
        } catch (DatabaseException e) {
            CliTheme.printDatabaseError(e.getMessage());
        }
    }

    private void addTrainer() {
        CliTheme.printSectionTitle("Add New Coach");
        String name = input.readRawString("Full Name (required): ");
        if (name == null) {
            return;
        }
        String phone = input.readRawString("Phone (min 7 digits, or blank): ");
        String email = input.readRawString("Email (example: coach@gymos.local, or blank): ");
        String spec = input.readOptionalString("Specialization", "Strength & Conditioning");
        int exp = input.readIntOrDefault("Experience Years", 3);
        String certs = input.readOptionalString("Certifications", "NASM-CPT");
        String hireDate = input.readIsoDate("Hire Date", LocalDate.now().toString());
        BigDecimal rate = input.readBigDecimal("Hourly Rate (>=0)", new BigDecimal("50.00"));

        Trainer t = new Trainer(0, name, phone, email, spec, exp, certs, hireDate, rate);
        try {
            Trainer saved = trainerDao.insert(t);
            CliTheme.printSuccess("Added Coach #" + saved.getId() + ": " + saved.getFullName());
        } catch (ValidationException e) {
            CliTheme.printValidationError(e.getMessage());
        } catch (DatabaseException e) {
            CliTheme.printDatabaseError(e.getMessage());
        }
    }

    private void openTrainerProfile(int trainerId) {
        while (!session.shouldStopSubMenu()) {
            try {
                Trainer t = trainerDao.findById(trainerId);
                if (t == null) {
                    CliTheme.printValidationError("Coach #" + trainerId + " was not found.");
                    input.pause();
                    return;
                }
                List<Customer> roster = findAssignedCustomers(trainerId);
                int noShows = ops.countTrainerNoShows(trainerId);

                CliTheme.printHeader("COACH PROFILE (#" + t.getId() + ")",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Coaches > Profile #" + t.getId());

                System.out.println("  Name         : " + t.getFullName() + " | Hourly Rate: $" + t.getHourlyRate());
                System.out.println("  Specialty    : " + CliTheme.safeText(t.getSpecialization()) + " (" + t.getExperienceYears() + " yrs)");
                System.out.println("  Certs        : " + CliTheme.safeText(t.getCertifications()));
                System.out.println("  Email/Phone  : " + CliTheme.safeText(t.getEmail()) + " / " + CliTheme.safeText(t.getPhone()));
                System.out.println("  NO_SHOW Stat : " + noShows + " | Assigned Roster: " + roster.size() + " member(s)");

                if (!roster.isEmpty()) {
                    CliTheme.printSectionTitle("Assigned Members");
                    for (int i = 0; i < roster.size(); i++) {
                        Customer c = roster.get(i);
                        System.out.println("    - Member #" + c.getId() + ": " + c.getFullName() + " (" + c.getStatus() + ")");
                    }
                }

                CliTheme.printSingleLine();
                System.out.println("  [1] Edit Coach Details");
                System.out.println("  [2] Read Member Medical Profile (assigned trainer check)");
                System.out.println("  [3] Hard-Delete Coach [ADMIN Only + Ref Guard]");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    t.setFullName(input.readOptionalString("Full Name", t.getFullName()));
                    t.setSpecialization(input.readOptionalString("Specialization", t.getSpecialization()));
                    t.setHourlyRate(input.readBigDecimal("Hourly Rate", t.getHourlyRate()));
                    trainerDao.update(t);
                    CliTheme.printSuccess("Coach #" + t.getId() + " updated.");
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer cid = input.readInt("Enter Customer ID to inspect medical profile: ", 1);
                    if (cid != null) {
                        MedicalProfile mp = ops.getMedicalProfileForTrainer(t.getId(), cid.intValue());
                        CliTheme.printSuccess("Access GRANTED: "
                                + (mp != null ? "Blood=" + mp.getBloodType() + ", Conditions=" + mp.getConditions()
                                : "Assigned member has no medical profile yet."));
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    if (input.readYesNo("PERMANENTLY delete coach #" + t.getId() + " (" + t.getFullName() + ")?")) {
                        ops.hardDeleteTrainer(session.getCurrentUser(), t.getId());
                        CliTheme.printSuccess("Coach #" + t.getId() + " deleted.");
                        input.pause();
                        return;
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void openAvailabilityMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<TrainerAvailability> slots = availDao.findAll();
                CliTheme.printHeader("TRAINER AVAILABILITY SLOTS",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Coaches > Availability Slots");

                String[] headers = {"ID", "Coach", "Date", "Start", "End", "Booked", "Effective"};
                int[] widths = {4, 6, 10, 6, 6, 8, 12};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < slots.size(); i++) {
                    TrainerAvailability s = slots.get(i);
                    int booked = ops.countConfirmedInSlot(s.getId());
                    AvailabilityStatus eff = ops.effectiveSlotStatus(s);
                    rows.add(new String[]{
                        String.valueOf(s.getId()),
                        "#" + s.getTrainerId(),
                        s.getDate(),
                        s.getStartTime(),
                        s.getEndTime(),
                        booked + "/" + s.getMaxCustomers(),
                        eff.name()
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Publish New Availability Slot (validates startTime < endTime & max > 0)");
                System.out.println("  [2] Toggle OPEN / CLOSED Status");
                System.out.println("  [3] Update Slot Capacity (maxCustomers)");
                System.out.println("  [4] Delete Slot");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    Integer tid = input.readInt("Trainer ID: ", 1);
                    String date = input.readIsoDate("Slot Date", LocalDate.now().plusDays(1).toString());
                    String start = input.readIsoTime("Start Time", "10:00");
                    String end = input.readIsoTime("End Time", "11:00");
                    Integer maxCap = input.readInt("Max Customers (>=1): ", 0);
                    if (tid != null && date != null && start != null && end != null && maxCap != null) {
                        TrainerAvailability slot = availDao.insert(new TrainerAvailability(0, tid.intValue(),
                                date, start, end, maxCap.intValue(), AvailabilityStatus.OPEN));
                        CliTheme.printSuccess("Created Slot #" + slot.getId());
                    }
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer sid = input.readInt("Slot ID to toggle OPEN/CLOSED: ", 1);
                    if (sid != null) {
                        TrainerAvailability s = availDao.findById(sid.intValue());
                        if (s == null) {
                            CliTheme.printValidationError("Slot not found: #" + sid);
                        } else {
                            s.setStatus(s.getStatus() == AvailabilityStatus.CLOSED
                                    ? AvailabilityStatus.OPEN : AvailabilityStatus.CLOSED);
                            availDao.update(s);
                            CliTheme.printSuccess("Slot #" + s.getId() + " set to " + s.getStatus());
                        }
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer sid = input.readInt("Slot ID: ", 1);
                    Integer newMax = input.readInt("New Max Customers: ", 0);
                    if (sid != null && newMax != null) {
                        TrainerAvailability s = availDao.findById(sid.intValue());
                        if (s == null) {
                            CliTheme.printValidationError("Slot not found: #" + sid);
                        } else {
                            s.setMaxCustomers(newMax.intValue());
                            availDao.update(s);
                            CliTheme.printSuccess("Updated Slot #" + s.getId() + " capacity to " + newMax);
                        }
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    Integer sid = input.readInt("Slot ID to delete: ", 1);
                    if (sid != null) {
                        TrainerAvailability slot = availDao.findById(sid.intValue());
                        if (slot == null) {
                            CliTheme.printValidationError("Slot not found: #" + sid);
                        } else if (ops.countConfirmedInSlot(slot.getId()) > 0) {
                            CliTheme.printValidationError("Slot #" + sid + " still has CONFIRMED sessions. "
                                    + "Cancel or move them before deleting the slot.");
                        } else if (input.readYesNo("Delete slot #" + sid + "?")) {
                            availDao.delete(sid.intValue());
                            CliTheme.printSuccess("Deleted Slot #" + sid);
                        }
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void openSubscriptionMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<TrainerSubscription> list = subDao.findAll();
                CliTheme.printHeader("TRAINER SUBSCRIPTIONS",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Coaches > Subscriptions");

                String[] headers = {"ID", "Member", "Coach", "Start", "End", "Rate", "Status"};
                int[] widths = {4, 8, 8, 10, 10, 8, 10};
                List<String[]> rows = new ArrayList<>();
                String today = LocalDate.now().toString();
                for (int i = 0; i < list.size(); i++) {
                    TrainerSubscription s = list.get(i);
                    SubscriptionStatus eff = (s.getStatus() == SubscriptionStatus.ACTIVE
                            && s.getEndDate() != null && s.getEndDate().compareTo(today) < 0)
                            ? SubscriptionStatus.EXPIRED : s.getStatus();
                    rows.add(new String[]{
                        String.valueOf(s.getId()),
                        "#" + s.getCustomerId(),
                        "#" + s.getTrainerId(),
                        s.getStartDate(),
                        s.getEndDate(),
                        "$" + s.getRatePerPeriod().toPlainString(),
                        eff.name()
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Create Trainer Subscription (must match the member's assigned trainer)");
                System.out.println("  [2] Renew Subscription (+30 days)");
                System.out.println("  [3] Cancel Subscription");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    Integer cid = input.readInt("Customer ID: ", 1);
                    Integer tid = input.readInt("Trainer ID (must match member's assigned trainerId): ", 1);
                    String start = input.readIsoDate("Start Date", LocalDate.now().toString());
                    String end = input.readIsoDate("End Date", LocalDate.now().plusDays(30).toString());
                    BigDecimal rate = input.readBigDecimal("Rate Per Period", new BigDecimal("120.00"));
                    if (cid != null && tid != null && start != null && end != null && rate != null) {
                        TrainerSubscription sub = subDao.insert(new TrainerSubscription(0, cid.intValue(),
                                tid.intValue(), start, end, rate, SubscriptionStatus.ACTIVE));
                        CliTheme.printSuccess("Created Subscription #" + sub.getId());
                    }
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer subId = input.readInt("Subscription ID to renew: ", 1);
                    if (subId != null) {
                        TrainerSubscription s = subDao.findById(subId.intValue());
                        if (s == null) {
                            CliTheme.printValidationError("Subscription not found: #" + subId);
                        } else if (s.getStatus() == SubscriptionStatus.CANCELLED) {
                            CliTheme.printValidationError("Cancelled subscription cannot be renewed.");
                        } else {
                            LocalDate base;
                            try {
                                base = LocalDate.parse(s.getEndDate());
                            } catch (DateTimeParseException e) {
                                CliTheme.printValidationError("Subscription #" + s.getId()
                                        + " has an invalid stored end date: " + s.getEndDate());
                                base = null;
                            }
                            if (base != null) {
                                if (base.isBefore(LocalDate.now())) {
                                    base = LocalDate.now();
                                }
                                s.setEndDate(base.plusDays(30).toString());
                                s.setStatus(SubscriptionStatus.ACTIVE);
                                subDao.update(s);
                                CliTheme.printSuccess("Renewed Subscription #" + s.getId() + " until " + s.getEndDate());
                            }
                        }
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer subId = input.readInt("Subscription ID to cancel: ", 1);
                    if (subId != null) {
                        TrainerSubscription s = subDao.findById(subId.intValue());
                        if (s == null) {
                            CliTheme.printValidationError("Subscription not found: #" + subId);
                        } else if (s.getStatus() == SubscriptionStatus.CANCELLED) {
                            CliTheme.printValidationError("Subscription #" + subId + " is already CANCELLED.");
                        } else if (input.readYesNo("Cancel subscription #" + subId + "?")) {
                            s.setStatus(SubscriptionStatus.CANCELLED);
                            subDao.update(s);
                            CliTheme.printSuccess("Cancelled Subscription #" + s.getId());
                        }
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private List<Customer> findAssignedCustomers(int trainerId) throws DatabaseException {
        List<Customer> all = customerDao.findAll();
        List<Customer> assigned = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Customer c = all.get(i);
            if (c.getTrainerId() != null && c.getTrainerId().intValue() == trainerId) {
                assigned.add(c);
            }
        }
        return assigned;
    }
}

// ===== MembershipBillingCliMenu =====

// Terminal screens for Memberships and Payments / Billing Ledger
class MembershipBillingCliMenu {

    private final CliInput input;
    private final CliSession session;
    private final GymOperations ops = new GymOperations();
    private final MembershipDao membershipDao = new MembershipDao();
    private final PaymentDao paymentDao = new PaymentDao();

    public MembershipBillingCliMenu(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openMembershipsMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Membership> list = membershipDao.findAll();
                CliTheme.printHeader("MEMBERSHIPS & PLAN LIFECYCLE",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Memberships");

                String today = LocalDate.now().toString();
                String[] headers = {"ID", "Member", "Plan", "Price", "Start", "End", "Effective", "Frozen"};
                int[] widths = {4, 7, 10, 8, 10, 10, 10, 6};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Membership m = list.get(i);
                    MembershipStatus eff = ops.effectiveMembershipStatus(m, today);
                    rows.add(new String[]{
                        String.valueOf(m.getId()),
                        "#" + m.getCustomerId(),
                        m.getPlan().name(),
                        "$" + m.getPrice().toPlainString(),
                        m.getStartDate(),
                        m.getEndDate(),
                        eff.name(),
                        String.valueOf(m.getFrozenDays())
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Sell New Membership Plan");
                System.out.println("  [2] Freeze ACTIVE Membership");
                System.out.println("  [3] Unfreeze FROZEN Membership -> Shifts endDate");
                System.out.println("  [4] Renew Membership -> Refuses CANCELLED");
                System.out.println("  [5] Cancel Membership");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    sellMembership();
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to freeze: ", 1);
                    Integer days = input.readInt("Freeze days (>0): ", 0);
                    if (mid != null && days != null) {
                        ops.freezeMembership(mid.intValue(), days.intValue());
                        CliTheme.printSuccess("Membership #" + mid + " frozen for " + days + " day(s).");
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to unfreeze: ", 1);
                    if (mid != null) {
                        Membership updated = ops.unfreezeMembership(mid.intValue());
                        CliTheme.printSuccess("Unfrozen Membership #" + updated.getId()
                                + " - new endDate: " + updated.getEndDate());
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to renew: ", 1);
                    if (mid != null) {
                        Membership renewed = ops.renewMembership(mid.intValue());
                        CliTheme.printSuccess("Renewed Membership #" + renewed.getId()
                                + " - new endDate: " + renewed.getEndDate());
                    }
                    input.pause();
                } else if ("5".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to cancel: ", 1);
                    if (mid != null) {
                        Membership m = membershipDao.findById(mid.intValue());
                        if (m == null) {
                            CliTheme.printValidationError("Membership not found: #" + mid);
                        } else if (m.getStatus() == MembershipStatus.CANCELLED) {
                            CliTheme.printValidationError("Membership #" + mid + " is already CANCELLED.");
                        } else if (input.readYesNo("Cancel membership #" + mid + "? (a cancelled plan can never be renewed)")) {
                            m.setStatus(MembershipStatus.CANCELLED);
                            membershipDao.update(m);
                            CliTheme.printSuccess("Cancelled Membership #" + m.getId());
                        }
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    public void openPaymentsMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Payment> list = paymentDao.findAll();
                CliTheme.printHeader("PAYMENTS & POS BILLING LEDGER",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Payments");

                String[] headers = {"ID", "Cust", "Target", "Amount", "Method", "Date", "Receipt", "Status"};
                int[] widths = {4, 5, 8, 8, 8, 10, 10, 6};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Payment p = list.get(i);
                    String target = "-";
                    if (p.getMembershipId() != null) {
                        target = "MEM#" + p.getMembershipId();
                    } else if (p.getSubscriptionId() != null) {
                        target = "SUB#" + p.getSubscriptionId();
                    } else if (p.getSessionId() != null) {
                        target = "SES#" + p.getSessionId();
                    }
                    rows.add(new String[]{
                        String.valueOf(p.getId()),
                        "#" + p.getCustomerId(),
                        target,
                        "$" + p.getAmount().toPlainString(),
                        p.getMethod().name(),
                        p.getDate(),
                        p.getReceiptNo(),
                        p.isVoided() ? "VOID" : "PAID"
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Record Payment (one target link + customer match + unique receipt)");
                System.out.println("  [2] Record Payment with Manual Links (every insert is validated)");
                System.out.println("  [3] Customer Outstanding Balance Breakdown");
                System.out.println("  [4] Lookup Receipt Number");
                System.out.println("  [5] Void & Issue Replacement Payment");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    recordValidPayment();
                    input.pause();
                } else if ("2".equals(choice)) {
                    recordManualPayment();
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer cid = input.readInt("Enter Customer ID: ", 1);
                    if (cid != null) {
                        BigDecimal balance = ops.computeOutstandingBalance(cid.intValue());
                        CliTheme.printSuccess("Outstanding Balance for Customer #" + cid
                                + " = $" + balance.toPlainString());
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    String rec = input.readRequiredString("Receipt Number (e.g. REC-1002): ");
                    if (rec != null) {
                        Payment found = paymentDao.findByReceiptNo(rec);
                        if (found == null) {
                            CliTheme.printValidationError("No payment found for receipt: " + rec);
                        } else {
                            CliTheme.printSuccess(found.toString()
                                    + (found.isVoided() ? " | Void Reason: " + found.getVoidReason() : ""));
                        }
                    }
                    input.pause();
                } else if ("5".equals(choice)) {
                    voidAndReplaceFlow();
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void sellMembership() throws DatabaseException, ValidationException {
        Integer cid = input.readInt("Customer ID: ", 1);
        if (cid == null) {
            return;
        }
        System.out.println("  Plan Types: [1] MONTHLY ($60 / 30d)  [2] QUARTERLY ($150 / 90d)  [3] YEARLY ($500 / 365d)");
        PlanType plan;
        BigDecimal defPrice;
        // Keep asking until the user picks a real plan (no silent MONTHLY default)
        while (true) {
            String pChoice = input.readLine("Select Plan Type [1-3]: ");
            if (pChoice == null) {
                return;
            }
            if ("1".equals(pChoice)) {
                plan = PlanType.MONTHLY;
                defPrice = new BigDecimal("60.00");
                break;
            }
            if ("2".equals(pChoice)) {
                plan = PlanType.QUARTERLY;
                defPrice = new BigDecimal("150.00");
                break;
            }
            if ("3".equals(pChoice)) {
                plan = PlanType.YEARLY;
                defPrice = new BigDecimal("500.00");
                break;
            }
            System.out.println("[!] Enter 1, 2 or 3.");
        }
        BigDecimal price = input.readBigDecimal("Price", defPrice);
        String start = input.readIsoDate("Start Date", LocalDate.now().toString());
        if (start == null || price == null) {
            return;
        }
        String suggestedEnd = LocalDate.parse(start).plusDays(ops.daysForPlan(plan)).toString();
        String end = input.readIsoDate("End Date", suggestedEnd);
        if (end == null) {
            return;
        }
        Membership m = membershipDao.insert(new Membership(0, cid.intValue(), plan, price,
                start, end, MembershipStatus.ACTIVE, 0));
        CliTheme.printSuccess("Sold Membership #" + m.getId() + " (" + plan + ") for Customer #" + cid);
    }

    private void recordValidPayment() throws DatabaseException, ValidationException {
        Integer cid = input.readInt("Customer ID: ", 1);
        if (cid == null) {
            return;
        }
        System.out.println("  Link Target (exactly one): [1] Membership  [2] TrainerSubscription  [3] Session");
        // Keep asking until the user picks a real target type (no silent Membership default)
        String targetType;
        while (true) {
            targetType = input.readLine("Target Type [1-3]: ");
            if (targetType == null) {
                return;
            }
            if ("1".equals(targetType) || "2".equals(targetType) || "3".equals(targetType)) {
                break;
            }
            System.out.println("[!] Enter 1, 2 or 3.");
        }
        Integer targetId = input.readInt("Target Record ID: ", 1);
        BigDecimal amount = input.readBigDecimal("Amount (>0)", new BigDecimal("45.00"));
        PaymentMethod method = readPaymentMethod();
        String receipt = input.readRawString("Unique Receipt No (e.g. REC-" + System.currentTimeMillis() % 10000 + "): ");
        if (targetId == null || amount == null || receipt == null) {
            return;
        }
        String today = LocalDate.now().toString();
        Payment p;
        if ("2".equals(targetType)) {
            p = Payment.forSubscription(cid.intValue(), targetId.intValue(), amount, method, today, receipt);
        } else if ("3".equals(targetType)) {
            p = Payment.forSession(cid.intValue(), targetId.intValue(), amount, method, today, receipt);
        } else {
            p = Payment.forMembership(cid.intValue(), targetId.intValue(), amount, method, today, receipt);
        }
        Payment saved = paymentDao.insert(p);
        CliTheme.printSuccess("Recorded Payment #" + saved.getId() + " Receipt=" + saved.getReceiptNo());
    }

    private void recordManualPayment() throws DatabaseException, ValidationException {
        CliTheme.printSectionTitle("Record Payment (Manual Links - validated insert)");
        Integer cid = input.readInt("Customer ID: ", 1);
        Integer memId = readNullableInt("Membership ID (Enter for null): ");
        Integer subId = readNullableInt("Subscription ID (Enter for null): ");
        Integer sesId = readNullableInt("Session ID (Enter for null): ");
        BigDecimal amount = input.readBigDecimal("Amount", new BigDecimal("50.00"));
        String receipt = input.readRawString("Receipt No: ");
        if (cid == null || amount == null) {
            return;
        }
        Payment p = new Payment(0, cid.intValue(), memId, subId, sesId,
                amount, PaymentMethod.CARD, LocalDate.now().toString(), receipt, false);
        Payment saved = paymentDao.insert(p);
        CliTheme.printSuccess("Inserted Payment #" + saved.getId());
    }

    private void voidAndReplaceFlow() throws DatabaseException, ValidationException {
        Integer pid = input.readInt("Payment ID to void: ", 1);
        if (pid == null) {
            return;
        }
        String reason = input.readRawString("Void Reason (required): ");
        boolean replace = input.readYesNo("Issue replacement payment now?");
        BigDecimal newAmt = null;
        PaymentMethod newMethod = PaymentMethod.CARD;
        String newReceipt = null;
        if (replace) {
            newAmt = input.readBigDecimal("Replacement Amount", new BigDecimal("50.00"));
            newMethod = readPaymentMethod();
            // Receipt is required when a replacement was promised, so the success
            // message below always means a replacement payment was really created
            newReceipt = input.readRequiredString("New Unique Receipt No: ");
        }
        Payment res = ops.voidAndReplacePayment(pid.intValue(), reason, LocalDate.now().toString(),
                newAmt, newMethod, newReceipt);
        CliTheme.printSuccess("Void completed. Updated/Created Payment #" + res.getId()
                + " (Receipt " + res.getReceiptNo() + ")");
    }

    private PaymentMethod readPaymentMethod() {
        System.out.println("  Method: [1] CASH  [2] CARD  [3] TRANSFER");
        // Keep asking until the user picks a real method (no silent CASH default)
        while (true) {
            String m = input.readLine("Select [1-3]: ");
            if (m == null) {
                return null;
            }
            if ("1".equals(m)) {
                return PaymentMethod.CASH;
            }
            if ("2".equals(m)) {
                return PaymentMethod.CARD;
            }
            if ("3".equals(m)) {
                return PaymentMethod.TRANSFER;
            }
            System.out.println("[!] Enter 1, 2 or 3.");
        }
    }

    private Integer readNullableInt(String prompt) {
        while (true) {
            String raw = input.readLine(prompt);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Enter digits or press Enter for null.");
            }
        }
    }
}

// ===== ScheduleWorkoutCliMenu =====

// Terminal screens for Training Schedule / Sessions and Workouts + Live HUD
class ScheduleWorkoutCliMenu {

    private final CliInput input;
    private final CliSession session;
    private final GymOperations ops = new GymOperations();
    private final SessionDao sessionDao = new SessionDao();
    private final TrainerAvailabilityDao availDao = new TrainerAvailabilityDao();
    private final WorkoutProgramDao workoutDao = new WorkoutProgramDao();
    private final EquipmentDao equipmentDao = new EquipmentDao();

    public ScheduleWorkoutCliMenu(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openScheduleMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Session> list = sessionDao.findAll();
                CliTheme.printHeader("TRAINING SCHEDULE & SESSIONS",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Schedule");

                String[] headers = {"ID", "Cust", "Coach", "Date", "Time", "Dur", "Status", "Slot", "Price"};
                int[] widths = {4, 5, 5, 10, 5, 4, 10, 5, 7};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Session s = list.get(i);
                    rows.add(new String[]{
                        String.valueOf(s.getId()),
                        "#" + s.getCustomerId(),
                        "#" + s.getTrainerId(),
                        s.getDate(),
                        s.getStartTime(),
                        s.getDurationMin() + "m",
                        s.getStatus().name(),
                        s.getAvailabilityId() != null ? "#" + s.getAvailabilityId() : "-",
                        "$" + (s.getPrice() != null ? s.getPrice().toPlainString() : "0")
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Book Published OPEN Slot -> CONFIRMED");
                System.out.println("  [2] Request Session Outside Slots -> REQUESTED");
                System.out.println("  [3] Admin Override Reserve -> CONFIRMED [ADMIN Only]");
                System.out.println("  [4] Confirm REQUESTED Session");
                System.out.println("  [5] Complete CONFIRMED Session");
                System.out.println("  [6] Mark CONFIRMED Session as NO_SHOW");
                System.out.println("  [7] Cancel CONFIRMED Session (2h Notice Check)");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    bookOpenSlotFlow();
                    input.pause();
                } else if ("2".equals(choice)) {
                    requestOutsideSlotFlow();
                    input.pause();
                } else if ("3".equals(choice)) {
                    adminReserveFlow();
                    input.pause();
                } else if ("4".equals(choice)) {
                    Integer sid = input.readInt("Session ID to confirm: ", 1);
                    if (sid != null) {
                        ops.confirmSession(sid.intValue());
                        CliTheme.printSuccess("Session #" + sid + " transitioned to CONFIRMED.");
                    }
                    input.pause();
                } else if ("5".equals(choice)) {
                    Integer sid = input.readInt("Session ID to mark COMPLETED: ", 1);
                    if (sid != null) {
                        ops.completeSession(sid.intValue());
                        CliTheme.printSuccess("Session #" + sid + " transitioned to COMPLETED.");
                    }
                    input.pause();
                } else if ("6".equals(choice)) {
                    Integer sid = input.readInt("Session ID to mark NO_SHOW: ", 1);
                    if (sid != null) {
                        ops.markNoShow(sid.intValue());
                        CliTheme.printSuccess("Session #" + sid + " marked NO_SHOW.");
                    }
                    input.pause();
                } else if ("7".equals(choice)) {
                    Integer sid = input.readInt("Session ID to cancel: ", 1);
                    if (sid != null) {
                        String reason = input.readRawString("Cancellation Reason (required): ");
                        String resultMsg = ops.cancelSession(sid.intValue(), reason);
                        CliTheme.printSuccess(resultMsg);
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    public void openWorkoutsMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<WorkoutProgram> list = workoutDao.findAll();
                CliTheme.printHeader("WORKOUT PROGRAMS & LIVE WORKOUT HUD",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Workouts & HUD");

                String[] headers = {"ID", "Cust", "Coach", "Program Name", "Ver", "Date"};
                int[] widths = {4, 5, 5, 24, 4, 10};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    WorkoutProgram w = list.get(i);
                    rows.add(new String[]{
                        String.valueOf(w.getId()),
                        "#" + w.getCustomerId(),
                        "#" + w.getCreatorTrainerId(),
                        w.getName(),
                        "v" + w.getVersion(),
                        w.getCreatedDate()
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Create New Workout Program");
                System.out.println("  [2] Publish Next Version (+1) for Existing Program");
                System.out.println("  [3] View Exercises & Reassign Creator Coach");
                System.out.println("  [4] Delete Workout Program");
                System.out.println("  [5] Launch Terminal Live Workout HUD Simulator");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    Integer cid = input.readInt("Customer ID: ", 1);
                    Integer tid = input.readInt("Creator Trainer ID: ", 1);
                    String name = input.readRawString("Program Name (required): ");
                    String ex = input.readOptionalString("Exercises", "Squat 4x8, Bench 4x8, Row 4x10");
                    int version = input.readIntOrDefault("Version (>=1)", 1);
                    if (cid != null && tid != null) {
                        WorkoutProgram created = workoutDao.insert(new WorkoutProgram(0, cid.intValue(),
                                tid.intValue(), name, ex, version, LocalDate.now().toString()));
                        CliTheme.printSuccess("Created WorkoutProgram #" + created.getId() + " v" + created.getVersion());
                    }
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer wid = input.readInt("Base Workout Program ID: ", 1);
                    if (wid != null) {
                        WorkoutProgram base = workoutDao.findById(wid.intValue());
                        if (base == null) {
                            CliTheme.printValidationError("Workout program not found: #" + wid);
                        } else {
                            int maxVer = findHighestVersion(base.getCustomerId());
                            String updatedEx = input.readOptionalString("Updated Exercises", base.getExercises());
                            WorkoutProgram next = workoutDao.insert(new WorkoutProgram(0, base.getCustomerId(),
                                    base.getCreatorTrainerId(), base.getName(), updatedEx,
                                    maxVer + 1, LocalDate.now().toString()));
                            CliTheme.printSuccess("Published " + next.getName() + " v" + next.getVersion());
                        }
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer wid = input.readInt("Workout Program ID: ", 1);
                    if (wid != null) {
                        WorkoutProgram w = workoutDao.findById(wid.intValue());
                        if (w == null) {
                            CliTheme.printValidationError("Workout program not found: #" + wid);
                        } else {
                            System.out.println("  Name      : " + w.getName() + " (v" + w.getVersion() + ")");
                            System.out.println("  Exercises : " + w.getExercises());
                            if (input.readYesNo("Reassign creator coach?")) {
                                Integer newCoach = input.readInt("New Creator Trainer ID: ", 1);
                                if (newCoach != null) {
                                    w.setCreatorTrainerId(newCoach.intValue());
                                    workoutDao.update(w);
                                    CliTheme.printSuccess("Reassigned creator coach to #" + newCoach);
                                }
                            }
                        }
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    Integer wid = input.readInt("Workout Program ID to delete: ", 1);
                    if (wid != null) {
                        WorkoutProgram target = workoutDao.findById(wid.intValue());
                        if (target == null) {
                            CliTheme.printValidationError("Workout program not found: #" + wid);
                        } else if (input.readYesNo("Delete '" + target.getName() + "' v" + target.getVersion() + "?")) {
                            workoutDao.delete(wid.intValue());
                            CliTheme.printSuccess("Deleted WorkoutProgram #" + wid);
                        }
                    }
                    input.pause();
                } else if ("5".equals(choice)) {
                    runLiveWorkoutHudSimulator();
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void bookOpenSlotFlow() throws DatabaseException, ValidationException {
        List<TrainerAvailability> slots = availDao.findAll();
        CliTheme.printSectionTitle("Available Coach Slots");
        for (int i = 0; i < slots.size(); i++) {
            TrainerAvailability s = slots.get(i);
            int booked = ops.countConfirmedInSlot(s.getId());
            System.out.println("  Slot #" + s.getId() + " | Coach #" + s.getTrainerId() + " | "
                    + s.getDate() + " " + s.getStartTime() + "-" + s.getEndTime()
                    + " | Occupancy: " + booked + "/" + s.getMaxCustomers()
                    + " [" + ops.effectiveSlotStatus(s) + "]");
        }
        Integer cid = input.readInt("Customer ID: ", 1);
        Integer slotId = input.readInt("Availability Slot ID: ", 1);
        Integer subId = readNullableInt("TrainerSubscription ID (Enter for pay-per-session, or ID for free): ");
        BigDecimal price = BigDecimal.ZERO;
        if (subId == null) {
            price = input.readBigDecimal("Session Price", new BigDecimal("45.00"));
        } else {
            price = input.readBigDecimal("Session Price (0 if covered by subscription)", BigDecimal.ZERO);
        }
        String notes = input.readOptionalString("Notes", "Open slot booking");
        if (cid != null && slotId != null) {
            Session saved = ops.bookOpenSlot(cid.intValue(), slotId.intValue(), 60, subId, price, notes);
            CliTheme.printSuccess("Booked CONFIRMED Session #" + saved.getId() + " in Slot #" + slotId);
        }
    }

    private void requestOutsideSlotFlow() throws DatabaseException, ValidationException {
        Integer cid = input.readInt("Customer ID: ", 1);
        Integer tid = input.readInt("Trainer ID: ", 1);
        String date = input.readIsoDate("Date", LocalDate.now().plusDays(2).toString());
        String time = input.readIsoTime("Start Time", "17:00");
        int duration = input.readIntOrDefault("Duration min (0 -> defaults to 60)", 60);
        BigDecimal price = input.readBigDecimal("Price", new BigDecimal("45.00"));
        String notes = input.readOptionalString("Notes", "Requested outside published slots");
        if (cid != null && tid != null && date != null && time != null) {
            Session saved = ops.requestOutsideSlots(cid.intValue(), tid.intValue(),
                    date, time, duration, null, price, notes);
            CliTheme.printSuccess("Requested Session #" + saved.getId() + " (status=REQUESTED, dur="
                    + saved.getDurationMin() + "m)");
        }
    }

    private void adminReserveFlow() throws DatabaseException, ValidationException {
        Integer cid = input.readInt("Customer ID: ", 1);
        Integer tid = input.readInt("Trainer ID: ", 1);
        String date = input.readIsoDate("Date", LocalDate.now().plusDays(1).toString());
        String time = input.readIsoTime("Start Time", "20:00");
        Integer slotId = readNullableInt("Optional Slot ID (Enter for null - admin override ignores cap): ");
        BigDecimal price = input.readBigDecimal("Price", new BigDecimal("55.00"));
        String notes = input.readOptionalString("Notes", "VIP Admin Override");
        if (cid != null && tid != null && date != null && time != null) {
            Session saved = ops.adminReserve(session.getCurrentUser(), cid.intValue(), tid.intValue(),
                    date, time, 60, slotId, price, notes);
            CliTheme.printSuccess("Admin Override created CONFIRMED Session #" + saved.getId());
        }
    }

    private void runLiveWorkoutHudSimulator() throws DatabaseException {
        Integer wid = input.readInt("Select Workout Program ID to run in HUD: ", 1);
        if (wid == null) {
            return;
        }
        WorkoutProgram wp = workoutDao.findById(wid.intValue());
        if (wp == null) {
            CliTheme.printValidationError("Workout program not found: #" + wid);
            return;
        }
        List<Equipment> gear = equipmentDao.findAll();
        CliTheme.printHeader("LIVE WORKOUT HUD SIMULATOR",
                session.userLabel(), DatabaseConnection.getDbUrl(),
                "Main Menu > Workouts > Live HUD");
        System.out.println("  Member #" + wp.getCustomerId() + " | Routine: " + wp.getName() + " (v" + wp.getVersion() + ")");
        System.out.println("  Available Equipment Units: " + gear.size());
        CliTheme.printSingleLine();

        String rawEx = wp.getExercises() != null ? wp.getExercises() : "Warmup 10m";
        String[] blocks = rawEx.split(",");
        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i].trim();
            int pct = ((i + 1) * 100) / blocks.length;
            System.out.println("  [HUD Block " + (i + 1) + "/" + blocks.length + "] " + block
                    + "   | Target HR: Zone 3 (145 bpm) | Progress: " + pct + "%");
            if (!input.readYesNo("  Mark exercise set complete?")) {
                System.out.println("  Workout paused at block " + (i + 1) + ".");
                return;
            }
        }
        CliTheme.printSuccess("Live Workout HUD completed 100% of blocks for Member #" + wp.getCustomerId() + "!");
    }

    private int findHighestVersion(int customerId) throws DatabaseException {
        List<WorkoutProgram> list = workoutDao.findByCustomerId(customerId);
        int max = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getVersion() > max) {
                max = list.get(i).getVersion();
            }
        }
        return max;
    }

    private Integer readNullableInt(String prompt) {
        while (true) {
            String raw = input.readLine(prompt);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Enter digits or press Enter for none.");
            }
        }
    }
}

// ===== FacilityAdminCliMenu =====

// Terminal screens for Attendance, Equipment & Floor Map,
// Staff/Admin Controls, and Serialization/DB Tools
class FacilityAdminCliMenu {

    private static final String DEFAULT_BACKUP_PATH = "backups" + File.separator + "gym_backup.ser";

    private final CliInput input;
    private final CliSession session;
    private final GymOperations ops = new GymOperations();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final AdministratorDao adminDao = new AdministratorDao();

    public FacilityAdminCliMenu(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openAttendanceMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Attendance> list = attendanceDao.findAll();
                CliTheme.printHeader("ATTENDANCE - FAST CHECK-IN / CHECK-OUT",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Attendance");

                String[] headers = {"ID", "Member", "Date", "Check-In", "Check-Out"};
                int[] widths = {4, 8, 12, 10, 10};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Attendance a = list.get(i);
                    rows.add(new String[]{
                        String.valueOf(a.getId()),
                        "#" + a.getCustomerId(),
                        a.getDate(),
                        a.getCheckIn(),
                        a.getCheckOut() != null ? a.getCheckOut() : "-"
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Fast Check-In (ACTIVE member + unexpired plan; reuses today's row)");
                System.out.println("  [2] Check-Out Member");
                System.out.println("  [3] Daily Attendance Report (by Date yyyy-MM-dd)");
                System.out.println("  [4] Correct Attendance Record Time (correct, never hard-delete)");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                String today = LocalDate.now().toString();
                String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                if ("1".equals(choice)) {
                    Integer cid = input.readInt("Member ID to check in: ", 1);
                    String time = input.readIsoTime("Check-In Time", nowTime);
                    if (cid != null && time != null) {
                        Attendance saved = ops.checkIn(cid.intValue(), today, time);
                        CliTheme.printSuccess("Checked In Customer #" + cid + " (Attendance row #" + saved.getId() + ")");
                    }
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer cid = input.readInt("Member ID to check out: ", 1);
                    String time = input.readIsoTime("Check-Out Time", nowTime);
                    if (cid != null && time != null) {
                        Attendance updated = ops.checkOut(cid.intValue(), today, time);
                        CliTheme.printSuccess("Checked Out Customer #" + cid + " at " + updated.getCheckOut());
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    String date = input.readIsoDate("Report Date", today);
                    if (date != null) {
                        int count = 0;
                        for (int i = 0; i < list.size(); i++) {
                            Attendance a = list.get(i);
                            if (date.equals(a.getDate())) {
                                count++;
                                System.out.println("  - Member #" + a.getCustomerId()
                                        + " IN=" + a.getCheckIn()
                                        + " OUT=" + (a.getCheckOut() != null ? a.getCheckOut() : "Still inside"));
                            }
                        }
                        CliTheme.printSuccess("Total check-ins on " + date + ": " + count);
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    Integer aid = input.readInt("Attendance ID to correct: ", 1);
                    if (aid != null) {
                        Attendance a = attendanceDao.findById(aid.intValue());
                        if (a == null) {
                            CliTheme.printValidationError("Attendance record not found: #" + aid);
                        } else {
                            a.setCheckIn(input.readIsoTime("Corrected Check-In", a.getCheckIn()));
                            String out = input.readOptionalString("Corrected Check-Out (Enter to keep, '-' to clear)",
                                    a.getCheckOut());
                            if ("-".equals(out) || (out != null && out.isEmpty())) {
                                out = null;
                            }
                            a.setCheckOut(out);
                            attendanceDao.update(a);
                            CliTheme.printSuccess("Corrected Attendance #" + a.getId());
                        }
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    public void openEquipmentMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Equipment> list = equipmentDao.findAll();
                CliTheme.printHeader("EQUIPMENT INVENTORY & ASCII FLOOR MAP",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Equipment");

                String[] headers = {"ID", "Equipment Name", "Zone/Cat", "Condition", "Status", "Last Maint"};
                int[] widths = {4, 24, 12, 9, 14, 10};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Equipment eq = list.get(i);
                    rows.add(new String[]{
                        String.valueOf(eq.getId()),
                        eq.getName(),
                        eq.getCategory(),
                        eq.getCondition().name(),
                        eq.getStatus().name(),
                        eq.getLastMaintenance() != null ? eq.getLastMaintenance() : "-"
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Add Equipment");
                System.out.println("  [2] Change Status / Condition / Log Maintenance Date");
                System.out.println("  [3] Delete Equipment");
                System.out.println("  [4] Open Facility Floor Zone Map (ASCII Blueprint View)");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    String name = input.readRawString("Equipment Name (required): ");
                    String cat = input.readOptionalString("Zone / Category (CARDIO/STRENGTH/FREE WEIGHTS/RECOVERY)", "CARDIO");
                    String pDate = input.readIsoDate("Purchase Date", LocalDate.now().toString());
                    String mDate = input.readIsoDate("Last Maintenance Date", pDate);
                    Equipment eq = equipmentDao.insert(new Equipment(0, name, cat, pDate,
                            EquipmentCondition.NEW, EquipmentStatus.AVAILABLE, mDate));
                    CliTheme.printSuccess("Added Equipment #" + eq.getId());
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer eid = input.readInt("Equipment ID: ", 1);
                    if (eid != null) {
                        Equipment eq = equipmentDao.findById(eid.intValue());
                        if (eq == null) {
                            CliTheme.printValidationError("Equipment not found: #" + eid);
                        } else {
                            eq.setStatus(readEquipmentStatus(eq.getStatus()));
                            eq.setCondition(readEquipmentCondition(eq.getCondition()));
                            eq.setLastMaintenance(input.readIsoDate("Last Maintenance Date", LocalDate.now().toString()));
                            equipmentDao.update(eq);
                            CliTheme.printSuccess("Updated Equipment #" + eq.getId());
                        }
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer eid = input.readInt("Equipment ID to delete: ", 1);
                    if (eid != null) {
                        Equipment target = equipmentDao.findById(eid.intValue());
                        if (target == null) {
                            CliTheme.printValidationError("Equipment not found: #" + eid);
                        } else if (input.readYesNo("Delete equipment #" + eid + " (" + target.getName() + ")?")) {
                            equipmentDao.delete(eid.intValue());
                            CliTheme.printSuccess("Deleted Equipment #" + eid);
                        }
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    renderFloorMap(list);
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    public void openStaffAdminMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Administrator> list = adminDao.findAll();
                CliTheme.printHeader("STAFF & ADMIN CONTROLS (Role-Based Access)",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Staff & Admin");

                String[] headers = {"ID", "Full Name", "Username", "Role", "Active", "Created"};
                int[] widths = {4, 20, 12, 9, 7, 10};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Administrator a = list.get(i);
                    rows.add(new String[]{
                        String.valueOf(a.getId()),
                        a.getFullName(),
                        a.getUsername(),
                        a.getRole().name(),
                        a.isActive() ? "YES" : "NO",
                        a.getCreatedDate()
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Add Staff Account [ADMIN Only]");
                System.out.println("  [2] Activate / Deactivate Account [ADMIN Only]");
                System.out.println("  [3] Reset Staff Password / Role [ADMIN Only]");
                System.out.println("  [4] Delete Staff Account [ADMIN Only]");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                ops.requireAdmin(session.getCurrentUser());

                if ("1".equals(choice)) {
                    String name = input.readRawString("Full Name (required): ");
                    String user = input.readRawString("Username (>=3 chars, no spaces): ");
                    String pass = input.readRawString("Password (required): ");
                    if (name == null || user == null || pass == null) {
                        return;
                    }
                    StaffRole role = readStaffRole(StaffRole.STAFF);
                    String defaultEmail = user.isEmpty() ? "" : user + "@gymos.local";
                    String email = input.readOptionalString("Email", defaultEmail);
                    Administrator saved = adminDao.insert(new Administrator(0, name, user, pass,
                            role, "01000000000", email, true, LocalDate.now().toString()));
                    CliTheme.printSuccess("Created Staff Account #" + saved.getId() + " (" + saved.getUsername() + ")");
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer aid = input.readInt("Staff ID to toggle active flag: ", 1);
                    if (aid != null) {
                        Administrator target = adminDao.findById(aid.intValue());
                        if (target == null) {
                            CliTheme.printValidationError("Staff not found: #" + aid);
                        } else if (target.getId() == session.getCurrentUser().getId()) {
                            CliTheme.printValidationError("Cannot deactivate the currently logged-in admin account.");
                        } else {
                            target.setActive(!target.isActive());
                            adminDao.update(target);
                            CliTheme.printSuccess("Staff '" + target.getUsername() + "' active=" + target.isActive());
                        }
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer aid = input.readInt("Staff ID to edit: ", 1);
                    if (aid != null) {
                        Administrator target = adminDao.findById(aid.intValue());
                        if (target == null) {
                            CliTheme.printValidationError("Staff not found: #" + aid);
                        } else {
                            target.setPasswordHash(input.readOptionalString("New Password", target.getPasswordHash()));
                            StaffRole newRole = readStaffRole(target.getRole());
                            if (target.getId() == session.getCurrentUser().getId() && newRole != StaffRole.ADMIN) {
                                CliTheme.printValidationError("You cannot change your own role away from ADMIN "
                                        + "while logged in. Ask another ADMIN to do it.");
                            } else {
                                target.setRole(newRole);
                                adminDao.update(target);
                                CliTheme.printSuccess("Updated Staff #" + target.getId());
                            }
                        }
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    Integer aid = input.readInt("Staff ID to delete: ", 1);
                    if (aid != null) {
                        if (aid.intValue() == session.getCurrentUser().getId()) {
                            CliTheme.printValidationError("Cannot delete the currently logged-in admin account.");
                        } else if (adminDao.findById(aid.intValue()) == null) {
                            CliTheme.printValidationError("Staff not found: #" + aid);
                        } else if (input.readYesNo("PERMANENTLY delete staff account #" + aid + "?")) {
                            adminDao.delete(aid.intValue());
                            CliTheme.printSuccess("Deleted Staff Account #" + aid);
                        }
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    public void openBackupAndDbMenu() {
        while (!session.shouldStopSubMenu()) {
            CliTheme.printHeader("SERIALIZATION (.SER BACKUP) & DATABASE TOOLS",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu > Backup & DB");

            System.out.println("  Active SQLite URL : " + DatabaseConnection.getDbUrl());
            System.out.println("  Default .ser Path : " + DEFAULT_BACKUP_PATH);
            CliTheme.printSingleLine();
            System.out.println("  [1] Export Full 13-Table Binary Snapshot (.ser via SerializationUtil)");
            System.out.println("  [2] Load & Inspect Existing .ser Snapshot (SerializationUtil.loadFromFile)");
            System.out.println("  [3] Simulate Database Errors (missing .ser file / invalid DB URL)");
            System.out.println("  [4] Reset Database & Re-Seed Fresh Sample Data");
            CliTheme.printFooterNav(true);

            String choice = input.readLine("Select option > ");
            if (choice == null || "0".equals(choice)) {
                return;
            }
            if ("H".equalsIgnoreCase(choice)) {
                session.setReturnHome(true);
                return;
            }
            if ("L".equalsIgnoreCase(choice)) {
                session.setLogoutRequested(true);
                return;
            }

            if ("1".equals(choice)) {
                String path = input.readOptionalString("Backup File Path", DEFAULT_BACKUP_PATH);
                try {
                    GymBackupSnapshot snap = ops.exportSnapshotToFile(path, session.getCurrentUser().getUsername());
                    CliTheme.printSuccess("Serialized " + snap.totalRecordsCount()
                            + " total domain objects across 13 tables into " + path);
                } catch (DatabaseException e) {
                    CliTheme.printDatabaseError(e.getMessage());
                }
                input.pause();
            } else if ("2".equals(choice)) {
                String path = input.readOptionalString("Backup File Path to Load", DEFAULT_BACKUP_PATH);
                try {
                    GymBackupSnapshot snap = ops.loadSnapshotFromFile(path);
                    CliTheme.printSuccess("Loaded .ser snapshot created at " + snap.getCreatedAt()
                            + " by " + snap.getCreatedBy());
                    System.out.println("    Members=" + snap.getCustomers().size()
                            + ", Coaches=" + snap.getTrainers().size()
                            + ", Plans=" + snap.getMemberships().size()
                            + ", Sessions=" + snap.getSessions().size()
                            + ", Payments=" + snap.getPayments().size()
                            + ", Total=" + snap.totalRecordsCount());
                } catch (DatabaseException e) {
                    CliTheme.printDatabaseError(e.getMessage());
                }
                input.pause();
            } else if ("3".equals(choice)) {
                simulateDatabaseErrors();
                input.pause();
            } else if ("4".equals(choice)) {
                if (input.readYesNo("Reset all tables and re-seed fresh sample data?")) {
                    try {
                        SampleDataSeeder.resetAndSeed();
                        CliTheme.printSuccess("Database cleared and re-seeded cleanly.");
                    } catch (ValidationException e) {
                        CliTheme.printValidationError(e.getMessage());
                    } catch (DatabaseException e) {
                        CliTheme.printDatabaseError(e.getMessage());
                    }
                }
                input.pause();
            } else {
                System.out.println("[!] Unknown option: " + choice);
            }
        }
    }

    private void renderFloorMap(List<Equipment> list) {
        CliTheme.printSectionTitle("GYM FLOOR ZONE MAP (ASCII BLUEPRINT)");
        // "OTHER" collects equipment whose category is not one of the 4 named zones
        String[] zones = {"CARDIO", "STRENGTH", "FREE WEIGHTS", "RECOVERY", "OTHER"};
        for (int z = 0; z < zones.length; z++) {
            String zone = zones[z];
            System.out.println("  +--- ZONE: " + zone + " " + CliTheme.repeat('-', 58 - zone.length()) + "+");
            int found = 0;
            for (int i = 0; i < list.size(); i++) {
                Equipment eq = list.get(i);
                String cat = eq.getCategory() != null ? eq.getCategory().trim().toUpperCase() : "";
                boolean known = cat.equals("CARDIO") || cat.equals("STRENGTH")
                        || cat.equals("FREE WEIGHTS") || cat.equals("RECOVERY");
                if (cat.equals(zone) || (zone.equals("OTHER") && !known)) {
                    found++;
                    String badge = eq.getStatus() == EquipmentStatus.AVAILABLE ? "[OK]"
                            : (eq.getStatus() == EquipmentStatus.IN_MAINTENANCE ? "[MAINT]" : "[RET]");
                    System.out.println("  |  " + badge + " #" + eq.getId() + " " + eq.getName()
                            + " (" + eq.getCondition() + ")");
                }
            }
            if (found == 0) {
                System.out.println("  |  (No equipment assigned to this zone)");
            }
            System.out.println("  +" + CliTheme.repeat('-', 70) + "+");
        }
    }

    private void simulateDatabaseErrors() {
        CliTheme.printSectionTitle("Simulated DatabaseException Handling (errors below are expected)");
        try {
            SerializationUtil.loadFromFile("backups" + File.separator + "does_not_exist_999.ser");
        } catch (DatabaseException e) {
            CliTheme.printSuccess("Caught expected missing-file DatabaseException -> " + e.getMessage());
        }

        try {
            SerializationUtil.saveToFile(null, DEFAULT_BACKUP_PATH);
        } catch (DatabaseException e) {
            CliTheme.printSuccess("Caught expected null-object DatabaseException -> " + e.getMessage());
        }

        String savedUrl = DatabaseConnection.getDbUrl();
        try {
            DatabaseConnection.setDbUrl("jdbc:sqlite:/nonexistent_dir_xyz/bad.db");
            DatabaseConnection.getConnection();
        } catch (DatabaseException e) {
            CliTheme.printSuccess("Caught expected unreachable-DB DatabaseException -> " + e.getMessage());
        } finally {
            DatabaseConnection.setDbUrl(savedUrl);
        }
    }

    private EquipmentStatus readEquipmentStatus(EquipmentStatus current) {
        System.out.println("  Status: [1] AVAILABLE  [2] IN_MAINTENANCE  [3] RETIRED");
        String s = input.readLine("Select [1-3] (Enter to keep " + current + "): ");
        if ("1".equals(s)) {
            return EquipmentStatus.AVAILABLE;
        }
        if ("2".equals(s)) {
            return EquipmentStatus.IN_MAINTENANCE;
        }
        if ("3".equals(s)) {
            return EquipmentStatus.RETIRED;
        }
        return current;
    }

    private EquipmentCondition readEquipmentCondition(EquipmentCondition current) {
        System.out.println("  Condition: [1] NEW  [2] GOOD  [3] FAIR  [4] POOR");
        String c = input.readLine("Select [1-4] (Enter to keep " + current + "): ");
        if ("1".equals(c)) {
            return EquipmentCondition.NEW;
        }
        if ("2".equals(c)) {
            return EquipmentCondition.GOOD;
        }
        if ("3".equals(c)) {
            return EquipmentCondition.FAIR;
        }
        if ("4".equals(c)) {
            return EquipmentCondition.POOR;
        }
        return current;
    }

    // Pressing Enter keeps the current role, so editing only a password never changes the role
    private StaffRole readStaffRole(StaffRole current) {
        System.out.println("  Role: [1] ADMIN  [2] MANAGER  [3] STAFF");
        while (true) {
            String r = input.readLine("Select [1-3] (Enter to keep " + current + "): ");
            if (r == null || r.isEmpty()) {
                return current;
            }
            if ("1".equals(r)) {
                return StaffRole.ADMIN;
            }
            if ("2".equals(r)) {
                return StaffRole.MANAGER;
            }
            if ("3".equals(r)) {
                return StaffRole.STAFF;
            }
            System.out.println("[!] Enter 1, 2, 3, or press Enter.");
        }
    }
}

// ===== ValidationSuiteRunner =====

// Automated and interactive suite verifying all entities, validations, and error handling.
// Runs against an isolated sandbox database so real gym.db data is never touched.
class ValidationSuiteRunner {

    private static final String SANDBOX_DB_FILE = "gym_sandbox.db";
    private static final String SANDBOX_SER_FILE = "backups" + File.separator + "sandbox_snapshot.ser";

    private final CliInput input;
    private final CliSession session;

    public ValidationSuiteRunner(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openMenu() {
        while (!session.shouldStopSubMenu()) {
            CliTheme.printHeader("VALIDATION & EXCEPTION SUITE",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu > Validation Suite");

            System.out.println("  Runs against an isolated sandbox DB (" + SANDBOX_DB_FILE + ") so your");
            System.out.println("  interactive data in gym.db is never modified or lost.");
            CliTheme.printSingleLine();
            System.out.println("  [1] Run Full 22-Scenario End-to-End Verification Suite (Auto Pass/Fail Report)");
            System.out.println("  [2] Run Individual Scenario Category (Pick #1 - #11)");
            CliTheme.printFooterNav(true);

            String choice = input.readLine("Select option > ");
            if (choice == null || "0".equals(choice)) {
                return;
            }
            if ("H".equalsIgnoreCase(choice)) {
                session.setReturnHome(true);
                return;
            }
            if ("L".equalsIgnoreCase(choice)) {
                session.setLogoutRequested(true);
                return;
            }

            if ("1".equals(choice)) {
                runAllScenarios();
                input.pause();
            } else if ("2".equals(choice)) {
                runSingleCategoryMenu();
            } else {
                System.out.println("[!] Unknown option: " + choice);
            }
        }
    }

    private void runSingleCategoryMenu() {
        while (!session.shouldStopSubMenu()) {
            CliTheme.printHeader("PICK A SCENARIO CATEGORY",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu > Validation Suite > Picker");
            System.out.println("  [1]  Customer / Trainer / Staff Field Validation (blank, email, rate, dup user)");
            System.out.println("  [2]  Login Inactive Block & ADMIN RBAC");
            System.out.println("  [3]  Availability Cap, Open Slot Booking & Derived FULL");
            System.out.println("  [4]  Subscription Trainer Match & Zero-Price Session");
            System.out.println("  [5]  Session State Machine, Cancel 2h Check & NO_SHOW");
            System.out.println("  [6]  Check-In Active Plan Guard & Same-Day Row Reuse");
            System.out.println("  [7]  Freeze / Unfreeze Date Shift, Cancelled Renew Block & Suspend Cascade");
            System.out.println("  [8]  Medical Profile Single Record & Trainer Access");
            System.out.println("  [9]  Workout Version Uniqueness & Progress Range Checks");
            System.out.println("  [10] Payment XOR, Customer Match, Void/Replace & Balance");
            System.out.println("  [11] Hard-Delete Guard + Serialization .ser & DatabaseException Handling");
            CliTheme.printFooterNav(true);

            String choice = input.readLine("Select scenario [1-11] > ");
            if (choice == null || "0".equals(choice)) {
                return;
            }
            if ("H".equalsIgnoreCase(choice)) {
                session.setReturnHome(true);
                return;
            }
            if ("L".equalsIgnoreCase(choice)) {
                session.setLogoutRequested(true);
                return;
            }

            runCategoryByChoice(choice);
            input.pause();
        }
    }

    private void runAllScenarios() {
        String previousUrl = DatabaseConnection.getDbUrl();
        int passed = 0;
        int total = 22;

        CliTheme.printHeader("22-SCENARIO END-TO-END SUITE",
                session.userLabel(), SANDBOX_DB_FILE,
                "Main Menu > Validation Suite > Automated Run");

        try {
            prepareSandbox();
            // runCheck keeps one broken scenario from skipping the rest of the scoreboard
            if (runCheck("#01", this::test01CustomerValidation)) passed++;
            if (runCheck("#02", this::test02CustomerBadFormat)) passed++;
            if (runCheck("#03", this::test03TrainerNegativeRate)) passed++;
            if (runCheck("#04", this::test04DuplicateUsername)) passed++;
            if (runCheck("#05", this::test05InactiveLoginBlocked)) passed++;
            if (runCheck("#06", this::test06NonAdminBlocked)) passed++;
            if (runCheck("#07", this::test07InvalidAvailabilitySlot)) passed++;
            if (runCheck("#08", this::test08SlotCapAndFullStatus)) passed++;
            if (runCheck("#09", this::test09SubscriptionTrainerMismatch)) passed++;
            if (runCheck("#10", this::test10SubscriptionZeroPriceSession)) passed++;
            if (runCheck("#11", this::test11SessionTransitionsAndNoShow)) passed++;
            if (runCheck("#12", this::test12SessionCancel2hCheck)) passed++;
            if (runCheck("#13", this::test13AdminOverrideBypass)) passed++;
            if (runCheck("#14", this::test14CheckInDeniedForInvalidMember)) passed++;
            if (runCheck("#15", this::test15CheckInSameDayReuse)) passed++;
            if (runCheck("#16", this::test16MembershipFreezeUnfreezeRenew)) passed++;
            if (runCheck("#17", this::test17SuspendCascadesFreeze)) passed++;
            if (runCheck("#18", this::test18MedicalProfileChecks)) passed++;
            if (runCheck("#19", this::test19WorkoutAndProgressValidation)) passed++;
            if (runCheck("#20", this::test20PaymentsXorVoidBalance)) passed++;
            if (runCheck("#21", this::test21HardDeleteGuard)) passed++;
            if (runCheck("#22", this::test22SerializationAndDatabaseException)) passed++;
        } catch (DatabaseException e) {
            System.out.println("  [FAIL] Sandbox setup failed: " + e.getMessage());
        } catch (ValidationException e) {
            System.out.println("  [FAIL] Sandbox setup failed: " + e.getMessage());
        } finally {
            cleanupSandbox(previousUrl);
        }

        CliTheme.printSingleLine();
        System.out.println("  SCOREBOARD: " + passed + " / " + total + " verification checks PASSED.");
        CliTheme.printSingleLine();
    }

    private void runCategoryByChoice(String choice) {
        String previousUrl = DatabaseConnection.getDbUrl();
        int passed = 0;
        int ran = 0;
        try {
            prepareSandbox();
            if ("1".equals(choice)) {
                ran = 4;
                if (runCheck("#01", this::test01CustomerValidation)) passed++;
                if (runCheck("#02", this::test02CustomerBadFormat)) passed++;
                if (runCheck("#03", this::test03TrainerNegativeRate)) passed++;
                if (runCheck("#04", this::test04DuplicateUsername)) passed++;
            } else if ("2".equals(choice)) {
                ran = 2;
                if (runCheck("#05", this::test05InactiveLoginBlocked)) passed++;
                if (runCheck("#06", this::test06NonAdminBlocked)) passed++;
            } else if ("3".equals(choice)) {
                ran = 3;
                if (runCheck("#07", this::test07InvalidAvailabilitySlot)) passed++;
                if (runCheck("#08", this::test08SlotCapAndFullStatus)) passed++;
                if (runCheck("#13", this::test13AdminOverrideBypass)) passed++;
            } else if ("4".equals(choice)) {
                ran = 2;
                if (runCheck("#09", this::test09SubscriptionTrainerMismatch)) passed++;
                if (runCheck("#10", this::test10SubscriptionZeroPriceSession)) passed++;
            } else if ("5".equals(choice)) {
                ran = 2;
                if (runCheck("#11", this::test11SessionTransitionsAndNoShow)) passed++;
                if (runCheck("#12", this::test12SessionCancel2hCheck)) passed++;
            } else if ("6".equals(choice)) {
                ran = 2;
                if (runCheck("#14", this::test14CheckInDeniedForInvalidMember)) passed++;
                if (runCheck("#15", this::test15CheckInSameDayReuse)) passed++;
            } else if ("7".equals(choice)) {
                ran = 2;
                if (runCheck("#16", this::test16MembershipFreezeUnfreezeRenew)) passed++;
                if (runCheck("#17", this::test17SuspendCascadesFreeze)) passed++;
            } else if ("8".equals(choice)) {
                ran = 1;
                if (runCheck("#18", this::test18MedicalProfileChecks)) passed++;
            } else if ("9".equals(choice)) {
                ran = 1;
                if (runCheck("#19", this::test19WorkoutAndProgressValidation)) passed++;
            } else if ("10".equals(choice)) {
                ran = 1;
                if (runCheck("#20", this::test20PaymentsXorVoidBalance)) passed++;
            } else if ("11".equals(choice)) {
                ran = 2;
                if (runCheck("#21", this::test21HardDeleteGuard)) passed++;
                if (runCheck("#22", this::test22SerializationAndDatabaseException)) passed++;
            } else {
                System.out.println("[!] Unknown scenario choice.");
                return;
            }
            System.out.println("  Category Result: " + passed + " / " + ran + " passed.");
        } catch (DatabaseException e) {
            System.out.println("  [FAIL] Sandbox setup failed: " + e.getMessage());
        } catch (ValidationException e) {
            System.out.println("  [FAIL] Sandbox setup failed: " + e.getMessage());
        } finally {
            cleanupSandbox(previousUrl);
        }
    }

    // One scenario crashes or errors -> it is counted as FAIL and the suite keeps going
    private boolean runCheck(String code, ScenarioStep step) {
        try {
            return step.run();
        } catch (DatabaseException e) {
            return fail(code, "Unexpected DatabaseException: " + e.getMessage());
        } catch (ValidationException e) {
            return fail(code, "Unexpected ValidationException: " + e.getMessage());
        } catch (RuntimeException e) {
            return fail(code, "Unexpected error: " + e);
        }
    }

    private interface ScenarioStep {
        boolean run() throws DatabaseException, ValidationException;
    }

    private void prepareSandbox() throws DatabaseException, ValidationException {
        deleteFileIfExists(SANDBOX_DB_FILE);
        DatabaseConnection.setDbUrl("jdbc:sqlite:" + SANDBOX_DB_FILE);
        DatabaseConnection.initializeDatabase();
        SampleDataSeeder.resetAndSeed();
    }

    private void cleanupSandbox(String previousUrl) {
        DatabaseConnection.setDbUrl(previousUrl);
        deleteFileIfExists(SANDBOX_DB_FILE);
        deleteFileIfExists(SANDBOX_SER_FILE);
    }

    private void deleteFileIfExists(String path) {
        File file = new File(path);
        if (file.exists() && !file.delete()) {
            System.out.println("[!] Could not delete sandbox file: " + path);
        }
    }

    private boolean test01CustomerValidation() throws DatabaseException {
        try {
            new CustomerDao().insert(new Customer());
            return fail("#01", "Blank Customer should fail");
        } catch (ValidationException e) {
            return pass("#01", "Blank Customer rejected -> " + e.getMessage());
        }
    }

    private boolean test02CustomerBadFormat() throws DatabaseException {
        try {
            Customer bad = new Customer(0, "Bad User", "123", "not-an-email", "Cairo",
                    "1999-99-99", "Male", "Em", "123", CustomerStatus.ACTIVE, "2026-11-09", null);
            new CustomerDao().insert(bad);
            return fail("#02", "Invalid email/phone/DOB should fail");
        } catch (ValidationException e) {
            return pass("#02", "Bad Customer format rejected -> " + e.getMessage());
        }
    }

    private boolean test03TrainerNegativeRate() throws DatabaseException {
        try {
            Trainer t = new Trainer(0, "Bad Coach", "01012345678", "bad@gym.com", "Strength",
                    2, "Cert", "2026-01-01", new BigDecimal("-10.00"));
            new TrainerDao().insert(t);
            return fail("#03", "Negative hourlyRate should fail");
        } catch (ValidationException e) {
            return pass("#03", "Negative Trainer rate rejected -> " + e.getMessage());
        }
    }

    private boolean test04DuplicateUsername() throws DatabaseException {
        try {
            Administrator dup = new Administrator(0, "Dup Admin", "admin", "pw",
                    StaffRole.STAFF, "01000001111", "dup@gym.com", true, "2026-11-09");
            new AdministratorDao().insert(dup);
            return fail("#04", "Duplicate username should fail");
        } catch (ValidationException e) {
            return pass("#04", "Duplicate username rejected -> " + e.getMessage());
        }
    }

    private boolean test05InactiveLoginBlocked() throws DatabaseException {
        try {
            new GymOperations().authenticate("oldstaff", "old123");
            return fail("#05", "Inactive login should fail");
        } catch (ValidationException e) {
            return pass("#05", "Inactive account login blocked -> " + e.getMessage());
        }
    }

    private boolean test06NonAdminBlocked() throws DatabaseException {
        try {
            GymOperations helper = new GymOperations();
            Administrator staff = helper.authenticate("staff", "staff123");
            helper.adminReserve(staff, 1, 1, LocalDate.now().plusDays(1).toString(),
                    "21:00", 60, null, BigDecimal.TEN, "Unauthorized");
            return fail("#06", "Non-ADMIN adminReserve should fail");
        } catch (ValidationException e) {
            return pass("#06", "STAFF blocked on ADMIN action -> " + e.getMessage());
        }
    }

    private boolean test07InvalidAvailabilitySlot() throws DatabaseException {
        try {
            TrainerAvailability bad = new TrainerAvailability(0, 1, LocalDate.now().plusDays(1).toString(),
                    "14:00", "13:00", 2, AvailabilityStatus.OPEN);
            new TrainerAvailabilityDao().insert(bad);
            return fail("#07", "Start time >= End time should fail");
        } catch (ValidationException e) {
            return pass("#07", "Invalid availability time order rejected -> " + e.getMessage());
        }
    }

    private boolean test08SlotCapAndFullStatus() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        TrainerAvailabilityDao slotDao = new TrainerAvailabilityDao();
        // Slot #2 has maxCustomers=1 and 0 booked initially
        TrainerAvailability slot = slotDao.findById(2);
        if (slot == null) {
            return fail("#08", "Seed data missing: Slot #2 not found");
        }
        helper.bookOpenSlot(2, 2, 60, null, new BigDecimal("45.00"), "Take last spot");
        AvailabilityStatus eff = helper.effectiveSlotStatus(slotDao.findById(2));
        if (eff != AvailabilityStatus.FULL) {
            return fail("#08", "Slot #2 expected derived FULL status");
        }
        try {
            helper.bookOpenSlot(1, 2, 60, null, new BigDecimal("45.00"), "Overflow");
            return fail("#08", "Overflow booking into FULL slot should fail");
        } catch (ValidationException e) {
            return pass("#08", "Slot FULL derived & overflow rejected -> " + e.getMessage());
        }
    }

    private boolean test09SubscriptionTrainerMismatch() throws DatabaseException {
        try {
            // Customer #1 is assigned to Trainer #1; attempting Trainer #2 breaks the trainer-match validation
            TrainerSubscription badSub = new TrainerSubscription(0, 1, 2,
                    LocalDate.now().toString(), LocalDate.now().plusDays(30).toString(),
                    new BigDecimal("100.00"), SubscriptionStatus.ACTIVE);
            new TrainerSubscriptionDao().insert(badSub);
            return fail("#09", "Trainer mismatch in subscription should fail");
        } catch (ValidationException e) {
            return pass("#09", "Subscription trainer mismatch blocked -> " + e.getMessage());
        }
    }

    private boolean test10SubscriptionZeroPriceSession() throws DatabaseException {
        try {
            Session nonZero = new Session(0, 1, 1, LocalDate.now().plusDays(2).toString(),
                    "11:00", 60, SessionStatus.REQUESTED, null, 1, new BigDecimal("25.00"), null, "Double bill");
            new SessionDao().insert(nonZero);
            return fail("#10", "Subscription session with price > 0 should fail");
        } catch (ValidationException e) {
            return pass("#10", "Double-billing prevented (sub price > 0 blocked) -> " + e.getMessage());
        }
    }

    private boolean test11SessionTransitionsAndNoShow() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        // Session #2 is REQUESTED
        try {
            helper.completeSession(2);
            return fail("#11", "REQUESTED -> COMPLETED without CONFIRMED should fail");
        } catch (ValidationException expected) {
            helper.confirmSession(2);
            helper.markNoShow(2);
            int noShows = helper.countTrainerNoShows(2);
            if (noShows < 2) {
                return fail("#11", "Trainer #2 NO_SHOW count should reflect marked session");
            }
            return pass("#11", "Illegal session transition caught & Trainer #2 NO_SHOWs=" + noShows);
        }
    }

    private boolean test12SessionCancel2hCheck() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        // Session #1 is tomorrow -> >= 2h notice -> free cancellation
        String notice = helper.cancelSession(1, "Work travel conflict");
        Session cancelled = new SessionDao().findById(1);
        if (cancelled == null) {
            return fail("#12", "Seed data missing: Session #1 not found");
        }
        if (cancelled.getStatus() != SessionStatus.CANCELLED || notice.indexOf("Free cancellation") < 0) {
            return fail("#12", "Expected free cancellation status message for tomorrow session");
        }
        return pass("#12", notice);
    }

    private boolean test13AdminOverrideBypass() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        Administrator admin = helper.authenticate("admin", "admin123");
        // Slot #2 is FULL; adminReserve succeeds anyway (admin override)
        Session vip = helper.adminReserve(admin, 1, 1, LocalDate.now().plusDays(1).toString(),
                "12:00", 60, 2, new BigDecimal("80.00"), "VIP Admin Override");
        if (vip.getStatus() != SessionStatus.CONFIRMED) {
            return fail("#13", "Admin override expected CONFIRMED session");
        }
        return pass("#13", "ADMIN override booked FULL Slot #2 as Session #" + vip.getId() + " (admin override)");
    }

    private boolean test14CheckInDeniedForInvalidMember() throws DatabaseException {
        GymOperations helper = new GymOperations();
        String today = LocalDate.now().toString();
        try {
            // Customer #3 has EXPIRED membership
            helper.checkIn(3, today, "10:00");
            return fail("#14", "Expired membership check-in should fail");
        } catch (ValidationException expected) {
            return pass("#14", "Expired membership check-in blocked -> " + expected.getMessage());
        }
    }

    private boolean test15CheckInSameDayReuse() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        String today = LocalDate.now().toString();
        AttendanceDao attDao = new AttendanceDao();
        int beforeCount = attDao.findByCustomerId(1).size();
        Attendance reused = helper.checkIn(1, today, "09:15");
        int afterCount = attDao.findByCustomerId(1).size();
        if (beforeCount != afterCount || !"09:15".equals(reused.getCheckIn())) {
            return fail("#15", "Second check-in today should reuse existing row instead of duplicating");
        }
        return pass("#15", "Same-day check-in reused Attendance #" + reused.getId() + " (same-day reuse)");
    }

    private boolean test16MembershipFreezeUnfreezeRenew() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        Membership before = new MembershipDao().findById(4); // c4 FROZEN 10 days
        if (before == null) {
            return fail("#16", "Seed data missing: Membership #4 not found");
        }
        String oldEnd = before.getEndDate();
        Membership unfrozen = helper.unfreezeMembership(4);
        String expectedEnd = LocalDate.parse(oldEnd).plusDays(10).toString();
        if (!expectedEnd.equals(unfrozen.getEndDate()) || unfrozen.getFrozenDays() != 0) {
            return fail("#16", "Unfreeze failed to shift endDate by 10 days");
        }
        try {
            helper.renewMembership(5); // CANCELLED plan
            return fail("#16", "Renewing CANCELLED membership should fail");
        } catch (ValidationException expected) {
            return pass("#16", "Unfreeze shifted endDate (+10d -> " + unfrozen.getEndDate() + ") & CANCELLED renew blocked");
        }
    }

    private boolean test17SuspendCascadesFreeze() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        int frozenCount = helper.suspendCustomer(2); // Customer #2 has 1 ACTIVE plan
        Membership m2 = new MembershipDao().findById(2);
        if (m2 == null) {
            return fail("#17", "Seed data missing: Membership #2 not found");
        }
        if (frozenCount != 1 || m2.getStatus() != MembershipStatus.FROZEN) {
            return fail("#17", "Expected Customer #2 suspension to freeze active Membership #2");
        }
        return pass("#17", "Suspend Member #2 cascaded FROZEN to " + frozenCount + " active membership");
    }

    private boolean test18MedicalProfileChecks() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        MedicalProfileDao medDao = new MedicalProfileDao();
        try {
            // Customer #1 already has MedicalProfile #1
            medDao.insert(new MedicalProfile(0, 1, "A+", "None", "None", "None", "None",
                    "Dr", "01000000000", "Dup", LocalDate.now().toString()));
            return fail("#18", "Second medical profile for same customer should fail");
        } catch (ValidationException expected) {
            try {
                // Trainer #2 is NOT assigned to Customer #1
                helper.getMedicalProfileForTrainer(2, 1);
                return fail("#18", "Unassigned trainer read medical profile should fail");
            } catch (ValidationException denied) {
                MedicalProfile allowed = helper.getMedicalProfileForTrainer(1, 1);
                return pass("#18", "Single medical profile enforced & trainer access check ok (Blood="
                        + allowed.getBloodType() + ")");
            }
        }
    }

    private boolean test19WorkoutAndProgressValidation() throws DatabaseException {
        WorkoutProgramDao wpDao = new WorkoutProgramDao();
        ProgressRecordDao prDao = new ProgressRecordDao();
        try {
            // Customer #1 already has version 1
            wpDao.insert(new WorkoutProgram(0, 1, 1, "Dup Ver", "Squat", 1, LocalDate.now().toString()));
            return fail("#19", "Duplicate workout version per customer should fail");
        } catch (ValidationException wpEx) {
            try {
                prDao.insert(new ProgressRecord(0, 1, LocalDate.now().toString(), 75.0, 120.0, null, null, "Bad"));
                return fail("#19", "bodyFatPct > 100 should fail");
            } catch (ValidationException prEx) {
                return pass("#19", "Duplicate workout version & invalid bodyFatPct caught cleanly");
            }
        }
    }

    private boolean test20PaymentsXorVoidBalance() throws DatabaseException, ValidationException {
        PaymentDao payDao = new PaymentDao();
        GymOperations helper = new GymOperations();
        try {
            // 0 target links breaks the exactly-one-link payment validation
            payDao.insert(new Payment(0, 1, null, null, null, new BigDecimal("50.00"),
                    PaymentMethod.CASH, LocalDate.now().toString(), "REC-BAD-0", false));
            return fail("#20", "0-link payment should be rejected");
        } catch (ValidationException expected) {
            try {
                // Mismatched customerId breaks the owner check (Membership #2 belongs to Customer #2, not #1)
                payDao.insert(Payment.forMembership(1, 2, new BigDecimal("60.00"),
                        PaymentMethod.CASH, LocalDate.now().toString(), "REC-BAD-OWNER"));
                return fail("#20", "Mismatched owner payment should be rejected");
            } catch (ValidationException ownerEx) {
                // Void existing Payment #4 and check the outstanding balance increases
                BigDecimal beforeVoidOwed = helper.computeOutstandingBalance(2);
                helper.voidAndReplacePayment(4, "Card chargeback", LocalDate.now().toString(),
                        null, null, null);
                BigDecimal afterVoidOwed = helper.computeOutstandingBalance(2);
                if (afterVoidOwed.compareTo(beforeVoidOwed) <= 0) {
                    return fail("#20", "Voided payment must not count toward balance");
                }
                return pass("#20", "Payment link + void + balance verified ($"
                        + beforeVoidOwed.toPlainString() + " -> $" + afterVoidOwed.toPlainString() + ")");
            }
        }
    }

    private boolean test21HardDeleteGuard() throws DatabaseException, ValidationException {
        GymOperations helper = new GymOperations();
        Administrator admin = helper.authenticate("admin", "admin123");
        try {
            helper.hardDeleteCustomer(admin, 1);
            return fail("#21", "Hard delete of Customer #1 with child records should fail");
        } catch (ValidationException expected) {
            Equipment standalone = new EquipmentDao().insert(new Equipment(0, "Temp Rope", "RECOVERY",
                    LocalDate.now().toString(), EquipmentCondition.NEW, EquipmentStatus.AVAILABLE,
                    LocalDate.now().toString()));
            new EquipmentDao().delete(standalone.getId());
            return pass("#21", "Hard delete refused referenced member (#1); standalone delete succeeded");
        }
    }

    private boolean test22SerializationAndDatabaseException() {
        GymOperations helper = new GymOperations();
        try {
            GymBackupSnapshot snap = helper.exportSnapshotToFile(SANDBOX_SER_FILE, "admin");
            GymBackupSnapshot loaded = helper.loadSnapshotFromFile(SANDBOX_SER_FILE);
            if (loaded.totalRecordsCount() != snap.totalRecordsCount() || loaded.totalRecordsCount() < 15) {
                return fail("#22", ".ser round-trip record count mismatch");
            }
            try {
                SerializationUtil.loadFromFile("backups" + File.separator + "missing_xyz.ser");
                return fail("#22", "Missing .ser file should throw DatabaseException");
            } catch (DatabaseException expected) {
                List<Customer> customers = loaded.getCustomers();
                return pass("#22", ".ser round-trip restored " + loaded.totalRecordsCount()
                        + " objects (" + customers.size() + " customers) & caught DatabaseException");
            }
        } catch (DatabaseException e) {
            return fail("#22", "Unexpected serialization DatabaseException: " + e.getMessage());
        }
    }

    private boolean pass(String code, String msg) {
        System.out.println("  [PASS] " + code + " " + msg);
        return true;
    }

    private boolean fail(String code, String msg) {
        System.out.println("  [FAIL] " + code + " " + msg);
        return false;
    }
}
