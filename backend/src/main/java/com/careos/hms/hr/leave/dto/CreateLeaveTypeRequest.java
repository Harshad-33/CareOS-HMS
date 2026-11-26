package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.LeaveApprovalWorkflowType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLeaveTypeRequest(
        @NotBlank(message = "Leave type code is required")
        @Size(max = 32, message = "Code must not exceed 32 characters")
        String code,

        @NotBlank(message = "Leave type name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Annual allowance is required")
        @Min(value = 0, message = "Annual allowance cannot be negative")
        Integer annualAllowance,

        boolean carryForwardEnabled,

        @Min(value = 0, message = "Max carry forward days cannot be negative")
        int maxCarryForwardDays,

        boolean medicalCertRequired,

        @Min(value = 0, message = "Minimum service days cannot be negative")
        int minServiceDaysRequired,

        boolean paid,

        @NotNull(message = "Approval workflow type is required")
        LeaveApprovalWorkflowType approvalWorkflowType,

        @Min(value = 1, message = "Approval levels count must be at least 1")
        int approvalLevelsCount
) {
}
