package com.gym.cli;

import com.gym.dao.AdministratorDao;
import com.gym.dao.AttendanceDao;
import com.gym.dao.CustomerDao;
import com.gym.dao.EquipmentDao;
import com.gym.dao.MedicalProfileDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.PaymentDao;
import com.gym.dao.ProgressRecordDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.TrainerSubscriptionDao;
import com.gym.dao.WorkoutProgramDao;
import com.gym.exception.DatabaseException;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Automated and interactive Teaching Assistant suite verifying all entities, rules 1-19, and exceptions
public class TaShowcaseRunner {

    private static final String SANDBOX_DB_FILE = "gym_ta_showcase.db";
    private static final String SANDBOX_SER_FILE = "backups" + File.separator + "ta_showcase_snapshot.ser";

    private final CliInput input;
    private final CliSession session;

    public TaShowcaseRunner(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openMenu() {
        while (!session.shouldStopSubMenu()) {
            CliTheme.printHeader("TA VALIDATION, BUSINESS RULES & EXCEPTION SHOWCASE",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu > TA Showcase");

            System.out.println("  Runs against an isolated sandbox DB (" + SANDBOX_DB_FILE + ") so your");
            System.out.println("  interactive data in gym.db is never modified or lost.");
            CliTheme.printSingleLine();
            System.out.println("  [1] Run Full 22-Scenario End-to-End Verification Suite (Auto Pass/Fail Report)");
            System.out.println("  [2] Test Individual Scenario Category (Pick #1 - #11)");
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
            CliTheme.printHeader("PICK TA TEST SCENARIO",
                    session.userLabel(), DatabaseConnection.getDbUrl(),
                    "Main Menu > TA Showcase > Picker");
            System.out.println("  [1]  Customer / Trainer / Staff Field Validation (blank, email, rate, dup user)");
            System.out.println("  [2]  Login Inactive Block & ADMIN RBAC (Rule 16)");
            System.out.println("  [3]  Availability Cap, Open Slot Booking & Derived FULL (Rules 1, 8, 13, 17)");
            System.out.println("  [4]  Subscription Trainer Match & Zero-Price Session (Rule 11)");
            System.out.println("  [5]  Session State Machine, Cancel 2h Check & NO_SHOW (Rules 2, 10, 18)");
            System.out.println("  [6]  Check-In Active Plan Guard & Same-Day Row Reuse (Rule 5)");
            System.out.println("  [7]  Freeze / Unfreeze Date Shift, Cancelled Renew Block & Suspend Cascade (Rules 7, 14)");
            System.out.println("  [8]  Medical Profile Single Record & Trainer Access (Rules 3, 15)");
            System.out.println("  [9]  Workout Version Uniqueness & Progress Range Checks");
            System.out.println("  [10] Payment XOR, Customer Match, Void/Replace & Balance (Rules 6, 12, 19)");
            System.out.println("  [11] Hard-Delete Guard (Rule 4) + Serialization .ser & DatabaseException Trap");
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

        CliTheme.printHeader("TA 22-SCENARIO END-TO-END SUITE",
                session.userLabel(), SANDBOX_DB_FILE,
                "Main Menu > TA Showcase > Automated Run");

        try {
            prepareSandbox();
            if (test01CustomerValidation()) passed++;
            if (test02CustomerBadFormat()) passed++;
            if (test03TrainerNegativeRate()) passed++;
            if (test04DuplicateUsername()) passed++;
            if (test05InactiveLoginBlocked()) passed++;
            if (test06NonAdminBlocked()) passed++;
            if (test07InvalidAvailabilitySlot()) passed++;
            if (test08SlotCapAndFullStatus()) passed++;
            if (test09SubscriptionTrainerMismatch()) passed++;
            if (test10SubscriptionZeroPriceSession()) passed++;
            if (test11SessionTransitionsAndNoShow()) passed++;
            if (test12SessionCancel2hRule()) passed++;
            if (test13AdminOverrideBypass()) passed++;
            if (test14CheckInDeniedForInvalidMember()) passed++;
            if (test15CheckInSameDayReuse()) passed++;
            if (test16MembershipFreezeUnfreezeRenew()) passed++;
            if (test17SuspendCascadesFreeze()) passed++;
            if (test18MedicalProfileRules()) passed++;
            if (test19WorkoutAndProgressValidation()) passed++;
            if (test20PaymentsXorVoidBalance()) passed++;
            if (test21HardDeleteGuard()) passed++;
            if (test22SerializationAndDatabaseException()) passed++;
        } catch (DatabaseException e) {
            System.out.println("  [FAIL] Unexpected DatabaseException during setup: " + e.getMessage());
        } catch (ValidationException e) {
            System.out.println("  [FAIL] Unexpected ValidationException during setup: " + e.getMessage());
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
                if (test01CustomerValidation()) passed++;
                if (test02CustomerBadFormat()) passed++;
                if (test03TrainerNegativeRate()) passed++;
                if (test04DuplicateUsername()) passed++;
            } else if ("2".equals(choice)) {
                ran = 2;
                if (test05InactiveLoginBlocked()) passed++;
                if (test06NonAdminBlocked()) passed++;
            } else if ("3".equals(choice)) {
                ran = 3;
                if (test07InvalidAvailabilitySlot()) passed++;
                if (test08SlotCapAndFullStatus()) passed++;
                if (test13AdminOverrideBypass()) passed++;
            } else if ("4".equals(choice)) {
                ran = 2;
                if (test09SubscriptionTrainerMismatch()) passed++;
                if (test10SubscriptionZeroPriceSession()) passed++;
            } else if ("5".equals(choice)) {
                ran = 2;
                if (test11SessionTransitionsAndNoShow()) passed++;
                if (test12SessionCancel2hRule()) passed++;
            } else if ("6".equals(choice)) {
                ran = 2;
                if (test14CheckInDeniedForInvalidMember()) passed++;
                if (test15CheckInSameDayReuse()) passed++;
            } else if ("7".equals(choice)) {
                ran = 2;
                if (test16MembershipFreezeUnfreezeRenew()) passed++;
                if (test17SuspendCascadesFreeze()) passed++;
            } else if ("8".equals(choice)) {
                ran = 1;
                if (test18MedicalProfileRules()) passed++;
            } else if ("9".equals(choice)) {
                ran = 1;
                if (test19WorkoutAndProgressValidation()) passed++;
            } else if ("10".equals(choice)) {
                ran = 1;
                if (test20PaymentsXorVoidBalance()) passed++;
            } else if ("11".equals(choice)) {
                ran = 2;
                if (test21HardDeleteGuard()) passed++;
                if (test22SerializationAndDatabaseException()) passed++;
            } else {
                System.out.println("[!] Unknown scenario choice.");
                return;
            }
            System.out.println("  Category Result: " + passed + " / " + ran + " passed.");
        } catch (DatabaseException e) {
            System.out.println("  [FAIL] Database error: " + e.getMessage());
        } catch (ValidationException e) {
            System.out.println("  [FAIL] Validation error: " + e.getMessage());
        } finally {
            cleanupSandbox(previousUrl);
        }
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
        if (file.exists()) {
            file.delete();
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
            new GymRulesHelper().authenticate("oldstaff", "old123");
            return fail("#05", "Inactive login should fail (Rule 16)");
        } catch (ValidationException e) {
            return pass("#05", "Inactive account login blocked -> " + e.getMessage());
        }
    }

    private boolean test06NonAdminBlocked() throws DatabaseException {
        try {
            GymRulesHelper helper = new GymRulesHelper();
            Administrator staff = helper.authenticate("staff", "staff123");
            helper.adminReserve(staff, 1, 1, LocalDate.now().plusDays(1).toString(),
                    "21:00", 60, null, BigDecimal.TEN, "Unauthorized");
            return fail("#06", "Non-ADMIN adminReserve should fail (Rule 16)");
        } catch (ValidationException e) {
            return pass("#06", "STAFF blocked on ADMIN action -> " + e.getMessage());
        }
    }

    private boolean test07InvalidAvailabilitySlot() throws DatabaseException {
        try {
            TrainerAvailability bad = new TrainerAvailability(0, 1, LocalDate.now().plusDays(1).toString(),
                    "14:00", "13:00", 2, AvailabilityStatus.OPEN);
            new TrainerAvailabilityDao().insert(bad);
            return fail("#07", "Start time >= End time should fail (Rule 8)");
        } catch (ValidationException e) {
            return pass("#07", "Invalid availability time order rejected -> " + e.getMessage());
        }
    }

    private boolean test08SlotCapAndFullStatus() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        // Slot #2 has maxCustomers=1 and 0 booked initially
        helper.bookOpenSlot(2, 2, 60, null, new BigDecimal("45.00"), "Take last spot");
        AvailabilityStatus eff = helper.effectiveSlotStatus(new TrainerAvailabilityDao().findById(2));
        if (eff != AvailabilityStatus.FULL) {
            return fail("#08", "Slot #2 expected derived FULL status");
        }
        try {
            helper.bookOpenSlot(1, 2, 60, null, new BigDecimal("45.00"), "Overflow");
            return fail("#08", "Overflow booking into FULL slot should fail (Rule 17)");
        } catch (ValidationException e) {
            return pass("#08", "Slot FULL derived & overflow rejected -> " + e.getMessage());
        }
    }

    private boolean test09SubscriptionTrainerMismatch() throws DatabaseException {
        try {
            // Customer #1 is assigned to Trainer #1; attempting Trainer #2 breaks Rule 11
            TrainerSubscription badSub = new TrainerSubscription(0, 1, 2,
                    LocalDate.now().toString(), LocalDate.now().plusDays(30).toString(),
                    new BigDecimal("100.00"), SubscriptionStatus.ACTIVE);
            new TrainerSubscriptionDao().insert(badSub);
            return fail("#09", "Trainer mismatch in subscription should fail (Rule 11)");
        } catch (ValidationException e) {
            return pass("#09", "Subscription trainer mismatch blocked -> " + e.getMessage());
        }
    }

    private boolean test10SubscriptionZeroPriceSession() throws DatabaseException {
        try {
            Session nonZero = new Session(0, 1, 1, LocalDate.now().plusDays(2).toString(),
                    "11:00", 60, SessionStatus.REQUESTED, null, 1, new BigDecimal("25.00"), null, "Double bill");
            new SessionDao().insert(nonZero);
            return fail("#10", "Subscription session with price > 0 should fail (Rule 11)");
        } catch (ValidationException e) {
            return pass("#10", "Double-billing prevented (sub price > 0 blocked) -> " + e.getMessage());
        }
    }

    private boolean test11SessionTransitionsAndNoShow() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        // Session #2 is REQUESTED
        try {
            helper.completeSession(2);
            return fail("#11", "REQUESTED -> COMPLETED without CONFIRMED should fail (Rule 18)");
        } catch (ValidationException expected) {
            helper.confirmSession(2);
            helper.markNoShow(2);
            int noShows = helper.countTrainerNoShows(2);
            if (noShows < 2) {
                return fail("#11", "Trainer #2 NO_SHOW count should reflect marked session (Rule 2)");
            }
            return pass("#11", "Rule 18 illegal transition caught & Trainer #2 NO_SHOWs=" + noShows);
        }
    }

