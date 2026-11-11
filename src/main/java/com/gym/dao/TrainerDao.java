package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Trainer;
import com.gym.util.DatabaseConnection;
import com.gym.util.PhoneUtil;

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

// Database operations for trainers
public class TrainerDao {

    public Trainer insert(Trainer trainer) throws DatabaseException, ValidationException {
        validate(trainer);
        String sql = "INSERT INTO trainers (full_name, phone, email, specialization, experience_years, "
                + "certifications, hire_date, hourly_rate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, trainer.getFullName().trim());
            ps.setString(2, trainer.getPhone());
            ps.setString(3, trainer.getEmail());
            ps.setString(4, trainer.getSpecialization());
            ps.setInt(5, trainer.getExperienceYears());
            ps.setString(6, trainer.getCertifications());
            ps.setString(7, trainer.getHireDate());
            ps.setBigDecimal(8, trainer.getHourlyRate());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    trainer.setId(rs.getInt(1));
                }
            }
            return trainer;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert trainer", e);
        }
    }

    public void update(Trainer trainer) throws DatabaseException, ValidationException {
        validate(trainer);
        String sql = "UPDATE trainers SET full_name = ?, phone = ?, email = ?, specialization = ?, "
                + "experience_years = ?, certifications = ?, hire_date = ?, hourly_rate = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, trainer.getFullName().trim());
            ps.setString(2, trainer.getPhone());
            ps.setString(3, trainer.getEmail());
            ps.setString(4, trainer.getSpecialization());
            ps.setInt(5, trainer.getExperienceYears());
            ps.setString(6, trainer.getCertifications());
            ps.setString(7, trainer.getHireDate());
            ps.setBigDecimal(8, trainer.getHourlyRate());
            ps.setInt(9, trainer.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update trainer with id: " + trainer.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM trainers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete trainer with id: " + id, e);
        }
    }

    public Trainer findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM trainers WHERE id = ?";
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
            throw new DatabaseException("Failed to find trainer with id: " + id, e);
        }
    }

    public List<Trainer> findAll() throws DatabaseException {
        List<Trainer> list = new ArrayList<>();
        String sql = "SELECT * FROM trainers ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load trainers", e);
        }
    }

    private Trainer mapRow(ResultSet rs) throws SQLException {
        Trainer t = new Trainer();
        t.setId(rs.getInt("id"));
        t.setFullName(rs.getString("full_name"));
        t.setPhone(rs.getString("phone"));
        t.setEmail(rs.getString("email"));
        t.setSpecialization(rs.getString("specialization"));
        t.setExperienceYears(rs.getInt("experience_years"));
        t.setCertifications(rs.getString("certifications"));
        t.setHireDate(rs.getString("hire_date"));
        t.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        return t;
    }

    private void validate(Trainer t) throws ValidationException {
        if (t == null) {
            throw new ValidationException("Trainer cannot be null");
        }
        if (t.getFullName() == null || t.getFullName().trim().isEmpty()) {
            throw new ValidationException("Trainer name is required");
        }
        if (t.getEmail() != null && !t.getEmail().trim().isEmpty() && !isValidEmail(t.getEmail())) {
            throw new ValidationException("Invalid trainer email format: " + t.getEmail());
        }
        PhoneUtil.requireValid(t.getPhone(), "Trainer phone");
        if (t.getExperienceYears() < 0) {
            throw new ValidationException("Experience years cannot be negative");
        }
        if (t.getHireDate() != null && !t.getHireDate().trim().isEmpty() && !isValidDate(t.getHireDate())) {
            throw new ValidationException("Hire date must be in yyyy-MM-dd format");
        }
        if (t.getHourlyRate() == null || t.getHourlyRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Hourly rate must be zero or positive");
        }
    }

    private boolean isValidEmail(String email) {
        String trimmed = email.trim();
        int atPos = trimmed.indexOf('@');
        int dotPos = trimmed.lastIndexOf('.');
        return atPos > 0 && dotPos > atPos + 1 && dotPos < trimmed.length() - 1 && !trimmed.contains(" ");
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
