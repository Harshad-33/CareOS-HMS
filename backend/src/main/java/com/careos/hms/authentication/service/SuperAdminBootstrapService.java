package com.careos.hms.authentication.service;

import com.careos.hms.authentication.validator.PasswordPolicyValidator;
import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.role.entity.Role;
import com.careos.hms.role.entity.SystemRoleCode;
import com.careos.hms.role.entity.UserRole;
import com.careos.hms.role.repository.RoleRepository;
import com.careos.hms.role.repository.UserRoleRepository;
import com.careos.hms.security.CareOsSecurityProperties;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuperAdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapService.class);

    private final CareOsSecurityProperties securityProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public SuperAdminBootstrapService(
            CareOsSecurityProperties securityProperties,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator
    ) {
        this.securityProperties = securityProperties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    @Transactional
    public boolean bootstrapInitialSuperAdminIfConfigured() {
        CareOsSecurityProperties.BootstrapProperties config = securityProperties.getBootstrap();
        if (!config.isEnabled()) {
            return false;
        }

        if (userRepository.existsByRoleCode(SystemRoleCode.PLATFORM_SUPER_ADMIN)) {
            log.info("PLATFORM_SUPER_ADMIN account already exists; bootstrap skipped.");
            return false;
        }

        validateBootstrapInputs(config);

        Role superAdminRole = roleRepository.findByCodeAndDeletedFalseAndActiveTrue(SystemRoleCode.PLATFORM_SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "System role PLATFORM_SUPER_ADMIN is missing; ensure Flyway RBAC seed ran cleanly."
                ));

        String username = config.getUsername().trim();
        String email = config.getEmail().trim();

        if (userRepository.existsByUsernameIgnoreCaseOrEmailIgnoreCase(username, email)) {
            throw new IllegalStateException(
                    "Bootstrap administrator username or email conflicts with an existing user account."
            );
        }

        passwordPolicyValidator.validatePasswordStrength(config.getPassword(), username);

        User adminUser = new User(
                null,
                UserScope.PLATFORM,
                username,
                email,
                passwordEncoder.encode(config.getPassword()),
                config.getFirstName(),
                config.getLastName()
        );
        User savedUser = userRepository.saveAndFlush(adminUser);
        userRoleRepository.saveAndFlush(new UserRole(savedUser, superAdminRole, ApiConstants.SYSTEM_AUDITOR));

        log.info("Initial PLATFORM_SUPER_ADMIN account provisioned: userPublicId={}", savedUser.getPublicId());
        return true;
    }

    private void validateBootstrapInputs(CareOsSecurityProperties.BootstrapProperties config) {
        if (config.getUsername() == null || config.getUsername().isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME is required when BOOTSTRAP_ADMIN_ENABLED=true.");
        }
        if (config.getEmail() == null || config.getEmail().isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL is required when BOOTSTRAP_ADMIN_ENABLED=true.");
        }
        if (config.getPassword() == null || config.getPassword().isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD is required when BOOTSTRAP_ADMIN_ENABLED=true.");
        }
    }
}
