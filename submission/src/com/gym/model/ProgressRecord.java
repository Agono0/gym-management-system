package com.gym.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a customer's physical progress measurement.
 */
public class ProgressRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private String date;
    private double weightKg;
    private double bodyFatPct;
    private String measurements;
    private Double targetWeightKg;
    private String notes;

    public ProgressRecord() {
    }

    public ProgressRecord(int id, int customerId, String date, double weightKg,
                          double bodyFatPct, String measurements, Double targetWeightKg, String notes) {
        this.id = id;
        this.customerId = customerId;
        this.date = date;
        this.weightKg = weightKg;
        this.bodyFatPct = bodyFatPct;
        this.measurements = measurements;
        this.targetWeightKg = targetWeightKg;
        this.notes = notes;
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

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public double getBodyFatPct() {
        return bodyFatPct;
    }

    public void setBodyFatPct(double bodyFatPct) {
        this.bodyFatPct = bodyFatPct;
    }

    public String getMeasurements() {
        return measurements;
    }

    public void setMeasurements(String measurements) {
        this.measurements = measurements;
    }

    public Double getTargetWeightKg() {
        return targetWeightKg;
    }

    public void setTargetWeightKg(Double targetWeightKg) {
        this.targetWeightKg = targetWeightKg;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgressRecord that = (ProgressRecord) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProgressRecord{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", date='" + date + '\'' +
                ", weightKg=" + weightKg +
                ", bodyFatPct=" + bodyFatPct +
                ", targetWeightKg=" + targetWeightKg +
                '}';
    }
}
