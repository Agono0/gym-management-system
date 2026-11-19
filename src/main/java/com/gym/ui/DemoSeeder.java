package com.gym.ui;

import com.gym.cli.SampleDataSeeder;
import com.gym.dao.AttendanceDao;
import com.gym.dao.CustomerDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.MedicalProfileDao;
import com.gym.dao.PaymentDao;
import com.gym.dao.ProgressRecordDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Attendance;
import com.gym.model.AvailabilityStatus;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.MedicalProfile;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Payment;
import com.gym.model.PaymentMethod;
import com.gym.model.PlanType;
import com.gym.model.ProgressRecord;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * First-run demo dataset for the GUI: extends the CLI's {@link SampleDataSeeder} base
 * (4 administrators, 3 trainers, 5 customers and one row set per remaining table) up to the
 * volume the screens need — 6 trainers, ~20 customers with valid Egyptian phones
 * ({@link PhoneUtil}), memberships across all {@link PlanType} values, sessions this week,
 * attendance spread over the past 8 weeks, medical profiles for some customers (rule 3) and
 * payments per rule 12.
 *
 * <p>Every insert goes through a DAO, so every row passes the DECISIONS.md 78 validation —
 * the dataset is seeded through the rules, not around them (a rule change that breaks a
 * candidate row fails loudly here instead of shipping inconsistent demo data).
 *
 * <p>Idempotency: the base seeder guards on the administrators table; this extension guards
 * on the customer count, and every dependent row (membership, medical profile, progress,
 * attendance, sessions, payments) is inserted only for customers created in the same pass,
 * so a partially completed run resumes without duplicating rows.
 *
 * <p>Must run off the FX thread (blocking JDBC) — screens call it via {@link Data#ensureReady()}.
 */
public final class DemoSeeder {

    /** One extension customer candidate; dependents derive from the flags (see seedRecords). */
    private record Candidate(String name, String phone, String email, String address,
                             String dateOfBirth, String gender, String emergencyName,
                             String emergencyPhone, CustomerStatus status, int trainerIndex,
                             PlanType plan, int joinDaysAgo, int membershipEndsInDays,
                             boolean withMedical, boolean withPayment, double weightKg) {
    }

    private static final int TARGET_CUSTOMERS = 20;

    private DemoSeeder() {
    }

    /**
     * Seeds the base dataset through {@link SampleDataSeeder} and then the GUI extension,
     * leaving ~20 customers and 6 trainers in a database that started empty. Safe to call
     * repeatedly; returns without writing when the dataset is already present.
     */
    public static void seedIfEmpty() throws DatabaseException, ValidationException {
        SampleDataSeeder.seedIfEmpty();

        CustomerDao customerDao = new CustomerDao();
        if (customerDao.findAll().size() >= TARGET_CUSTOMERS) {
            return;
        }
        seedRecords(customerDao);
    }

    private static void seedRecords(CustomerDao customerDao)
            throws DatabaseException, ValidationException {
        LocalDate today = LocalDate.now();

        // 1. Three more trainers (the base seeder provided the first three).
        TrainerDao trainerDao = new TrainerDao();
        Set<String> trainerNames = new HashSet<>();
        for (Trainer trainer : trainerDao.findAll()) {
            trainerNames.add(trainer.getFullName());
        }
        if (!trainerNames.contains("Coach Hana Farouk")) {
            trainerDao.insert(new Trainer(0, "Coach Hana Farouk", "01501234561",
                    "hana.f@gymos.local", "Yoga & Mobility", 6, "RYT-500, CPR",
                    "2023-09-05", new BigDecimal("48.00")));
        }
        if (!trainerNames.contains("Coach Mahmoud Saleh")) {
            trainerDao.insert(new Trainer(0, "Coach Mahmoud Saleh", "01011223344",
                    "mahmoud.s@gymos.local", "CrossFit & Endurance", 8, "CF-L3, NASM-PES",
                    "2021-11-20", new BigDecimal("55.00")));
        }
        if (!trainerNames.contains("Coach Yara Ibrahim")) {
            trainerDao.insert(new Trainer(0, "Coach Yara Ibrahim", "01222334455",
                    "yara.i@gymos.local", "Rehab & Pre/Postnatal", 4, "BSc PT, ACE-CPT",
                    "2024-05-15", new BigDecimal("42.00")));
        }
        List<Integer> trainerIds = new ArrayList<>();
        for (Trainer trainer : trainerDao.findAll()) {
            trainerIds.add(trainer.getId());
        }

        // 2. Extension customers up to the roster volume, each with its dependent rows.
        Customer[] created = createCustomers(customerDao, trainerIds, today);

        // 3. Past-8-week attendance for the base customers too, so the whole roster's
        //    activity column is alive; per-date skip keeps the base rows (c1 today,
        //    c2 yesterday) untouched (rule 5 duplicates are refused by the DAO).
        List<Customer> allCustomers = customerDao.findAll();
        if (created.length > 0) {
            seedBaseCustomerAttendance(customerDao, allCustomers);
        }
    }

    /**
     * Inserts the extension customers that are not present yet (matched by phone) with their
     * memberships, medical profiles, progress records, attendance, sessions and payments.
     * Returns the customers created by this pass; dependent rows are seeded only for them,
     * which is what makes a resumed run non-duplicating.
     */
    private static Customer[] createCustomers(CustomerDao customerDao, List<Integer> trainerIds,
                                              LocalDate today)
            throws DatabaseException, ValidationException {
        Set<String> phones = new HashSet<>();
        for (Customer customer : customerDao.findAll()) {
            phones.add(customer.getPhone());
        }

        List<Customer> created = new ArrayList<>();
        Candidate[] candidates = candidates();
        for (int i = 0; i < candidates.length; i++) {
            Candidate candidate = candidates[i];
            if (phones.contains(candidate.phone())) {
                continue;
            }
            Integer trainerId = trainerIds.isEmpty() ? null
                    : trainerIds.get(candidate.trainerIndex() % trainerIds.size());
            Customer customer = customerDao.insert(new Customer(0, candidate.name(),
                    candidate.phone(), candidate.email(), candidate.address(),
                    candidate.dateOfBirth(), candidate.gender(), candidate.emergencyName(),
                    candidate.emergencyPhone(), candidate.status(),
                    today.minusDays(candidate.joinDaysAgo()).toString(), trainerId));
            created.add(customer);

            seedMembership(customer, candidate, today);
            if (candidate.withMedical()) {
                seedMedicalProfile(customer, today, i);
            }
            if (candidate.weightKg() > 0) {
                seedProgressRecords(customer, candidate, today);
            }
            if (candidate.status() == CustomerStatus.ACTIVE) {
                seedAttendance(customer, today, i);
            }
        }

        seedSessionsAndPayments(created, today);
        return created.toArray(new Customer[0]);
    }

    /** The 15 extension customers (bringing the roster to ~20 with the CLI base). */
    private static Candidate[] candidates() {
        return new Candidate[] {
            new Candidate("Ahmed Salah", "01014256378", "ahmed.salah@mail.com",
                    "Nasr City, Cairo", "1997-03-14", "Male", "Salah Mohamed",
                    "01098765001", CustomerStatus.ACTIVE, 0, PlanType.YEARLY, 3, 365,
                    true, true, 88.0),
            new Candidate("Mona Hegazy", "01102567489", "mona.h@mail.com",
                    "Dokki, Giza", "1994-12-02", "Female", "Amal Hegazy",
                    "01187654002", CustomerStatus.ACTIVE, 1, PlanType.MONTHLY, 34, 5,
                    false, true, 64.5),
            new Candidate("Karim Shawky", "01503789152", "karim.sh@gym.io",
                    "Heliopolis, Cairo", "2001-07-19", "Male", "Shawky Ali",
                    "01576543003", CustomerStatus.ACTIVE, 2, PlanType.QUARTERLY, 22, 90,
                    false, false, 81.0),
            new Candidate("Fatma Zaki", "01244891263", "fatma.z@mail.com",
                    "Maadi, Cairo", "1999-05-25", "Female", "Zaki Ibrahim",
                    "01265432004", CustomerStatus.ACTIVE, 3, PlanType.MONTHLY, 9, 30,
                    true, true, 58.0),
            new Candidate("Youssef Hany", "01025912374", "youssef.h@gym.io",
                    "Zamalek, Cairo", "1996-10-08", "Male", "Hany Samir",
                    "01054321005", CustomerStatus.ACTIVE, 4, PlanType.MONTHLY, 28, 30,
                    false, false, 92.5),
            new Candidate("Dalia Mourad", "01136943485", "dalia.m@mail.com",
                    "Nasr City, Cairo", "1993-08-30", "Female", "Mourad Adel",
                    "01143210006", CustomerStatus.ACTIVE, 5, PlanType.QUARTERLY, 41, 90,
                    false, false, 60.0),
            new Candidate("Hazem Ehab", "01247094516", "hazem.e@gym.io",
                    "Dokki, Giza", "2000-01-22", "Male", "Ehab Tarek",
                    "01232100007", CustomerStatus.ACTIVE, 0, PlanType.MONTHLY, 16, 30,
                    true, false, 85.0),
            new Candidate("Nada Sherif", "01558205627", "nada.s@mail.com",
                    "Heliopolis, Cairo", "1998-02-17", "Female", "Sherif Kamal",
                    "01521000008", CustomerStatus.ACTIVE, 1, PlanType.MONTHLY, 5, 3,
                    false, true, 56.0),
            new Candidate("Amr Galal", "01069316738", "amr.g@gym.io",
                    "Dokki, Giza", "1995-06-11", "Male", "Galal Fouad",
                    "01010000009", CustomerStatus.ACTIVE, 2, PlanType.QUARTERLY, 45, 90,
                    false, true, 90.0),
            new Candidate("Reem Alaa", "01171427849", "reem.a@mail.com",
                    "Maadi, Cairo", "2002-09-09", "Female", "Alaa Hosny",
                    "01101000010", CustomerStatus.ACTIVE, 3, PlanType.MONTHLY, 20, 30,
                    true, false, 55.5),
            new Candidate("Ziad Magdy", "01282538950", "ziad.m@gym.io",
                    "Zamalek, Cairo", "1997-11-27", "Male", "Magdy Ramy",
                    "01201000011", CustomerStatus.ACTIVE, 4, PlanType.YEARLY, 32, 365,
                    false, true, 78.0),
            new Candidate("Habiba Tamer", "01593649161", "habiba.t@mail.com",
                    "Nasr City, Cairo", "2001-04-03", "Female", "Tamer Wael",
                    "01501000012", CustomerStatus.ACTIVE, 5, PlanType.MONTHLY, 26, 3,
                    false, false, 61.0),
            new Candidate("Seif Eldin Nasser", "01014759272", "seif.n@gym.io",
                    "Dokki, Giza", "1994-03-21", "Male", "Nasser Fouad",
                    "01002000013", CustomerStatus.SUSPENDED, 1, PlanType.MONTHLY, 48, 30,
                    true, false, 95.0),
            new Candidate("Malak Sameh", "01125860383", "malak.s@mail.com",
                    "Heliopolis, Cairo", "1999-12-15", "Female", "Sameh Reda",
                    "01103000014", CustomerStatus.SUSPENDED, 2, PlanType.QUARTERLY, 37, 90,
                    false, false, 63.0),
            new Candidate("Fady William", "01236971494", "fady.w@gym.io",
                    "Giza, Giza", "1988-08-05", "Male", "William Nasr",
                    "01204000015", CustomerStatus.INACTIVE, 0, PlanType.MONTHLY, 70, 0,
                    false, false, 0.0),
        };
    }

    /**
     * Membership per new customer: ACTIVE with the plan window by default, ending within
     * {@code membershipEndsInDays} when the candidate pins a short horizon (the expiring-soon
     * demo rows), FROZEN for SUSPENDED customers (rule 14) and CANCELLED for the INACTIVE one
     * (rule 4 lifecycle demo).
     */
    private static void seedMembership(Customer customer, Candidate candidate, LocalDate today)
            throws DatabaseException, ValidationException {
        MembershipStatus status = switch (candidate.status()) {
            case SUSPENDED -> MembershipStatus.FROZEN;
            case INACTIVE -> MembershipStatus.CANCELLED;
            case ACTIVE -> MembershipStatus.ACTIVE;
        };
        int endsInDays = candidate.membershipEndsInDays() > 0
                ? candidate.membershipEndsInDays() : planDays(candidate.plan());
        int startDaysAgo = Math.min(candidate.joinDaysAgo(), endsInDays);
        BigDecimal price = planPrice(candidate.plan());
        int frozenDays = status == MembershipStatus.FROZEN ? 14 : 0;
        new MembershipDao().insert(new Membership(0, customer.getId(), candidate.plan(),
                price, today.minusDays(startDaysAgo).toString(),
                today.plusDays(endsInDays).toString(), status, frozenDays));
    }

    /** One medical profile for the candidates flagged {@code withMedical} (rule 3: at most one). */
    private static void seedMedicalProfile(Customer customer, LocalDate today, int index)
            throws DatabaseException, ValidationException {
        String[] bloodTypes = {"O+", "A+", "B+", "AB-", "O-"};
        String[] injuries = {"None", "Mild lower-back tightness (2025)", "None",
                "Left ankle sprain, cleared 2025", "None"};
        new MedicalProfileDao().insert(new MedicalProfile(0, customer.getId(),
                bloodTypes[index % bloodTypes.length],
                index % 2 == 0 ? "None" : "Mild seasonal asthma",
                index % 2 == 0 ? "None" : "Peanuts",
                "None", injuries[index % injuries.length],
                "Dr. Sherif Kamel", "0110607110" + (1 + index % 9),
                "Cleared for supervised training", today.toString()));
    }

    /** Three progress records per flagged candidate: 8 weeks ago, 4 weeks ago, today. */
    private static void seedProgressRecords(Customer customer, Candidate candidate,
                                            LocalDate today)
            throws DatabaseException, ValidationException {
        double start = candidate.weightKg();
        double target = Math.round((start - 5.0) * 10.0) / 10.0;
        ProgressRecordDao progressDao = new ProgressRecordDao();
        progressDao.insert(new ProgressRecord(0, customer.getId(),
                today.minusWeeks(8).toString(), start, startBodyFat(start),
                "Baseline assessment", target, "Starting block"));
        progressDao.insert(new ProgressRecord(0, customer.getId(),
                today.minusWeeks(4).toString(), start - 1.8, startBodyFat(start) - 0.8,
                "Mid-block check-in", target, "Down 1.8 kg at week 4"));
        progressDao.insert(new ProgressRecord(0, customer.getId(),
                today.toString(), start - 3.2, startBodyFat(start) - 1.5,
                "Chest and waist re-measured", target, "Down 3.2 kg over 8 weeks"));
    }

    /** Plausible body-fat entry derived from the start weight (kept within the 0-100 rule). */
    private static double startBodyFat(double weightKg) {
        double value = weightKg < 70 ? 24.0 : 19.0;
        return Math.max(value, 0);
    }

    /**
     * Attendance over the past ~4 weeks for each new ACTIVE customer (rule 5 gating is the
     * DAO's job: the customer is ACTIVE by construction and the membership seeded above is
     * unexpired; a customer gets at most one row per day by construction of the offsets).
     */
    private static void seedAttendance(Customer customer, LocalDate today, int index)
            throws DatabaseException, ValidationException {
        int[] offsets = {1, 3, 5, 8, 10, 12, 15, 17, 19, 22, 24, 26};
        String[] checkIns = {"07:30", "12:15", "18:00", "20:30"};
        AttendanceDao attendanceDao = new AttendanceDao();
        for (int i = 0; i < offsets.length; i++) {
            if ((offsets[i] + index) % 2 != 0) {
                continue; // stagger the pattern per member so activity percentages differ
            }
            LocalTime checkIn = LocalTime.parse(checkIns[(i + index) % checkIns.length]);
            attendanceDao.insert(new Attendance(0, customer.getId(),
                    today.minusDays(offsets[i]).toString(),
                    checkIn.toString(), checkIn.plusMinutes(75).toString()));
        }
    }

    /**
     * Attendance for the base CLI customers (the first three by id): c1 and c2 get past-week
     * visits on dates their base rows do not already occupy; c3 keeps dates up to yesterday
     * where its base membership (ending yesterday) still satisfies rule 5.
     */
    private static void seedBaseCustomerAttendance(CustomerDao customerDao,
                                                   List<Customer> allCustomers)
            throws DatabaseException, ValidationException {
        LocalDate today = LocalDate.now();
        AttendanceDao attendanceDao = new AttendanceDao();
        int[][] baseOffsets = {
            {2, 4, 7, 9, 12, 14, 16, 19, 21, 23, 26, 28},   // c1: base row occupies today
            {3, 5, 8, 10, 13, 15, 17, 20, 22, 24, 27, 29},  // c2: base row occupies yesterday
            {3, 5, 8, 10, 13, 15, 17, 20, 22, 24, 26, 29},  // c3: membership ends yesterday
        };
        for (int c = 0; c < baseOffsets.length && c < allCustomers.size(); c++) {
            Customer customer = allCustomers.get(c);
            Set<String> existingDates = new HashSet<>();
            for (Attendance existing : attendanceDao.findByCustomerId(customer.getId())) {
                existingDates.add(existing.getDate());
            }
            for (int offset : baseOffsets[c]) {
                String date = today.minusDays(offset).toString();
                if (existingDates.contains(date)) {
                    continue;
                }
                LocalTime checkIn = LocalTime.of(8 + (offset % 3) * 3, 15);
                attendanceDao.insert(new Attendance(0, customer.getId(), date,
                        checkIn.toString(), checkIn.plusMinutes(75).toString()));
            }
        }
    }

    /**
     * Sessions this week and payments for the customers created in this pass. Two CONFIRMED
     * sessions book newly published OPEN slots (rule 1 bookOpenSlot) and one REQUESTED
     * session sits outside slots awaiting trainer confirmation; payments link to exactly one
     * membership each (rule 12) with unique receipt numbers.
     */
    private static void seedSessionsAndPayments(List<Customer> created, LocalDate today)
            throws DatabaseException, ValidationException {
        if (created.isEmpty()) {
            return;
        }
        TrainerAvailabilityDao availabilityDao = new TrainerAvailabilityDao();
        SessionDao sessionDao = new SessionDao();
        String tomorrow = today.plusDays(1).toString();
        String inTwoDays = today.plusDays(2).toString();

        if (!created.isEmpty() && created.get(0).getTrainerId() != null) {
            TrainerAvailability strengthSlot = availabilityDao.insert(
                    new TrainerAvailability(0, created.get(0).getTrainerId(), tomorrow,
                            "09:00", "10:00", 3, AvailabilityStatus.OPEN));
            sessionDao.insert(new Session(0, created.get(0).getId(),
                    created.get(0).getTrainerId(), tomorrow, "09:00", 60,
                    SessionStatus.CONFIRMED, strengthSlot.getId(), null,
                    new BigDecimal("48.00"), null, "Booked from the published slot"));
        }
        if (created.size() > 1 && created.get(1).getTrainerId() != null) {
            TrainerAvailability eveningSlot = availabilityDao.insert(
                    new TrainerAvailability(0, created.get(1).getTrainerId(), tomorrow,
                            "18:00", "19:00", 2, AvailabilityStatus.OPEN));
            sessionDao.insert(new Session(0, created.get(1).getId(),
                    created.get(1).getTrainerId(), tomorrow, "18:00", 60,
                    SessionStatus.CONFIRMED, eveningSlot.getId(), null,
                    new BigDecimal("55.00"), null, "Booked from the published slot"));
        }
        if (created.size() > 4 && created.get(4).getTrainerId() != null) {
            sessionDao.insert(new Session(0, created.get(4).getId(),
                    created.get(4).getTrainerId(), inTwoDays, "07:00", 60,
                    SessionStatus.REQUESTED, null, null, new BigDecimal("42.00"),
                    null, "Requested outside published slots"));
        }

        seedPayments(created, today);
    }

    /** Membership payments for the paid candidates (rule 12: one linked target, owner match). */
    private static void seedPayments(List<Customer> created, LocalDate today)
            throws DatabaseException, ValidationException {
        Set<String> receipts = new HashSet<>();
        PaymentDao paymentDao = new PaymentDao();
        for (Payment existing : paymentDao.findAll()) {
            receipts.add(existing.getReceiptNo());
        }
        int receiptCounter = 1005;
        for (Customer customer : created) {
            if (!isPaidCandidate(customer.getFullName())) {
                continue;
            }
            List<Membership> memberships = new MembershipDao().findByCustomerId(customer.getId());
            if (memberships.isEmpty()) {
                continue;
            }
            Membership membership = memberships.get(memberships.size() - 1);
            String receiptNo = "REC-" + receiptCounter;
            receiptCounter++;
            if (receipts.contains(receiptNo)) {
                continue; // a resumed run already issued this receipt
            }
            receipts.add(receiptNo);
            paymentDao.insert(Payment.forMembership(customer.getId(), membership.getId(),
                    membership.getPrice(),
                    receiptCounter % 2 == 0 ? PaymentMethod.CASH : PaymentMethod.CARD,
                    today.toString(), receiptNo));
        }
    }

    /** Which candidates arrive prepaid; the rest intentionally show outstanding balances. */
    private static boolean isPaidCandidate(String fullName) {
        return switch (fullName) {
            case "Ahmed Salah", "Mona Hegazy", "Fatma Zaki", "Nada Sherif",
                 "Amr Galal", "Ziad Magdy" -> true;
            default -> false;
        };
    }

    /** Plan window length in days, used when the candidate does not pin a horizon. */
    private static int planDays(PlanType plan) {
        return switch (plan) {
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case YEARLY -> 365;
        };
    }

    /** List price per plan (mirrors the CLI base seeder). */
    private static BigDecimal planPrice(PlanType plan) {
        return switch (plan) {
            case MONTHLY -> new BigDecimal("60.00");
            case QUARTERLY -> new BigDecimal("150.00");
            case YEARLY -> new BigDecimal("500.00");
        };
    }
}
