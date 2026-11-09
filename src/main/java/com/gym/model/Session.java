package com.gym.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a personal training session between a customer and a trainer.
 */
public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private int trainerId;
    private String date;
    private String startTime;
    private int durationMin;
    private SessionStatus status;
    private Integer availabilityId;
    private Integer subscriptionId;
    private BigDecimal price;
    private String cancelReason;
    private String sessionNotes;

    public Session() {
    }

    public Session(int id, int customerId, int trainerId, String date, String startTime,
                   int durationMin, SessionStatus status, Integer availabilityId,
                   Integer subscriptionId, BigDecimal price, String cancelReason, String sessionNotes) {
        this.id = id;
        this.customerId = customerId;
        this.trainerId = trainerId;
        this.date = date;
        this.startTime = startTime;
        this.durationMin = durationMin;
        this.status = status;
        this.availabilityId = availabilityId;
        this.subscriptionId = subscriptionId;
        this.price = price;
        this.cancelReason = cancelReason;
        this.sessionNotes = sessionNotes;
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

    public int getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(int durationMin) {
        this.durationMin = durationMin;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Integer getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(Integer availabilityId) {
        this.availabilityId = availabilityId;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getSessionNotes() {
        return sessionNotes;
    }

    public void setSessionNotes(String sessionNotes) {
        this.sessionNotes = sessionNotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return id == session.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", trainerId=" + trainerId +
                ", date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", status=" + status +
                ", price=" + price +
                '}';
    }
}
