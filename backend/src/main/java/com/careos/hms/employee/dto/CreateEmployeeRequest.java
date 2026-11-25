package com.careos.hms.employee.dto;

import com.careos.hms.employee.entity.EmployeeGender;
import com.careos.hms.employee.entity.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateEmployeeRequest(
        // Personal Details
        @NotBlank(message = "First name is required")
        @Size(max = 80, message = "First name must not exceed 80 characters")
        String firstName,

        @Size(max = 80, message = "Middle name must not exceed 80 characters")
        String middleName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80, message = "Last name must not exceed 80 characters")
        String lastName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotNull(message = "Gender is required")
        EmployeeGender gender,

        @Size(max = 16, message = "Blood group must not exceed 16 characters")
        String bloodGroup,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 160, message = "Email must not exceed 160 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Invalid phone number format")
        String phoneNumber,

        @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Invalid alternate phone number format")
        String alternatePhoneNumber,

        // Address
        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        String addressLine2,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "District must not exceed 100 characters")
        String district,

        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @Size(max = 20, message = "PIN/postal code must not exceed 20 characters")
        String pinCode,

        // Emergency Contact
        @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
        String emergencyContactName,

        @Size(max = 50, message = "Emergency contact relation must not exceed 50 characters")
        String emergencyContactRelation,

        @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Invalid emergency contact phone format")
        String emergencyContactPhone,

        // Professional Details
        @NotBlank(message = "Designation is required")
        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designation,

        @NotBlank(message = "Department public ID is required")
        String departmentPublicId,

        String branchPublicId,

        @NotNull(message = "Employment type is required")
        EmploymentType employmentType,

        @NotNull(message = "Joining date is required")
        LocalDate joiningDate,

        @Size(max = 32, message = "Employee number must not exceed 32 characters")
        String employeeNumber, // Optional manual code. If omitted, generated automatically

        String reportingManagerPublicId,

        // Optional User Account Provisioning
        Boolean createUserAccount,
        String username,
        String password,
        Set<String> roleCodes
) {
}
