package com.gym.model;

import java.io.Serializable;
import java.util.Objects;

public class MedicalProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private String bloodType;
    private String conditions;
    private String allergies;
    private String medications;
    private String injuries;
    private String doctorName;
    private String doctorPhone;
    private String notes;
    private String updatedDate;

    public MedicalProfile() {
    }

    public MedicalProfile(int id, int customerId, String bloodType, String conditions,
                          String allergies, String medications, String injuries,
                          String doctorName, String doctorPhone, String notes, String updatedDate) {
        this.id = id;
        this.customerId = customerId;
        this.bloodType = bloodType;
        this.conditions = conditions;
        this.allergies = allergies;
        this.medications = medications;
        this.injuries = injuries;
        this.doctorName = doctorName;
        this.doctorPhone = doctorPhone;
        this.notes = notes;
        this.updatedDate = updatedDate;
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

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public String getInjuries() {
        return injuries;
    }

    public void setInjuries(String injuries) {
        this.injuries = injuries;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorPhone() {
        return doctorPhone;
    }

    public void setDoctorPhone(String doctorPhone) {
        this.doctorPhone = doctorPhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalProfile that = (MedicalProfile) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MedicalProfile{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", bloodType='" + bloodType + '\'' +
                ", doctorName='" + doctorName + '\'' +
                ", updatedDate='" + updatedDate + '\'' +
                '}';
    }
}
