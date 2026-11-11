package com.gym.cli;

import com.gym.dao.CustomerDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.TrainerSubscriptionDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.AvailabilityStatus;
import com.gym.model.Customer;
import com.gym.model.MedicalProfile;
import com.gym.model.SubscriptionStatus;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;
import com.gym.model.TrainerSubscription;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Terminal screens for Coaches, Coach Profile, Availability Slots, and Trainer Subscriptions
public class TrainerCliMenu {

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
                    ops.hardDeleteTrainer(session.getCurrentUser(), t.getId());
                    CliTheme.printSuccess("Coach #" + t.getId() + " deleted.");
                    input.pause();
                    return;
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
                        if (s != null) {
                            s.setStatus(SubscriptionStatus.CANCELLED);
                            subDao.update(s);
                            CliTheme.printSuccess("Cancelled Subscription #" + s.getId());
                        } else {
                            CliTheme.printValidationError("Subscription not found: #" + subId);
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

