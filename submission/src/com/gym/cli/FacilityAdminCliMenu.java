package com.gym.cli;

import com.gym.dao.AdministratorDao;
import com.gym.dao.AttendanceDao;
import com.gym.dao.EquipmentDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Administrator;
import com.gym.model.Attendance;
import com.gym.model.Equipment;
import com.gym.model.EquipmentCondition;
import com.gym.model.EquipmentStatus;
import com.gym.model.StaffRole;
import com.gym.util.DatabaseConnection;
import com.gym.util.SerializationUtil;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Terminal screens for Attendance, Equipment & Floor Map,
// Staff/Admin Controls, and Serialization/DB Tools
public class FacilityAdminCliMenu {

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
                            String out = input.readOptionalString("Corrected Check-Out (or blank)", a.getCheckOut());
                            a.setCheckOut(out != null && out.isEmpty() ? null : out);
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
                    StaffRole role = readStaffRole();
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
                            StaffRole newRole = readStaffRole();
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
                        } else {
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
        String[] zones = {"CARDIO", "STRENGTH", "FREE WEIGHTS", "RECOVERY"};
        for (int z = 0; z < zones.length; z++) {
            String zone = zones[z];
            System.out.println("  +--- ZONE: " + zone + " " + CliTheme.repeat('-', 58 - zone.length()) + "+");
            int found = 0;
            for (int i = 0; i < list.size(); i++) {
                Equipment eq = list.get(i);
                String cat = eq.getCategory() != null ? eq.getCategory().toUpperCase() : "";
                if (cat.equals(zone)) {
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

    private StaffRole readStaffRole() {
        System.out.println("  Role: [1] ADMIN  [2] MANAGER  [3] STAFF");
        String r = input.readLine("Select [1-3]: ");
        if ("1".equals(r)) {
            return StaffRole.ADMIN;
        }
        if ("2".equals(r)) {
            return StaffRole.MANAGER;
        }
        return StaffRole.STAFF;
    }
}
