package com.careos.hms.hr.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustLeaveBalanceRequest(
        @NotNull(message = "Adjustment days amount is required")
        BigDecimal adjustmentDays,

        @NotBlank(message = "Adjustment reason is mandatory")
        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason
) {
}
