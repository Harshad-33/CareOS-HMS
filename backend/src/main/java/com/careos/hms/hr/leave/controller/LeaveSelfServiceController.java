package com.careos.hms.hr.leave.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.storage.DocumentStorageService;
import com.careos.hms.employee.entity.Employee;
import com.careos.hms.employee.repository.EmployeeRepository;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.hr.leave.dto.ApplyLeaveRequest;
import com.careos.hms.hr.leave.dto.LeaveApplicationResponse;
import com.careos.hms.hr.leave.dto.LeaveApplicationSummaryResponse;
import com.careos.hms.hr.leave.dto.LeaveBalanceResponse;
import com.careos.hms.hr.leave.dto.LeaveBalanceTransactionResponse;
import com.careos.hms.hr.leave.dto.LeaveOverviewResponse;
import com.careos.hms.hr.leave.dto.LeaveTypeSummaryResponse;
import com.careos.hms.hr.leave.service.LeaveApplicationService;
import com.careos.hms.hr.leave.service.LeaveBalanceService;
import com.careos.hms.hr.leave.service.LeaveTypeService;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/hr/self/leaves")
@Tag(name = "Employee Leave Self-Service", description = "Personal leave portal for authenticated employees to apply, track, and manage their leaves")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('HOSPITAL_HR_SELF_SERVICE') or hasAuthority('HOSPITAL_LEAVES_APPLY') or hasRole('HOSPITAL_ADMIN') or hasRole('PLATFORM_SUPER_ADMIN')")
public class LeaveSelfServiceController {

    private final LeaveApplicationService leaveApplicationService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveTypeService leaveTypeService;
    private final EmployeeRepository employeeRepository;
    private final HospitalRepository hospitalRepository;
    private final DocumentStorageService documentStorageService;
    private final TenantContext tenantContext;

