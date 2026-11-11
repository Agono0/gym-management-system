package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Attendance;
import com.gym.model.CustomerStatus;
import com.gym.model.MembershipStatus;
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

// Database operations for attendance check-ins
public class AttendanceDao {

    public Attendance insert(Attendance attendance) throws DatabaseException, ValidationException {
        validate(attendance, false);
        String sql = "INSERT INTO attendance (customer_id, attendance_date, check_in, check_out) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, attendance.getCustomerId());
            ps.setString(2, attendance.getDate());
            ps.setString(3, attendance.getCheckIn());
            ps.setString(4, attendance.getCheckOut());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    attendance.setId(rs.getInt(1));
                }
            }
            return attendance;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert attendance record", e);
        }
    }

    public void update(Attendance attendance) throws DatabaseException, ValidationException {
        validate(attendance, true);
        String sql = "UPDATE attendance SET customer_id = ?, attendance_date = ?, check_in = ?, check_out = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attendance.getCustomerId());
            ps.setString(2, attendance.getDate());
            ps.setString(3, attendance.getCheckIn());
            ps.setString(4, attendance.getCheckOut());
            ps.setInt(5, attendance.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No attendance record found with id: " + attendance.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update attendance with id: " + attendance.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException, ValidationException {
        String sql = "DELETE FROM attendance WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ValidationException("No attendance record found with id: " + id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete attendance with id: " + id, e);
        }
    }

    public Attendance findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM attendance WHERE id = ?";
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
            throw new DatabaseException("Failed to find attendance with id: " + id, e);
        }
    }

    public List<Attendance> findByCustomerId(int customerId) throws DatabaseException {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance WHERE customer_id = ? ORDER BY attendance_date DESC, check_in DESC";
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
            throw new DatabaseException("Failed to load attendance for customer: " + customerId, e);
        }
    }

    public List<Attendance> findAll() throws DatabaseException {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load attendance records", e);
        }
    }

    private Attendance mapRow(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setCustomerId(rs.getInt("customer_id"));
        a.setDate(rs.getString("attendance_date"));
        a.setCheckIn(rs.getString("check_in"));
        a.setCheckOut(rs.getString("check_out"));
        return a;
    }

    private void validate(Attendance a, boolean isUpdate) throws ValidationException, DatabaseException {
        if (a == null) {
            throw new ValidationException("Attendance record cannot be null");
        }
        if (a.getCustomerId() <= 0) {
            throw new ValidationException("Valid customer id is required");
        }
        if (a.getDate() == null || !isValidDate(a.getDate())) {
            throw new ValidationException("Attendance date must be in yyyy-MM-dd format");
        }
        if (a.getCheckIn() == null || !isValidTime(a.getCheckIn())) {
            throw new ValidationException("Check-in time must be in HH:mm format");
        }
        if (a.getCheckOut() != null && !a.getCheckOut().trim().isEmpty()) {
            if (!isValidTime(a.getCheckOut())) {
                throw new ValidationException("Check-out time must be in HH:mm format");
            }
            LocalTime inTime = LocalTime.parse(a.getCheckIn().trim());
            LocalTime outTime = LocalTime.parse(a.getCheckOut().trim());
            if (outTime.isBefore(inTime)) {
                throw new ValidationException("Check-out time cannot precede check-in time");
            }
        }
        if (!isUpdate) {
            checkCustomerAndMembershipEligibility(a.getCustomerId(), a.getDate().trim());
            if (sameDayRowExists(a.getCustomerId(), a.getDate().trim(), -1)) {
                throw new ValidationException("Member already checked in on " + a.getDate() + " (reuse existing row)");
            }
        } else {
            // The same-day check also applies when correcting a row: moving a record to a date that
            // already has another row for this member would create a duplicate day
            if (sameDayRowExists(a.getCustomerId(), a.getDate().trim(), a.getId())) {
                throw new ValidationException("Member already has another attendance row on " + a.getDate());
            }
        }
    }

    private void checkCustomerAndMembershipEligibility(int customerId, String date)
            throws ValidationException, DatabaseException {
        String custSql = "SELECT status FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(custSql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Customer does not exist with id: " + customerId);
                }
                String status = rs.getString("status");
                if (!CustomerStatus.ACTIVE.name().equals(status)) {
                    throw new ValidationException("Check-in blocked - customer status is " + status);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify customer status", e);
        }

        String memSql = "SELECT 1 FROM memberships WHERE customer_id = ? AND status = ? AND end_date >= ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(memSql)) {
            ps.setInt(1, customerId);
            ps.setString(2, MembershipStatus.ACTIVE.name());
            ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Check-in blocked - no ACTIVE unexpired membership for " + date);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify active membership", e);
        }
    }

    private boolean sameDayRowExists(int customerId, String date, int excludeId) throws DatabaseException {
        String sql = "SELECT id FROM attendance WHERE customer_id = ? AND attendance_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("id") != excludeId) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check existing daily attendance", e);
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
