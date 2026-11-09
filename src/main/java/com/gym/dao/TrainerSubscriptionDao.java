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
import java.util.ArrayList;
import java.util.List;

// Database operations for trainer subscriptions
public class TrainerSubscriptionDao {

    public TrainerSubscription insert(TrainerSubscription sub) throws DatabaseException, ValidationException {
        validate(sub);
        String sql = "INSERT INTO trainer_subscriptions (customer_id, trainer_id, start_date, end_date, "
                + "rate_per_period, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sub.getCustomerId());
            ps.setInt(2, sub.getTrainerId());
            ps.setString(3, sub.getStartDate());
            ps.setString(4, sub.getEndDate());
            ps.setBigDecimal(5, sub.getRatePerPeriod() != null ? sub.getRatePerPeriod() : BigDecimal.ZERO);
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
        validate(sub);
        String sql = "UPDATE trainer_subscriptions SET customer_id = ?, trainer_id = ?, start_date = ?, "
                + "end_date = ?, rate_per_period = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sub.getCustomerId());
            ps.setInt(2, sub.getTrainerId());
            ps.setString(3, sub.getStartDate());
            ps.setString(4, sub.getEndDate());
            ps.setBigDecimal(5, sub.getRatePerPeriod() != null ? sub.getRatePerPeriod() : BigDecimal.ZERO);
            ps.setString(6, sub.getStatus() != null ? sub.getStatus().name() : SubscriptionStatus.ACTIVE.name());
            ps.setInt(7, sub.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update trainer subscription with id: " + sub.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM trainer_subscriptions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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

    private void validate(TrainerSubscription s) throws ValidationException {
        if (s == null) {
            throw new ValidationException("Trainer subscription cannot be null");
        }
        if (s.getCustomerId() <= 0 || s.getTrainerId() <= 0) {
            throw new ValidationException("Valid customer and trainer ids are required");
        }
    }
}
