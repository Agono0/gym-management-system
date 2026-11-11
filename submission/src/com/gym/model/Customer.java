package com.gym.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a gym member.
 */
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String dateOfBirth;
    private String gender;
    private String emergencyName;
    private String emergencyPhone;
    private CustomerStatus status;
    private String joinDate;
    private Integer trainerId;

    public Customer() {
    }

    public Customer(int id, String fullName, String phone, String email, String address,
                    String dateOfBirth, String gender, String emergencyName, String emergencyPhone,
                    CustomerStatus status, String joinDate, Integer trainerId) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.emergencyName = emergencyName;
        this.emergencyPhone = emergencyPhone;
        this.status = status;
        this.joinDate = joinDate;
        this.trainerId = trainerId;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmergencyName() {
        return emergencyName;
    }

    public void setEmergencyName(String emergencyName) {
        this.emergencyName = emergencyName;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public Integer getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(Integer trainerId) {
        this.trainerId = trainerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id == customer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", joinDate='" + joinDate + '\'' +
                ", trainerId=" + trainerId +
                '}';
    }
}
