package com.gym.cli;

import com.gym.model.Administrator;
import com.gym.model.Attendance;
import com.gym.model.Customer;
import com.gym.model.Equipment;
import com.gym.model.MedicalProfile;
import com.gym.model.Membership;
import com.gym.model.Payment;
import com.gym.model.ProgressRecord;
import com.gym.model.Session;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;
import com.gym.model.TrainerSubscription;
import com.gym.model.WorkoutProgram;

import java.io.Serializable;
import java.util.ArrayList;

// Serializable snapshot of all 13 database tables for binary .ser backup and restore
public class GymBackupSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String createdAt;
    private String createdBy;
    private ArrayList<Administrator> administrators;
    private ArrayList<Trainer> trainers;
    private ArrayList<Customer> customers;
    private ArrayList<Membership> memberships;
    private ArrayList<WorkoutProgram> workoutPrograms;
    private ArrayList<TrainerAvailability> trainerAvailabilities;
    private ArrayList<TrainerSubscription> trainerSubscriptions;
    private ArrayList<Session> sessions;
    private ArrayList<Equipment> equipment;
    private ArrayList<Attendance> attendanceRecords;
    private ArrayList<Payment> payments;
    private ArrayList<ProgressRecord> progressRecords;
    private ArrayList<MedicalProfile> medicalProfiles;

    public GymBackupSnapshot() {
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ArrayList<Administrator> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(ArrayList<Administrator> administrators) {
        this.administrators = administrators;
    }

    public ArrayList<Trainer> getTrainers() {
        return trainers;
    }

    public void setTrainers(ArrayList<Trainer> trainers) {
        this.trainers = trainers;
    }

    public ArrayList<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(ArrayList<Customer> customers) {
        this.customers = customers;
    }

    public ArrayList<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(ArrayList<Membership> memberships) {
        this.memberships = memberships;
    }

    public ArrayList<WorkoutProgram> getWorkoutPrograms() {
        return workoutPrograms;
    }

    public void setWorkoutPrograms(ArrayList<WorkoutProgram> workoutPrograms) {
        this.workoutPrograms = workoutPrograms;
    }

    public ArrayList<TrainerAvailability> getTrainerAvailabilities() {
        return trainerAvailabilities;
    }

    public void setTrainerAvailabilities(ArrayList<TrainerAvailability> trainerAvailabilities) {
        this.trainerAvailabilities = trainerAvailabilities;
    }

    public ArrayList<TrainerSubscription> getTrainerSubscriptions() {
        return trainerSubscriptions;
    }

    public void setTrainerSubscriptions(ArrayList<TrainerSubscription> trainerSubscriptions) {
        this.trainerSubscriptions = trainerSubscriptions;
    }

    public ArrayList<Session> getSessions() {
        return sessions;
    }

    public void setSessions(ArrayList<Session> sessions) {
        this.sessions = sessions;
    }

    public ArrayList<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(ArrayList<Equipment> equipment) {
        this.equipment = equipment;
    }

    public ArrayList<Attendance> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(ArrayList<Attendance> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public ArrayList<Payment> getPayments() {
        return payments;
    }

    public void setPayments(ArrayList<Payment> payments) {
        this.payments = payments;
    }

    public ArrayList<ProgressRecord> getProgressRecords() {
        return progressRecords;
    }

    public void setProgressRecords(ArrayList<ProgressRecord> progressRecords) {
        this.progressRecords = progressRecords;
    }

    public ArrayList<MedicalProfile> getMedicalProfiles() {
        return medicalProfiles;
    }

    public void setMedicalProfiles(ArrayList<MedicalProfile> medicalProfiles) {
        this.medicalProfiles = medicalProfiles;
    }

    public int totalRecordsCount() {
        int total = 0;
        if (administrators != null) total += administrators.size();
        if (trainers != null) total += trainers.size();
        if (customers != null) total += customers.size();
        if (memberships != null) total += memberships.size();
        if (workoutPrograms != null) total += workoutPrograms.size();
        if (trainerAvailabilities != null) total += trainerAvailabilities.size();
        if (trainerSubscriptions != null) total += trainerSubscriptions.size();
        if (sessions != null) total += sessions.size();
        if (equipment != null) total += equipment.size();
        if (attendanceRecords != null) total += attendanceRecords.size();
        if (payments != null) total += payments.size();
        if (progressRecords != null) total += progressRecords.size();
        if (medicalProfiles != null) total += medicalProfiles.size();
        return total;
    }
}
