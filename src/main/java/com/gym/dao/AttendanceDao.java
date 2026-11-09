package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Attendance;
import com.gym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Database operations for attendance check-ins
public class AttendanceDao {

    public Attendance insert(Attendance attendance) throws DatabaseException, ValidationException {
        validate(attendance);
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
        validate(attendance);
        String sql = "UPDATE attendance SET customer_id = ?, attendance_date = ?, check_in = ?, check_out = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attendance.getCustomerId());
            ps.setString(2, attendance.getDate());
            ps.setString(3, attendance.getCheckIn());
            ps.setString(4, attendance.getCheckOut());
            ps.setInt(5, attendance.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update attendance with id: " + attendance.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM attendance WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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

    private void validate(Attendance a) throws ValidationException {
        if (a == null) {
            throw new ValidationException("Attendance record cannot be null");
        }
        if (a.getCustomerId() <= 0) {
            throw new ValidationException("Valid customer id is required");
        }
    }
}
