package com.careos.hms.hr.leave.entity;

import com.careos.hms.common.audit.TenantAwareEntity;
import com.careos.hms.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_applications")
public class LeaveApplication extends TenantAwareEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false, precision = 4, scale = 2)
    private BigDecimal totalDays;

    @Column(name = "is_half_day", nullable = false)
    private boolean halfDay = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_period", length = 16)
    private HalfDayPeriod halfDayPeriod;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "contact_phone_during_leave", length = 32)
    private String contactPhoneDuringLeave;

    @Column(name = "document_storage_key", length = 255)
    private String documentStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private LeaveApplicationStatus status = LeaveApplicationStatus.PENDING;

    @Column(name = "current_step_number", nullable = false)
    private int currentStepNumber = 1;

    @Column(name = "required_steps_count", nullable = false)
    private int requiredStepsCount = 1;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();

    @Column(name = "final_decision_at")
    private Instant finalDecisionAt;

    @Column(name = "final_rejection_reason", length = 255)
    private String finalRejectionReason;

    protected LeaveApplication() {
    }

    public LeaveApplication(Long hospitalId, Employee employee, LeaveType leaveType,
                            LocalDate startDate, LocalDate endDate, BigDecimal totalDays,
                            String reason, int requiredStepsCount) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.reason = reason;
        this.requiredStepsCount = requiredStepsCount;
        this.currentStepNumber = 1;
        this.status = LeaveApplicationStatus.PENDING;
        this.appliedAt = Instant.now();
    }

    public LeaveApplication(Long hospitalId, Employee employee, LeaveType leaveType,
                            LocalDate startDate, LocalDate endDate, BigDecimal totalDays,
                            boolean halfDay, HalfDayPeriod halfDayPeriod, String reason,
                            String contactPhoneDuringLeave, String documentStorageKey) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.halfDay = halfDay;
        this.halfDayPeriod = halfDayPeriod;
        this.reason = reason;
        this.contactPhoneDuringLeave = contactPhoneDuringLeave;
        this.documentStorageKey = documentStorageKey;
        this.currentStepNumber = 1;
        this.status = LeaveApplicationStatus.PENDING;
        this.appliedAt = Instant.now();
    }

    public void setHospitalId(Long hospitalId) {
        assignHospitalId(hospitalId);
    }

    @PrePersist
    void ensurePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.appliedAt == null) {
            this.appliedAt = Instant.now();
        }
    }

    public String getPublicId() {
        return publicId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(BigDecimal totalDays) {
        this.totalDays = totalDays;
    }

    public boolean isHalfDay() {
        return halfDay;
    }

    public void setHalfDay(boolean halfDay) {
        this.halfDay = halfDay;
    }

    public HalfDayPeriod getHalfDayPeriod() {
        return halfDayPeriod;
    }

    public void setHalfDayPeriod(HalfDayPeriod halfDayPeriod) {
        this.halfDayPeriod = halfDayPeriod;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getContactPhoneDuringLeave() {
        return contactPhoneDuringLeave;
    }

    public void setContactPhoneDuringLeave(String contactPhoneDuringLeave) {
        this.contactPhoneDuringLeave = contactPhoneDuringLeave;
    }

    public String getDocumentStorageKey() {
        return documentStorageKey;
    }

    public void setDocumentStorageKey(String documentStorageKey) {
        this.documentStorageKey = documentStorageKey;
    }

    public LeaveApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveApplicationStatus status) {
        this.status = status;
    }

    public int getCurrentStepNumber() {
        return currentStepNumber;
    }

    public void setCurrentStepNumber(int currentStepNumber) {
        this.currentStepNumber = currentStepNumber;
    }

    public int getRequiredStepsCount() {
        return requiredStepsCount;
    }

    public void setRequiredStepsCount(int requiredStepsCount) {
        this.requiredStepsCount = requiredStepsCount;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public Instant getFinalDecisionAt() {
        return finalDecisionAt;
    }

    public void setFinalDecisionAt(Instant finalDecisionAt) {
        this.finalDecisionAt = finalDecisionAt;
    }

    public String getFinalRejectionReason() {
        return finalRejectionReason;
    }

    public void setFinalRejectionReason(String finalRejectionReason) {
        this.finalRejectionReason = finalRejectionReason;
    }
}
