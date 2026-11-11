package com.gym.model;

import java.io.Serializable;
import java.util.Objects;

public class Equipment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String category;
    private String purchaseDate;
    private EquipmentCondition condition;
    private EquipmentStatus status;
    private String lastMaintenance;

    public Equipment() {
    }

    public Equipment(int id, String name, String category, String purchaseDate,
                     EquipmentCondition condition, EquipmentStatus status, String lastMaintenance) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.purchaseDate = purchaseDate;
        this.condition = condition;
        this.status = status;
        this.lastMaintenance = lastMaintenance;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public EquipmentCondition getCondition() {
        return condition;
    }

    public void setCondition(EquipmentCondition condition) {
        this.condition = condition;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public String getLastMaintenance() {
        return lastMaintenance;
    }

    public void setLastMaintenance(String lastMaintenance) {
        this.lastMaintenance = lastMaintenance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Equipment equipment = (Equipment) o;
        return id == equipment.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Equipment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", condition=" + condition +
                ", status=" + status +
                ", lastMaintenance='" + lastMaintenance + '\'' +
                '}';
    }
}
