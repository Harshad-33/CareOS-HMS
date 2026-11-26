package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.HalfDayPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ApplyLeaveRequest(
        // Optional for Self-Service (inferred from JWT authenticated user); required if manager/HR submits on behalf
        String employeePublicId,

        @NotBlank(message = "Leave type is required")
        String leaveTypePublicId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        boolean halfDay,

        HalfDayPeriod halfDayPeriod,

        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @Size(max = 32, message = "Contact phone must not exceed 32 characters")
        String contactPhoneDuringLeave,

        String documentStorageKey
) {
}
