package com.careos.hms.department.dto;

import com.careos.hms.department.entity.DepartmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank(message = "Department code is required")
        @Pattern(regexp = "^[A-Z0-9_-]{2,32}$", message = "Department code must be 2-32 uppercase alphanumeric characters")
        String departmentCode,

        @NotBlank(message = "Department name is required")
        @Size(max = 100, message = "Department name cannot exceed 100 characters")
        String departmentName,

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description,

        DepartmentStatus status
) {}
