package com.gym.model;

import java.io.Serializable;
import java.util.Objects;

public class WorkoutProgram implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private int creatorTrainerId;
    private String name;
    private String exercises;
    private int version;
    private String createdDate;

    public WorkoutProgram() {
    }

    public WorkoutProgram(int id, int customerId, int creatorTrainerId, String name,
                          String exercises, int version, String createdDate) {
        this.id = id;
        this.customerId = customerId;
        this.creatorTrainerId = creatorTrainerId;
        this.name = name;
        this.exercises = exercises;
        this.version = version;
        this.createdDate = createdDate;
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

    public int getCreatorTrainerId() {
        return creatorTrainerId;
    }

    public void setCreatorTrainerId(int creatorTrainerId) {
        this.creatorTrainerId = creatorTrainerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExercises() {
        return exercises;
    }

    public void setExercises(String exercises) {
        this.exercises = exercises;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkoutProgram that = (WorkoutProgram) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkoutProgram{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", creatorTrainerId=" + creatorTrainerId +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", createdDate='" + createdDate + '\'' +
                '}';
    }
}
