package com.careos.hms.hospital.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.authentication.entity.TokenRevocationReason;
import com.careos.hms.authentication.repository.RefreshTokenRepository;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.hospital.dto.HospitalResponse;
import com.careos.hms.hospital.dto.UpdateHospitalProfileRequest;
import com.careos.hms.hospital.dto.UpdateHospitalStatusRequest;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class HospitalService {

    private static final Logger log = LoggerFactory.getLogger(HospitalService.class);

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;

    public HospitalService(
            HospitalRepository hospitalRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuditLogService auditLogService,
            TenantContext tenantContext
    ) {
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public Page<HospitalResponse> getAllHospitals(HospitalStatus status, String search, Pageable pageable) {
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return hospitalRepository.searchHospitals(status, trimmedSearch, pageable)
                .map(HospitalResponse::from);
    }

    @Transactional(readOnly = true)
    public HospitalResponse getHospitalByPublicId(String publicId) {
        Hospital hospital = getEntityByPublicId(publicId);
        assertHospitalReadAccess(hospital);
        return HospitalResponse.from(hospital);
    }

    @Transactional(readOnly = true)
    public HospitalResponse getMyHospital() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException("TENANT_REQUIRED", "User is not bound to a hospital tenant");
        }
        Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(principal.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", principal.getHospitalId()));
        return HospitalResponse.from(hospital);
    }

    @Transactional
    public HospitalResponse updateHospitalProfile(String publicId, UpdateHospitalProfileRequest request, String clientIp) {
        Hospital hospital = getEntityByPublicId(publicId);
        assertHospitalWriteAccess(hospital);

        hospital.setName(request.name().trim());
        hospital.setHospitalType(request.hospitalType());
        hospital.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        hospital.setPrimaryContactNumber(request.primaryContactNumber().trim());
        hospital.setAlternateContactNumber(request.alternateContactNumber() != null ? request.alternateContactNumber().trim() : null);
        hospital.setWebsite(request.website() != null ? request.website().trim() : null);
        hospital.setAddressLine1(request.addressLine1().trim());
        hospital.setAddressLine2(request.addressLine2() != null ? request.addressLine2().trim() : null);
        hospital.setCity(request.city().trim());
        hospital.setDistrict(request.district().trim());
        hospital.setState(request.state().trim());
        if (request.country() != null && !request.country().isBlank()) {
            hospital.setCountry(request.country().trim());
        }
        hospital.setPinCode(request.pinCode().trim());
        hospital.setEstablishedDate(request.establishedDate());

        Hospital saved = hospitalRepository.saveAndFlush(hospital);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                saved.getId(),
                AuditAction.HOSPITAL_UPDATED,
                "Hospital",
                saved.getPublicId(),
                "Updated profile for hospital: " + saved.getName(),
                null,
                clientIp
        );

        log.info("Hospital profile updated: hospitalPublicId={}, actor={}", saved.getPublicId(), principal.getUsername());
        return HospitalResponse.from(saved);
    }

    @Transactional
    public HospitalResponse updateHospitalStatus(String publicId, UpdateHospitalStatusRequest request, String clientIp) {
        Hospital hospital = getEntityByPublicId(publicId);
        HospitalStatus currentStatus = hospital.getStatus();
        HospitalStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            return HospitalResponse.from(hospital);
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidRequestException(
                    "Invalid status transition from " + currentStatus + " to " + targetStatus
            );
        }

        hospital.setStatus(targetStatus);
        Hospital saved = hospitalRepository.saveAndFlush(hospital);

        // If status changed to SUSPENDED or INACTIVE, revoke sessions for all hospital users
        if (targetStatus == HospitalStatus.SUSPENDED || targetStatus == HospitalStatus.INACTIVE) {
            List<User> hospitalUsers = userRepository.findAllByHospitalIdAndDeletedFalse(saved.getId());
            Instant now = Instant.now();
            for (User user : hospitalUsers) {
                refreshTokenRepository.revokeAllActiveTokensForUser(
                        user.getId(),
                        TokenRevocationReason.ACCOUNT_DISABLED,
                        now
                );
            }
            log.info("Revoked active sessions for {} user(s) due to hospital suspension: hospitalCode={}",
                    hospitalUsers.size(), saved.getHospitalCode());
        }

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        String desc = "Status changed from " + currentStatus + " to " + targetStatus +
                (request.reason() != null ? ". Reason: " + request.reason() : "");

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                saved.getId(),
                AuditAction.HOSPITAL_STATUS_CHANGED,
                "Hospital",
                saved.getPublicId(),
                desc,
                null,
                clientIp
        );

        log.info("Hospital status updated: hospitalPublicId={}, from={}, to={}",
                saved.getPublicId(), currentStatus, targetStatus);
        return HospitalResponse.from(saved);
    }

    public Hospital getEntityByPublicId(String publicId) {
        return hospitalRepository.findByPublicIdAndDeletedFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", publicId));
    }

    private void assertHospitalReadAccess(Hospital hospital) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (principal.isPlatformUser()) {
            return;
        }
        if (!Objects.equals(principal.getHospitalId(), hospital.getId())) {
            throw new ForbiddenOperationException("FORBIDDEN", "Access denied for requested hospital");
        }
    }

    private void assertHospitalWriteAccess(Hospital hospital) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (principal.isPlatformUser()) {
            return;
        }
        if (!Objects.equals(principal.getHospitalId(), hospital.getId())) {
            throw new ForbiddenOperationException("FORBIDDEN", "Cannot modify details of another hospital");
        }
    }
}
