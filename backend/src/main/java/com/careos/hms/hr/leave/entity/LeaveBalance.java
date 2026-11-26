package com.careos.hms.hr.leave.entity;

import com.careos.hms.common.audit.BaseEntity;
import com.careos.hms.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "leave_balances")
public class LeaveBalance extends BaseEntity {

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

    @Column(name = "calendar_year", nullable = false)
    private int calendarYear;

    @Column(name = "total_allocated", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalAllocated = BigDecimal.ZERO;

    @Column(name = "used_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(name = "pending_approval_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal pendingApprovalDays = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 5, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected LeaveBalance() {
    }

    public LeaveBalance(Long hospitalId, Employee employee, LeaveType leaveType, int calendarYear, BigDecimal totalAllocated) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.employee = employee;
        this.leaveType = leaveType;
        this.calendarYear = calendarYear;
        this.totalAllocated = totalAllocated != null ? totalAllocated : BigDecimal.ZERO;
        this.usedDays = BigDecimal.ZERO;
        this.pendingApprovalDays = BigDecimal.ZERO;
        this.availableBalance = this.totalAllocated;
    }

    @PrePersist
    void ensurePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
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

    public int getCalendarYear() {
        return calendarYear;
    }

    public BigDecimal getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(BigDecimal totalAllocated) {
        this.totalAllocated = totalAllocated;
        recalculateAvailableBalance();
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(BigDecimal usedDays) {
        this.usedDays = usedDays;
        recalculateAvailableBalance();
    }

    public BigDecimal getPendingApprovalDays() {
        return pendingApprovalDays;
    }

    public void setPendingApprovalDays(BigDecimal pendingApprovalDays) {
        this.pendingApprovalDays = pendingApprovalDays;
        recalculateAvailableBalance();
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public Long getVersion() {
        return version;
    }

    public void recalculateAvailableBalance() {
        this.availableBalance = this.totalAllocated.subtract(this.usedDays).subtract(this.pendingApprovalDays);
    }
}
