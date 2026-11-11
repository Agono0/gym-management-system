package com.gym.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a customer's monthly subscription with a trainer.
 */
public class TrainerSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private int trainerId;
    private String startDate;
    private String endDate;
    private BigDecimal ratePerPeriod;
    private SubscriptionStatus status;

    public TrainerSubscription() {
    }

    public TrainerSubscription(int id, int customerId, int trainerId, String startDate,
                               String endDate, BigDecimal ratePerPeriod, SubscriptionStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.trainerId = trainerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.ratePerPeriod = ratePerPeriod;
        this.status = status;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getRatePerPeriod() {
        return ratePerPeriod;
    }

    public void setRatePerPeriod(BigDecimal ratePerPeriod) {
        this.ratePerPeriod = ratePerPeriod;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainerSubscription that = (TrainerSubscription) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TrainerSubscription{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", trainerId=" + trainerId +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", ratePerPeriod=" + ratePerPeriod +
                ", status=" + status +
                '}';
    }
}
