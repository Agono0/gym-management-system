package com.gym.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a customer's check-in and check-out record.
 */
public class Attendance implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private String date;
    private String checkIn;
    private String checkOut;

    public Attendance() {
    }

    public Attendance(int id, int customerId, String date, String checkIn, String checkOut) {
        this.id = id;
        this.customerId = customerId;
        this.date = date;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attendance that = (Attendance) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", date='" + date + '\'' +
                ", checkIn='" + checkIn + '\'' +
                ", checkOut='" + checkOut + '\'' +
                '}';
    }
}