    private boolean test12SessionCancel2hRule() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        // Session #1 is tomorrow -> >= 2h notice -> free cancellation
        String notice = helper.cancelSession(1, "Work travel conflict");
        Session cancelled = new SessionDao().findById(1);
        if (cancelled.getStatus() != SessionStatus.CANCELLED || notice.indexOf("Free cancellation") < 0) {
            return fail("#12", "Expected free cancellation status message for tomorrow session");
        }
        return pass("#12", notice);
    }

    private boolean test13AdminOverrideBypass() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        Administrator admin = helper.authenticate("admin", "admin123");
        // Slot #2 is FULL; adminReserve succeeds anyway per Rule 1 & 16
        Session vip = helper.adminReserve(admin, 1, 1, LocalDate.now().plusDays(1).toString(),
                "12:00", 60, 2, new BigDecimal("80.00"), "VIP Admin Override");
        if (vip.getStatus() != SessionStatus.CONFIRMED) {
            return fail("#13", "Admin override expected CONFIRMED session");
        }
        return pass("#13", "ADMIN override booked FULL Slot #2 as Session #" + vip.getId() + " (Rule 1 & 16)");
    }

    private boolean test14CheckInDeniedForInvalidMember() throws DatabaseException {
        GymRulesHelper helper = new GymRulesHelper();
        String today = LocalDate.now().toString();
        try {
            // Customer #3 has EXPIRED membership
            helper.checkIn(3, today, "10:00");
            return fail("#14", "Expired membership check-in should fail (Rule 5)");
        } catch (ValidationException expected) {
            return pass("#14", "Expired membership check-in blocked -> " + expected.getMessage());
        }
    }

    private boolean test15CheckInSameDayReuse() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        String today = LocalDate.now().toString();
        AttendanceDao attDao = new AttendanceDao();
        int beforeCount = attDao.findByCustomerId(1).size();
        Attendance reused = helper.checkIn(1, today, "09:15");
        int afterCount = attDao.findByCustomerId(1).size();
        if (beforeCount != afterCount || !"09:15".equals(reused.getCheckIn())) {
            return fail("#15", "Second check-in today should reuse existing row instead of duplicating");
        }
        return pass("#15", "Same-day check-in reused Attendance #" + reused.getId() + " (Rule 5)");
    }

    private boolean test16MembershipFreezeUnfreezeRenew() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        Membership before = new MembershipDao().findById(4); // c4 FROZEN 10 days
        String oldEnd = before.getEndDate();
        Membership unfrozen = helper.unfreezeMembership(4);
        String expectedEnd = LocalDate.parse(oldEnd).plusDays(10).toString();
        if (!expectedEnd.equals(unfrozen.getEndDate()) || unfrozen.getFrozenDays() != 0) {
            return fail("#16", "Unfreeze failed to shift endDate by 10 days");
        }
        try {
            helper.renewMembership(5); // CANCELLED plan
            return fail("#16", "Renewing CANCELLED membership should fail (Rule 7)");
        } catch (ValidationException expected) {
            return pass("#16", "Unfreeze shifted endDate (+10d -> " + unfrozen.getEndDate() + ") & CANCELLED renew blocked");
        }
    }

    private boolean test17SuspendCascadesFreeze() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        int frozenCount = helper.suspendCustomer(2); // Customer #2 has 1 ACTIVE plan
        Membership m2 = new MembershipDao().findById(2);
        if (frozenCount != 1 || m2.getStatus() != MembershipStatus.FROZEN) {
            return fail("#17", "Expected Customer #2 suspension to freeze active Membership #2 (Rule 14)");
        }
        return pass("#17", "Suspend Member #2 cascaded FROZEN to " + frozenCount + " active membership (Rule 14)");
    }

    private boolean test18MedicalProfileRules() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        MedicalProfileDao medDao = new MedicalProfileDao();
        try {
            // Customer #1 already has MedicalProfile #1
            medDao.insert(new MedicalProfile(0, 1, "A+", "None", "None", "None", "None",
                    "Dr", "01000000000", "Dup", LocalDate.now().toString()));
            return fail("#18", "Second medical profile for same customer should fail (Rule 3)");
        } catch (ValidationException expected) {
            try {
                // Trainer #2 is NOT assigned to Customer #1
                helper.getMedicalProfileForTrainer(2, 1);
                return fail("#18", "Unassigned trainer read medical profile should fail (Rule 15)");
            } catch (ValidationException denied) {
                MedicalProfile allowed = helper.getMedicalProfileForTrainer(1, 1);
                return pass("#18", "Rule 3 single profile enforced & Rule 15 trainer access ok (Blood="
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
        GymRulesHelper helper = new GymRulesHelper();
        try {
            // 0 target links breaks Rule 12 XOR
            payDao.insert(new Payment(0, 1, null, null, null, new BigDecimal("50.00"),
                    PaymentMethod.CASH, LocalDate.now().toString(), "REC-BAD-0", false));
            return fail("#20", "0-link payment should fail Rule 12 XOR");
        } catch (ValidationException expected) {
            try {
                // Mismatched customerId breaks Rule 12 (Membership #2 belongs to Customer #2, not #1)
                payDao.insert(Payment.forMembership(1, 2, new BigDecimal("60.00"),
                        PaymentMethod.CASH, LocalDate.now().toString(), "REC-BAD-OWNER"));
                return fail("#20", "Mismatched owner payment should fail Rule 12");
            } catch (ValidationException ownerEx) {
                // Void existing Payment #4 and check Rule 6 balance increases
                BigDecimal beforeVoidOwed = helper.computeOutstandingBalance(2);
                helper.voidAndReplacePayment(4, "Card chargeback", LocalDate.now().toString(),
                        null, null, null);
                BigDecimal afterVoidOwed = helper.computeOutstandingBalance(2);
                if (afterVoidOwed.compareTo(beforeVoidOwed) <= 0) {
                    return fail("#20", "Voided payment must not count toward balance (Rule 6 & 19)");
                }
                return pass("#20", "Rule 12 XOR + Rule 19 Void + Rule 6 Balance verified ($"
                        + beforeVoidOwed.toPlainString() + " -> $" + afterVoidOwed.toPlainString() + ")");
            }
        }
    }

    private boolean test21HardDeleteGuard() throws DatabaseException, ValidationException {
        GymRulesHelper helper = new GymRulesHelper();
        Administrator admin = helper.authenticate("admin", "admin123");
        try {
            helper.hardDeleteCustomer(admin, 1);
            return fail("#21", "Hard delete of Customer #1 with child records should fail (Rule 4)");
        } catch (ValidationException expected) {
            Equipment standalone = new EquipmentDao().insert(new Equipment(0, "Temp Rope", "RECOVERY",
                    LocalDate.now().toString(), EquipmentCondition.NEW, EquipmentStatus.AVAILABLE,
                    LocalDate.now().toString()));
            new EquipmentDao().delete(standalone.getId());
            return pass("#21", "Hard delete refused referenced member (#1); standalone delete succeeded");
        }
    }

    private boolean test22SerializationAndDatabaseException() {
        GymRulesHelper helper = new GymRulesHelper();
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

