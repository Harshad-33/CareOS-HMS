package com.careos.hms.hospital.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.hospital.dto.HospitalSettingsRequest;
import com.careos.hms.hospital.dto.HospitalSettingsResponse;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalSettings;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.hospital.repository.HospitalSettingsRepository;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class HospitalSettingsService {

    private final HospitalSettingsRepository hospitalSettingsRepository;
    private final HospitalRepository hospitalRepository;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;

    public HospitalSettingsService(
            HospitalSettingsRepository hospitalSettingsRepository,
            HospitalRepository hospitalRepository,
            AuditLogService auditLogService,
            TenantContext tenantContext
    ) {
        this.hospitalSettingsRepository = hospitalSettingsRepository;
        this.hospitalRepository = hospitalRepository;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public HospitalSettingsResponse getSettings(Long hospitalId) {
        assertHospitalAccess(hospitalId);
        HospitalSettings settings = hospitalSettingsRepository.findByHospitalId(hospitalId)
                .orElseGet(() -> createDefaultSettingsForHospitalId(hospitalId));
        return HospitalSettingsResponse.from(settings);
    }

    @Transactional(readOnly = true)
    public HospitalSettingsResponse getSettingsByPublicId(String hospitalPublicId) {
        Hospital hospital = hospitalRepository.findByPublicIdAndDeletedFalse(hospitalPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalPublicId));
        return getSettings(hospital.getId());
    }

    @Transactional(readOnly = true)
    public HospitalSettingsResponse getMySettings() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException("TENANT_REQUIRED", "Hospital context is required");
        }
        return getSettings(principal.getHospitalId());
    }

    @Transactional
    public HospitalSettingsResponse updateSettings(Long hospitalId, HospitalSettingsRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        HospitalSettings settings = hospitalSettingsRepository.findByHospitalId(hospitalId)
                .orElseGet(() -> createDefaultSettingsForHospitalId(hospitalId));

        settings.setTimezone(request.timezone().trim());
        settings.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        settings.setDateFormat(request.dateFormat().trim());
        settings.setTimeFormat(request.timeFormat().trim());
        settings.setWorkingDays(request.workingDays().trim());
        settings.setWorkingHoursStart(request.workingHoursStart());
        settings.setWorkingHoursEnd(request.workingHoursEnd());
        settings.setAppointmentSlotDurationMinutes(request.appointmentSlotDurationMinutes());
        settings.setMaxAdvanceBookingDays(request.maxAdvanceBookingDays());
        settings.setEmergencyContactNumber(request.emergencyContactNumber() != null ? request.emergencyContactNumber().trim() : null);
        settings.setSupportEmail(request.supportEmail() != null ? request.supportEmail().trim().toLowerCase(Locale.ROOT) : null);

        HospitalSettings saved = hospitalSettingsRepository.saveAndFlush(settings);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.SETTINGS_UPDATED,
                "HospitalSettings",
                String.valueOf(saved.getId()),
                "Updated operational settings",
                null,
                clientIp
        );

        return HospitalSettingsResponse.from(saved);
    }

    @Transactional
    public HospitalSettingsResponse updateMySettings(HospitalSettingsRequest request, String clientIp) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException("TENANT_REQUIRED", "Hospital context is required");
        }
        return updateSettings(principal.getHospitalId(), request, clientIp);
    }

    private HospitalSettings createDefaultSettingsForHospitalId(Long hospitalId) {
        Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        HospitalSettings defaultSettings = new HospitalSettings(hospital);
        return hospitalSettingsRepository.saveAndFlush(defaultSettings);
    }

    private void assertHospitalAccess(Long hospitalId) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (principal.isPlatformUser()) {
            return;
        }
        if (!Objects.equals(principal.getHospitalId(), hospitalId)) {
            throw new ForbiddenOperationException("FORBIDDEN", "Cannot access settings of another hospital");
        }
    }
}
