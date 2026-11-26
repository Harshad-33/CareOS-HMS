package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.LeaveApprovalWorkflowType;
import com.careos.hms.hr.leave.entity.LeaveType;
import com.careos.hms.hr.leave.entity.LeaveTypeStatus;

import java.time.Instant;

public record LeaveTypeResponse(
        String publicId,
        String code,
        String name,
        String description,
        int annualAllowance,
        boolean carryForwardEnabled,
        int maxCarryForwardDays,
        boolean medicalCertRequired,
        int minServiceDaysRequired,
        boolean paid,
        LeaveApprovalWorkflowType approvalWorkflowType,
        int approvalLevelsCount,
        LeaveTypeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static LeaveTypeResponse from(LeaveType entity) {
        return new LeaveTypeResponse(
                entity.getPublicId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getAnnualAllowance(),
                entity.isCarryForwardEnabled(),
                entity.getMaxCarryForwardDays(),
                entity.isMedicalCertRequired(),
                entity.getMinServiceDaysRequired(),
                entity.isPaid(),
                entity.getApprovalWorkflowType(),
                entity.getApprovalLevelsCount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
