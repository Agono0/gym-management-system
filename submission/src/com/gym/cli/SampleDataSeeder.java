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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

// Seeds repeatable sample gym data when the SQLite database starts empty
public class SampleDataSeeder {

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
