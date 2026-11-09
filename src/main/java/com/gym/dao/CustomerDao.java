package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// Database operations for customers
public class CustomerDao {

    public Customer insert(Customer customer) throws DatabaseException, ValidationException {
        validate(customer);
        String sql = "INSERT INTO customers (full_name, phone, email, address, date_of_birth, gender, "
                + "emergency_name, emergency_phone, status, join_date, trainer_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setString(5, customer.getDateOfBirth());
            ps.setString(6, customer.getGender());
            ps.setString(7, customer.getEmergencyName());
            ps.setString(8, customer.getEmergencyPhone());
            ps.setString(9, customer.getStatus() != null ? customer.getStatus().name() : CustomerStatus.ACTIVE.name());
            ps.setString(10, customer.getJoinDate());
            if (customer.getTrainerId() != null) {
                ps.setInt(11, customer.getTrainerId());
            } else {
                ps.setNull(11, Types.INTEGER);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    customer.setId(rs.getInt(1));
                }
            }
            return customer;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert customer", e);
        }
    }

    public void update(Customer customer) throws DatabaseException, ValidationException {
        validate(customer);
        String sql = "UPDATE customers SET full_name = ?, phone = ?, email = ?, address = ?, date_of_birth = ?, "
                + "gender = ?, emergency_name = ?, emergency_phone = ?, status = ?, join_date = ?, trainer_id = ? "
                + "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setString(5, customer.getDateOfBirth());
            ps.setString(6, customer.getGender());
            ps.setString(7, customer.getEmergencyName());
            ps.setString(8, customer.getEmergencyPhone());
            ps.setString(9, customer.getStatus() != null ? customer.getStatus().name() : CustomerStatus.ACTIVE.name());
            ps.setString(10, customer.getJoinDate());
            if (customer.getTrainerId() != null) {
                ps.setInt(11, customer.getTrainerId());
            } else {
                ps.setNull(11, Types.INTEGER);
            }
            ps.setInt(12, customer.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update customer with id: " + customer.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete customer with id: " + id, e);
        }
    }

    public Customer findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM customers WHERE id = ?";
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
            throw new DatabaseException("Failed to find customer with id: " + id, e);
        }
    }

    public List<Customer> findAll() throws DatabaseException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load customers", e);
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setDateOfBirth(rs.getString("date_of_birth"));
        c.setGender(rs.getString("gender"));
        c.setEmergencyName(rs.getString("emergency_name"));
        c.setEmergencyPhone(rs.getString("emergency_phone"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                c.setStatus(CustomerStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                c.setStatus(CustomerStatus.ACTIVE);
            }
        }
        c.setJoinDate(rs.getString("join_date"));
        int trainerIdVal = rs.getInt("trainer_id");
        c.setTrainerId(rs.wasNull() ? null : trainerIdVal);
        return c;
    }

    private void validate(Customer c) throws ValidationException {
        if (c == null) {
            throw new ValidationException("Customer cannot be null");
        }
        if (c.getFullName() == null || c.getFullName().trim().isEmpty()) {
            throw new ValidationException("Customer name is required");
        }
    }
}
