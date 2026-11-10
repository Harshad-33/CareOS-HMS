package com.careos.hms.health.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.health.dto.HealthResponse;
import com.careos.hms.health.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/health")
@Tag(name = "Health", description = "System and database readiness endpoints")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    @Operation(
            summary = "Get service health status",
            description = "Returns backend application status and MySQL database connectivity status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service and database are healthy",
                    content = @Content(schema = @Schema(implementation = HealthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database connectivity check failed",
                    content = @Content(schema = @Schema(implementation = HealthResponse.class))
            )
    })
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse response = healthService.checkHealth();
        HttpStatus status = "UP".equals(response.status())
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }
}
