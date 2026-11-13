package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.HospitalType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HospitalRegistrationRequest(
        @NotBlank(message = "Hospital name is required")
        @Size(max = 150, message = "Hospital name cannot exceed 150 characters")
        String hospitalName,

        @Pattern(regexp = "^[A-Z0-9_-]{3,32}$", message = "Hospital code must be 3-32 uppercase alphanumeric characters if provided")
        String hospitalCode,

        @NotBlank(message = "Registration number is required")
        @Size(max = 64, message = "Registration number cannot exceed 64 characters")
        String registrationNumber,

        @NotNull(message = "Hospital type is required")
        HospitalType hospitalType,

        @NotBlank(message = "Hospital official email is required")
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

        LocalDate establishedDate,

        @NotBlank(message = "Initial administrator username is required")
        @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
        String adminUsername,

        @NotBlank(message = "Initial administrator email is required")
        @Email(message = "Invalid administrator email format")
        @Size(max = 160, message = "Email cannot exceed 160 characters")
        String adminEmail,

        @NotBlank(message = "Initial administrator password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        String adminPassword,

        @NotBlank(message = "Administrator first name is required")
        @Size(max = 80, message = "First name cannot exceed 80 characters")
        String adminFirstName,

        @NotBlank(message = "Administrator last name is required")
        @Size(max = 80, message = "Last name cannot exceed 80 characters")
        String adminLastName
) {}
