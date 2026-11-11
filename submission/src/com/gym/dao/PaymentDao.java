package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Payment;
import com.gym.model.PaymentMethod;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
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

// Database operations for payments
public class PaymentDao {

    public Payment insert(Payment payment) throws DatabaseException, ValidationException {
        validate(payment, false);
        String sql = "INSERT INTO payments (customer_id, membership_id, subscription_id, session_id, "
                + "amount, method, payment_date, receipt_no, voided, void_reason, voided_date, previous_amount) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, payment.getCustomerId());
            if (payment.getMembershipId() != null) {
                ps.setInt(2, payment.getMembershipId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (payment.getSubscriptionId() != null) {
                ps.setInt(3, payment.getSubscriptionId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            if (payment.getSessionId() != null) {
                ps.setInt(4, payment.getSessionId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setBigDecimal(5, payment.getAmount());
            ps.setString(6, payment.getMethod().name());
            ps.setString(7, payment.getDate());
            ps.setString(8, payment.getReceiptNo().trim());
            ps.setInt(9, payment.isVoided() ? 1 : 0);
            ps.setString(10, payment.getVoidReason());
            ps.setString(11, payment.getVoidedDate());
            if (payment.getPreviousAmount() != null) {
                ps.setBigDecimal(12, payment.getPreviousAmount());
            } else {
                ps.setNull(12, Types.NUMERIC);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    payment.setId(rs.getInt(1));
                }
            }
            return payment;
        } catch (SQLException e) {
            // UNIQUE receipt race: same message as the app-level check
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new ValidationException("Duplicate receipt number: " + payment.getReceiptNo().trim());
            }
            throw new DatabaseException("Failed to insert payment", e);
        }
    }

    public void update(Payment payment) throws DatabaseException, ValidationException {
        validate(payment, true);
        String sql = "UPDATE payments SET customer_id = ?, membership_id = ?, subscription_id = ?, session_id = ?, "
                + "amount = ?, method = ?, payment_date = ?, receipt_no = ?, voided = ?, "
                + "void_reason = ?, voided_date = ?, previous_amount = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, payment.getCustomerId());
            if (payment.getMembershipId() != null) {
                ps.setInt(2, payment.getMembershipId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (payment.getSubscriptionId() != null) {
                ps.setInt(3, payment.getSubscriptionId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            if (payment.getSessionId() != null) {
                ps.setInt(4, payment.getSessionId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setBigDecimal(5, payment.getAmount());
            ps.setString(6, payment.getMethod().name());
            ps.setString(7, payment.getDate());
            ps.setString(8, payment.getReceiptNo().trim());
            ps.setInt(9, payment.isVoided() ? 1 : 0);
            ps.setString(10, payment.getVoidReason());
            ps.setString(11, payment.getVoidedDate());
            if (payment.getPreviousAmount() != null) {
                ps.setBigDecimal(12, payment.getPreviousAmount());
            } else {
                ps.setNull(12, Types.NUMERIC);
            }
            ps.setInt(13, payment.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No payment found with id: " + payment.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update payment with id: " + payment.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException, ValidationException {
        String sql = "DELETE FROM payments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No payment found with id: " + id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete payment with id: " + id, e);
        }
    }

    public Payment findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM payments WHERE id = ?";
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
            throw new DatabaseException("Failed to find payment with id: " + id, e);
        }
    }

    public Payment findByReceiptNo(String receiptNo) throws DatabaseException {
        String sql = "SELECT * FROM payments WHERE receipt_no = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receiptNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment with receipt: " + receiptNo, e);
        }
    }

    public List<Payment> findByCustomerId(int customerId) throws DatabaseException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE customer_id = ? ORDER BY payment_date DESC";
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
            throw new DatabaseException("Failed to load payments for customer: " + customerId, e);
        }
    }

    public List<Payment> findAll() throws DatabaseException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load payments", e);
        }
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        p.setCustomerId(rs.getInt("customer_id"));
        int memId = rs.getInt("membership_id");
        p.setMembershipId(rs.wasNull() ? null : memId);
        int subId = rs.getInt("subscription_id");
        p.setSubscriptionId(rs.wasNull() ? null : subId);
        int sesId = rs.getInt("session_id");
        p.setSessionId(rs.wasNull() ? null : sesId);
        p.setAmount(rs.getBigDecimal("amount"));
        String methStr = rs.getString("method");
        if (methStr != null) {
            try {
                p.setMethod(PaymentMethod.valueOf(methStr));
            } catch (IllegalArgumentException e) {
                p.setMethod(PaymentMethod.CASH);
            }
        }
        p.setDate(rs.getString("payment_date"));
        p.setReceiptNo(rs.getString("receipt_no"));
        p.setVoided(rs.getInt("voided") == 1);
        p.setVoidReason(rs.getString("void_reason"));
        p.setVoidedDate(rs.getString("voided_date"));
        p.setPreviousAmount(rs.getBigDecimal("previous_amount"));
        return p;
    }

    private void validate(Payment p, boolean isUpdate) throws ValidationException, DatabaseException {
        if (p == null) {
            throw new ValidationException("Payment cannot be null");
        }
        if (p.getCustomerId() <= 0 || !customerExists(p.getCustomerId())) {
            throw new ValidationException("Customer does not exist with id: " + p.getCustomerId());
        }
        if (p.getAmount() == null || p.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than zero");
        }
        if (p.getMethod() == null) {
            throw new ValidationException("Payment method is required");
        }
        if (p.getDate() == null || !isValidDate(p.getDate())) {
            throw new ValidationException("Payment date must be in yyyy-MM-dd format");
        }
        if (p.getVoidedDate() != null && !p.getVoidedDate().trim().isEmpty() && !isValidDate(p.getVoidedDate())) {
            throw new ValidationException("Voided date must be in yyyy-MM-dd format");
        }
        if (p.getReceiptNo() == null || p.getReceiptNo().trim().isEmpty()) {
            throw new ValidationException("Receipt number is required");
        }

        Payment duplicate = findByReceiptNo(p.getReceiptNo().trim());
        if (duplicate != null && (!isUpdate || duplicate.getId() != p.getId())) {
            throw new ValidationException("Duplicate receipt number: " + p.getReceiptNo().trim());
        }

        int links = 0;
        if (p.getMembershipId() != null) {
            links++;
        }
        if (p.getSubscriptionId() != null) {
            links++;
        }
        if (p.getSessionId() != null) {
            links++;
        }
        if (links != 1) {
            throw new ValidationException("Payment must link to exactly one target (membership, subscription, or session)");
        }

        if (p.getMembershipId() != null) {
            verifyLinkedOwner("memberships", p.getMembershipId().intValue(), p.getCustomerId());
        } else if (p.getSubscriptionId() != null) {
            verifyLinkedOwner("trainer_subscriptions", p.getSubscriptionId().intValue(), p.getCustomerId());
        } else if (p.getSessionId() != null) {
            verifyLinkedOwner("sessions", p.getSessionId().intValue(), p.getCustomerId());
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

    private void verifyLinkedOwner(String table, int targetId, int expectedCustomerId)
            throws ValidationException, DatabaseException {
        String sql = "SELECT customer_id FROM " + table + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Linked record does not exist in " + table + " with id " + targetId);
                }
                if (rs.getInt("customer_id") != expectedCustomerId) {
                    throw new ValidationException("Payment customerId (" + expectedCustomerId
                            + ") does not match linked record owner (" + rs.getInt("customer_id") + ")");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify payment target ownership", e);
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
