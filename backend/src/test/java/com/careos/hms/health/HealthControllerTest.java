package com.careos.hms.health;

import com.careos.hms.health.controller.HealthController;
import com.careos.hms.health.dto.HealthResponse;
import com.careos.hms.health.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    void returnsOkWhenDatabaseIsUp() throws Exception {
        HealthResponse upResponse = new HealthResponse(
                "UP",
                "careos-hms-backend",
                "0.1.0-SNAPSHOT",
                "UP",
                Instant.parse("2026-11-10T09:00:00Z")
        );
        when(healthService.checkHealth()).thenReturn(upResponse);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("careos-hms-backend"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void returnsServiceUnavailableWhenDatabaseIsDown() throws Exception {
        HealthResponse downResponse = new HealthResponse(
                "DOWN",
                "careos-hms-backend",
                "0.1.0-SNAPSHOT",
                "DOWN",
                Instant.parse("2026-11-10T09:00:00Z")
        );
        when(healthService.checkHealth()).thenReturn(downResponse);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("DOWN"));
    }
}
