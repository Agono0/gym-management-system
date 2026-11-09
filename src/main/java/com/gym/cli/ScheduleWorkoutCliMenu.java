package com.gym.cli;

import com.gym.dao.EquipmentDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.WorkoutProgramDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Equipment;
import com.gym.model.Session;
import com.gym.model.TrainerAvailability;
import com.gym.model.WorkoutProgram;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Terminal screens for Training Schedule / Sessions (Module 4) and Workouts + Live HUD (Module 5)
public class ScheduleWorkoutCliMenu {

    private final CliInput input;
    private final CliSession session;
    private final GymRulesHelper rules = new GymRulesHelper();
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

                System.out.println("  [1] Book Published OPEN Slot -> CONFIRMED (Rules 1, 11, 13, 17)");
                System.out.println("  [2] Request Session Outside Slots -> REQUESTED (Rule 1)");
                System.out.println("  [3] Admin Override Reserve -> CONFIRMED [ADMIN Only] (Rule 1 & 16)");
                System.out.println("  [4] Confirm REQUESTED Session (Rule 18)");
                System.out.println("  [5] Complete CONFIRMED Session (Rule 18)");
                System.out.println("  [6] Mark CONFIRMED Session as NO_SHOW (Rule 2 & 18)");
                System.out.println("  [7] Cancel CONFIRMED Session (Rule 10 2h Check & Rule 18)");
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
                        rules.confirmSession(sid.intValue());
                        CliTheme.printSuccess("Session #" + sid + " transitioned to CONFIRMED.");
                    }
                    input.pause();
                } else if ("5".equals(choice)) {
                    Integer sid = input.readInt("Session ID to mark COMPLETED: ", 1);
                    if (sid != null) {
                        rules.completeSession(sid.intValue());
                        CliTheme.printSuccess("Session #" + sid + " transitioned to COMPLETED.");
                    }
                    input.pause();
                } else if ("6".equals(choice)) {
                    Integer sid = input.readInt("Session ID to mark NO_SHOW: ", 1);
                    if (sid != null) {
                        rules.markNoShow(sid.intValue());
                        CliTheme.printSuccess("Session #" + sid + " marked NO_SHOW.");
                    }
                    input.pause();
                } else if ("7".equals(choice)) {
                    Integer sid = input.readInt("Session ID to cancel: ", 1);
                    if (sid != null) {
                        String reason = input.readRawString("Cancellation Reason (required): ");
                        String resultMsg = rules.cancelSession(sid.intValue(), reason);
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
                        workoutDao.delete(wid.intValue());
                        CliTheme.printSuccess("Deleted WorkoutProgram #" + wid);
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
            int booked = rules.countConfirmedInSlot(s.getId());
            System.out.println("  Slot #" + s.getId() + " | Coach #" + s.getTrainerId() + " | "
                    + s.getDate() + " " + s.getStartTime() + "-" + s.getEndTime()
                    + " | Occupancy: " + booked + "/" + s.getMaxCustomers()
                    + " [" + rules.effectiveSlotStatus(s) + "]");
        }
        Integer cid = input.readInt("Customer ID: ", 1);
        Integer slotId = input.readInt("Availability Slot ID: ", 1);
        Integer subId = readNullableInt("TrainerSubscription ID (Enter for pay-per-session, or ID for free): ");
        BigDecimal price = BigDecimal.ZERO;
        if (subId == null) {
            price = input.readBigDecimal("Session Price", new BigDecimal("45.00"));
        } else {
            price = input.readBigDecimal("Session Price (Rule 11 requires 0 for subscription)", BigDecimal.ZERO);
        }
        String notes = input.readOptionalString("Notes", "Open slot booking");
        if (cid != null && slotId != null) {
            Session saved = rules.bookOpenSlot(cid.intValue(), slotId.intValue(), 60, subId, price, notes);
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
            Session saved = rules.requestOutsideSlots(cid.intValue(), tid.intValue(),
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
            Session saved = rules.adminReserve(session.getCurrentUser(), cid.intValue(), tid.intValue(),
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
