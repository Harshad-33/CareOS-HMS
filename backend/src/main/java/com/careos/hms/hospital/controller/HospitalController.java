package com.careos.hms.hospital.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.hospital.dto.HospitalRegistrationRequest;
import com.careos.hms.hospital.dto.HospitalResponse;
import com.careos.hms.hospital.dto.UpdateHospitalProfileRequest;
import com.careos.hms.hospital.dto.UpdateHospitalStatusRequest;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.hospital.service.HospitalRegistrationService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/hospitals")
@Tag(name = "Hospital Administration", description = "Endpoints for hospital registration, profiles, status management, and tenant governance")
@SecurityRequirement(name = "bearerAuth")
public class HospitalController {

    private final HospitalService hospitalService;
    private final HospitalRegistrationService registrationService;

    public HospitalController(HospitalService hospitalService, HospitalRegistrationService registrationService) {
        this.hospitalService = hospitalService;
        this.registrationService = registrationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Register new hospital tenant", description = "Atomically creates hospital, default settings, and initial administrator user")
    public ResponseEntity<ApiResponse<HospitalResponse>> registerHospital(
            @Valid @RequestBody HospitalRegistrationRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalResponse response = registrationService.registerHospital(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Hospital registered successfully", httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "List hospitals with pagination and search", description = "Platform administrators can filter and paginate all registered hospitals")
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> getHospitals(
            @RequestParam(required = false) HospitalStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Page<HospitalResponse> page = hospitalService.getAllHospitals(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Hospitals retrieved successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('HOSPITAL_WORKSPACE_READ')")
    @Operation(summary = "Get current authenticated hospital profile", description = "Hospital administrators can view their own hospital details")
    public ResponseEntity<ApiResponse<HospitalResponse>> getMyHospital(HttpServletRequest httpRequest) {
        HospitalResponse response = hospitalService.getMyHospital();
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital profile retrieved", httpRequest.getRequestURI()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('HOSPITAL_PROFILE_UPDATE')")
    @Operation(summary = "Update current authenticated hospital profile", description = "Hospital administrators can update their own hospital details")
    public ResponseEntity<ApiResponse<HospitalResponse>> updateMyHospital(
            @Valid @RequestBody UpdateHospitalProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        HospitalResponse myHospital = hospitalService.getMyHospital();
        String clientIp = extractClientIp(httpRequest);
        HospitalResponse response = hospitalService.updateHospitalProfile(myHospital.publicId(), request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital profile updated successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyAuthority('PLATFORM_HOSPITALS_MANAGE', 'HOSPITAL_WORKSPACE_READ')")
    @Operation(summary = "Get hospital by public ID", description = "Platform admins or the hospital's own admins can fetch hospital details")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalByPublicId(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        HospitalResponse response = hospitalService.getHospitalByPublicId(publicId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital details retrieved", httpRequest.getRequestURI()));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAnyAuthority('PLATFORM_HOSPITALS_MANAGE', 'HOSPITAL_PROFILE_UPDATE')")
    @Operation(summary = "Update hospital profile by public ID", description = "Update hospital profile details")
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospital(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateHospitalProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalResponse response = hospitalService.updateHospitalProfile(publicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital profile updated successfully", httpRequest.getRequestURI()));
    }

    @PatchMapping("/{publicId}/status")
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Update hospital operational status", description = "Platform administrators can activate, deactivate, or suspend a hospital tenant")
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospitalStatus(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateHospitalStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalResponse response = hospitalService.updateHospitalStatus(publicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital status updated successfully", httpRequest.getRequestURI()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
