package com.careos.hms.health.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        String version,
        String database,
        Instant timestamp
) {
}
