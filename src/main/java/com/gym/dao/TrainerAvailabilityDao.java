package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.AvailabilityStatus;
import com.gym.model.TrainerAvailability;
import com.gym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Database operations for trainer availability slots
public class TrainerAvailabilityDao {

    public TrainerAvailability insert(TrainerAvailability slot) throws DatabaseException, ValidationException {
        validate(slot);
        String sql = "INSERT INTO trainer_availability (trainer_id, slot_date, start_time, end_time, max_customers, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, slot.getTrainerId());
            ps.setString(2, slot.getDate());
            ps.setString(3, slot.getStartTime());
            ps.setString(4, slot.getEndTime());
            ps.setInt(5, slot.getMaxCustomers());
            ps.setString(6, slot.getStatus() != null ? slot.getStatus().name() : AvailabilityStatus.OPEN.name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    slot.setId(rs.getInt(1));
                }
            }
            return slot;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert trainer availability", e);
        }
    }

    public void update(TrainerAvailability slot) throws DatabaseException, ValidationException {
        validate(slot);
        String sql = "UPDATE trainer_availability SET trainer_id = ?, slot_date = ?, start_time = ?, "
                + "end_time = ?, max_customers = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, slot.getTrainerId());
            ps.setString(2, slot.getDate());
            ps.setString(3, slot.getStartTime());
            ps.setString(4, slot.getEndTime());
            ps.setInt(5, slot.getMaxCustomers());
            ps.setString(6, slot.getStatus() != null ? slot.getStatus().name() : AvailabilityStatus.OPEN.name());
            ps.setInt(7, slot.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update trainer availability with id: " + slot.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM trainer_availability WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete trainer availability with id: " + id, e);
        }
    }

    public TrainerAvailability findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM trainer_availability WHERE id = ?";
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
            throw new DatabaseException("Failed to find trainer availability with id: " + id, e);
        }
    }

    public List<TrainerAvailability> findByTrainerId(int trainerId) throws DatabaseException {
        List<TrainerAvailability> list = new ArrayList<>();
        String sql = "SELECT * FROM trainer_availability WHERE trainer_id = ? ORDER BY slot_date ASC, start_time ASC";
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
            throw new DatabaseException("Failed to load availability for trainer: " + trainerId, e);
        }
    }

    public List<TrainerAvailability> findAll() throws DatabaseException {
        List<TrainerAvailability> list = new ArrayList<>();
        String sql = "SELECT * FROM trainer_availability ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load trainer availability", e);
        }
    }

    private TrainerAvailability mapRow(ResultSet rs) throws SQLException {
        TrainerAvailability s = new TrainerAvailability();
        s.setId(rs.getInt("id"));
        s.setTrainerId(rs.getInt("trainer_id"));
        s.setDate(rs.getString("slot_date"));
        s.setStartTime(rs.getString("start_time"));
        s.setEndTime(rs.getString("end_time"));
        s.setMaxCustomers(rs.getInt("max_customers"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                s.setStatus(AvailabilityStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                s.setStatus(AvailabilityStatus.OPEN);
            }
        }
        return s;
    }

    private void validate(TrainerAvailability s) throws ValidationException, DatabaseException {
        if (s == null) {
            throw new ValidationException("Trainer availability cannot be null");
        }
        if (s.getTrainerId() <= 0 || !trainerExists(s.getTrainerId())) {
            throw new ValidationException("Trainer does not exist with id: " + s.getTrainerId());
        }
        if (s.getDate() == null || s.getDate().trim().isEmpty() || !isValidDate(s.getDate())) {
            throw new ValidationException("Slot date must be in yyyy-MM-dd format");
        }
        if (s.getStartTime() == null || !isValidTime(s.getStartTime())) {
            throw new ValidationException("Slot start time must be in HH:mm format");
        }
        if (s.getEndTime() == null || !isValidTime(s.getEndTime())) {
            throw new ValidationException("Slot end time must be in HH:mm format");
        }
        LocalTime start = LocalTime.parse(s.getStartTime().trim());
        LocalTime end = LocalTime.parse(s.getEndTime().trim());
        if (!start.isBefore(end)) {
            throw new ValidationException("Rule 8: Slot start time must precede end time");
        }
        if (s.getMaxCustomers() <= 0) {
            throw new ValidationException("Rule 8: Slot maxCustomers must be greater than zero");
        }
    }

    private boolean trainerExists(int trainerId) throws DatabaseException {
        String sql = "SELECT 1 FROM trainers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify trainer id: " + trainerId, e);
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
