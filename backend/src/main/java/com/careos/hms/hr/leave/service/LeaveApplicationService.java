package com.careos.hms.hr.leave.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.employee.entity.Employee;
import com.careos.hms.employee.repository.EmployeeRepository;
import com.careos.hms.hr.leave.dto.ApplyLeaveRequest;
import com.careos.hms.hr.leave.dto.ApproveLeaveStepRequest;
import com.careos.hms.hr.leave.dto.LeaveApplicationResponse;
import com.careos.hms.hr.leave.dto.LeaveApplicationSummaryResponse;
import com.careos.hms.hr.leave.dto.LeaveApprovalInboxItemResponse;
import com.careos.hms.hr.leave.dto.LeaveBalanceResponse;
import com.careos.hms.hr.leave.dto.LeaveCalendarItemResponse;
import com.careos.hms.hr.leave.dto.LeaveOverviewResponse;
import com.careos.hms.hr.leave.dto.RejectLeaveStepRequest;
import com.careos.hms.hr.leave.entity.LeaveApplication;
import com.careos.hms.hr.leave.entity.LeaveApplicationStatus;
import com.careos.hms.hr.leave.entity.LeaveApproval;
import com.careos.hms.hr.leave.entity.LeaveApprovalStatus;
import com.careos.hms.hr.leave.entity.LeaveType;
import com.careos.hms.hr.leave.entity.LeaveTypeStatus;
import com.careos.hms.hr.leave.repository.LeaveApplicationRepository;
import com.careos.hms.hr.leave.repository.LeaveApprovalRepository;
import com.careos.hms.hr.leave.repository.LeaveTypeRepository;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveApprovalEngine leaveApprovalEngine;
    private final AuditLogService auditLogService;

    public LeaveApplicationService(
            LeaveApplicationRepository leaveApplicationRepository,
            LeaveApprovalRepository leaveApprovalRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            LeaveBalanceService leaveBalanceService,
            LeaveApprovalEngine leaveApprovalEngine,
            AuditLogService auditLogService
    ) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveApprovalRepository = leaveApprovalRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveApprovalEngine = leaveApprovalEngine;
        this.auditLogService = auditLogService;
    }

    public LeaveApplicationResponse applyLeave(
            Long hospitalId,
            ApplyLeaveRequest request,
            Long currentUserId,
            boolean isPlatformOrAdmin,
            String actor,
            String clientIp
    ) {
        Employee employee = resolveApplicantEmployee(hospitalId, request.employeePublicId(), currentUserId, isPlatformOrAdmin);

        LeaveType leaveType = leaveTypeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(request.leaveTypePublicId(), hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + request.leaveTypePublicId()));

        if (leaveType.getStatus() != LeaveTypeStatus.ACTIVE) {
            throw new InvalidRequestException("Cannot apply for leave with an inactive leave type: " + leaveType.getName());
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        if (startDate.isAfter(endDate)) {
            throw new InvalidRequestException("Start date cannot be after end date.");
        }

        BigDecimal totalDays;
        if (request.halfDay()) {
            if (!startDate.isEqual(endDate)) {
                throw new InvalidRequestException("Half-day leave must start and end on the same date.");
            }
            if (request.halfDayPeriod() == null) {
                throw new InvalidRequestException("Half-day period (FIRST_HALF or SECOND_HALF) is required for half-day leave.");
            }
            totalDays = BigDecimal.valueOf(0.5);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            totalDays = BigDecimal.valueOf(daysBetween);
        }

        // Overlapping leave detection
        List<LeaveApplication> overlapping = leaveApplicationRepository.findOverlappingActiveLeaves(
                hospitalId,
                employee.getId(),
                List.of(LeaveApplicationStatus.PENDING, LeaveApplicationStatus.APPROVED),
                startDate,
                endDate
        );
        if (!overlapping.isEmpty()) {
            LeaveApplication first = overlapping.getFirst();
            throw new InvalidRequestException("An active leave application already exists between "
                    + first.getStartDate() + " and " + first.getEndDate() + " with status " + first.getStatus());
        }

        // Medical certificate requirement
        if (leaveType.isMedicalCertRequired() && totalDays.compareTo(BigDecimal.valueOf(2)) > 0) {
            if (request.documentStorageKey() == null || request.documentStorageKey().isBlank()) {
                throw new InvalidRequestException("A medical certificate is mandatory for " + leaveType.getName() + " for more than 2 consecutive days.");
            }
        }

        // Reserve pending balance (validates balance sufficiency and throws InvalidRequestException if inadequate)
        int calendarYear = startDate.getYear();
        leaveBalanceService.reservePendingLeave(hospitalId, employee.getId(), leaveType.getId(), calendarYear, totalDays);

        LeaveApplication application = new LeaveApplication(
                hospitalId,
                employee,
                leaveType,
                startDate,
                endDate,
                totalDays,
                request.halfDay(),
                request.halfDayPeriod(),
                request.reason().trim(),
                request.contactPhoneDuringLeave(),
                request.documentStorageKey()
        );
        application.setRequiredStepsCount(leaveType.getApprovalLevelsCount());
        application.setCurrentStepNumber(1);

        LeaveApplication saved = leaveApplicationRepository.save(application);

        // Initialize approval steps
        List<LeaveApproval> steps = leaveApprovalEngine.initializeApprovalSteps(hospitalId, saved, employee, leaveType);

        auditLogService.record(
                currentUserId,
                actor,
                hospitalId,
                AuditAction.LEAVE_APPLIED,
                "LeaveApplication",
                saved.getPublicId(),
                "Leave application submitted for " + totalDays + " days of " + leaveType.getName(),
                "{\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\",\"totalDays\":" + totalDays + "}",
                clientIp
        );

        return LeaveApplicationResponse.from(saved, steps);
    }

    public LeaveApplicationResponse approveApplicationStep(
            Long hospitalId,
            String applicationPublicId,
            int stepNumber,
            ApproveLeaveStepRequest request,
            Long currentUserId,
            boolean isPlatformOrAdmin,
            boolean hasHrPermission,
            String actor,
            String clientIp
    ) {
        LeaveApplication application = getEntityByPublicId(hospitalId, applicationPublicId);

        if (application.getStatus() != LeaveApplicationStatus.PENDING) {
            throw new InvalidRequestException("Leave application is not in PENDING status (current: " + application.getStatus() + ")");
        }

        if (application.getCurrentStepNumber() != stepNumber) {
            throw new InvalidRequestException("Cannot approve step " + stepNumber + ". Current active step is " + application.getCurrentStepNumber());
        }

        LeaveApproval step = leaveApprovalRepository.findByLeaveApplicationIdAndStepNumber(application.getId(), stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Approval step " + stepNumber + " not found for application."));

        if (!leaveApprovalEngine.canUserApproveStep(step, currentUserId, isPlatformOrAdmin, hasHrPermission)) {
            throw new ForbiddenOperationException("FORBIDDEN", "You are not authorized to approve step " + stepNumber + " of this leave application.");
        }

        User actingUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Acting user not found"));

        step.setStatus(LeaveApprovalStatus.APPROVED);
        step.setActionTakenByUser(actingUser);
        step.setDecisionTimestamp(Instant.now());
        if (request != null && request.comments() != null) {
            step.setComments(request.comments().trim());
        }
        leaveApprovalRepository.save(step);

        if (stepNumber >= application.getRequiredStepsCount()) {
            // Final approval!
            application.setStatus(LeaveApplicationStatus.APPROVED);
            application.setFinalDecisionAt(Instant.now());

            leaveBalanceService.finalizeApprovedLeave(
                    hospitalId,
                    application.getEmployee().getId(),
                    application.getLeaveType().getId(),
                    application.getStartDate().getYear(),
                    application.getTotalDays(),
                    application.getPublicId(),
                    actor
            );
        } else {
            // Advance to next step
            application.setCurrentStepNumber(stepNumber + 1);
        }

        LeaveApplication saved = leaveApplicationRepository.save(application);
        List<LeaveApproval> allSteps = leaveApprovalRepository.findByLeaveApplicationIdOrderByStepNumberAsc(saved.getId());

        auditLogService.record(
                currentUserId,
                actor,
                hospitalId,
                AuditAction.LEAVE_APPROVED,
                "LeaveApplication",
                saved.getPublicId(),
                "Step " + stepNumber + " approved by " + actor + " (Status: " + saved.getStatus() + ")",
                "{\"stepNumber\":" + stepNumber + ",\"status\":\"" + saved.getStatus() + "\"}",
                clientIp
        );

        return LeaveApplicationResponse.from(saved, allSteps);
    }

    public LeaveApplicationResponse rejectApplicationStep(
            Long hospitalId,
            String applicationPublicId,
            int stepNumber,
            RejectLeaveStepRequest request,
            Long currentUserId,
            boolean isPlatformOrAdmin,
            boolean hasHrPermission,
            String actor,
            String clientIp
    ) {
        if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw new InvalidRequestException("Rejection reason is mandatory when rejecting a leave application.");
        }

        LeaveApplication application = getEntityByPublicId(hospitalId, applicationPublicId);

        if (application.getStatus() != LeaveApplicationStatus.PENDING) {
            throw new InvalidRequestException("Leave application is not in PENDING status (current: " + application.getStatus() + ")");
        }

        if (application.getCurrentStepNumber() != stepNumber) {
            throw new InvalidRequestException("Cannot reject step " + stepNumber + ". Current active step is " + application.getCurrentStepNumber());
        }

        LeaveApproval step = leaveApprovalRepository.findByLeaveApplicationIdAndStepNumber(application.getId(), stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Approval step " + stepNumber + " not found for application."));

        if (!leaveApprovalEngine.canUserApproveStep(step, currentUserId, isPlatformOrAdmin, hasHrPermission)) {
            throw new ForbiddenOperationException("FORBIDDEN", "You are not authorized to reject step " + stepNumber + " of this leave application.");
        }

        User actingUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Acting user not found"));

        step.setStatus(LeaveApprovalStatus.REJECTED);
        step.setActionTakenByUser(actingUser);
        step.setDecisionTimestamp(Instant.now());
        step.setRejectionReason(request.rejectionReason().trim());
        if (request.comments() != null) {
            step.setComments(request.comments().trim());
        }
        leaveApprovalRepository.save(step);

        application.setStatus(LeaveApplicationStatus.REJECTED);
        application.setFinalDecisionAt(Instant.now());
        application.setFinalRejectionReason(request.rejectionReason().trim());

        // Release the pending leave reservation back to available
        leaveBalanceService.releasePendingLeave(
                hospitalId,
                application.getEmployee().getId(),
                application.getLeaveType().getId(),
                application.getStartDate().getYear(),
                application.getTotalDays()
        );

        LeaveApplication saved = leaveApplicationRepository.save(application);
        List<LeaveApproval> allSteps = leaveApprovalRepository.findByLeaveApplicationIdOrderByStepNumberAsc(saved.getId());

        auditLogService.record(
                currentUserId,
                actor,
                hospitalId,
                AuditAction.LEAVE_REJECTED,
                "LeaveApplication",
                saved.getPublicId(),
                "Step " + stepNumber + " rejected by " + actor + ". Reason: " + request.rejectionReason().trim(),
                "{\"stepNumber\":" + stepNumber + ",\"reason\":\"" + request.rejectionReason().trim() + "\"}",
                clientIp
        );

        return LeaveApplicationResponse.from(saved, allSteps);
    }

    public LeaveApplicationResponse cancelApplication(
            Long hospitalId,
            String applicationPublicId,
            Long currentUserId,
            boolean isPlatformOrAdmin,
            String actor,
            String clientIp
    ) {
        LeaveApplication application = getEntityByPublicId(hospitalId, applicationPublicId);

        // Security check: Must be the employee who submitted or a manager/admin
        boolean isOwner = application.getEmployee().getUser() != null
                && application.getEmployee().getUser().getId().equals(currentUserId);
        if (!isOwner && !isPlatformOrAdmin) {
            throw new ForbiddenOperationException("FORBIDDEN", "You are not authorized to cancel this leave application.");
        }

        if (application.getStatus() == LeaveApplicationStatus.PENDING) {
            leaveBalanceService.releasePendingLeave(
                    hospitalId,
                    application.getEmployee().getId(),
                    application.getLeaveType().getId(),
                    application.getStartDate().getYear(),
                    application.getTotalDays()
            );

            // Mark pending steps CANCELLED
            List<LeaveApproval> steps = leaveApprovalRepository.findByLeaveApplicationIdOrderByStepNumberAsc(application.getId());
            for (LeaveApproval s : steps) {
                if (s.getStatus() == LeaveApprovalStatus.PENDING) {
                    s.setStatus(LeaveApprovalStatus.CANCELLED);
                    leaveApprovalRepository.save(s);
                }
            }

            application.setStatus(LeaveApplicationStatus.CANCELLED);
        } else if (application.getStatus() == LeaveApplicationStatus.APPROVED) {
            if (application.getStartDate().isBefore(LocalDate.now())) {
                throw new InvalidRequestException("Cannot cancel leave that has already commenced or concluded.");
            }

            leaveBalanceService.reverseApprovedLeave(
                    hospitalId,
                    application.getEmployee().getId(),
                    application.getLeaveType().getId(),
                    application.getStartDate().getYear(),
                    application.getTotalDays(),
                    application.getPublicId(),
                    actor
            );

            application.setStatus(LeaveApplicationStatus.CANCELLED);
        } else {
            throw new InvalidRequestException("Cannot cancel leave application with status " + application.getStatus());
        }

        LeaveApplication saved = leaveApplicationRepository.save(application);
        List<LeaveApproval> allSteps = leaveApprovalRepository.findByLeaveApplicationIdOrderByStepNumberAsc(saved.getId());

        auditLogService.record(
                currentUserId,
                actor,
                hospitalId,
                AuditAction.LEAVE_CANCELLED,
                "LeaveApplication",
                saved.getPublicId(),
                "Leave application cancelled by " + actor,
                "{\"status\":\"CANCELLED\"}",
                clientIp
        );

        return LeaveApplicationResponse.from(saved, allSteps);
    }

    @Transactional(readOnly = true)
    public LeaveApplicationResponse getApplication(Long hospitalId, String applicationPublicId, Long currentUserId, boolean isPlatformOrAdmin, boolean hasManagementPermission) {
        LeaveApplication app = getEntityByPublicId(hospitalId, applicationPublicId);

        boolean isOwner = app.getEmployee().getUser() != null && app.getEmployee().getUser().getId().equals(currentUserId);
        boolean isManager = app.getEmployee().getReportingManager() != null
                && app.getEmployee().getReportingManager().getUser() != null
                && app.getEmployee().getReportingManager().getUser().getId().equals(currentUserId);

        if (!isOwner && !isManager && !isPlatformOrAdmin && !hasManagementPermission) {
            throw new ForbiddenOperationException("FORBIDDEN", "You are not authorized to view this leave application.");
        }

        List<LeaveApproval> steps = leaveApprovalRepository.findByLeaveApplicationIdOrderByStepNumberAsc(app.getId());
        return LeaveApplicationResponse.from(app, steps);
    }

    @Transactional(readOnly = true)
    public Page<LeaveApplicationSummaryResponse> getSelfApplications(Long hospitalId, Long employeeId, Pageable pageable) {
        return leaveApplicationRepository.findByHospitalIdAndEmployeeIdAndDeletedFalseOrderByAppliedAtDesc(hospitalId, employeeId, pageable)
                .map(LeaveApplicationSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LeaveApprovalInboxItemResponse> getPendingApprovals(Long hospitalId, Long currentUserId, boolean isHr, Pageable pageable) {
        Employee currentEmployee = employeeRepository.findByUserIdAndDeletedFalse(currentUserId).orElse(null);
        Long approverEmpId = currentEmployee != null ? currentEmployee.getId() : -1L;

        return leaveApprovalRepository.findPendingApprovalsForUser(
                hospitalId,
                LeaveApprovalStatus.PENDING,
                approverEmpId,
                isHr,
                pageable
        ).map(LeaveApprovalInboxItemResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LeaveApplicationSummaryResponse> getHospitalApplications(Long hospitalId, LeaveApplicationStatus status, Pageable pageable) {
        if (status != null) {
            return leaveApplicationRepository.findByHospitalIdAndStatusAndDeletedFalseOrderByAppliedAtDesc(hospitalId, status, pageable)
                    .map(LeaveApplicationSummaryResponse::from);
        }
        return leaveApplicationRepository.findByHospitalIdAndDeletedFalseOrderByAppliedAtDesc(hospitalId, pageable)
                .map(LeaveApplicationSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public List<LeaveCalendarItemResponse> getLeaveCalendar(Long hospitalId, LocalDate startDate, LocalDate endDate) {
        return leaveApplicationRepository.findCalendarLeaves(hospitalId, startDate, endDate)
                .stream()
                .map(LeaveCalendarItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveOverviewResponse getLeaveOverview(Long hospitalId, Long currentUserId, boolean isHr) {
        long activeTypes = leaveTypeRepository.findByHospitalIdAndStatusAndDeletedFalseOrderByCodeAsc(hospitalId, LeaveTypeStatus.ACTIVE).size();

        Employee employee = employeeRepository.findByUserIdAndDeletedFalse(currentUserId).orElse(null);
        Long approverEmpId = employee != null ? employee.getId() : -1L;

        long pendingCount = leaveApprovalRepository.countPendingApprovalsForUser(hospitalId, approverEmpId, isHr);
        long onLeaveToday = leaveApplicationRepository.countOnLeaveToday(hospitalId, LocalDate.now());

        BigDecimal myAvailable = BigDecimal.ZERO;
        List<LeaveBalanceResponse> myBalances = Collections.emptyList();
        List<LeaveApplicationSummaryResponse> recentApplications = Collections.emptyList();

        if (employee != null) {
            myBalances = leaveBalanceService.getSelfBalances(hospitalId, employee.getId(), LocalDate.now().getYear());
            myAvailable = myBalances.stream()
                    .map(LeaveBalanceResponse::availableBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Page<LeaveApplicationSummaryResponse> recent = getSelfApplications(hospitalId, employee.getId(), PageRequest.of(0, 5));
            recentApplications = recent.getContent();
        }

        return new LeaveOverviewResponse(
                activeTypes,
                pendingCount,
                onLeaveToday,
                myAvailable,
                recentApplications,
                myBalances
        );
    }

    private Employee resolveApplicantEmployee(Long hospitalId, String employeePublicId, Long currentUserId, boolean isPlatformOrAdmin) {
        if (employeePublicId != null && !employeePublicId.isBlank()) {
            Employee emp = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(employeePublicId, hospitalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeePublicId));

            if (!isPlatformOrAdmin) {
                // Must be the owner or manager
                boolean isOwner = emp.getUser() != null && emp.getUser().getId().equals(currentUserId);
                if (!isOwner) {
                    throw new ForbiddenOperationException("FORBIDDEN", "You are not authorized to submit leave on behalf of another employee.");
                }
            }
            return emp;
        }

        return employeeRepository.findByUserIdAndDeletedFalse(currentUserId)
                .filter(e -> e.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new InvalidRequestException("Your user account is not linked to an active employee profile in this hospital."));
    }

    private LeaveApplication getEntityByPublicId(Long hospitalId, String publicId) {
        return leaveApplicationRepository.findWithDetailsByPublicIdAndHospitalId(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with ID: " + publicId));
    }
}
