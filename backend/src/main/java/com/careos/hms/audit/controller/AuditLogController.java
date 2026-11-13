package com.careos.hms.audit.controller;

import com.careos.hms.audit.dto.AuditLogResponse;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.service.HospitalService;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/audits")
@Tag(name = "Audit Logs", description = "Endpoints for accessing administrative and security audit trail")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final HospitalService hospitalService;
    private final TenantContext tenantContext;

    public AuditLogController(AuditLogService auditLogService, HospitalService hospitalService, TenantContext tenantContext) {
        this.auditLogService = auditLogService;
        this.hospitalService = hospitalService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_AUDIT_READ')")
    @Operation(summary = "Get global audit trail", description = "Platform administrators can view system-wide audit activity")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getGlobalAuditLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Page<AuditLogResponse> logs = auditLogService.getAllAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.ok(logs, "Global audit trail retrieved", httpRequest.getRequestURI()));
    }

    @GetMapping("/hospital")
    @PreAuthorize("hasAuthority('HOSPITAL_AUDIT_READ')")
    @Operation(summary = "Get audit trail for current hospital", description = "Hospital administrators can view activity logs for their hospital")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getMyHospitalAuditLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException("TENANT_REQUIRED", "Hospital context is required");
        }
        Page<AuditLogResponse> logs = auditLogService.getHospitalAuditLogs(principal.getHospitalId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(logs, "Hospital audit trail retrieved", httpRequest.getRequestURI()));
    }

    @GetMapping("/hospital/{hospitalPublicId}")
    @PreAuthorize("hasAuthority('PLATFORM_AUDIT_READ')")
    @Operation(summary = "Get audit trail for a specific hospital", description = "Platform administrators can view activity logs for any hospital")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getHospitalAuditLogsByPublicId(
            @PathVariable String hospitalPublicId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Hospital hospital = hospitalService.getEntityByPublicId(hospitalPublicId);
        Page<AuditLogResponse> logs = auditLogService.getHospitalAuditLogs(hospital.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(logs, "Hospital audit trail retrieved", httpRequest.getRequestURI()));
    }
}
