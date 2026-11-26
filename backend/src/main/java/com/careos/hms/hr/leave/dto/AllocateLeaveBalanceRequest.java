package com.careos.hms.hr.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AllocateLeaveBalanceRequest(
        @NotBlank(message = "Employee public ID is required")
        String employeePublicId,

        @NotBlank(message = "Leave type public ID is required")
        String leaveTypePublicId,

        @NotNull(message = "Calendar year is required")
        Integer calendarYear,

        @NotNull(message = "Allocated days is required")
        @DecimalMin(value = "0.00", message = "Allocated days cannot be negative")
        BigDecimal allocatedDays,

        @Size(max = 255, message = "Remarks must not exceed 255 characters")
        String remarks
) {
}
