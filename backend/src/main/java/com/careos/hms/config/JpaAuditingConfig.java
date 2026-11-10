package com.careos.hms.config;

import com.careos.hms.common.constants.ApiConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Phase 2 will resolve the authenticated user principal from the security context.
        return () -> Optional.of(ApiConstants.SYSTEM_AUDITOR);
    }
}
