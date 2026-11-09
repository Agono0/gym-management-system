package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.PlanType;
import com.gym.util.DatabaseConnection;

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

// Database operations for memberships
public class MembershipDao {

    public Membership insert(Membership membership) throws DatabaseException, ValidationException {
        validate(membership, false);
        String sql = "INSERT INTO memberships (customer_id, plan, price, start_date, end_date, status, frozen_days) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, membership.getCustomerId());
            ps.setString(2, membership.getPlan().name());
            ps.setBigDecimal(3, membership.getPrice());
            ps.setString(4, membership.getStartDate());
            ps.setString(5, membership.getEndDate());
            ps.setString(6, membership.getStatus() != null ? membership.getStatus().name() : MembershipStatus.ACTIVE.name());
            ps.setInt(7, membership.getFrozenDays());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    membership.setId(rs.getInt(1));
                }
            }
            return membership;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert membership", e);
        }
    }

    public void update(Membership membership) throws DatabaseException, ValidationException {
        validate(membership, true);
        String sql = "UPDATE memberships SET customer_id = ?, plan = ?, price = ?, start_date = ?, "
                + "end_date = ?, status = ?, frozen_days = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, membership.getCustomerId());
            ps.setString(2, membership.getPlan().name());
            ps.setBigDecimal(3, membership.getPrice());
            ps.setString(4, membership.getStartDate());
            ps.setString(5, membership.getEndDate());
            ps.setString(6, membership.getStatus() != null ? membership.getStatus().name() : MembershipStatus.ACTIVE.name());
            ps.setInt(7, membership.getFrozenDays());
            ps.setInt(8, membership.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update membership with id: " + membership.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM memberships WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete membership with id: " + id, e);
        }
    }

    public Membership findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM memberships WHERE id = ?";
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
            throw new DatabaseException("Failed to find membership with id: " + id, e);
        }
    }

    public List<Membership> findByCustomerId(int customerId) throws DatabaseException {
        List<Membership> list = new ArrayList<>();
        String sql = "SELECT * FROM memberships WHERE customer_id = ? ORDER BY id DESC";
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
            throw new DatabaseException("Failed to load memberships for customer: " + customerId, e);
        }
    }

    public List<Membership> findAll() throws DatabaseException {
        List<Membership> list = new ArrayList<>();
        String sql = "SELECT * FROM memberships ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load memberships", e);
        }
    }

    private Membership mapRow(ResultSet rs) throws SQLException {
        Membership m = new Membership();
        m.setId(rs.getInt("id"));
        m.setCustomerId(rs.getInt("customer_id"));
        String planStr = rs.getString("plan");
        if (planStr != null) {
            try {
                m.setPlan(PlanType.valueOf(planStr));
            } catch (IllegalArgumentException e) {
                m.setPlan(PlanType.MONTHLY);
            }
        }
        m.setPrice(rs.getBigDecimal("price"));
        m.setStartDate(rs.getString("start_date"));
        m.setEndDate(rs.getString("end_date"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                m.setStatus(MembershipStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                m.setStatus(MembershipStatus.ACTIVE);
            }
        }
        m.setFrozenDays(rs.getInt("frozen_days"));
        return m;
    }

    private void validate(Membership m, boolean isUpdate) throws ValidationException, DatabaseException {
        if (m == null) {
            throw new ValidationException("Membership cannot be null");
        }
        if (m.getCustomerId() <= 0 || !customerExists(m.getCustomerId())) {
            throw new ValidationException("Customer does not exist with id: " + m.getCustomerId());
        }
        if (m.getPlan() == null) {
            throw new ValidationException("Membership plan type is required");
        }
        if (m.getPrice() == null || m.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Membership price cannot be negative");
        }
        if (m.getStartDate() == null || m.getStartDate().trim().isEmpty() || !isValidDate(m.getStartDate())) {
            throw new ValidationException("Start date must be in yyyy-MM-dd format");
        }
        if (m.getEndDate() == null || m.getEndDate().trim().isEmpty() || !isValidDate(m.getEndDate())) {
            throw new ValidationException("End date must be in yyyy-MM-dd format");
        }
        LocalDate start = LocalDate.parse(m.getStartDate().trim());
        LocalDate end = LocalDate.parse(m.getEndDate().trim());
        if (end.isBefore(start)) {
            throw new ValidationException("Membership end date cannot precede start date");
        }
        if (m.getFrozenDays() < 0) {
            throw new ValidationException("Frozen days cannot be negative");
        }
        if (isUpdate) {
            Membership existing = findById(m.getId());
            if (existing != null && existing.getStatus() == MembershipStatus.CANCELLED
                    && m.getStatus() != MembershipStatus.CANCELLED) {
                throw new ValidationException("Rule 7: Cancelled membership cannot be renewed or reactivated");
            }
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