    public LeaveSelfServiceController(
            LeaveApplicationService leaveApplicationService,
            LeaveBalanceService leaveBalanceService,
            LeaveTypeService leaveTypeService,
            EmployeeRepository employeeRepository,
            HospitalRepository hospitalRepository,
            DocumentStorageService documentStorageService,
            TenantContext tenantContext
    ) {
        this.leaveApplicationService = leaveApplicationService;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveTypeService = leaveTypeService;
        this.employeeRepository = employeeRepository;
        this.hospitalRepository = hospitalRepository;
        this.documentStorageService = documentStorageService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get personal leave overview", description = "Returns balance summary and recent applications for current employee")
    public ResponseEntity<ApiResponse<LeaveOverviewResponse>> getMyLeaveOverview(
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveOverviewResponse overview = leaveApplicationService.getLeaveOverview(
                principal.getHospitalId(),
                principal.getUserId(),
                false
        );
        return ResponseEntity.ok(ApiResponse.ok(overview, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/balances")
    @Operation(summary = "Get personal leave balances", description = "Returns current employee's leave balance ledger across all leave types")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getMyBalances(
            @RequestParam(required = false) Integer year,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        Employee employee = resolveCurrentEmployee(principal.getUserId(), principal.getHospitalId());
        int targetYear = year != null ? year : LocalDate.now().getYear();

        List<LeaveBalanceResponse> balances = leaveBalanceService.getSelfBalances(
                principal.getHospitalId(),
                employee.getId(),
                targetYear
        );
        return ResponseEntity.ok(ApiResponse.ok(balances, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/balances/transactions")
    @Operation(summary = "Get personal leave transactions", description = "Returns chronological balance allocation, debit, and credit transactions")
    public ResponseEntity<ApiResponse<Page<LeaveBalanceTransactionResponse>>> getMyTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        Employee employee = resolveCurrentEmployee(principal.getUserId(), principal.getHospitalId());

        Page<LeaveBalanceTransactionResponse> page = leaveBalanceService.getSelfTransactions(
                principal.getHospitalId(),
                employee.getId(),
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/types")
    @Operation(summary = "Get available leave types", description = "Returns active leave policies available for application")
    public ResponseEntity<ApiResponse<List<LeaveTypeSummaryResponse>>> getAvailableLeaveTypes(
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        List<LeaveTypeSummaryResponse> types = leaveTypeService.getLeaveTypeSummaries(principal.getHospitalId());
        return ResponseEntity.ok(ApiResponse.ok(types, httpServletRequest.getRequestURI()));
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply for leave", description = "Submits a personal leave application (full-day or half-day)")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> applyForLeave(
            @Valid @RequestBody ApplyLeaveRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveApplicationResponse response = leaveApplicationService.applyLeave(
                principal.getHospitalId(),
                request,
                principal.getUserId(),
                false,
                principal.getUsername(),
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Leave application submitted successfully", httpServletRequest.getRequestURI()));
    }

    @PostMapping("/upload-document")
    @Operation(summary = "Upload supporting document", description = "Uploads medical certificate or document, returning storage key to link to leave application")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadSupportingDocument(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpServletRequest
    ) {
        if (file.isEmpty()) {
            throw new InvalidRequestException("Uploaded file cannot be empty");
        }

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(principal.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", principal.getHospitalId()));

        try {
            String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String storageKey = documentStorageService.storeFile(
                    hospital.getPublicId(),
                    file.getOriginalFilename(),
                    file.getBytes(),
                    contentType
            );
            return ResponseEntity.ok(ApiResponse.ok(
                    Map.of(
                            "documentStorageKey", storageKey,
                            "originalFilename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"
                    ),
                    "Document uploaded successfully",
                    httpServletRequest.getRequestURI()
            ));
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to store uploaded document: " + e.getMessage());
        }
    }

    @GetMapping("/applications")
    @Operation(summary = "List personal leave applications", description = "Returns paginated list of personal leave requests")
    public ResponseEntity<ApiResponse<Page<LeaveApplicationSummaryResponse>>> getMyApplications(
            @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        Employee employee = resolveCurrentEmployee(principal.getUserId(), principal.getHospitalId());

        Page<LeaveApplicationSummaryResponse> page = leaveApplicationService.getSelfApplications(
                principal.getHospitalId(),
                employee.getId(),
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/applications/{publicId}")
    @Operation(summary = "Get personal leave application details", description = "Fetches a personal leave request with its step approval history")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> getMyApplicationDetails(
            @PathVariable String publicId,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveApplicationResponse response = leaveApplicationService.getApplication(
                principal.getHospitalId(),
                publicId,
                principal.getUserId(),
                false,
                false
        );
        return ResponseEntity.ok(ApiResponse.ok(response, httpServletRequest.getRequestURI()));
    }

    @PostMapping("/applications/{publicId}/cancel")
    @Operation(summary = "Cancel personal leave application", description = "Cancels a pending or approved personal leave application")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> cancelMyApplication(
            @PathVariable String publicId,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveApplicationResponse response = leaveApplicationService.cancelApplication(
                principal.getHospitalId(),
                publicId,
                principal.getUserId(),
                false,
                principal.getUsername(),
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave application cancelled successfully", httpServletRequest.getRequestURI()));
    }

    @GetMapping("/applications/{publicId}/document")
    @Operation(summary = "Download personal application document", description = "Downloads supporting medical certificate or attachment")
    public ResponseEntity<byte[]> downloadMyAttachment(
            @PathVariable String publicId
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveApplicationResponse app = leaveApplicationService.getApplication(
                principal.getHospitalId(),
                publicId,
                principal.getUserId(),
                false,
                false
        );

        if (app.documentStorageKey() == null || app.documentStorageKey().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = documentStorageService.loadFile(app.documentStorageKey());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leave_document_" + publicId + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    private Employee resolveCurrentEmployee(Long userId, Long hospitalId) {
        return employeeRepository.findByUserIdAndDeletedFalse(userId)
                .filter(e -> e.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new InvalidRequestException("Your user account is not linked to an active employee profile in this hospital."));
    }
}
