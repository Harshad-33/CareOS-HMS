package com.careos.hms.hr.leave.entity;

import com.careos.hms.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "leave_types")
public class LeaveType extends TenantAwareEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "annual_allowance", nullable = false)
    private int annualAllowance = 12;

    @Column(name = "carry_forward_enabled", nullable = false)
    private boolean carryForwardEnabled = false;

    @Column(name = "max_carry_forward_days", nullable = false)
    private int maxCarryForwardDays = 0;

    @Column(name = "medical_cert_required", nullable = false)
    private boolean medicalCertRequired = false;

    @Column(name = "min_service_days_required", nullable = false)
    private int minServiceDaysRequired = 0;

    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_workflow_type", nullable = false, length = 32)
    private LeaveApprovalWorkflowType approvalWorkflowType = LeaveApprovalWorkflowType.MANAGER_THEN_HR;

    @Column(name = "approval_levels_count", nullable = false)
    private int approvalLevelsCount = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private LeaveTypeStatus status = LeaveTypeStatus.ACTIVE;

    protected LeaveType() {
    }

    public LeaveType(Long hospitalId, String code, String name, int annualAllowance) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.annualAllowance = annualAllowance;
        this.status = LeaveTypeStatus.ACTIVE;
    }

    public LeaveType(Long hospitalId, String code, String name, String description,
                     int annualAllowance, boolean carryForwardEnabled, int maxCarryForwardDays,
                     boolean medicalCertRequired, int minServiceDaysRequired, boolean paid,
                     LeaveApprovalWorkflowType approvalWorkflowType, int approvalLevelsCount) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.description = description;
        this.annualAllowance = annualAllowance;
        this.carryForwardEnabled = carryForwardEnabled;
        this.maxCarryForwardDays = maxCarryForwardDays;
        this.medicalCertRequired = medicalCertRequired;
        this.minServiceDaysRequired = minServiceDaysRequired;
        this.paid = paid;
        this.approvalWorkflowType = approvalWorkflowType;
        this.approvalLevelsCount = approvalLevelsCount;
        this.status = LeaveTypeStatus.ACTIVE;
    }

    public void setHospitalId(Long hospitalId) {
        assignHospitalId(hospitalId);
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAnnualAllowance() {
        return annualAllowance;
    }

    public void setAnnualAllowance(int annualAllowance) {
        this.annualAllowance = annualAllowance;
    }

    public boolean isCarryForwardEnabled() {
        return carryForwardEnabled;
    }

    public void setCarryForwardEnabled(boolean carryForwardEnabled) {
        this.carryForwardEnabled = carryForwardEnabled;
    }

    public int getMaxCarryForwardDays() {
        return maxCarryForwardDays;
    }

    public void setMaxCarryForwardDays(int maxCarryForwardDays) {
        this.maxCarryForwardDays = maxCarryForwardDays;
    }

    public boolean isMedicalCertRequired() {
        return medicalCertRequired;
    }

    public void setMedicalCertRequired(boolean medicalCertRequired) {
        this.medicalCertRequired = medicalCertRequired;
    }

    public int getMinServiceDaysRequired() {
        return minServiceDaysRequired;
    }

    public void setMinServiceDaysRequired(int minServiceDaysRequired) {
        this.minServiceDaysRequired = minServiceDaysRequired;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public LeaveApprovalWorkflowType getApprovalWorkflowType() {
        return approvalWorkflowType;
    }

    public void setApprovalWorkflowType(LeaveApprovalWorkflowType approvalWorkflowType) {
        this.approvalWorkflowType = approvalWorkflowType;
    }

    public int getApprovalLevelsCount() {
        return approvalLevelsCount;
    }

    public void setApprovalLevelsCount(int approvalLevelsCount) {
        this.approvalLevelsCount = approvalLevelsCount;
    }

    public LeaveTypeStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveTypeStatus status) {
        this.status = status;
    }
}
