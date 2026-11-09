package com.gym.dao;

import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.WorkoutProgram;
import com.gym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Database operations for workout programs
public class WorkoutProgramDao {

    public WorkoutProgram insert(WorkoutProgram program) throws DatabaseException, ValidationException {
        validate(program);
        String sql = "INSERT INTO workout_programs (customer_id, creator_trainer_id, name, exercises, version, created_date) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, program.getCustomerId());
            ps.setInt(2, program.getCreatorTrainerId());
            ps.setString(3, program.getName());
            ps.setString(4, program.getExercises());
            ps.setInt(5, program.getVersion());
            ps.setString(6, program.getCreatedDate());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    program.setId(rs.getInt(1));
                }
            }
            return program;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert workout program", e);
        }
    }

    public void update(WorkoutProgram program) throws DatabaseException, ValidationException {
        validate(program);
        String sql = "UPDATE workout_programs SET customer_id = ?, creator_trainer_id = ?, name = ?, "
                + "exercises = ?, version = ?, created_date = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, program.getCustomerId());
            ps.setInt(2, program.getCreatorTrainerId());
            ps.setString(3, program.getName());
            ps.setString(4, program.getExercises());
            ps.setInt(5, program.getVersion());
            ps.setString(6, program.getCreatedDate());
            ps.setInt(7, program.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update workout program with id: " + program.getId(), e);
        }
    }

    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM workout_programs WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete workout program with id: " + id, e);
        }
    }

    public WorkoutProgram findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM workout_programs WHERE id = ?";
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
            throw new DatabaseException("Failed to find workout program with id: " + id, e);
        }
    }

    public List<WorkoutProgram> findByCustomerId(int customerId) throws DatabaseException {
        List<WorkoutProgram> list = new ArrayList<>();
        String sql = "SELECT * FROM workout_programs WHERE customer_id = ? ORDER BY version DESC";
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
            throw new DatabaseException("Failed to load workout programs for customer: " + customerId, e);
        }
    }

    public List<WorkoutProgram> findAll() throws DatabaseException {
        List<WorkoutProgram> list = new ArrayList<>();
        String sql = "SELECT * FROM workout_programs ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load workout programs", e);
        }
    }

    private WorkoutProgram mapRow(ResultSet rs) throws SQLException {
        WorkoutProgram p = new WorkoutProgram();
        p.setId(rs.getInt("id"));
        p.setCustomerId(rs.getInt("customer_id"));
        p.setCreatorTrainerId(rs.getInt("creator_trainer_id"));
        p.setName(rs.getString("name"));
        p.setExercises(rs.getString("exercises"));
        p.setVersion(rs.getInt("version"));
        p.setCreatedDate(rs.getString("created_date"));
        return p;
    }

    private void validate(WorkoutProgram p) throws ValidationException {
        if (p == null) {
            throw new ValidationException("Workout program cannot be null");
        }
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new ValidationException("Program name is required");
        }
    }
}
