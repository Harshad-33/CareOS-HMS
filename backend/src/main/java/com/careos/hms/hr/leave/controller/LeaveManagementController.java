package com.careos.hms.hr.leave.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.storage.DocumentStorageService;
import com.careos.hms.hr.leave.dto.AdjustLeaveBalanceRequest;
import com.careos.hms.hr.leave.dto.AllocateLeaveBalanceRequest;
import com.careos.hms.hr.leave.dto.ApproveLeaveStepRequest;
import com.careos.hms.hr.leave.dto.CreateLeaveTypeRequest;
import com.careos.hms.hr.leave.dto.LeaveApplicationResponse;
import com.careos.hms.hr.leave.dto.LeaveApplicationSummaryResponse;
import com.careos.hms.hr.leave.dto.LeaveApprovalInboxItemResponse;
import com.careos.hms.hr.leave.dto.LeaveBalanceResponse;
import com.careos.hms.hr.leave.dto.LeaveBalanceTransactionResponse;
import com.careos.hms.hr.leave.dto.LeaveCalendarItemResponse;
import com.careos.hms.hr.leave.dto.LeaveOverviewResponse;
import com.careos.hms.hr.leave.dto.LeaveTypeResponse;
import com.careos.hms.hr.leave.dto.LeaveTypeSummaryResponse;
import com.careos.hms.hr.leave.dto.RejectLeaveStepRequest;
import com.careos.hms.hr.leave.dto.UpdateLeaveTypeRequest;
import com.careos.hms.hr.leave.entity.LeaveApplicationStatus;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/hr/management/leaves")
@Tag(name = "HR Leave Management", description = "Endpoints for leave types, policies, balance allocations, approvals, and hospital rosters")
@SecurityRequirement(name = "bearerAuth")
public class LeaveManagementController {

    private final LeaveTypeService leaveTypeService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveApplicationService leaveApplicationService;
    private final DocumentStorageService documentStorageService;
    private final TenantContext tenantContext;

    public LeaveManagementController(
            LeaveTypeService leaveTypeService,
            LeaveBalanceService leaveBalanceService,
            LeaveApplicationService leaveApplicationService,
            DocumentStorageService documentStorageService,
            TenantContext tenantContext
    ) {
        this.leaveTypeService = leaveTypeService;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveApplicationService = leaveApplicationService;
        this.documentStorageService = documentStorageService;
        this.tenantContext = tenantContext;
    }

    // ==========================================
    // LEAVE TYPES & POLICIES
    // ==========================================

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Create leave type", description = "Defines a new hospital leave type and its approval policy")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> createLeaveType(
            @Valid @RequestBody CreateLeaveTypeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveTypeResponse response = leaveTypeService.createLeaveType(
                principal.getHospitalId(),
                request,
                principal.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Leave type created successfully", httpServletRequest.getRequestURI()));
    }

