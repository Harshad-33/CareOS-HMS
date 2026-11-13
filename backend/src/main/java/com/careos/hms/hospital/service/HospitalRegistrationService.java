package com.careos.hms.hospital.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.authentication.validator.PasswordPolicyValidator;
import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.exception.DuplicateResourceException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.hospital.dto.HospitalRegistrationRequest;
import com.careos.hms.hospital.dto.HospitalResponse;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalSettings;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.hospital.repository.HospitalSettingsRepository;
import com.careos.hms.role.entity.Role;
import com.careos.hms.role.entity.SystemRoleCode;
import com.careos.hms.role.entity.UserRole;
import com.careos.hms.role.repository.RoleRepository;
import com.careos.hms.role.repository.UserRoleRepository;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class HospitalRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(HospitalRegistrationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final HospitalRepository hospitalRepository;
    private final HospitalSettingsRepository hospitalSettingsRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;

    public HospitalRegistrationService(
            HospitalRepository hospitalRepository,
            HospitalSettingsRepository hospitalSettingsRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            AuditLogService auditLogService,
            TenantContext tenantContext
    ) {
        this.hospitalRepository = hospitalRepository;
        this.hospitalSettingsRepository = hospitalSettingsRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public HospitalResponse registerHospital(HospitalRegistrationRequest request, String clientIp) {
        String regNumber = request.registrationNumber().trim();
        if (hospitalRepository.existsByRegistrationNumberAndDeletedFalse(regNumber)) {
            throw new DuplicateResourceException("A hospital with registration number '" + regNumber + "' already exists.");
        }

        String hospitalCode = resolveHospitalCode(request);

        String adminUsername = request.adminUsername().trim().toLowerCase(Locale.ROOT);
        String adminEmail = request.adminEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCaseOrEmailIgnoreCase(adminUsername, adminEmail)) {
            throw new DuplicateResourceException("Initial administrator username or email conflicts with an existing account.");
        }

        passwordPolicyValidator.validatePasswordStrength(request.adminPassword(), adminUsername);

        Role hospitalAdminRole = roleRepository.findByCodeAndDeletedFalseAndActiveTrue(SystemRoleCode.HOSPITAL_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role", SystemRoleCode.HOSPITAL_ADMIN));

        // 1. Create Hospital
        Hospital hospital = new Hospital(hospitalCode, request.hospitalName().trim());
        hospital.setRegistrationNumber(regNumber);
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
        hospital.setCountry(request.country() != null && !request.country().isBlank() ? request.country().trim() : "India");
        hospital.setPinCode(request.pinCode().trim());
        hospital.setEstablishedDate(request.establishedDate());
        hospital.setStatus(HospitalStatus.ACTIVE);

        Hospital savedHospital = hospitalRepository.saveAndFlush(hospital);

        // 2. Create Default Hospital Settings
        HospitalSettings settings = new HospitalSettings(savedHospital);
        settings.setEmergencyContactNumber(savedHospital.getPrimaryContactNumber());
        settings.setSupportEmail(savedHospital.getEmail());
        hospitalSettingsRepository.saveAndFlush(settings);

        // 3. Create Hospital Admin User
        User adminUser = new User(
                savedHospital,
                UserScope.HOSPITAL,
                adminUsername,
                adminEmail,
                passwordEncoder.encode(request.adminPassword()),
                request.adminFirstName().trim(),
                request.adminLastName().trim()
        );
        User savedAdminUser = userRepository.saveAndFlush(adminUser);

        // 4. Assign HOSPITAL_ADMIN Role
        userRoleRepository.saveAndFlush(new UserRole(savedAdminUser, hospitalAdminRole, ApiConstants.SYSTEM_AUDITOR));

        // 5. Audit Log
        Long actorUserId = tenantContext.findCurrentPrincipal().map(CareOsUserPrincipal::getUserId).orElse(null);
        String actorUsername = tenantContext.findCurrentPrincipal().map(CareOsUserPrincipal::getUsername).orElse("SYSTEM");

        auditLogService.record(
                actorUserId,
                actorUsername,
                savedHospital.getId(),
                AuditAction.HOSPITAL_REGISTERED,
                "Hospital",
                savedHospital.getPublicId(),
                "Registered new hospital: " + savedHospital.getName() + " (" + savedHospital.getHospitalCode() + ")",
                null,
                clientIp
        );

        log.info("Hospital registered successfully: hospitalPublicId={}, code={}, adminPublicId={}",
                savedHospital.getPublicId(), savedHospital.getHospitalCode(), savedAdminUser.getPublicId());

        return HospitalResponse.from(savedHospital);
    }

    private String resolveHospitalCode(HospitalRegistrationRequest request) {
        if (request.hospitalCode() != null && !request.hospitalCode().isBlank()) {
            String code = request.hospitalCode().trim().toUpperCase(Locale.ROOT);
            if (hospitalRepository.existsByHospitalCodeAndDeletedFalse(code)) {
                throw new DuplicateResourceException("Hospital code '" + code + "' is already in use.");
            }
            return code;
        }

        // Auto-generate code: prefix (from name or default) + 4 random alphanumeric characters
        String prefix = request.hospitalName().replaceAll("[^a-zA-Z]", "").toUpperCase(Locale.ROOT);
        if (prefix.length() < 3) {
            prefix = "HOSP";
        } else {
            prefix = prefix.substring(0, Math.min(prefix.length(), 4));
        }

        for (int attempts = 0; attempts < 20; attempts++) {
            int randomNum = 1000 + RANDOM.nextInt(9000);
            String candidate = prefix + "-" + randomNum;
            if (!hospitalRepository.existsByHospitalCodeAndDeletedFalse(candidate)) {
                return candidate;
            }
        }
        return prefix + "-" + System.currentTimeMillis() % 100000;
    }
}
