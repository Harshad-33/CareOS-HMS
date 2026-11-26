package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.ApproverRole;
import com.careos.hms.hr.leave.entity.LeaveApproval;
import com.careos.hms.hr.leave.entity.LeaveApprovalStatus;

import java.time.Instant;

public record LeaveApprovalStepResponse(
        String publicId,
        int stepNumber,
        ApproverRole approverRole,
        String assignedApproverEmployeePublicId,
        String assignedApproverName,
        String actionTakenByUsername,
        LeaveApprovalStatus status,
        Instant decisionTimestamp,
        String comments,
        String rejectionReason
) {
    public static LeaveApprovalStepResponse from(LeaveApproval approval) {
        String approverPubId = approval.getAssignedApproverEmployee() != null
                ? approval.getAssignedApproverEmployee().getPublicId()
                : null;
        String approverName = approval.getAssignedApproverEmployee() != null
                ? approval.getAssignedApproverEmployee().getFirstName() + " " + approval.getAssignedApproverEmployee().getLastName()
                : (approval.getApproverRole() == ApproverRole.HR_MANAGER ? "HR Department" : null);

        String actionUser = approval.getActionTakenByUser() != null
                ? approval.getActionTakenByUser().getUsername()
                : null;

        return new LeaveApprovalStepResponse(
                approval.getPublicId(),
                approval.getStepNumber(),
                approval.getApproverRole(),
                approverPubId,
                approverName,
                actionUser,
                approval.getStatus(),
                approval.getDecisionTimestamp(),
                approval.getComments(),
                approval.getRejectionReason()
        );
    }
}
