package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.HospitalType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateHospitalProfileRequest(
        @NotBlank(message = "Hospital name is required")
        @Size(max = 150, message = "Hospital name cannot exceed 150 characters")
        String name,

        @NotNull(message = "Hospital type is required")
        HospitalType hospitalType,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 160, message = "Email cannot exceed 160 characters")
        String email,

        @NotBlank(message = "Primary contact number is required")
        @Size(max = 32, message = "Primary contact number cannot exceed 32 characters")
        String primaryContactNumber,

        @Size(max = 32, message = "Alternate contact number cannot exceed 32 characters")
        String alternateContactNumber,

        @Size(max = 255, message = "Website URL cannot exceed 255 characters")
        String website,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @NotBlank(message = "District is required")
        @Size(max = 100, message = "District cannot exceed 100 characters")
        String district,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @Size(max = 100, message = "Country cannot exceed 100 characters")
        String country,

        @NotBlank(message = "PIN code is required")
        @Size(max = 20, message = "PIN code cannot exceed 20 characters")
        String pinCode,

        LocalDate establishedDate
) {}
