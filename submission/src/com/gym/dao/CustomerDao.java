package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.util.DatabaseConnection;
import com.gym.util.PhoneUtil;

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

// Database operations for customers
public class CustomerDao {

    public Customer insert(Customer customer) throws DatabaseException, ValidationException {
        validate(customer);
        String sql = "INSERT INTO customers (full_name, phone, email, address, date_of_birth, gender, "
                + "emergency_name, emergency_phone, status, join_date, trainer_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, customer.getFullName().trim());
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

            ps.setString(1, customer.getFullName().trim());
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

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No customer found with id: " + customer.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update customer with id: " + customer.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException, ValidationException {
        // Second layer of defense against orphan rows: refuse deleting a customer that other tables still reference,
        // so no orphan memberships/payments/sessions are left behind
        int refs = countReferences("memberships", "customer_id", id)
                + countReferences("payments", "customer_id", id)
                + countReferences("sessions", "customer_id", id)
                + countReferences("attendance", "customer_id", id)
                + countReferences("trainer_subscriptions", "customer_id", id)
                + countReferences("workout_programs", "customer_id", id)
                + countReferences("progress_records", "customer_id", id)
                + countReferences("medical_profiles", "customer_id", id);
        if (refs > 0) {
            throw new ValidationException("Customer #" + id + " still has " + refs
                    + " referenced record(s) and cannot be hard-deleted.");
        }
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No customer found with id: " + id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete customer with id: " + id, e);
        }
    }

    private int countReferences(String table, String column, int targetId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed counting references in " + table, e);
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

    private void validate(Customer c) throws ValidationException, DatabaseException {
        if (c == null) {
            throw new ValidationException("Customer cannot be null");
        }
        if (c.getFullName() == null || c.getFullName().trim().isEmpty()) {
            throw new ValidationException("Customer name is required");
        }
        if (c.getEmail() != null && !c.getEmail().trim().isEmpty() && !isValidEmail(c.getEmail())) {
            throw new ValidationException("Invalid customer email format: " + c.getEmail());
        }
        PhoneUtil.requireValid(c.getPhone(), "Customer phone");
        PhoneUtil.requireValid(c.getEmergencyPhone(), "Emergency phone");
        if (c.getDateOfBirth() != null && !c.getDateOfBirth().trim().isEmpty()) {
            if (!isValidDate(c.getDateOfBirth())) {
                throw new ValidationException("Date of birth must be in yyyy-MM-dd format");
            }
            if (LocalDate.parse(c.getDateOfBirth().trim()).isAfter(LocalDate.now())) {
                throw new ValidationException("Date of birth cannot be in the future");
            }
        }
        if (c.getJoinDate() != null && !c.getJoinDate().trim().isEmpty() && !isValidDate(c.getJoinDate())) {
            throw new ValidationException("Join date must be in yyyy-MM-dd format");
        }
        if (c.getTrainerId() != null) {
            if (c.getTrainerId().intValue() <= 0 || !trainerExists(c.getTrainerId().intValue())) {
                throw new ValidationException("Assigned trainer does not exist with id: " + c.getTrainerId());
            }
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

    private boolean isValidEmail(String email) {
        String trimmed = email.trim();
        int atPos = trimmed.indexOf('@');
        int dotPos = trimmed.lastIndexOf('.');
        boolean oneAtSign = atPos == trimmed.lastIndexOf('@');
        return oneAtSign && atPos > 0 && dotPos > atPos + 1 && dotPos < trimmed.length() - 1 && !trimmed.contains(" ");
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
