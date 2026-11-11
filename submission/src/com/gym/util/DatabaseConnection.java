package com.gym.util;

import com.gym.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Manages the SQLite connection and creates tables on startup
public class DatabaseConnection {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:gym.db";
    private static String dbUrl = DEFAULT_DB_URL;

    private DatabaseConnection() {
    }

    public static void setDbUrl(String newUrl) {
        dbUrl = newUrl;
    }

    public static void resetDbUrl() {
        dbUrl = DEFAULT_DB_URL;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static Connection getConnection() throws DatabaseException {
        try {
            Connection conn = DriverManager.getConnection(dbUrl);
            // Wait up to 5 seconds on the SQLite file lock instead of failing instantly
            try (Statement pragmaStmt = conn.createStatement()) {
                pragmaStmt.execute("PRAGMA busy_timeout = 5000");
            } catch (SQLException e) {
                // Close the connection we just opened so it is not left open (leaked)
                conn.close();
                throw e;
            }
            return conn;
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to SQLite database: " + dbUrl, e);
        }
    }

    public static void initializeDatabase() throws DatabaseException {
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS customers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "full_name TEXT NOT NULL,"
                + "phone TEXT,"
                + "email TEXT,"
                + "address TEXT,"
                + "date_of_birth TEXT,"
                + "gender TEXT,"
                + "emergency_name TEXT,"
                + "emergency_phone TEXT,"
                + "status TEXT NOT NULL,"
                + "join_date TEXT,"
                + "trainer_id INTEGER);",

            "CREATE TABLE IF NOT EXISTS trainers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "full_name TEXT NOT NULL,"
                + "phone TEXT,"
                + "email TEXT,"
                + "specialization TEXT,"
                + "experience_years INTEGER,"
                + "certifications TEXT,"
                + "hire_date TEXT,"
                + "hourly_rate NUMERIC NOT NULL);",

            "CREATE TABLE IF NOT EXISTS memberships ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL,"
                + "plan TEXT NOT NULL,"
                + "price NUMERIC NOT NULL,"
                + "start_date TEXT,"
                + "end_date TEXT,"
                + "status TEXT NOT NULL,"
                + "frozen_days INTEGER DEFAULT 0);",

            "CREATE TABLE IF NOT EXISTS workout_programs ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL,"
                + "creator_trainer_id INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "exercises TEXT,"
                + "version INTEGER NOT NULL,"
                + "created_date TEXT);",

            "CREATE TABLE IF NOT EXISTS sessions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL,"
                + "trainer_id INTEGER NOT NULL,"
                + "session_date TEXT,"
                + "start_time TEXT,"
                + "duration_min INTEGER NOT NULL,"
                + "status TEXT NOT NULL,"
                + "availability_id INTEGER,"
                + "subscription_id INTEGER,"
                + "price NUMERIC,"
                + "cancel_reason TEXT,"
                + "session_notes TEXT);",

            "CREATE TABLE IF NOT EXISTS equipment ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "category TEXT,"
                + "purchase_date TEXT,"
                + "condition TEXT NOT NULL,"
                + "status TEXT NOT NULL,"
                + "last_maintenance TEXT);",

            "CREATE TABLE IF NOT EXISTS attendance ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL,"
                + "attendance_date TEXT,"
                + "check_in TEXT,"
                + "check_out TEXT);",

            "CREATE TABLE IF NOT EXISTS payments ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL,"
                + "membership_id INTEGER,"
                + "subscription_id INTEGER,"
                + "session_id INTEGER,"
                + "amount NUMERIC NOT NULL,"
                + "method TEXT NOT NULL,"
                + "payment_date TEXT,"
                + "receipt_no TEXT UNIQUE NOT NULL,"
                + "voided INTEGER DEFAULT 0,"
                + "void_reason TEXT,"
                + "voided_date TEXT,"
                + "previous_amount NUMERIC);",

            "CREATE TABLE IF NOT EXISTS progress_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL,"
                + "record_date TEXT,"
                + "weight_kg REAL NOT NULL,"
                + "body_fat_pct REAL NOT NULL,"
                + "measurements TEXT,"
                + "target_weight_kg REAL,"
                + "notes TEXT);",

            "CREATE TABLE IF NOT EXISTS medical_profiles ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER UNIQUE NOT NULL,"
                + "blood_type TEXT,"
                + "conditions TEXT,"
                + "allergies TEXT,"
                + "medications TEXT,"
                + "injuries TEXT,"
                + "doctor_name TEXT,"
                + "doctor_phone TEXT,"
                + "notes TEXT,"
                + "updated_date TEXT);",

            "CREATE TABLE IF NOT EXISTS administrators ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "full_name TEXT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password_hash TEXT,"
                + "role TEXT NOT NULL,"
                + "phone TEXT,"
                + "email TEXT,"
                + "active INTEGER DEFAULT 1,"
                + "created_date TEXT);",

            "CREATE TABLE IF NOT EXISTS trainer_subscriptions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL,"
                + "trainer_id INTEGER NOT NULL,"
                + "start_date TEXT,"
                + "end_date TEXT,"
                + "rate_per_period NUMERIC NOT NULL,"
                + "status TEXT NOT NULL);",

            "CREATE TABLE IF NOT EXISTS trainer_availability ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "trainer_id INTEGER NOT NULL,"
                + "slot_date TEXT,"
                + "start_time TEXT,"
                + "end_time TEXT,"
                + "max_customers INTEGER NOT NULL,"
                + "status TEXT NOT NULL);"
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (int i = 0; i < tables.length; i++) {
                stmt.execute(tables[i]);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create database tables", e);
        }
    }
}
