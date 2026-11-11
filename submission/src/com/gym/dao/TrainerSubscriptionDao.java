package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.SubscriptionStatus;
import com.gym.model.TrainerSubscription;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Database operations for trainer subscriptions
public class TrainerSubscriptionDao {

    public TrainerSubscription insert(TrainerSubscription sub) throws DatabaseException, ValidationException {
        validate(sub, false);
        String sql = "INSERT INTO trainer_subscriptions (customer_id, trainer_id, start_date, end_date, "
                + "rate_per_period, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sub.getCustomerId());
            ps.setInt(2, sub.getTrainerId());
            ps.setString(3, sub.getStartDate());
            ps.setString(4, sub.getEndDate());
            ps.setBigDecimal(5, sub.getRatePerPeriod());
            ps.setString(6, sub.getStatus() != null ? sub.getStatus().name() : SubscriptionStatus.ACTIVE.name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    sub.setId(rs.getInt(1));
                }
            }
            return sub;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert trainer subscription", e);
        }
    }

    public void update(TrainerSubscription sub) throws DatabaseException, ValidationException {
        validate(sub, true);
        String sql = "UPDATE trainer_subscriptions SET customer_id = ?, trainer_id = ?, start_date = ?, "
                + "end_date = ?, rate_per_period = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sub.getCustomerId());
            ps.setInt(2, sub.getTrainerId());
            ps.setString(3, sub.getStartDate());
            ps.setString(4, sub.getEndDate());
            ps.setBigDecimal(5, sub.getRatePerPeriod());
            ps.setString(6, sub.getStatus() != null ? sub.getStatus().name() : SubscriptionStatus.ACTIVE.name());
            ps.setInt(7, sub.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No trainer subscription found with id: " + sub.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update trainer subscription with id: " + sub.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException, ValidationException {
        String sql = "DELETE FROM trainer_subscriptions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No trainer subscription found with id: " + id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete trainer subscription with id: " + id, e);
        }
    }

    public TrainerSubscription findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM trainer_subscriptions WHERE id = ?";
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
            throw new DatabaseException("Failed to find trainer subscription with id: " + id, e);
        }
    }

    public List<TrainerSubscription> findByCustomerId(int customerId) throws DatabaseException {
        List<TrainerSubscription> list = new ArrayList<>();
        String sql = "SELECT * FROM trainer_subscriptions WHERE customer_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load subscriptions for customer: " + customerId, e);
        }
    }

    public List<TrainerSubscription> findAll() throws DatabaseException {
        List<TrainerSubscription> list = new ArrayList<>();
        String sql = "SELECT * FROM trainer_subscriptions ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load trainer subscriptions", e);
        }
    }

    private TrainerSubscription mapRow(ResultSet rs) throws SQLException {
        TrainerSubscription s = new TrainerSubscription();
        s.setId(rs.getInt("id"));
        s.setCustomerId(rs.getInt("customer_id"));
        s.setTrainerId(rs.getInt("trainer_id"));
        s.setStartDate(rs.getString("start_date"));
        s.setEndDate(rs.getString("end_date"));
        s.setRatePerPeriod(rs.getBigDecimal("rate_per_period"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                s.setStatus(SubscriptionStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                s.setStatus(SubscriptionStatus.ACTIVE);
            }
        }
        return s;
    }

    private void validate(TrainerSubscription s, boolean isUpdate) throws ValidationException, DatabaseException {
        if (s == null) {
            throw new ValidationException("Trainer subscription cannot be null");
        }
        if (s.getCustomerId() <= 0 || s.getTrainerId() <= 0) {
            throw new ValidationException("Valid customer and trainer ids are required");
        }
        Integer assignedTrainerId = findAssignedTrainerId(s.getCustomerId());
        if (!recordExists("trainers", s.getTrainerId())) {
            throw new ValidationException("Trainer does not exist with id: " + s.getTrainerId());
        }
        // The trainer match is only checked when the subscription is created. If the member
        // is later moved to a new trainer, the old subscription must still be cancellable/renewable.
        if (!isUpdate && (assignedTrainerId == null || assignedTrainerId.intValue() != s.getTrainerId())) {
            throw new ValidationException("Subscription trainerId must match customer's assigned trainerId");
        }
        if (s.getStartDate() == null || !isValidDate(s.getStartDate()) || s.getEndDate() == null || !isValidDate(s.getEndDate())) {
            throw new ValidationException("Subscription start and end dates must be in yyyy-MM-dd format");
        }
        LocalDate start = LocalDate.parse(s.getStartDate().trim());
        LocalDate end = LocalDate.parse(s.getEndDate().trim());
        if (end.isBefore(start)) {
            throw new ValidationException("Subscription end date cannot precede start date");
        }
        if (s.getRatePerPeriod() == null || s.getRatePerPeriod().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Subscription rate cannot be negative");
        }
    }

    private Integer findAssignedTrainerId(int customerId) throws ValidationException, DatabaseException {
        String sql = "SELECT trainer_id FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Customer does not exist with id: " + customerId);
                }
                int trainerId = rs.getInt("trainer_id");
                return rs.wasNull() ? null : trainerId;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify customer trainer assignment", e);
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
}
