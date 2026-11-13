package com.careos.hms.branch.controller;

import com.careos.hms.branch.dto.HospitalBranchRequest;
import com.careos.hms.branch.dto.HospitalBranchResponse;
import com.careos.hms.branch.service.HospitalBranchService;
import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
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
@Tag(name = "Hospital Branches", description = "Endpoints for managing hospital multi-location branches and centers")
@SecurityRequirement(name = "bearerAuth")
public class HospitalBranchController {

    private final HospitalBranchService branchService;
    private final HospitalService hospitalService;

    public HospitalBranchController(HospitalBranchService branchService, HospitalService hospitalService) {
        this.branchService = branchService;
        this.hospitalService = hospitalService;
    }

    @GetMapping("/branches")
    @PreAuthorize("hasAuthority('HOSPITAL_BRANCHES_MANAGE')")
    @Operation(summary = "Get branches for current hospital", description = "Hospital administrators can view all branches for their hospital")
    public ResponseEntity<ApiResponse<List<HospitalBranchResponse>>> getMyBranches(HttpServletRequest httpRequest) {
        List<HospitalBranchResponse> list = branchService.getMyBranches();
        return ResponseEntity.ok(ApiResponse.ok(list, "Branches retrieved successfully", httpRequest.getRequestURI()));
    }

    @PostMapping("/branches")
    @PreAuthorize("hasAuthority('HOSPITAL_BRANCHES_MANAGE')")
    @Operation(summary = "Create branch for current hospital", description = "Hospital administrators can add a branch with unique code")
    public ResponseEntity<ApiResponse<HospitalBranchResponse>> createMyBranch(
            @Valid @RequestBody HospitalBranchRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalBranchResponse response = branchService.createMyBranch(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Branch created successfully", httpRequest.getRequestURI()));
    }

    @PutMapping("/branches/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_BRANCHES_MANAGE')")
    @Operation(summary = "Update branch for current hospital", description = "Hospital administrators can update an existing branch")
    public ResponseEntity<ApiResponse<HospitalBranchResponse>> updateMyBranch(
            @PathVariable String publicId,
            @Valid @RequestBody HospitalBranchRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalBranchResponse response = branchService.updateMyBranch(publicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Branch updated successfully", httpRequest.getRequestURI()));
    }

    @PatchMapping("/branches/{publicId}/status")
    @PreAuthorize("hasAuthority('HOSPITAL_BRANCHES_MANAGE')")
    @Operation(summary = "Toggle branch active/inactive status", description = "Hospital administrators can activate or deactivate a branch")
    public ResponseEntity<ApiResponse<HospitalBranchResponse>> toggleMyBranchStatus(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalBranchResponse response = branchService.toggleMyBranchStatus(publicId, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Branch status toggled successfully", httpRequest.getRequestURI()));
    }

    @DeleteMapping("/branches/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_BRANCHES_MANAGE')")
    @Operation(summary = "Delete branch for current hospital", description = "Hospital administrators can soft-delete a branch")
    public ResponseEntity<ApiResponse<Void>> deleteMyBranch(
            @PathVariable String publicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        branchService.deleteMyBranch(publicId, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Branch deleted successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/hospitals/{hospitalPublicId}/branches")
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Get branches by hospital public ID", description = "Platform administrators can view branches of any hospital")
    public ResponseEntity<ApiResponse<List<HospitalBranchResponse>>> getBranchesByHospital(
            @PathVariable String hospitalPublicId,
            HttpServletRequest httpRequest
    ) {
        Hospital hospital = hospitalService.getEntityByPublicId(hospitalPublicId);
        List<HospitalBranchResponse> list = branchService.getBranches(hospital.getId());
        return ResponseEntity.ok(ApiResponse.ok(list, "Branches retrieved successfully", httpRequest.getRequestURI()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
