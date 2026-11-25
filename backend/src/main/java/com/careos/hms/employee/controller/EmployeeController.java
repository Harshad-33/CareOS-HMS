package com.careos.hms.employee.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.employee.dto.CreateEmployeeRequest;
import com.careos.hms.employee.dto.EmployeeLifecycleEventResponse;
import com.careos.hms.employee.dto.EmployeeResponse;
import com.careos.hms.employee.dto.EmployeeSummaryResponse;
import com.careos.hms.employee.dto.LinkUserAccountRequest;
import com.careos.hms.employee.dto.UpdateEmployeeRequest;
import com.careos.hms.employee.dto.UpdateEmployeeStatusRequest;
import com.careos.hms.employee.entity.EmployeeStatus;
import com.careos.hms.employee.entity.EmploymentType;
import com.careos.hms.employee.service.EmployeeService;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH)
@Tag(name = "Employee Management", description = "Endpoints for employee directory, profiles, lifecycle governance, and user account linking")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final HospitalService hospitalService;

    public EmployeeController(EmployeeService employeeService, HospitalService hospitalService) {
        this.employeeService = employeeService;
        this.hospitalService = hospitalService;
    }

    // ==========================================
    // CURRENT HOSPITAL CONTEXT (Hospital Admins)
    // ==========================================

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_READ')")
    @Operation(summary = "Get employees directory for current hospital", description = "Search and filter hospital employees")
    public ResponseEntity<ApiResponse<Page<EmployeeSummaryResponse>>> getMyEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String departmentPublicId,
            @RequestParam(required = false) String branchPublicId,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Page<EmployeeSummaryResponse> page = employeeService.getMyEmployees(
                search, departmentPublicId, branchPublicId, designation, employmentType, status, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page, "Employees retrieved successfully", httpRequest.getRequestURI()));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_MANAGE')")
    @Operation(summary = "Register new employee", description = "Create employee profile with optional linked user account")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createMyEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        EmployeeResponse response = employeeService.createMyEmployee(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Employee registered successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/employees/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_READ')")
    @Operation(summary = "Get employee by public ID", description = "Retrieve full employee profile")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getMyEmployeeByPublicId(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        EmployeeResponse response = employeeService.getMyEmployeeByPublicId(publicId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Employee details retrieved successfully", httpRequest.getRequestURI()));
    }

    @PutMapping("/employees/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_MANAGE')")
    @Operation(summary = "Update employee", description = "Update employee profile details")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateMyEmployee(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateEmployeeRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        EmployeeResponse response = employeeService.updateMyEmployee(publicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Employee updated successfully", httpRequest.getRequestURI()));
    }

    @PatchMapping("/employees/{publicId}/status")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_STATUS')")
    @Operation(summary = "Update employee status", description = "Change employee lifecycle status with mandatory reason and history tracking")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateMyEmployeeStatus(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateEmployeeStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        EmployeeResponse response = employeeService.updateMyEmployeeStatus(publicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Employee status updated successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/employees/{publicId}/lifecycle-history")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_READ')")
    @Operation(summary = "Get employee lifecycle history", description = "Retrieve full history of status changes for an employee")
    public ResponseEntity<ApiResponse<List<EmployeeLifecycleEventResponse>>> getMyEmployeeLifecycleHistory(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        List<EmployeeLifecycleEventResponse> history = employeeService.getMyEmployeeLifecycleHistory(publicId);
        return ResponseEntity.ok(ApiResponse.ok(history, "Lifecycle history retrieved successfully", httpRequest.getRequestURI()));
    }

    @PostMapping("/employees/{publicId}/user-account")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_ACCOUNT_LINK')")
    @Operation(summary = "Link user account", description = "Link existing user account or create new user account for employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> linkMyEmployeeUserAccount(
            @PathVariable String publicId,
            @Valid @RequestBody LinkUserAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        EmployeeResponse response = employeeService.linkMyEmployeeUserAccount(publicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "User account linked successfully", httpRequest.getRequestURI()));
    }

    @DeleteMapping("/employees/{publicId}/user-account")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_ACCOUNT_LINK')")
    @Operation(summary = "Unlink user account", description = "Unlink user account from employee and revoke active sessions")
    public ResponseEntity<ApiResponse<EmployeeResponse>> unlinkMyEmployeeUserAccount(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        EmployeeResponse response = employeeService.unlinkMyEmployeeUserAccount(publicId, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "User account unlinked successfully", httpRequest.getRequestURI()));
    }

    @PostMapping(value = "/employees/{publicId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_MANAGE')")
    @Operation(summary = "Upload profile photo", description = "Upload or replace employee profile photo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> uploadMyEmployeeProfilePhoto(
            @PathVariable String publicId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        EmployeeResponse response = employeeService.uploadMyEmployeeProfilePhoto(publicId, file, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Profile photo updated successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/employees/{publicId}/photo")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_READ')")
    @Operation(summary = "Get profile photo", description = "Stream employee profile photo")
    public ResponseEntity<byte[]> getMyEmployeeProfilePhoto(@PathVariable String publicId) {
        byte[] data = employeeService.loadMyEmployeeProfilePhoto(publicId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(data);
    }

    @DeleteMapping("/employees/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_EMPLOYEES_MANAGE')")
    @Operation(summary = "Delete employee", description = "Soft-delete employee and revoke linked user session")
    public ResponseEntity<ApiResponse<Void>> deleteMyEmployee(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        employeeService.deleteMyEmployee(publicId, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Employee deleted successfully", httpRequest.getRequestURI()));
    }

    // ==========================================
    // PLATFORM SUPER ADMIN CONTEXT
    // ==========================================

    @GetMapping("/hospitals/{hospitalPublicId}/employees")
    @PreAuthorize("hasAuthority('PLATFORM_EMPLOYEES_READ')")
    @Operation(summary = "Get employees by hospital public ID", description = "Platform administrators can view employees for any hospital")
    public ResponseEntity<ApiResponse<Page<EmployeeSummaryResponse>>> getEmployeesByHospital(
            @PathVariable String hospitalPublicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String departmentPublicId,
            @RequestParam(required = false) String branchPublicId,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Hospital hospital = hospitalService.getEntityByPublicId(hospitalPublicId);
        Page<EmployeeSummaryResponse> page = employeeService.getEmployees(
                hospital.getId(), search, departmentPublicId, branchPublicId, designation, employmentType, status, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page, "Employees retrieved successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/hospitals/{hospitalPublicId}/employees/{publicId}")
    @PreAuthorize("hasAuthority('PLATFORM_EMPLOYEES_READ')")
    @Operation(summary = "Get employee by hospital and public ID", description = "Platform administrators can view detailed employee profile")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeByHospitalAndPublicId(
            @PathVariable String hospitalPublicId,
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        Hospital hospital = hospitalService.getEntityByPublicId(hospitalPublicId);
        EmployeeResponse response = employeeService.getEmployeeByPublicId(hospital.getId(), publicId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Employee details retrieved successfully", httpRequest.getRequestURI()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
