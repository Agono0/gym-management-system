package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Equipment;
import com.gym.model.EquipmentCondition;
import com.gym.model.EquipmentStatus;
import com.gym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Database operations for gym equipment
public class EquipmentDao {

    public Equipment insert(Equipment equipment) throws DatabaseException, ValidationException {
        validate(equipment);
        String sql = "INSERT INTO equipment (name, category, purchase_date, condition, status, last_maintenance) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, equipment.getName());
            ps.setString(2, equipment.getCategory());
            ps.setString(3, equipment.getPurchaseDate());
            ps.setString(4, equipment.getCondition() != null ? equipment.getCondition().name() : EquipmentCondition.NEW.name());
            ps.setString(5, equipment.getStatus() != null ? equipment.getStatus().name() : EquipmentStatus.AVAILABLE.name());
            ps.setString(6, equipment.getLastMaintenance());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    equipment.setId(rs.getInt(1));
                }
            }
            return equipment;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert equipment", e);
        }
    }

    public void update(Equipment equipment) throws DatabaseException, ValidationException {
        validate(equipment);
        String sql = "UPDATE equipment SET name = ?, category = ?, purchase_date = ?, condition = ?, "
                + "status = ?, last_maintenance = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, equipment.getName());
            ps.setString(2, equipment.getCategory());
            ps.setString(3, equipment.getPurchaseDate());
            ps.setString(4, equipment.getCondition() != null ? equipment.getCondition().name() : EquipmentCondition.NEW.name());
            ps.setString(5, equipment.getStatus() != null ? equipment.getStatus().name() : EquipmentStatus.AVAILABLE.name());
            ps.setString(6, equipment.getLastMaintenance());
            ps.setInt(7, equipment.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update equipment with id: " + equipment.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM equipment WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete equipment with id: " + id, e);
        }
    }

    public Equipment findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM equipment WHERE id = ?";
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
            throw new DatabaseException("Failed to find equipment with id: " + id, e);
        }
    }

    public List<Equipment> findAll() throws DatabaseException {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT * FROM equipment ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load equipment", e);
        }
    }

    private Equipment mapRow(ResultSet rs) throws SQLException {
        Equipment eq = new Equipment();
        eq.setId(rs.getInt("id"));
        eq.setName(rs.getString("name"));
        eq.setCategory(rs.getString("category"));
        eq.setPurchaseDate(rs.getString("purchase_date"));
        String condStr = rs.getString("condition");
        if (condStr != null) {
            try {
                eq.setCondition(EquipmentCondition.valueOf(condStr));
            } catch (IllegalArgumentException e) {
                eq.setCondition(EquipmentCondition.GOOD);
            }
        }
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                eq.setStatus(EquipmentStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                eq.setStatus(EquipmentStatus.AVAILABLE);
            }
        }
        eq.setLastMaintenance(rs.getString("last_maintenance"));
        return eq;
    }

    private void validate(Equipment eq) throws ValidationException {
        if (eq == null) {
            throw new ValidationException("Equipment cannot be null");
        }
        if (eq.getName() == null || eq.getName().trim().isEmpty()) {
            throw new ValidationException("Equipment name is required");
        }
    }
}
