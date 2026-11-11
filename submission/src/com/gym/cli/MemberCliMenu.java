package com.gym.cli;

import com.gym.dao.CustomerDao;
import com.gym.dao.MedicalProfileDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.ProgressRecordDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.WorkoutProgramDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.MedicalProfile;
import com.gym.model.Membership;
import com.gym.model.ProgressRecord;
import com.gym.model.Trainer;
import com.gym.model.WorkoutProgram;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Terminal screens for Customer List, Member 360 Profile, Medical Profile, and Progress Records
public class MemberCliMenu {

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
                    ops.hardDeleteCustomer(session.getCurrentUser(), c.getId());
                    CliTheme.printSuccess("Member #" + c.getId() + " permanently deleted.");
                    input.pause();
                    return;
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
