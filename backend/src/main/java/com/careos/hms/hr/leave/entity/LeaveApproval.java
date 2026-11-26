package com.careos.hms.hr.leave.entity;

import com.careos.hms.common.audit.BaseEntity;
import com.careos.hms.employee.entity.Employee;
import com.careos.hms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leave_approvals")
public class LeaveApproval extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_application_id", nullable = false)
    private LeaveApplication leaveApplication;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role", nullable = false, length = 32)
    private ApproverRole approverRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_approver_employee_id")
    private Employee assignedApproverEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_taken_by_user_id")
    private User actionTakenByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private LeaveApprovalStatus status = LeaveApprovalStatus.PENDING;

    @Column(name = "decision_timestamp")
    private Instant decisionTimestamp;

    @Column(name = "comments", length = 255)
    private String comments;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    protected LeaveApproval() {
    }

    public LeaveApproval(Long hospitalId, LeaveApplication leaveApplication, int stepNumber,
                         ApproverRole approverRole, Employee assignedApproverEmployee) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.leaveApplication = leaveApplication;
        this.stepNumber = stepNumber;
        this.approverRole = approverRole;
        this.assignedApproverEmployee = assignedApproverEmployee;
        this.status = LeaveApprovalStatus.PENDING;
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

    public LeaveApplication getLeaveApplication() {
        return leaveApplication;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public ApproverRole getApproverRole() {
        return approverRole;
    }

    public Employee getAssignedApproverEmployee() {
        return assignedApproverEmployee;
    }

    public void setAssignedApproverEmployee(Employee assignedApproverEmployee) {
        this.assignedApproverEmployee = assignedApproverEmployee;
    }

    public User getActionTakenByUser() {
        return actionTakenByUser;
    }

    public void setActionTakenByUser(User actionTakenByUser) {
        this.actionTakenByUser = actionTakenByUser;
    }

    public LeaveApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveApprovalStatus status) {
        this.status = status;
    }

    public Instant getDecisionTimestamp() {
        return decisionTimestamp;
    }

    public void setDecisionTimestamp(Instant decisionTimestamp) {
        this.decisionTimestamp = decisionTimestamp;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
