package com.careos.hms.employee.dto;

import com.careos.hms.employee.entity.EmployeeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateEmployeeStatusRequest(
        @NotNull(message = "Status is required")
        EmployeeStatus status,

        @NotBlank(message = "Reason is required")
        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason,

        LocalDate effectiveDate
) {
}
