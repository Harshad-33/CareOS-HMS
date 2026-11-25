package com.careos.hms.employee.dto;

import com.careos.hms.employee.entity.Employee;
import com.careos.hms.employee.entity.EmployeeGender;
import com.careos.hms.employee.entity.EmployeeStatus;
import com.careos.hms.employee.entity.EmploymentType;
import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public record EmployeeResponse(
        String publicId,
        String employeeNumber,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        EmployeeGender gender,
        String bloodGroup,
        String email,
        String phoneNumber,
        String alternatePhoneNumber,

        // Address
        String addressLine1,
        String addressLine2,
        String city,
        String district,
        String state,
        String country,
        String pinCode,

        // Emergency Contact
        String emergencyContactName,
        String emergencyContactRelation,
        String emergencyContactPhone,

        // Professional
        String designation,
        String departmentPublicId,
        String departmentName,
        String branchPublicId,
        String branchName,
        EmploymentType employmentType,
        EmployeeStatus status,
        LocalDate joiningDate,
        LocalDate leavingDate,

        // Reporting Manager
        String reportingManagerPublicId,
        String reportingManagerName,
        String reportingManagerDesignation,

        // Photo
        String profilePhotoKey,

        // User Account
        boolean hasUserAccount,
        LinkedUserAccountDto userAccount,

        // Metadata
        Instant createdAt,
        Instant updatedAt
) {
    public record LinkedUserAccountDto(
            String publicId,
            String username,
            String email,
            AccountStatus status,
            Set<String> roleCodes
    ) {
        public static LinkedUserAccountDto from(User user) {
            if (user == null) {
                return null;
            }
            Set<String> roles = user.getUserRoles() != null
                    ? user.getUserRoles().stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> ur.getRole().getCode())
                    .collect(Collectors.toSet())
                    : Collections.emptySet();

            return new LinkedUserAccountDto(
                    user.getPublicId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getStatus(),
                    roles
            );
        }
    }

    public static EmployeeResponse from(Employee employee) {
        String deptPublicId = employee.getDepartment() != null ? employee.getDepartment().getPublicId() : null;
        String deptName = employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null;
        String brPublicId = employee.getBranch() != null ? employee.getBranch().getPublicId() : null;
        String brName = employee.getBranch() != null ? employee.getBranch().getBranchName() : null;

        String mgrPublicId = employee.getReportingManager() != null ? employee.getReportingManager().getPublicId() : null;
        String mgrName = employee.getReportingManager() != null ? employee.getReportingManager().getFullName() : null;
        String mgrDesignation = employee.getReportingManager() != null ? employee.getReportingManager().getDesignation() : null;

        boolean hasUser = employee.getUser() != null;
        LinkedUserAccountDto userDto = hasUser ? LinkedUserAccountDto.from(employee.getUser()) : null;

        return new EmployeeResponse(
                employee.getPublicId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getMiddleName(),
                employee.getLastName(),
                employee.getFullName(),
                employee.getDateOfBirth(),
                employee.getGender(),
                employee.getBloodGroup(),
                employee.getEmail(),
                employee.getPhoneNumber(),
                employee.getAlternatePhoneNumber(),
                employee.getAddressLine1(),
                employee.getAddressLine2(),
                employee.getCity(),
                employee.getDistrict(),
                employee.getState(),
                employee.getCountry(),
                employee.getPinCode(),
                employee.getEmergencyContactName(),
                employee.getEmergencyContactRelation(),
                employee.getEmergencyContactPhone(),
                employee.getDesignation(),
                deptPublicId,
                deptName,
                brPublicId,
                brName,
                employee.getEmploymentType(),
                employee.getStatus(),
                employee.getJoiningDate(),
                employee.getLeavingDate(),
                mgrPublicId,
                mgrName,
                mgrDesignation,
                employee.getProfilePhotoKey(),
                hasUser,
                userDto,
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