    @PutMapping("/types/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Update leave type", description = "Updates an existing leave type's entitlement and workflow rules")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> updateLeaveType(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateLeaveTypeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveTypeResponse response = leaveTypeService.updateLeaveType(
                principal.getHospitalId(),
                publicId,
                request,
                principal.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave type updated successfully", httpServletRequest.getRequestURI()));
    }

    @PatchMapping("/types/{publicId}/activate")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Activate leave type", description = "Activates a leave type for employee applications")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> activateLeaveType(
            @PathVariable String publicId,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveTypeResponse response = leaveTypeService.activateLeaveType(
                principal.getHospitalId(),
                publicId,
                principal.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave type activated successfully", httpServletRequest.getRequestURI()));
    }

    @PatchMapping("/types/{publicId}/deactivate")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Deactivate leave type", description = "Deactivates a leave type")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> deactivateLeaveType(
            @PathVariable String publicId,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveTypeResponse response = leaveTypeService.deactivateLeaveType(
                principal.getHospitalId(),
                publicId,
                principal.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave type deactivated successfully", httpServletRequest.getRequestURI()));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "List leave types", description = "Returns leave types configured for the hospital")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> listLeaveTypes(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        List<LeaveTypeResponse> responses = leaveTypeService.getLeaveTypes(principal.getHospitalId(), activeOnly);
        return ResponseEntity.ok(ApiResponse.ok(responses, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/types/summary")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Summary list of leave types", description = "Lightweight leave type records for form selectors")
    public ResponseEntity<ApiResponse<List<LeaveTypeSummaryResponse>>> listLeaveTypeSummaries(
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        List<LeaveTypeSummaryResponse> responses = leaveTypeService.getLeaveTypeSummaries(principal.getHospitalId());
        return ResponseEntity.ok(ApiResponse.ok(responses, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/types/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Get leave type", description = "Fetches a leave type definition by its public ID")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> getLeaveType(
            @PathVariable String publicId,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveTypeResponse response = leaveTypeService.getLeaveType(principal.getHospitalId(), publicId);
        return ResponseEntity.ok(ApiResponse.ok(response, httpServletRequest.getRequestURI()));
    }

    // ==========================================
    // LEAVE BALANCES
    // ==========================================

    @PostMapping("/balances/allocate")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Allocate leave balance", description = "Assigns or tops up leave days for an employee")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> allocateLeaveBalance(
            @Valid @RequestBody AllocateLeaveBalanceRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveBalanceResponse response = leaveBalanceService.allocateBalance(
                principal.getHospitalId(),
                request,
                principal.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave balance allocated successfully", httpServletRequest.getRequestURI()));
    }

    @PostMapping("/balances/{publicId}/adjust")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Adjust leave balance", description = "Manually adjusts available leave balance with mandatory reason")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> adjustLeaveBalance(
            @PathVariable String publicId,
            @Valid @RequestBody AdjustLeaveBalanceRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveBalanceResponse response = leaveBalanceService.adjustBalance(
                principal.getHospitalId(),
                publicId,
                request,
                principal.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave balance adjusted successfully", httpServletRequest.getRequestURI()));
    }

    @GetMapping("/balances/employees/{employeePublicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Get employee leave balances", description = "Fetches leave balances for a specific employee")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getEmployeeBalances(
            @PathVariable String employeePublicId,
            @RequestParam(required = false) Integer year,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<LeaveBalanceResponse> responses = leaveBalanceService.getEmployeeBalances(
                principal.getHospitalId(),
                employeePublicId,
                targetYear
        );
        return ResponseEntity.ok(ApiResponse.ok(responses, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/balances/employees/{employeePublicId}/transactions")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Get employee balance transaction ledger", description = "Returns immutable audit ledger for employee's leave balance transactions")
    public ResponseEntity<ApiResponse<Page<LeaveBalanceTransactionResponse>>> getEmployeeBalanceTransactions(
            @PathVariable String employeePublicId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        Page<LeaveBalanceTransactionResponse> page = leaveBalanceService.getEmployeeTransactions(
                principal.getHospitalId(),
                employeePublicId,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page, httpServletRequest.getRequestURI()));
    }

    // ==========================================
    // APPLICATIONS & APPROVALS
    // ==========================================

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "List hospital leave applications", description = "Returns hospital leave applications with optional status filter")
    public ResponseEntity<ApiResponse<Page<LeaveApplicationSummaryResponse>>> listHospitalApplications(
            @RequestParam(required = false) LeaveApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        Page<LeaveApplicationSummaryResponse> page = leaveApplicationService.getHospitalApplications(
                principal.getHospitalId(),
                status,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/applications/{publicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Get leave application details", description = "Fetches a leave application with complete approval step history")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> getApplicationDetails(
            @PathVariable String publicId,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveApplicationResponse response = leaveApplicationService.getApplication(
                principal.getHospitalId(),
                publicId,
                principal.getUserId(),
                principal.isPlatformUser(),
                true
        );
        return ResponseEntity.ok(ApiResponse.ok(response, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/approvals/inbox")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_APPROVE') or hasRole('HOSPITAL_ADMIN') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Get pending approval inbox", description = "Returns leave applications awaiting decision from the current user or HR role")
    public ResponseEntity<ApiResponse<Page<LeaveApprovalInboxItemResponse>>> getPendingApprovalsInbox(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        boolean isHr = principal.isPlatformUser()
                || principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("HOSPITAL_LEAVES_APPROVE") || a.getAuthority().equals("ROLE_HOSPITAL_ADMIN"));

        Page<LeaveApprovalInboxItemResponse> page = leaveApplicationService.getPendingApprovals(
                principal.getHospitalId(),
                principal.getUserId(),
                isHr,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(page, httpServletRequest.getRequestURI()));
    }

    @PostMapping("/applications/{publicId}/steps/{stepNumber}/approve")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_APPROVE') or hasRole('HOSPITAL_ADMIN') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Approve leave step", description = "Approves a specific step in the leave approval workflow")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> approveStep(
            @PathVariable String publicId,
            @PathVariable int stepNumber,
            @RequestBody(required = false) ApproveLeaveStepRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        boolean isHr = principal.isPlatformUser()
                || principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("HOSPITAL_LEAVES_APPROVE") || a.getAuthority().equals("ROLE_HOSPITAL_ADMIN"));

        LeaveApplicationResponse response = leaveApplicationService.approveApplicationStep(
                principal.getHospitalId(),
                publicId,
                stepNumber,
                request,
                principal.getUserId(),
                principal.isPlatformUser(),
                isHr,
                principal.getUsername(),
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Step approved successfully", httpServletRequest.getRequestURI()));
    }

    @PostMapping("/applications/{publicId}/steps/{stepNumber}/reject")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_APPROVE') or hasRole('HOSPITAL_ADMIN') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Reject leave step", description = "Rejects a leave application with mandatory rejection reason")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> rejectStep(
            @PathVariable String publicId,
            @PathVariable int stepNumber,
            @Valid @RequestBody RejectLeaveStepRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        boolean isHr = principal.isPlatformUser()
                || principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("HOSPITAL_LEAVES_APPROVE") || a.getAuthority().equals("ROLE_HOSPITAL_ADMIN"));

        LeaveApplicationResponse response = leaveApplicationService.rejectApplicationStep(
                principal.getHospitalId(),
                publicId,
                stepNumber,
                request,
                principal.getUserId(),
                principal.isPlatformUser(),
                isHr,
                principal.getUsername(),
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave application rejected", httpServletRequest.getRequestURI()));
    }

    @PostMapping("/applications/{publicId}/cancel")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Cancel leave application", description = "Cancels a leave application and reverses or releases balance")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> cancelApplication(
            @PathVariable String publicId,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveApplicationResponse response = leaveApplicationService.cancelApplication(
                principal.getHospitalId(),
                publicId,
                principal.getUserId(),
                true,
                principal.getUsername(),
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Leave application cancelled successfully", httpServletRequest.getRequestURI()));
    }

    // ==========================================
    // CALENDAR & OVERVIEW
    // ==========================================

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Get leave calendar", description = "Returns active and approved leaves within a date window for clinical coverage rotas")
    public ResponseEntity<ApiResponse<List<LeaveCalendarItemResponse>>> getLeaveCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        List<LeaveCalendarItemResponse> calendar = leaveApplicationService.getLeaveCalendar(
                principal.getHospitalId(),
                startDate,
                endDate
        );
        return ResponseEntity.ok(ApiResponse.ok(calendar, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Get leave overview", description = "Returns aggregated metrics for leave management dashboard")
    public ResponseEntity<ApiResponse<LeaveOverviewResponse>> getLeaveOverview(
            HttpServletRequest httpServletRequest
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        boolean isHr = principal.isPlatformUser()
                || principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("HOSPITAL_LEAVES_APPROVE") || a.getAuthority().equals("ROLE_HOSPITAL_ADMIN"));

        LeaveOverviewResponse overview = leaveApplicationService.getLeaveOverview(
                principal.getHospitalId(),
                principal.getUserId(),
                isHr
        );
        return ResponseEntity.ok(ApiResponse.ok(overview, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/applications/{publicId}/document")
    @PreAuthorize("hasAuthority('HOSPITAL_LEAVES_READ') or hasAuthority('HOSPITAL_LEAVES_MANAGE') or hasRole('PLATFORM_SUPER_ADMIN')")
    @Operation(summary = "Download leave attachment", description = "Downloads supporting document/medical certificate")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable String publicId
    ) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        LeaveApplicationResponse app = leaveApplicationService.getApplication(
                principal.getHospitalId(),
                publicId,
                principal.getUserId(),
                principal.isPlatformUser(),
                true
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
}
