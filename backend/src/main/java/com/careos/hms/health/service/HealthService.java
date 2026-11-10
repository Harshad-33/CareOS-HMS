package com.careos.hms.health.service;

import com.careos.hms.health.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

@Service
public class HealthService {

    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final String applicationName;
    private final String applicationVersion;

    public HealthService(
            DataSource dataSource,
            @Value("${spring.application.name:careos-hms-backend}") String applicationName,
            @Value("${app.version:0.1.0-SNAPSHOT}") String applicationVersion
    ) {
        this.dataSource = dataSource;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    public HealthResponse checkHealth() {
        boolean databaseUp = isDatabaseReachable();
        String overallStatus = databaseUp ? "UP" : "DOWN";
        String databaseStatus = databaseUp ? "UP" : "DOWN";

        return new HealthResponse(
                overallStatus,
                applicationName,
                applicationVersion,
                databaseStatus,
                Instant.now()
        );
    }

    private boolean isDatabaseReachable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DB_VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException ex) {
            return false;
        }
    }
}
