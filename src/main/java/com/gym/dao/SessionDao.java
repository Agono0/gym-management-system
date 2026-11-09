package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Database operations for personal training sessions
public class SessionDao {

    public Session insert(Session session) throws DatabaseException, ValidationException {
        validate(session, false);
        String sql = "INSERT INTO sessions (customer_id, trainer_id, session_date, start_time, duration_min, "
                + "status, availability_id, subscription_id, price, cancel_reason, session_notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, session.getCustomerId());
            ps.setInt(2, session.getTrainerId());
            ps.setString(3, session.getDate());
            ps.setString(4, session.getStartTime());
            ps.setInt(5, session.getDurationMin());
            ps.setString(6, session.getStatus() != null ? session.getStatus().name() : SessionStatus.REQUESTED.name());
            if (session.getAvailabilityId() != null) {
                ps.setInt(7, session.getAvailabilityId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            if (session.getSubscriptionId() != null) {
                ps.setInt(8, session.getSubscriptionId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setBigDecimal(9, session.getPrice() != null ? session.getPrice() : BigDecimal.ZERO);
            ps.setString(10, session.getCancelReason());
            ps.setString(11, session.getSessionNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    session.setId(rs.getInt(1));
                }
            }
            return session;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert session", e);
        }
    }

    public void update(Session session) throws DatabaseException, ValidationException {
        validate(session, true);
        String sql = "UPDATE sessions SET customer_id = ?, trainer_id = ?, session_date = ?, start_time = ?, "
                + "duration_min = ?, status = ?, availability_id = ?, subscription_id = ?, "
                + "price = ?, cancel_reason = ?, session_notes = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, session.getCustomerId());
            ps.setInt(2, session.getTrainerId());
            ps.setString(3, session.getDate());
            ps.setString(4, session.getStartTime());
            ps.setInt(5, session.getDurationMin());
            ps.setString(6, session.getStatus() != null ? session.getStatus().name() : SessionStatus.REQUESTED.name());
            if (session.getAvailabilityId() != null) {
                ps.setInt(7, session.getAvailabilityId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            if (session.getSubscriptionId() != null) {
                ps.setInt(8, session.getSubscriptionId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setBigDecimal(9, session.getPrice() != null ? session.getPrice() : BigDecimal.ZERO);
            ps.setString(10, session.getCancelReason());
            ps.setString(11, session.getSessionNotes());
            ps.setInt(12, session.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update session with id: " + session.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM sessions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete session with id: " + id, e);
        }
    }

    public Session findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM sessions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find session with id: " + id, e);
        }
    }

    public List<Session> findByTrainerId(int trainerId) throws DatabaseException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM sessions WHERE trainer_id = ? ORDER BY session_date DESC, start_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load sessions for trainer: " + trainerId, e);
        }
    }

    public List<Session> findAll() throws DatabaseException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM sessions ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load sessions", e);
        }
    }

    private Session mapRow(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getInt("id"));
        s.setCustomerId(rs.getInt("customer_id"));
        s.setTrainerId(rs.getInt("trainer_id"));
        s.setDate(rs.getString("session_date"));
        s.setStartTime(rs.getString("start_time"));
        s.setDurationMin(rs.getInt("duration_min"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                s.setStatus(SessionStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                s.setStatus(SessionStatus.REQUESTED);
            }
        }
        int availId = rs.getInt("availability_id");
        s.setAvailabilityId(rs.wasNull() ? null : availId);
        int subId = rs.getInt("subscription_id");
        s.setSubscriptionId(rs.wasNull() ? null : subId);
        s.setPrice(rs.getBigDecimal("price"));
        s.setCancelReason(rs.getString("cancel_reason"));
        s.setSessionNotes(rs.getString("session_notes"));
        return s;
    }

    private void validate(Session s, boolean isUpdate) throws ValidationException, DatabaseException {
        if (s == null) {
            throw new ValidationException("Session cannot be null");
        }
        if (s.getCustomerId() <= 0 || !recordExists("customers", s.getCustomerId())) {
            throw new ValidationException("Customer does not exist with id: " + s.getCustomerId());
        }
        if (s.getTrainerId() <= 0 || !recordExists("trainers", s.getTrainerId())) {
            throw new ValidationException("Trainer does not exist with id: " + s.getTrainerId());
        }
        if (s.getDate() == null || !isValidDate(s.getDate())) {
            throw new ValidationException("Session date must be in yyyy-MM-dd format (Rule 9)");
        }
        if (s.getStartTime() == null || !isValidTime(s.getStartTime())) {
            throw new ValidationException("Session start time must be in HH:mm format (Rule 9)");
        }
        if (s.getDurationMin() <= 0) {
            throw new ValidationException("Session durationMin must be greater than zero");
        }
        if (s.getPrice() != null && s.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Session price cannot be negative");
        }
        if (s.getSubscriptionId() != null) {
            verifySubscriptionMatch(s.getSubscriptionId().intValue(), s.getCustomerId(), s.getTrainerId());
            if (s.getPrice() != null && s.getPrice().compareTo(BigDecimal.ZERO) != 0) {
                throw new ValidationException("Rule 11: Session covered by a subscription must have price == 0");
            }
        }
        if (s.getAvailabilityId() != null && !recordExists("trainer_availability", s.getAvailabilityId().intValue())) {
            throw new ValidationException("Availability slot does not exist with id: " + s.getAvailabilityId());
        }
        if (isUpdate) {
            Session existing = findById(s.getId());
            SessionStatus newStatus = s.getStatus() != null ? s.getStatus() : SessionStatus.REQUESTED;
            if (existing != null && existing.getStatus() != newStatus) {
                validateTransition(existing.getStatus(), newStatus);
            }
        }
    }

    private void validateTransition(SessionStatus oldStatus, SessionStatus newStatus) throws ValidationException {
        if (newStatus == SessionStatus.CONFIRMED && oldStatus != SessionStatus.REQUESTED) {
            throw new ValidationException("Rule 18: Only REQUESTED sessions may become CONFIRMED");
        }
        if ((newStatus == SessionStatus.COMPLETED
                || newStatus == SessionStatus.NO_SHOW
                || newStatus == SessionStatus.CANCELLED)
                && oldStatus != SessionStatus.CONFIRMED) {
            throw new ValidationException("Rule 18: Only CONFIRMED sessions may become " + newStatus);
        }
        if (newStatus == SessionStatus.REQUESTED && oldStatus != SessionStatus.REQUESTED) {
            throw new ValidationException("Rule 18: Cannot revert a session back to REQUESTED");
        }
    }

    private void verifySubscriptionMatch(int subId, int customerId, int trainerId) throws ValidationException, DatabaseException {
        String sql = "SELECT customer_id, trainer_id FROM trainer_subscriptions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Trainer subscription does not exist with id: " + subId);
                }
                if (rs.getInt("customer_id") != customerId || rs.getInt("trainer_id") != trainerId) {
                    throw new ValidationException("Rule 11: Session customer/trainer must match the linked subscription");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify trainer subscription id: " + subId, e);
        }
    }

    private boolean recordExists(String table, int id) throws DatabaseException {
        String sql = "SELECT 1 FROM " + table + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify record in " + table + " for id: " + id, e);
        }
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidTime(String value) {
        try {
            LocalTime.parse(value.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
