package com.careos.hms;

import com.careos.hms.common.constants.ApiConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareOsHmsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsAndHealthEndpointRespondsWithCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(ApiConstants.CORRELATION_ID_HEADER, "phase1-test-corr-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiConstants.CORRELATION_ID_HEADER, "phase1-test-corr-id"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void openApiDocsEndpointIsAvailableInDevOrTest() throws Exception {
        mockMvc.perform(get("/v3/api-docs/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("CareOS-HMS API"))
                .andExpect(jsonPath("$.paths['/api/v1/health']").exists());
    }
}
