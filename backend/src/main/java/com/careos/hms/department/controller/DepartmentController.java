package com.careos.hms.department.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.department.dto.DepartmentRequest;
import com.careos.hms.department.dto.DepartmentResponse;
import com.careos.hms.department.service.DepartmentService;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH)
@Tag(name = "Departments", description = "Endpoints for managing hospital departments and clinical divisions")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final HospitalService hospitalService;

    public DepartmentController(DepartmentService departmentService, HospitalService hospitalService) {
        this.departmentService = departmentService;
        this.hospitalService = hospitalService;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('HOSPITAL_DEPARTMENTS_MANAGE')")
    @Operation(summary = "Get departments for current hospital", description = "Hospital administrators can view departments configured for their hospital")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getMyDepartments(HttpServletRequest httpRequest) {
        List<DepartmentResponse> list = departmentService.getMyDepartments();
        return ResponseEntity.ok(ApiResponse.ok(list, "Departments retrieved successfully", httpRequest.getRequestURI()));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('HOSPITAL_DEPARTMENTS_MANAGE')")
    @Operation(summary = "Create department for current hospital", description = "Hospital administrators can add a new department with unique code")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createMyDepartment(
            @Valid @RequestBody DepartmentRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        DepartmentResponse response = departmentService.createMyDepartment(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Department created successfully", httpRequest.getRequestURI()));
    }

    @PutMapping("/departments/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_DEPARTMENTS_MANAGE')")
    @Operation(summary = "Update department for current hospital", description = "Hospital administrators can update an existing department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateMyDepartment(
            @PathVariable String publicId,
            @Valid @RequestBody DepartmentRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        DepartmentResponse response = departmentService.updateMyDepartment(publicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Department updated successfully", httpRequest.getRequestURI()));
    }

    @PatchMapping("/departments/{publicId}/status")
    @PreAuthorize("hasAuthority('HOSPITAL_DEPARTMENTS_MANAGE')")
    @Operation(summary = "Toggle department active/inactive status", description = "Hospital administrators can activate or deactivate a department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> toggleMyDepartmentStatus(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        DepartmentResponse response = departmentService.toggleMyDepartmentStatus(publicId, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Department status toggled successfully", httpRequest.getRequestURI()));
    }

    @DeleteMapping("/departments/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_DEPARTMENTS_MANAGE')")
    @Operation(summary = "Delete department for current hospital", description = "Hospital administrators can soft-delete a department")
    public ResponseEntity<ApiResponse<Void>> deleteMyDepartment(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        departmentService.deleteMyDepartment(publicId, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Department deleted successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/hospitals/{hospitalPublicId}/departments")
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Get departments by hospital public ID", description = "Platform administrators can view departments of any hospital")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartmentsByHospital(
            @PathVariable String hospitalPublicId,
            HttpServletRequest httpRequest
    ) {
        Hospital hospital = hospitalService.getEntityByPublicId(hospitalPublicId);
        List<DepartmentResponse> list = departmentService.getDepartments(hospital.getId());
        return ResponseEntity.ok(ApiResponse.ok(list, "Departments retrieved successfully", httpRequest.getRequestURI()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
