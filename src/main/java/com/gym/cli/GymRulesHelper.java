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
import com.gym.model.MedicalProfile;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Payment;
import com.gym.model.PaymentMethod;
import com.gym.model.PlanType;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.StaffRole;
import com.gym.model.SubscriptionStatus;
import com.gym.model.TrainerAvailability;
import com.gym.model.TrainerSubscription;
import com.gym.util.DatabaseConnection;
import com.gym.util.SerializationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// Coordinates cross-entity business rules 1-19 using the 13 concrete DAOs
public class GymRulesHelper {

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

    // Rule 16: Authenticate user and reject inactive accounts
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
            throw new ValidationException("Rule 16: Account '" + admin.getUsername() + "' is inactive and cannot log in");
        }
        return admin;
    }

    // Rule 16: Enforce ADMIN role for restricted actions
    public void requireAdmin(Administrator actor) throws ValidationException {
        if (actor == null || actor.getRole() != StaffRole.ADMIN) {
            String current = actor != null && actor.getRole() != null ? actor.getRole().name() : "NONE";
            throw new ValidationException("Rule 16: StaffRole.ADMIN required (current role: " + current + ")");
        }
    }

    // Rule 4: Soft delete customer sets INACTIVE
    public void softDeleteCustomer(int customerId) throws DatabaseException, ValidationException {
        Customer c = customerDao.findById(customerId);
        if (c == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        c.setStatus(CustomerStatus.INACTIVE);
        customerDao.update(c);
    }

    // Rule 4: Admin-only hard delete refuses while child references exist
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
            throw new ValidationException("Rule 4: Hard delete refused - customer #" + customerId
                    + " still has " + refs + " referenced record(s). Use soft-delete (INACTIVE) instead.");
        }
        customerDao.delete(customerId);
    }

    // Rule 4: Admin-only trainer hard delete refuses while references exist
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
            throw new ValidationException("Rule 4: Hard delete refused - trainer #" + trainerId
                    + " still has " + refs + " linked record(s).");
        }
        trainerDao.delete(trainerId);
    }

    // Rule 14: Suspending a customer freezes all active memberships
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

    // Rule 7: Only ACTIVE membership can be frozen
    public void freezeMembership(int membershipId, int frozenDays) throws DatabaseException, ValidationException {
        Membership m = membershipDao.findById(membershipId);
        if (m == null) {
            throw new ValidationException("Membership not found with id: " + membershipId);
        }
        if (m.getStatus() != MembershipStatus.ACTIVE) {
            throw new ValidationException("Rule 7: Only ACTIVE memberships can be frozen (current: " + m.getStatus() + ")");
        }
        if (frozenDays <= 0) {
            throw new ValidationException("Freeze days must be greater than zero");
        }
        m.setStatus(MembershipStatus.FROZEN);
        m.setFrozenDays(frozenDays);
        membershipDao.update(m);
    }

    // Rule 7: Unfreeze shifts endDate forward by frozenDays and clears frozenDays
    public Membership unfreezeMembership(int membershipId) throws DatabaseException, ValidationException {
        Membership m = membershipDao.findById(membershipId);
        if (m == null) {
            throw new ValidationException("Membership not found with id: " + membershipId);
        }
        if (m.getStatus() != MembershipStatus.FROZEN) {
            throw new ValidationException("Rule 7: Only FROZEN memberships can be unfrozen (current: " + m.getStatus() + ")");
        }
        LocalDate newEnd = LocalDate.parse(m.getEndDate()).plusDays(m.getFrozenDays());
        m.setEndDate(newEnd.toString());
        m.setFrozenDays(0);
        m.setStatus(MembershipStatus.ACTIVE);
        membershipDao.update(m);
        return m;
    }

    // Rule 7: Renew extends endDate for ACTIVE or EXPIRED plans; refuses CANCELLED
    public Membership renewMembership(int membershipId) throws DatabaseException, ValidationException {
        Membership m = membershipDao.findById(membershipId);
        if (m == null) {
            throw new ValidationException("Membership not found with id: " + membershipId);
        }
        if (m.getStatus() == MembershipStatus.CANCELLED) {
            throw new ValidationException("Rule 7: Renewing a CANCELLED membership is refused");
        }
        int days = daysForPlan(m.getPlan());
        LocalDate baseDate = LocalDate.parse(m.getEndDate());
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

    // Rule 13: Derived membership status (EXPIRED if endDate < today while stored ACTIVE)
    public MembershipStatus effectiveMembershipStatus(Membership m, String todayIso) {
        if (m.getStatus() == MembershipStatus.ACTIVE && m.getEndDate() != null && m.getEndDate().compareTo(todayIso) < 0) {
            return MembershipStatus.EXPIRED;
        }
        return m.getStatus();
    }

    // Rule 17: Occupancy counts CONFIRMED sessions only
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

    // Rule 13 & 17: Derived FULL status when CONFIRMED occupancy reaches maxCustomers
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

    // Rule 1, 11, 13, 17: Book a published OPEN availability slot directly as CONFIRMED
    public Session bookOpenSlot(int customerId, int availabilityId, int durationMin,
                                Integer subscriptionId, BigDecimal price, String notes)
            throws DatabaseException, ValidationException {
        TrainerAvailability slot = availabilityDao.findById(availabilityId);
        if (slot == null) {
            throw new ValidationException("Availability slot not found with id: " + availabilityId);
        }
        if (slot.getStatus() == AvailabilityStatus.CLOSED) {
            throw new ValidationException("Rule 1: Cannot book a CLOSED slot");
        }
        int booked = countConfirmedInSlot(availabilityId);
        if (booked >= slot.getMaxCustomers()) {
            throw new ValidationException("Rule 17: Slot #" + availabilityId + " is FULL ("
                    + booked + "/" + slot.getMaxCustomers() + ")");
        }
        int finalDuration = durationMin > 0 ? durationMin : 60;
        BigDecimal finalPrice = subscriptionId != null ? BigDecimal.ZERO : (price != null ? price : BigDecimal.ZERO);

        Session s = new Session(0, customerId, slot.getTrainerId(), slot.getDate(),
                slot.getStartTime(), finalDuration, SessionStatus.CONFIRMED,
                availabilityId, subscriptionId, finalPrice, null, notes);
        return sessionDao.insert(s);
    }

    // Rule 1: Request session outside published slots -> REQUESTED (default 60m)
    public Session requestOutsideSlots(int customerId, int trainerId, String date, String startTime,
                                       int durationMin, Integer subscriptionId, BigDecimal price, String notes)
            throws DatabaseException, ValidationException {
        int finalDuration = durationMin > 0 ? durationMin : 60;
        BigDecimal finalPrice = subscriptionId != null ? BigDecimal.ZERO : (price != null ? price : BigDecimal.ZERO);
        Session s = new Session(0, customerId, trainerId, date, startTime, finalDuration,
                SessionStatus.REQUESTED, null, subscriptionId, finalPrice, null, notes);
        return sessionDao.insert(s);
    }

    // Rule 1 & 16: Admin override booking ignores slot cap/status -> CONFIRMED
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

    // Rule 18 & 17: Confirm a REQUESTED session (checking slot cap if attached to an availability slot)
    public void confirmSession(int sessionId) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        if (s.getAvailabilityId() != null) {
            TrainerAvailability slot = availabilityDao.findById(s.getAvailabilityId().intValue());
            if (slot != null && countConfirmedInSlot(slot.getId()) >= slot.getMaxCustomers()) {
                throw new ValidationException("Rule 17: Cannot confirm session - linked slot is FULL");
            }
        }
        s.setStatus(SessionStatus.CONFIRMED);
        sessionDao.update(s);
    }

    // Rule 18: Complete a CONFIRMED session
    public void completeSession(int sessionId) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        s.setStatus(SessionStatus.COMPLETED);
        sessionDao.update(s);
    }

    // Rule 2 & 18: Mark CONFIRMED session as NO_SHOW
    public void markNoShow(int sessionId) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        s.setStatus(SessionStatus.NO_SHOW);
        sessionDao.update(s);
    }

    // Rule 10 & 18: Cancel CONFIRMED session, computing 2h notice status
    public String cancelSession(int sessionId, String cancelReason) throws DatabaseException, ValidationException {
        Session s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new ValidationException("Session not found with id: " + sessionId);
        }
        if (cancelReason == null || cancelReason.trim().isEmpty()) {
            throw new ValidationException("Cancel reason is required (Rule 10)");
        }
        LocalDateTime startDateTime = LocalDateTime.of(
                LocalDate.parse(s.getDate()), LocalTime.parse(s.getStartTime()));
        long minutesUntilStart = Duration.between(LocalDateTime.now(), startDateTime).toMinutes();

        s.setStatus(SessionStatus.CANCELLED);
        s.setCancelReason(cancelReason.trim());
        sessionDao.update(s);

        if (minutesUntilStart >= 120) {
            return "Rule 10: Free cancellation (>= 2h notice, " + minutesUntilStart + " min remaining).";
        }
        return "Rule 10: Late cancellation (< 2h before start). Marked CANCELLED with reason recorded.";
    }

    // Rule 2: Count trainer NO_SHOW sessions for trainer statistics
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

    // Rule 5: Check-in validates ACTIVE status + unexpired plan and reuses today's existing row
    public Attendance checkIn(int customerId, String dateIso, String timeIso) throws DatabaseException, ValidationException {
        Customer cust = customerDao.findById(customerId);
        if (cust == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        if (cust.getStatus() != CustomerStatus.ACTIVE) {
            throw new ValidationException("Rule 5: Check-in denied because member status is " + cust.getStatus());
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
            throw new ValidationException("Rule 5: Check-in denied - no ACTIVE unexpired membership on " + dateIso);
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

    // Rule 15 & 3: Medical profile read check for assigned trainer
    public MedicalProfile getMedicalProfileForTrainer(int trainerId, int customerId)
            throws DatabaseException, ValidationException {
        Customer c = customerDao.findById(customerId);
        if (c == null) {
            throw new ValidationException("Customer not found with id: " + customerId);
        }
        if (c.getTrainerId() == null || c.getTrainerId().intValue() != trainerId) {
            throw new ValidationException("Rule 15: Access denied - trainer #" + trainerId
                    + " is not the assigned trainer for customer #" + customerId);
        }
        return medicalDao.findByCustomerId(customerId);
    }

    // Rule 6: Outstanding unpaid balance across memberships, subscriptions, and non-zero ad-hoc sessions
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
            if (ses.getStatus() == SessionStatus.CANCELLED || ses.getPrice() == null) {
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

    // Rule 19: Void an existing payment and optionally create a replacement payment
    public Payment voidAndReplacePayment(int paymentId, String voidReason, String todayIso,
                                         BigDecimal replacementAmount, PaymentMethod replacementMethod,
                                         String replacementReceiptNo) throws DatabaseException, ValidationException {
        Payment old = paymentDao.findById(paymentId);
        if (old == null) {
            throw new ValidationException("Payment not found with id: " + paymentId);
        }
        if (old.isVoided()) {
            throw new ValidationException("Rule 19: Payment #" + paymentId + " is already voided");
        }
        if (voidReason == null || voidReason.trim().isEmpty()) {
            throw new ValidationException("Rule 19: Void reason is required");
        }
        old.setPreviousAmount(old.getAmount());
        old.setVoided(true);
        old.setVoidReason(voidReason.trim());
        old.setVoidedDate(todayIso);
        paymentDao.update(old);

        if (replacementReceiptNo != null && !replacementReceiptNo.trim().isEmpty()) {
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
