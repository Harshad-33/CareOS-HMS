package com.careos.hms.authentication.service;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.role.entity.SystemRoleCode;
import com.careos.hms.security.CareOsSecurityProperties;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SuperAdminBootstrapServiceTest {

    @Autowired
    private SuperAdminBootstrapService bootstrapService;

    @Autowired
    private CareOsSecurityProperties securityProperties;

    @Autowired
    private SecurityTestDataHelper testDataHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        testDataHelper.resetSecurityTables();
        testDataHelper.seedRbacCatalog();
        securityProperties.getBootstrap().setEnabled(false);
        securityProperties.getBootstrap().setUsername("");
        securityProperties.getBootstrap().setEmail("");
        securityProperties.getBootstrap().setPassword("");
    }

    @AfterEach
    void tearDown() {
        securityProperties.getBootstrap().setEnabled(false);
        securityProperties.getBootstrap().setUsername("");
        securityProperties.getBootstrap().setEmail("");
        securityProperties.getBootstrap().setPassword("");
    }

    @Test
    void shouldSkipWhenBootstrapDisabled() {
        securityProperties.getBootstrap().setEnabled(false);
        boolean created = bootstrapService.bootstrapInitialSuperAdminIfConfigured();
        assertThat(created).isFalse();
        assertThat(userRepository.existsByRoleCode(SystemRoleCode.PLATFORM_SUPER_ADMIN)).isFalse();
    }

    @Test
    void shouldProvisionSuperAdminOnceWithBCryptHashAndPreventRepeatedBootstrap() {
        securityProperties.getBootstrap().setEnabled(true);
        securityProperties.getBootstrap().setUsername("root.platform");
        securityProperties.getBootstrap().setEmail("root.platform@careos.local");
        securityProperties.getBootstrap().setPassword("B00tstrap!Secur3#99");
        securityProperties.getBootstrap().setFirstName("Root");
        securityProperties.getBootstrap().setLastName("Platform");

        boolean firstRun = bootstrapService.bootstrapInitialSuperAdminIfConfigured();
        assertThat(firstRun).isTrue();
        assertThat(userRepository.existsByRoleCode(SystemRoleCode.PLATFORM_SUPER_ADMIN)).isTrue();

        User createdUser = userRepository.findActiveGraphByUsernameOrEmail("root.platform").orElseThrow();
        assertThat(createdUser.getUserScope()).isEqualTo(UserScope.PLATFORM);
        assertThat(createdUser.getHospital()).isNull();
        assertThat(createdUser.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("B00tstrap!Secur3#99", createdUser.getPasswordHash())).isTrue();

        boolean secondRun = bootstrapService.bootstrapInitialSuperAdminIfConfigured();
        assertThat(secondRun).isFalse();
    }

    @Test
    void shouldRejectWeakBootstrapPassword() {
        securityProperties.getBootstrap().setEnabled(true);
        securityProperties.getBootstrap().setUsername("root.platform");
        securityProperties.getBootstrap().setEmail("root.platform@careos.local");
        securityProperties.getBootstrap().setPassword("weak");

        assertThatThrownBy(() -> bootstrapService.bootstrapInitialSuperAdminIfConfigured())
                .isInstanceOf(InvalidRequestException.class);
        assertThat(userRepository.existsByRoleCode(SystemRoleCode.PLATFORM_SUPER_ADMIN)).isFalse();
    }
}
