package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.MedicalProfile;
import com.gym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Database operations for medical profiles
public class MedicalProfileDao {

    public MedicalProfile insert(MedicalProfile profile) throws DatabaseException, ValidationException {
        validate(profile, false);
        String sql = "INSERT INTO medical_profiles (customer_id, blood_type, conditions, allergies, "
                + "medications, injuries, doctor_name, doctor_phone, notes, updated_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, profile.getCustomerId());
            ps.setString(2, profile.getBloodType());
            ps.setString(3, profile.getConditions());
            ps.setString(4, profile.getAllergies());
            ps.setString(5, profile.getMedications());
            ps.setString(6, profile.getInjuries());
            ps.setString(7, profile.getDoctorName());
            ps.setString(8, profile.getDoctorPhone());
            ps.setString(9, profile.getNotes());
            ps.setString(10, profile.getUpdatedDate());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    profile.setId(rs.getInt(1));
                }
            }
            return profile;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert medical profile", e);
        }
    }

    public void update(MedicalProfile profile) throws DatabaseException, ValidationException {
        validate(profile, true);
        String sql = "UPDATE medical_profiles SET customer_id = ?, blood_type = ?, conditions = ?, allergies = ?, "
                + "medications = ?, injuries = ?, doctor_name = ?, doctor_phone = ?, notes = ?, updated_date = ? "
                + "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, profile.getCustomerId());
            ps.setString(2, profile.getBloodType());
            ps.setString(3, profile.getConditions());
            ps.setString(4, profile.getAllergies());
            ps.setString(5, profile.getMedications());
            ps.setString(6, profile.getInjuries());
            ps.setString(7, profile.getDoctorName());
            ps.setString(8, profile.getDoctorPhone());
            ps.setString(9, profile.getNotes());
            ps.setString(10, profile.getUpdatedDate());
            ps.setInt(11, profile.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No medical profile found with id: " + profile.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update medical profile with id: " + profile.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException, ValidationException {
        String sql = "DELETE FROM medical_profiles WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No medical profile found with id: " + id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete medical profile with id: " + id, e);
        }
    }

    public MedicalProfile findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM medical_profiles WHERE id = ?";
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
            throw new DatabaseException("Failed to find medical profile with id: " + id, e);
        }
    }

    public MedicalProfile findByCustomerId(int customerId) throws DatabaseException {
        String sql = "SELECT * FROM medical_profiles WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load medical profile for customer: " + customerId, e);
        }
    }

    public List<MedicalProfile> findAll() throws DatabaseException {
        List<MedicalProfile> list = new ArrayList<>();
        String sql = "SELECT * FROM medical_profiles ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load medical profiles", e);
        }
    }

    private MedicalProfile mapRow(ResultSet rs) throws SQLException {
        MedicalProfile mp = new MedicalProfile();
        mp.setId(rs.getInt("id"));
        mp.setCustomerId(rs.getInt("customer_id"));
        mp.setBloodType(rs.getString("blood_type"));
        mp.setConditions(rs.getString("conditions"));
        mp.setAllergies(rs.getString("allergies"));
        mp.setMedications(rs.getString("medications"));
        mp.setInjuries(rs.getString("injuries"));
        mp.setDoctorName(rs.getString("doctor_name"));
        mp.setDoctorPhone(rs.getString("doctor_phone"));
        mp.setNotes(rs.getString("notes"));
        mp.setUpdatedDate(rs.getString("updated_date"));
        return mp;
    }

    private void validate(MedicalProfile mp, boolean isUpdate) throws ValidationException, DatabaseException {
        if (mp == null) {
            throw new ValidationException("Medical profile cannot be null");
        }
        if (mp.getCustomerId() <= 0 || !customerExists(mp.getCustomerId())) {
            throw new ValidationException("Customer does not exist with id: " + mp.getCustomerId());
        }
        if (mp.getUpdatedDate() != null && !mp.getUpdatedDate().trim().isEmpty() && !isValidDate(mp.getUpdatedDate())) {
            throw new ValidationException("Updated date must be in yyyy-MM-dd format");
        }
        MedicalProfile existing = findByCustomerId(mp.getCustomerId());
        if (existing != null && (!isUpdate || existing.getId() != mp.getId())) {
            throw new ValidationException("Customer " + mp.getCustomerId() + " already has a medical profile");
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
