package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.LeaveApprovalWorkflowType;
import com.careos.hms.hr.leave.entity.LeaveType;
import com.careos.hms.hr.leave.entity.LeaveTypeStatus;

public record LeaveTypeSummaryResponse(
        String publicId,
        String code,
        String name,
        int annualAllowance,
        boolean paid,
        boolean medicalCertRequired,
        LeaveApprovalWorkflowType approvalWorkflowType,
        LeaveTypeStatus status
) {
    public static LeaveTypeSummaryResponse from(LeaveType entity) {
        return new LeaveTypeSummaryResponse(
                entity.getPublicId(),
                entity.getCode(),
                entity.getName(),
                entity.getAnnualAllowance(),
                entity.isPaid(),
                entity.isMedicalCertRequired(),
                entity.getApprovalWorkflowType(),
                entity.getStatus()
        );
    }
}
