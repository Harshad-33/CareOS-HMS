package com.careos.hms.employee.dto;

import com.careos.hms.employee.entity.Employee;
import com.careos.hms.employee.entity.EmployeeGender;
import com.careos.hms.employee.entity.EmployeeStatus;
import com.careos.hms.employee.entity.EmploymentType;

import java.time.LocalDate;

public record EmployeeSummaryResponse(
        String publicId,
        String employeeNumber,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        EmployeeGender gender,
        String designation,
        String departmentPublicId,
        String departmentName,
        String branchPublicId,
        String branchName,
        EmploymentType employmentType,
        EmployeeStatus status,
        String email,
        String phoneNumber,
        LocalDate joiningDate,
        String profilePhotoKey,
        boolean hasUserAccount,
        String userPublicId,
        String username
) {
    public static EmployeeSummaryResponse from(Employee employee) {
        String deptPublicId = employee.getDepartment() != null ? employee.getDepartment().getPublicId() : null;
        String deptName = employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null;
        String brPublicId = employee.getBranch() != null ? employee.getBranch().getPublicId() : null;
        String brName = employee.getBranch() != null ? employee.getBranch().getBranchName() : null;

        boolean hasUser = employee.getUser() != null;
        String uPublicId = hasUser ? employee.getUser().getPublicId() : null;
        String uName = hasUser ? employee.getUser().getUsername() : null;

        return new EmployeeSummaryResponse(
                employee.getPublicId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getMiddleName(),
                employee.getLastName(),
                employee.getFullName(),
                employee.getGender(),
                employee.getDesignation(),
                deptPublicId,
                deptName,
                brPublicId,
                brName,
                employee.getEmploymentType(),
                employee.getStatus(),
                employee.getEmail(),
                employee.getPhoneNumber(),
                employee.getJoiningDate(),
                employee.getProfilePhotoKey(),
                hasUser,
                uPublicId,
                uName
        );
    }
}
