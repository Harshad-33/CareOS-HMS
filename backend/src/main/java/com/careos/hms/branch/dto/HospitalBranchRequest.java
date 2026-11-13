package com.careos.hms.branch.dto;

import com.careos.hms.branch.entity.BranchStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HospitalBranchRequest(
        @NotBlank(message = "Branch code is required")
        @Pattern(regexp = "^[A-Z0-9_-]{2,32}$", message = "Branch code must be 2-32 uppercase alphanumeric characters")
        String branchCode,

        @NotBlank(message = "Branch name is required")
        @Size(max = 100, message = "Branch name cannot exceed 100 characters")
        String branchName,

        @Size(max = 32, message = "Contact number cannot exceed 32 characters")
        String contactNumber,

        @Email(message = "Invalid email format")
        @Size(max = 160, message = "Email cannot exceed 160 characters")
        String email,

        @Size(max = 255, message = "Address cannot exceed 255 characters")
        String address,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "District cannot exceed 100 characters")
        String district,

        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @Size(max = 100, message = "Country cannot exceed 100 characters")
        String country,

        @Size(max = 20, message = "PIN code cannot exceed 20 characters")
        String pinCode,

        BranchStatus status
) {}
