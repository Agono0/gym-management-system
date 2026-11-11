package com.gym.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class Membership implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private PlanType plan;
    private BigDecimal price;
    private String startDate;
    private String endDate;
    private MembershipStatus status;
    private int frozenDays;

    public Membership() {
    }

    public Membership(int id, int customerId, PlanType plan, BigDecimal price,
                      String startDate, String endDate, MembershipStatus status, int frozenDays) {
        this.id = id;
        this.customerId = customerId;
        this.plan = plan;
        this.price = price;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.frozenDays = frozenDays;
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

    public PlanType getPlan() {
        return plan;
    }

    public void setPlan(PlanType plan) {
        this.plan = plan;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public int getFrozenDays() {
        return frozenDays;
    }

    public void setFrozenDays(int frozenDays) {
        this.frozenDays = frozenDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Membership that = (Membership) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Membership{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", plan=" + plan +
                ", price=" + price +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", status=" + status +
                ", frozenDays=" + frozenDays +
                '}';
    }
}
