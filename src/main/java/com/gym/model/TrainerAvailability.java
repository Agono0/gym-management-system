package com.gym.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an available time slot on a trainer's schedule.
 */
public class TrainerAvailability implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int trainerId;
    private String date;
    private String startTime;
    private String endTime;
    private int maxCustomers;
    private AvailabilityStatus status;

    public TrainerAvailability() {
    }

    public TrainerAvailability(int id, int trainerId, String date, String startTime,
                               String endTime, int maxCustomers, AvailabilityStatus status) {
        this.id = id;
        this.trainerId = trainerId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCustomers = maxCustomers;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(int trainerId) {
        this.trainerId = trainerId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getMaxCustomers() {
        return maxCustomers;
    }

    public void setMaxCustomers(int maxCustomers) {
        this.maxCustomers = maxCustomers;
    }

    public AvailabilityStatus getStatus() {
        return status;
    }

    public void setStatus(AvailabilityStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainerAvailability that = (TrainerAvailability) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TrainerAvailability{" +
                "id=" + id +
                ", trainerId=" + trainerId +
                ", date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", maxCustomers=" + maxCustomers +
                ", status=" + status +
                '}';
    }
}
