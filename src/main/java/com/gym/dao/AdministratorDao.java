package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Administrator;
import com.gym.model.StaffRole;
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

// Database operations for staff and administrators
public class AdministratorDao {

    public Administrator insert(Administrator admin) throws DatabaseException, ValidationException {
        validate(admin, false);
        String sql = "INSERT INTO administrators (full_name, username, password_hash, role, phone, email, active, created_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, admin.getFullName().trim());
            ps.setString(2, admin.getUsername().trim());
            ps.setString(3, admin.getPasswordHash());
            ps.setString(4, admin.getRole().name());
            ps.setString(5, admin.getPhone());
            ps.setString(6, admin.getEmail());
            ps.setInt(7, admin.isActive() ? 1 : 0);
            ps.setString(8, admin.getCreatedDate());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    admin.setId(rs.getInt(1));
                }
            }
            return admin;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert administrator", e);
        }
    }

    public void update(Administrator admin) throws DatabaseException, ValidationException {
        validate(admin, true);
        String sql = "UPDATE administrators SET full_name = ?, username = ?, password_hash = ?, role = ?, "
                + "phone = ?, email = ?, active = ?, created_date = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, admin.getFullName().trim());
            ps.setString(2, admin.getUsername().trim());
            ps.setString(3, admin.getPasswordHash());
            ps.setString(4, admin.getRole().name());
            ps.setString(5, admin.getPhone());
            ps.setString(6, admin.getEmail());
            ps.setInt(7, admin.isActive() ? 1 : 0);
            ps.setString(8, admin.getCreatedDate());
            ps.setInt(9, admin.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update administrator with id: " + admin.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM administrators WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete administrator with id: " + id, e);
        }
    }

    public Administrator findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM administrators WHERE id = ?";
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
            throw new DatabaseException("Failed to find administrator with id: " + id, e);
        }
    }

    public Administrator findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM administrators WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find administrator by username: " + username, e);
        }
    }

    public List<Administrator> findAll() throws DatabaseException {
        List<Administrator> list = new ArrayList<>();
        String sql = "SELECT * FROM administrators ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load administrators", e);
        }
    }

    private Administrator mapRow(ResultSet rs) throws SQLException {
        Administrator a = new Administrator();
        a.setId(rs.getInt("id"));
        a.setFullName(rs.getString("full_name"));
        a.setUsername(rs.getString("username"));
        a.setPasswordHash(rs.getString("password_hash"));
        String roleStr = rs.getString("role");
        if (roleStr != null) {
            try {
                a.setRole(StaffRole.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                a.setRole(StaffRole.STAFF);
            }
        }
        a.setPhone(rs.getString("phone"));
        a.setEmail(rs.getString("email"));
        a.setActive(rs.getInt("active") == 1);
        a.setCreatedDate(rs.getString("created_date"));
        return a;
    }

    private void validate(Administrator a, boolean isUpdate) throws ValidationException, DatabaseException {
        if (a == null) {
            throw new ValidationException("Administrator cannot be null");
        }
        if (a.getFullName() == null || a.getFullName().trim().isEmpty()) {
            throw new ValidationException("Staff full name is required");
        }
        if (a.getUsername() == null || a.getUsername().trim().length() < 3 || a.getUsername().trim().contains(" ")) {
            throw new ValidationException("Username must be at least 3 characters with no spaces");
        }
        if (a.getPasswordHash() == null || a.getPasswordHash().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }
        if (a.getRole() == null) {
            throw new ValidationException("Staff role is required");
        }
        if (a.getEmail() != null && !a.getEmail().trim().isEmpty() && !isValidEmail(a.getEmail())) {
            throw new ValidationException("Invalid staff email format: " + a.getEmail());
        }
        if (a.getCreatedDate() != null && !a.getCreatedDate().trim().isEmpty() && !isValidDate(a.getCreatedDate())) {
            throw new ValidationException("Created date must be in yyyy-MM-dd format");
        }
        Administrator existing = findByUsername(a.getUsername().trim());
        if (existing != null && (!isUpdate || existing.getId() != a.getId())) {
            throw new ValidationException("Username already exists: " + a.getUsername().trim());
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
