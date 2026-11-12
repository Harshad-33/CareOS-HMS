package com.careos.hms.authentication.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "careos.security.bootstrap.enabled", havingValue = "true")
public class SuperAdminBootstrapRunner implements ApplicationRunner {

    private final SuperAdminBootstrapService bootstrapService;

    public SuperAdminBootstrapRunner(SuperAdminBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.bootstrapInitialSuperAdminIfConfigured();
    }
}
