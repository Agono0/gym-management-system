package com.gym.cli;

import com.gym.dao.AttendanceDao;
import com.gym.dao.CustomerDao;
import com.gym.dao.EquipmentDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.PaymentDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Administrator;
import com.gym.model.Attendance;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.Equipment;
import com.gym.model.EquipmentStatus;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Payment;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

// Terminal UI entry point for Gym-OS (COMP 301 Lectures 1-6 console workflow)
public class GymCliApp {

    private final Scanner scanner;
    private final CliInput input;
    private final CliSession session;
    private final GymRulesHelper rules;

    public GymCliApp() {
        this.scanner = new Scanner(System.in);
        this.input = new CliInput(scanner);
        this.session = new CliSession();
        this.rules = new GymRulesHelper();
    }

    public static void main(String[] args) {
        GymCliApp app = new GymCliApp();
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

            System.out.println("  Seeded Accounts (ready for Rule 16 active / role tests):");
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
                    Administrator user = rules.authenticate(username, password);
                    CliTheme.printSuccess("Welcome back, " + user.getFullName() + " [" + user.getRole() + "]!");
                    return user;
                } else if ("2".equals(choice)) {
                    return rules.authenticate("admin", "admin123");
                } else if ("3".equals(choice)) {
                    return rules.authenticate("manager", "mgr123");
                } else if ("4".equals(choice)) {
                    return rules.authenticate("staff", "staff123");
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
        TaShowcaseRunner taRunner = new TaShowcaseRunner(input, session);

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
            System.out.println("  [T] TA Validation, Business Rules (1-19) & Exception Showcase");
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
            } else if ("T".equalsIgnoreCase(choice)) {
                taRunner.openMenu();
            } else {
                System.out.println("[!] Unknown option: " + choice);
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
                if (rules.effectiveMembershipStatus(plans.get(i), today) == MembershipStatus.ACTIVE) {
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

