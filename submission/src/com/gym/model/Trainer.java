package com.gym.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a gym trainer or coach.
 */
public class Trainer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String specialization;
    private int experienceYears;
    private String certifications;
    private String hireDate;
    private BigDecimal hourlyRate;

    public Trainer() {
    }

    public Trainer(int id, String fullName, String phone, String email, String specialization,
                   int experienceYears, String certifications, String hireDate, BigDecimal hourlyRate) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.specialization = specialization;
        this.experienceYears = experienceYears;
        this.certifications = certifications;
        this.hireDate = hireDate;
        this.hourlyRate = hourlyRate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getCertifications() {
        return certifications;
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trainer trainer = (Trainer) o;
        return id == trainer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Trainer{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", specialization='" + specialization + '\'' +
                ", experienceYears=" + experienceYears +
                ", hourlyRate=" + hourlyRate +
                '}';
    }
}
