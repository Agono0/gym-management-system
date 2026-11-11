package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.ProgressRecord;
import com.gym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Database operations for member progress records
public class ProgressRecordDao {

    public ProgressRecord insert(ProgressRecord record) throws DatabaseException, ValidationException {
        validate(record);
        String sql = "INSERT INTO progress_records (customer_id, record_date, weight_kg, body_fat_pct, "
                + "measurements, target_weight_kg, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, record.getCustomerId());
            ps.setString(2, record.getDate());
            ps.setDouble(3, record.getWeightKg());
            ps.setDouble(4, record.getBodyFatPct());
            ps.setString(5, record.getMeasurements());
            if (record.getTargetWeightKg() != null) {
                ps.setDouble(6, record.getTargetWeightKg());
            } else {
                ps.setNull(6, Types.REAL);
            }
            ps.setString(7, record.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getInt(1));
                }
            }
            return record;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert progress record", e);
        }
    }

    public void update(ProgressRecord record) throws DatabaseException, ValidationException {
        validate(record);
        String sql = "UPDATE progress_records SET customer_id = ?, record_date = ?, weight_kg = ?, "
                + "body_fat_pct = ?, measurements = ?, target_weight_kg = ?, notes = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, record.getCustomerId());
            ps.setString(2, record.getDate());
            ps.setDouble(3, record.getWeightKg());
            ps.setDouble(4, record.getBodyFatPct());
            ps.setString(5, record.getMeasurements());
            if (record.getTargetWeightKg() != null) {
                ps.setDouble(6, record.getTargetWeightKg());
            } else {
                ps.setNull(6, Types.REAL);
            }
            ps.setString(7, record.getNotes());
            ps.setInt(8, record.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No progress record found with id: " + record.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update progress record with id: " + record.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException, ValidationException {
        String sql = "DELETE FROM progress_records WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No progress record found with id: " + id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete progress record with id: " + id, e);
        }
    }

    public ProgressRecord findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM progress_records WHERE id = ?";
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
            throw new DatabaseException("Failed to find progress record with id: " + id, e);
        }
    }

    public List<ProgressRecord> findByCustomerId(int customerId) throws DatabaseException {
        List<ProgressRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM progress_records WHERE customer_id = ? ORDER BY record_date DESC";
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
            throw new DatabaseException("Failed to load progress records for customer: " + customerId, e);
        }
    }

    public List<ProgressRecord> findAll() throws DatabaseException {
        List<ProgressRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM progress_records ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load progress records", e);
        }
    }

    private ProgressRecord mapRow(ResultSet rs) throws SQLException {
        ProgressRecord r = new ProgressRecord();
        r.setId(rs.getInt("id"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setDate(rs.getString("record_date"));
        r.setWeightKg(rs.getDouble("weight_kg"));
        r.setBodyFatPct(rs.getDouble("body_fat_pct"));
        r.setMeasurements(rs.getString("measurements"));
        double targetVal = rs.getDouble("target_weight_kg");
        r.setTargetWeightKg(rs.wasNull() ? null : targetVal);
        r.setNotes(rs.getString("notes"));
        return r;
    }

    private void validate(ProgressRecord r) throws ValidationException, DatabaseException {
        if (r == null) {
            throw new ValidationException("Progress record cannot be null");
        }
        if (r.getCustomerId() <= 0 || !customerExists(r.getCustomerId())) {
            throw new ValidationException("Customer does not exist with id: " + r.getCustomerId());
        }
        if (r.getDate() == null || r.getDate().trim().isEmpty() || !isValidDate(r.getDate())) {
            throw new ValidationException("Progress date must be in yyyy-MM-dd format");
        }
        // NaN and Infinity slip past the < and > checks below, so block them first
        if (Double.isNaN(r.getWeightKg()) || Double.isInfinite(r.getWeightKg()) || r.getWeightKg() <= 0) {
            throw new ValidationException("Weight must be a real number greater than zero");
        }
        if (Double.isNaN(r.getBodyFatPct()) || r.getBodyFatPct() < 0 || r.getBodyFatPct() > 100) {
            throw new ValidationException("Body fat percentage must be between 0 and 100");
        }
        if (r.getTargetWeightKg() != null) {
            double target = r.getTargetWeightKg().doubleValue();
            if (Double.isNaN(target) || Double.isInfinite(target) || target <= 0) {
                throw new ValidationException("Target weight must be a real number greater than zero");
            }
        }
    }

    private boolean customerExists(int customerId) throws DatabaseException {
        String sql = "SELECT 1 FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify customer id: " + customerId, e);
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
