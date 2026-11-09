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
import java.util.ArrayList;
import java.util.List;

// Database operations for staff and administrators
public class AdministratorDao {

    public Administrator insert(Administrator admin) throws DatabaseException, ValidationException {
        validate(admin);
        String sql = "INSERT INTO administrators (full_name, username, password_hash, role, phone, email, active, created_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, admin.getFullName());
            ps.setString(2, admin.getUsername());
            ps.setString(3, admin.getPasswordHash());
            ps.setString(4, admin.getRole() != null ? admin.getRole().name() : StaffRole.STAFF.name());
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
        validate(admin);
        String sql = "UPDATE administrators SET full_name = ?, username = ?, password_hash = ?, role = ?, "
                + "phone = ?, email = ?, active = ?, created_date = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, admin.getFullName());
            ps.setString(2, admin.getUsername());
            ps.setString(3, admin.getPasswordHash());
            ps.setString(4, admin.getRole() != null ? admin.getRole().name() : StaffRole.STAFF.name());
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

    private void validate(Administrator a) throws ValidationException {
        if (a == null) {
            throw new ValidationException("Administrator cannot be null");
        }
        if (a.getUsername() == null || a.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }
    }
}
