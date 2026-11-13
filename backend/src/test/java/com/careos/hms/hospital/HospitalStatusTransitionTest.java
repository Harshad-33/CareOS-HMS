package com.careos.hms.hospital;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.hospital.dto.UpdateHospitalStatusRequest;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.security.LoginRateLimiterService;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalStatusTransitionTest {

    private static final String TEST_PASSWORD = "Care0s!Strong#Password1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityTestDataHelper testDataHelper;

    @Autowired
    private LoginRateLimiterService rateLimiterService;

    @Autowired
    private HospitalRepository hospitalRepository;

    private Hospital hospital;
    private User hospitalAdmin;
    private String platformSuperAdminToken;
    private String hospitalAdminToken;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiterService.resetAll();
        testDataHelper.resetSecurityTables();
        SecurityTestDataHelper.SeededRbac rbac = testDataHelper.seedRbacCatalog();

        testDataHelper.createUser(
                null,
                UserScope.PLATFORM,
                "platform.super",
                "platform.super@careos.local",
                TEST_PASSWORD,
                rbac.platformSuperAdminRole()
        );

        hospital = testDataHelper.createHospital("HOSP-STATUS", "Status Hospital");
        hospitalAdmin = testDataHelper.createUser(
                hospital,
                UserScope.HOSPITAL,
                "admin.status",
                "admin.status@careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );

        platformSuperAdminToken = loginAndExtractToken("platform.super", TEST_PASSWORD);
        hospitalAdminToken = loginAndExtractToken("admin.status", TEST_PASSWORD);
    }

    @Test
    void shouldHandleValidStatusTransitionsAndBlockLoginOnSuspension() throws Exception {
        // 1. Suspend hospital
        UpdateHospitalStatusRequest suspendReq = new UpdateHospitalStatusRequest(
                HospitalStatus.SUSPENDED,
                "Regulatory non-compliance audit"
        );

        mockMvc.perform(patch("/api/v1/hospitals/{publicId}/status", hospital.getPublicId())
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // Verify DB entity updated
        Hospital updated = hospitalRepository.findByPublicIdAndDeletedFalse(hospital.getPublicId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(HospitalStatus.SUSPENDED);

        // 2. Hospital admin login must now be blocked
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.status",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        // 3. Reactivate hospital
        UpdateHospitalStatusRequest reactivateReq = new UpdateHospitalStatusRequest(
                HospitalStatus.ACTIVE,
                "Compliance restored"
        );

        mockMvc.perform(patch("/api/v1/hospitals/{publicId}/status", hospital.getPublicId())
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 4. Hospital admin login succeeds again
        String freshToken = loginAndExtractToken("admin.status", TEST_PASSWORD);
        assertThat(freshToken).isNotBlank();
    }

    @Test
    void shouldRejectInvalidStatusTransition() throws Exception {
        // First suspend
        mockMvc.perform(patch("/api/v1/hospitals/{publicId}/status", hospital.getPublicId())
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateHospitalStatusRequest(HospitalStatus.SUSPENDED, "test"))))
                .andExpect(status().isOk());

        // Attempt illegal transition from SUSPENDED -> PENDING
        UpdateHospitalStatusRequest invalidReq = new UpdateHospitalStatusRequest(
                HospitalStatus.PENDING,
                "Illegal rollback"
        );

        mockMvc.perform(patch("/api/v1/hospitals/{publicId}/status", hospital.getPublicId())
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldPreventHospitalAdminFromUpdatingStatus() throws Exception {
        UpdateHospitalStatusRequest req = new UpdateHospitalStatusRequest(
                HospitalStatus.SUSPENDED,
                "Self suspension attempt"
        );

        mockMvc.perform(patch("/api/v1/hospitals/{publicId}/status", hospital.getPublicId())
                        .header("Authorization", "Bearer " + hospitalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
    }
}