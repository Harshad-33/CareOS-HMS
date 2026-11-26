package com.careos.hms.hr.leave.entity;

import com.careos.hms.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leave_balance_transactions")
public class LeaveBalanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private LeaveTransactionType transactionType;

    @Column(name = "days", nullable = false, precision = 5, scale = 2)
    private BigDecimal days;

    @Column(name = "balance_before", nullable = false, precision = 5, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 5, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "actor_username", nullable = false, length = 64)
    private String actorUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected LeaveBalanceTransaction() {
    }

    public LeaveBalanceTransaction(Long hospitalId, Employee employee, LeaveType leaveType,
                                   LeaveTransactionType transactionType, BigDecimal days,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter,
                                   String referenceId, String remarks, String actorUsername) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.employee = employee;
        this.leaveType = leaveType;
        this.transactionType = transactionType;
        this.days = days;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.remarks = remarks;
        this.actorUsername = actorUsername;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void ensureDefaults() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LeaveTransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getDays() {
        return days;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
