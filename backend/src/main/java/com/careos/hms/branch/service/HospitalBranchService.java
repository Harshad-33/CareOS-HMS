package com.careos.hms.branch.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.branch.dto.HospitalBranchRequest;
import com.careos.hms.branch.dto.HospitalBranchResponse;
import com.careos.hms.branch.entity.BranchStatus;
import com.careos.hms.branch.entity.HospitalBranch;
import com.careos.hms.branch.repository.HospitalBranchRepository;
import com.careos.hms.common.exception.DuplicateResourceException;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class HospitalBranchService {

    private final HospitalBranchRepository branchRepository;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;

    public HospitalBranchService(
            HospitalBranchRepository branchRepository,
            AuditLogService auditLogService,
            TenantContext tenantContext
    ) {
        this.branchRepository = branchRepository;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public List<HospitalBranchResponse> getBranches(Long hospitalId) {
        assertHospitalAccess(hospitalId);
        return branchRepository.findByHospitalIdAndDeletedFalseOrderByBranchNameAsc(hospitalId).stream()
                .map(HospitalBranchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HospitalBranchResponse> getMyBranches() {
        return getBranches(requireCurrentHospitalId());
    }

    @Transactional
    public HospitalBranchResponse createBranch(Long hospitalId, HospitalBranchRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        String code = request.branchCode().trim().toUpperCase(Locale.ROOT);

        if (branchRepository.existsByHospitalIdAndBranchCodeIgnoreCaseAndDeletedFalse(hospitalId, code)) {
            throw new DuplicateResourceException("Branch code '" + code + "' already exists in this hospital");
        }

        HospitalBranch branch = new HospitalBranch(hospitalId, code, request.branchName().trim());
        branch.setContactNumber(request.contactNumber() != null ? request.contactNumber().trim() : null);
        branch.setEmail(request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : null);
        branch.setAddress(request.address() != null ? request.address().trim() : null);
        branch.setCity(request.city() != null ? request.city().trim() : null);
        branch.setDistrict(request.district() != null ? request.district().trim() : null);
        branch.setState(request.state() != null ? request.state().trim() : null);
        if (request.country() != null && !request.country().isBlank()) {
            branch.setCountry(request.country().trim());
        }
        branch.setPinCode(request.pinCode() != null ? request.pinCode().trim() : null);
        if (request.status() != null) {
            branch.setStatus(request.status());
        }

        HospitalBranch saved = branchRepository.saveAndFlush(branch);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.BRANCH_CREATED,
                "HospitalBranch",
                saved.getPublicId(),
                "Created branch: " + saved.getBranchName() + " (" + saved.getBranchCode() + ")",
                null,
                clientIp
        );

        return HospitalBranchResponse.from(saved);
    }

    @Transactional
    public HospitalBranchResponse createMyBranch(HospitalBranchRequest request, String clientIp) {
        return createBranch(requireCurrentHospitalId(), request, clientIp);
    }

    @Transactional
    public HospitalBranchResponse updateBranch(Long hospitalId, String publicId, HospitalBranchRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        HospitalBranch branch = branchRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalBranch", publicId));

        String code = request.branchCode().trim().toUpperCase(Locale.ROOT);
        if (branchRepository.existsByHospitalIdAndBranchCodeIgnoreCaseAndIdNotAndDeletedFalse(hospitalId, code, branch.getId())) {
            throw new DuplicateResourceException("Branch code '" + code + "' already exists in this hospital");
        }

        branch.setBranchCode(code);
        branch.setBranchName(request.branchName().trim());
        branch.setContactNumber(request.contactNumber() != null ? request.contactNumber().trim() : null);
        branch.setEmail(request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : null);
        branch.setAddress(request.address() != null ? request.address().trim() : null);
        branch.setCity(request.city() != null ? request.city().trim() : null);
        branch.setDistrict(request.district() != null ? request.district().trim() : null);
        branch.setState(request.state() != null ? request.state().trim() : null);
        if (request.country() != null && !request.country().isBlank()) {
            branch.setCountry(request.country().trim());
        }
        branch.setPinCode(request.pinCode() != null ? request.pinCode().trim() : null);
        if (request.status() != null) {
            branch.setStatus(request.status());
        }

        HospitalBranch saved = branchRepository.saveAndFlush(branch);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.BRANCH_UPDATED,
                "HospitalBranch",
                saved.getPublicId(),
                "Updated branch: " + saved.getBranchName() + " (" + saved.getBranchCode() + ")",
                null,
                clientIp
        );

        return HospitalBranchResponse.from(saved);
    }

    @Transactional
    public HospitalBranchResponse updateMyBranch(String publicId, HospitalBranchRequest request, String clientIp) {
        return updateBranch(requireCurrentHospitalId(), publicId, request, clientIp);
    }

    @Transactional
    public HospitalBranchResponse toggleBranchStatus(Long hospitalId, String publicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        HospitalBranch branch = branchRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalBranch", publicId));

        BranchStatus newStatus = (branch.getStatus() == BranchStatus.ACTIVE)
                ? BranchStatus.INACTIVE
                : BranchStatus.ACTIVE;
        branch.setStatus(newStatus);

        HospitalBranch saved = branchRepository.saveAndFlush(branch);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.BRANCH_STATUS_CHANGED,
                "HospitalBranch",
                saved.getPublicId(),
                "Changed status of branch " + saved.getBranchCode() + " to " + newStatus,
                null,
                clientIp
        );

        return HospitalBranchResponse.from(saved);
    }

    @Transactional
    public HospitalBranchResponse toggleMyBranchStatus(String publicId, String clientIp) {
        return toggleBranchStatus(requireCurrentHospitalId(), publicId, clientIp);
    }

    @Transactional
    public void deleteBranch(Long hospitalId, String publicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        HospitalBranch branch = branchRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalBranch", publicId));

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        branch.markDeleted(principal.getUsername());
        branchRepository.saveAndFlush(branch);

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.BRANCH_DELETED,
                "HospitalBranch",
                branch.getPublicId(),
                "Deleted branch: " + branch.getBranchName() + " (" + branch.getBranchCode() + ")",
                null,
                clientIp
        );
    }

    @Transactional
    public void deleteMyBranch(String publicId, String clientIp) {
        deleteBranch(requireCurrentHospitalId(), publicId, clientIp);
    }

    private Long requireCurrentHospitalId() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException("TENANT_REQUIRED", "Hospital context is required");
        }
        return principal.getHospitalId();
    }

    private void assertHospitalAccess(Long hospitalId) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (principal.isPlatformUser()) {
            return;
        }
        if (!Objects.equals(principal.getHospitalId(), hospitalId)) {
            throw new ForbiddenOperationException("FORBIDDEN", "Cannot access branches of another hospital");
        }
    }
}
