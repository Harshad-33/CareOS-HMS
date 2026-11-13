package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.HospitalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateHospitalStatusRequest(
        @NotNull(message = "Target status is required")
        HospitalStatus status,

        @Size(max = 255, message = "Status change reason cannot exceed 255 characters")
        String reason
) {}
