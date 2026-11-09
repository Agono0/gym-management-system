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

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update progress record with id: " + record.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM progress_records WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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

    private void validate(ProgressRecord r) throws ValidationException {
        if (r == null) {
            throw new ValidationException("Progress record cannot be null");
        }
        if (r.getCustomerId() <= 0) {
            throw new ValidationException("Valid customer id is required");
        }
        if (r.getWeightKg() <= 0) {
            throw new ValidationException("Weight must be greater than zero");
        }
    }
}
