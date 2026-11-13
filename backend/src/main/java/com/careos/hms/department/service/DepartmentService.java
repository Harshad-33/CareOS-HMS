package com.careos.hms.department.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.common.exception.DuplicateResourceException;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.department.dto.DepartmentRequest;
import com.careos.hms.department.dto.DepartmentResponse;
import com.careos.hms.department.entity.Department;
import com.careos.hms.department.entity.DepartmentStatus;
import com.careos.hms.department.repository.DepartmentRepository;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            AuditLogService auditLogService,
            TenantContext tenantContext
    ) {
        this.departmentRepository = departmentRepository;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments(Long hospitalId) {
        assertHospitalAccess(hospitalId);
        return departmentRepository.findByHospitalIdAndDeletedFalseOrderByDepartmentNameAsc(hospitalId).stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getMyDepartments() {
        return getDepartments(requireCurrentHospitalId());
    }

    @Transactional
    public DepartmentResponse createDepartment(Long hospitalId, DepartmentRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        String code = request.departmentCode().trim().toUpperCase(Locale.ROOT);

        if (departmentRepository.existsByHospitalIdAndDepartmentCodeIgnoreCaseAndDeletedFalse(hospitalId, code)) {
            throw new DuplicateResourceException("Department code '" + code + "' already exists in this hospital");
        }

        Department department = new Department(
                hospitalId,
                code,
                request.departmentName().trim(),
                request.description() != null ? request.description().trim() : null
        );
        if (request.status() != null) {
            department.setStatus(request.status());
        }

        Department saved = departmentRepository.saveAndFlush(department);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.DEPARTMENT_CREATED,
                "Department",
                saved.getPublicId(),
                "Created department: " + saved.getDepartmentName() + " (" + saved.getDepartmentCode() + ")",
                null,
                clientIp
        );

        return DepartmentResponse.from(saved);
    }

    @Transactional
    public DepartmentResponse createMyDepartment(DepartmentRequest request, String clientIp) {
        return createDepartment(requireCurrentHospitalId(), request, clientIp);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long hospitalId, String publicId, DepartmentRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        Department department = departmentRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", publicId));

        String code = request.departmentCode().trim().toUpperCase(Locale.ROOT);
        if (departmentRepository.existsByHospitalIdAndDepartmentCodeIgnoreCaseAndIdNotAndDeletedFalse(hospitalId, code, department.getId())) {
            throw new DuplicateResourceException("Department code '" + code + "' already exists in this hospital");
        }

        department.setDepartmentCode(code);
        department.setDepartmentName(request.departmentName().trim());
        department.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.status() != null) {
            department.setStatus(request.status());
        }

        Department saved = departmentRepository.saveAndFlush(department);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.DEPARTMENT_UPDATED,
                "Department",
                saved.getPublicId(),
                "Updated department: " + saved.getDepartmentName() + " (" + saved.getDepartmentCode() + ")",
                null,
                clientIp
        );

        return DepartmentResponse.from(saved);
    }

    @Transactional
    public DepartmentResponse updateMyDepartment(String publicId, DepartmentRequest request, String clientIp) {
        return updateDepartment(requireCurrentHospitalId(), publicId, request, clientIp);
    }

    @Transactional
    public DepartmentResponse toggleDepartmentStatus(Long hospitalId, String publicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        Department department = departmentRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", publicId));

        DepartmentStatus newStatus = (department.getStatus() == DepartmentStatus.ACTIVE)
                ? DepartmentStatus.INACTIVE
                : DepartmentStatus.ACTIVE;
        department.setStatus(newStatus);

        Department saved = departmentRepository.saveAndFlush(department);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.DEPARTMENT_STATUS_CHANGED,
                "Department",
                saved.getPublicId(),
                "Changed status of department " + saved.getDepartmentCode() + " to " + newStatus,
                null,
                clientIp
        );

        return DepartmentResponse.from(saved);
    }

    @Transactional
    public DepartmentResponse toggleMyDepartmentStatus(String publicId, String clientIp) {
        return toggleDepartmentStatus(requireCurrentHospitalId(), publicId, clientIp);
    }

    @Transactional
    public void deleteDepartment(Long hospitalId, String publicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        Department department = departmentRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", publicId));

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        department.markDeleted(principal.getUsername());
        departmentRepository.saveAndFlush(department);

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.DEPARTMENT_DELETED,
                "Department",
                department.getPublicId(),
                "Deleted department: " + department.getDepartmentName() + " (" + department.getDepartmentCode() + ")",
                null,
                clientIp
        );
    }

    @Transactional
    public void deleteMyDepartment(String publicId, String clientIp) {
        deleteDepartment(requireCurrentHospitalId(), publicId, clientIp);
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
            throw new ForbiddenOperationException("FORBIDDEN", "Cannot access departments of another hospital");
        }
    }
}
