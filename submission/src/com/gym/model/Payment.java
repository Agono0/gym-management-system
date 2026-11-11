package com.gym.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private Integer membershipId;
    private Integer subscriptionId;
    private Integer sessionId;
    private BigDecimal amount;
    private PaymentMethod method;
    private String date;
    private String receiptNo;
    private boolean voided;
    private String voidReason;
    private String voidedDate;
    private BigDecimal previousAmount;

    public Payment() {
    }

    public Payment(int id, int customerId, Integer membershipId, Integer subscriptionId,
                   Integer sessionId, BigDecimal amount, PaymentMethod method,
                   String date, String receiptNo, boolean voided) {
        this.id = id;
        this.customerId = customerId;
        this.membershipId = membershipId;
        this.subscriptionId = subscriptionId;
        this.sessionId = sessionId;
        this.amount = amount;
        this.method = method;
        this.date = date;
        this.receiptNo = receiptNo;
        this.voided = voided;
    }

    public static Payment forMembership(int customerId, int membershipId, BigDecimal amount,
                                        PaymentMethod method, String date, String receiptNo) {
        Payment payment = new Payment(0, customerId, membershipId, null, null, amount, method, date, receiptNo, false);
        return payment;
    }

    public static Payment forSubscription(int customerId, int subscriptionId, BigDecimal amount,
                                          PaymentMethod method, String date, String receiptNo) {
        Payment payment = new Payment(0, customerId, null, subscriptionId, null, amount, method, date, receiptNo, false);
        return payment;
    }

    public static Payment forSession(int customerId, int sessionId, BigDecimal amount,
                                     PaymentMethod method, String date, String receiptNo) {
        Payment payment = new Payment(0, customerId, null, null, sessionId, amount, method, date, receiptNo, false);
        return payment;
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

    public Integer getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public String getVoidedDate() {
        return voidedDate;
    }

    public void setVoidedDate(String voidedDate) {
        this.voidedDate = voidedDate;
    }

    public BigDecimal getPreviousAmount() {
        return previousAmount;
    }

    public void setPreviousAmount(BigDecimal previousAmount) {
        this.previousAmount = previousAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return id == payment.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", amount=" + amount +
                ", method=" + method +
                ", date='" + date + '\'' +
                ", receiptNo='" + receiptNo + '\'' +
                ", voided=" + voided +
                '}';
    }
}
