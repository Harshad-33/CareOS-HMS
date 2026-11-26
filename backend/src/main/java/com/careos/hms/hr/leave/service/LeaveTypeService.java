package com.careos.hms.hr.leave.service;

import com.careos.hms.common.exception.DuplicateResourceException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.hr.leave.dto.CreateLeaveTypeRequest;
import com.careos.hms.hr.leave.dto.LeaveTypeResponse;
import com.careos.hms.hr.leave.dto.LeaveTypeSummaryResponse;
import com.careos.hms.hr.leave.dto.UpdateLeaveTypeRequest;
import com.careos.hms.hr.leave.entity.LeaveApprovalWorkflowType;
import com.careos.hms.hr.leave.entity.LeaveType;
import com.careos.hms.hr.leave.entity.LeaveTypeStatus;
import com.careos.hms.hr.leave.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    public LeaveTypeResponse createLeaveType(Long hospitalId, CreateLeaveTypeRequest request, String actor) {
        String cleanCode = request.code().trim().toUpperCase();
        if (leaveTypeRepository.existsByHospitalIdAndCodeIgnoreCaseAndDeletedFalse(hospitalId, cleanCode)) {
            throw new DuplicateResourceException("Leave type code '" + cleanCode + "' already exists for this hospital.");
        }

        int normalizedLevels = determineApprovalLevels(request.approvalWorkflowType(), request.approvalLevelsCount());

        LeaveType leaveType = new LeaveType(
                hospitalId,
                cleanCode,
                request.name().trim(),
                request.description(),
                request.annualAllowance(),
                request.carryForwardEnabled(),
                request.carryForwardEnabled() ? request.maxCarryForwardDays() : 0,
                request.medicalCertRequired(),
                request.minServiceDaysRequired(),
                request.paid(),
                request.approvalWorkflowType(),
                normalizedLevels
        );

        LeaveType saved = leaveTypeRepository.save(leaveType);
        return LeaveTypeResponse.from(saved);
    }

    public LeaveTypeResponse updateLeaveType(Long hospitalId, String publicId, UpdateLeaveTypeRequest request, String actor) {
        LeaveType leaveType = getEntityByPublicId(hospitalId, publicId);

        int normalizedLevels = determineApprovalLevels(request.approvalWorkflowType(), request.approvalLevelsCount());

        leaveType.setName(request.name().trim());
        leaveType.setDescription(request.description());
        leaveType.setAnnualAllowance(request.annualAllowance());
        leaveType.setCarryForwardEnabled(request.carryForwardEnabled());
        leaveType.setMaxCarryForwardDays(request.carryForwardEnabled() ? request.maxCarryForwardDays() : 0);
        leaveType.setMedicalCertRequired(request.medicalCertRequired());
        leaveType.setMinServiceDaysRequired(request.minServiceDaysRequired());
        leaveType.setPaid(request.paid());
        leaveType.setApprovalWorkflowType(request.approvalWorkflowType());
        leaveType.setApprovalLevelsCount(normalizedLevels);

        LeaveType updated = leaveTypeRepository.save(leaveType);
        return LeaveTypeResponse.from(updated);
    }

    public LeaveTypeResponse activateLeaveType(Long hospitalId, String publicId, String actor) {
        LeaveType leaveType = getEntityByPublicId(hospitalId, publicId);
        leaveType.setStatus(LeaveTypeStatus.ACTIVE);
        return LeaveTypeResponse.from(leaveTypeRepository.save(leaveType));
    }

    public LeaveTypeResponse deactivateLeaveType(Long hospitalId, String publicId, String actor) {
        LeaveType leaveType = getEntityByPublicId(hospitalId, publicId);
        leaveType.setStatus(LeaveTypeStatus.INACTIVE);
        return LeaveTypeResponse.from(leaveTypeRepository.save(leaveType));
    }

    @Transactional(readOnly = true)
    public LeaveTypeResponse getLeaveType(Long hospitalId, String publicId) {
        return LeaveTypeResponse.from(getEntityByPublicId(hospitalId, publicId));
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> getLeaveTypes(Long hospitalId, boolean activeOnly) {
        List<LeaveType> types = activeOnly
                ? leaveTypeRepository.findByHospitalIdAndStatusAndDeletedFalseOrderByCodeAsc(hospitalId, LeaveTypeStatus.ACTIVE)
                : leaveTypeRepository.findByHospitalIdAndDeletedFalseOrderByCodeAsc(hospitalId);
        return types.stream().map(LeaveTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeSummaryResponse> getLeaveTypeSummaries(Long hospitalId) {
        return leaveTypeRepository.findByHospitalIdAndStatusAndDeletedFalseOrderByCodeAsc(hospitalId, LeaveTypeStatus.ACTIVE)
                .stream()
                .map(LeaveTypeSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveType getEntityByPublicId(Long hospitalId, String publicId) {
        return leaveTypeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + publicId));
    }

    private int determineApprovalLevels(LeaveApprovalWorkflowType workflowType, int configuredCount) {
        return switch (workflowType) {
            case HR_ONLY, MANAGER_ONLY -> 1;
            case MANAGER_THEN_HR, DEPT_HEAD_THEN_HR -> 2;
        };
    }
}
