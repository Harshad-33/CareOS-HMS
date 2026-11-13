package com.careos.hms.hospital.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.hospital.dto.HospitalSettingsRequest;
import com.careos.hms.hospital.dto.HospitalSettingsResponse;
import com.careos.hms.hospital.service.HospitalSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/hospitals")
@Tag(name = "Hospital Settings", description = "Endpoints for configuring hospital timezone, working hours, booking policies, and defaults")
@SecurityRequirement(name = "bearerAuth")
public class HospitalSettingsController {

    private final HospitalSettingsService settingsService;

    public HospitalSettingsController(HospitalSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/me/settings")
    @PreAuthorize("hasAuthority('HOSPITAL_SETTINGS_MANAGE')")
    @Operation(summary = "Get current authenticated hospital settings", description = "Hospital administrators can view operational settings for their hospital")
    public ResponseEntity<ApiResponse<HospitalSettingsResponse>> getMySettings(HttpServletRequest httpRequest) {
        HospitalSettingsResponse response = settingsService.getMySettings();
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital settings retrieved", httpRequest.getRequestURI()));
    }

    @PutMapping("/me/settings")
    @PreAuthorize("hasAuthority('HOSPITAL_SETTINGS_MANAGE')")
    @Operation(summary = "Update current authenticated hospital settings", description = "Hospital administrators can update operational settings for their hospital")
    public ResponseEntity<ApiResponse<HospitalSettingsResponse>> updateMySettings(
            @Valid @RequestBody HospitalSettingsRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalSettingsResponse response = settingsService.updateMySettings(request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital settings updated successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/{hospitalPublicId}/settings")
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Get hospital settings by public ID", description = "Platform administrators can view settings of any hospital tenant")
    public ResponseEntity<ApiResponse<HospitalSettingsResponse>> getSettingsByHospitalPublicId(
            @PathVariable String hospitalPublicId,
            HttpServletRequest httpRequest
    ) {
        HospitalSettingsResponse response = settingsService.getSettingsByPublicId(hospitalPublicId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Hospital settings retrieved", httpRequest.getRequestURI()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
