package com.careos.hms.security;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.permission.entity.PermissionCode;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantAuthorizationIntegrationTest {

    private static final String TEST_PASSWORD = "Care0s!Strong#Password1";

    @TestConfiguration
    static class TestSecurityControllersConfig {

        @Bean
        TenantTestSecurityController tenantTestSecurityController(TenantAccessGuard tenantAccessGuard) {
            return new TenantTestSecurityController(tenantAccessGuard);
        }
    }

    @RestController
    @RequestMapping("/api/v1/test-security")
    static class TenantTestSecurityController {

        private final TenantAccessGuard tenantAccessGuard;

        TenantTestSecurityController(TenantAccessGuard tenantAccessGuard) {
            this.tenantAccessGuard = tenantAccessGuard;
        }

        @GetMapping("/platform/hospitals")
        @PreAuthorize("hasAuthority('" + PermissionCode.PLATFORM_HOSPITALS_MANAGE + "') and @tenantGuard.canAccessPlatform()")
        public ApiResponse<String> platformOnly() {
            return ApiResponse.ok("platform-ok", "Platform access granted", "/api/v1/test-security/platform/hospitals");
        }

        @GetMapping("/hospitals/{hospitalId}/workspace")
        @PreAuthorize("hasAuthority('" + PermissionCode.HOSPITAL_WORKSPACE_READ + "') and @tenantGuard.canAccessHospital(#hospitalId)")
        public ApiResponse<Long> hospitalWorkspace(@PathVariable Long hospitalId) {
            Long resolvedHospitalId = tenantAccessGuard.requireHospitalId();
            return ApiResponse.ok(resolvedHospitalId, "Hospital access granted", "/api/v1/test-security/hospitals/" + hospitalId + "/workspace");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityTestDataHelper testDataHelper;

    @Autowired
    private LoginRateLimiterService rateLimiterService;

    @Autowired
    private UserRepository userRepository;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private User userHospitalB;
    private String superAdminToken;
    private String hospitalAAdminToken;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiterService.resetAll();
        testDataHelper.resetSecurityTables();
        SecurityTestDataHelper.SeededRbac rbac = testDataHelper.seedRbacCatalog();

        hospitalA = testDataHelper.createHospital("HOSP-A", "Hospital Alpha");
        hospitalB = testDataHelper.createHospital("HOSP-B", "Hospital Beta");

        testDataHelper.createUser(
                null,
                UserScope.PLATFORM,
                "platform.super",
                "platform.super@careos.local",
                TEST_PASSWORD,
                rbac.platformSuperAdminRole()
        );
        testDataHelper.createUser(
                hospitalA,
                UserScope.HOSPITAL,
                "admin.alpha",
                "admin.alpha@careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );
        userHospitalB = testDataHelper.createUser(
                hospitalB,
                UserScope.HOSPITAL,
                "admin.beta",
                "admin.beta@careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );

        superAdminToken = loginAndExtractToken("platform.super");
        hospitalAAdminToken = loginAndExtractToken("admin.alpha");
    }

    @Test
    void shouldIsolatePlatformVsHospitalAndEnforceHospitalTenantBoundaries() throws Exception {
        // Platform super admin allowed on platform-only endpoint
        mockMvc.perform(get("/api/v1/test-security/platform/hospitals")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("platform-ok"));

        // Hospital admin blocked on platform-only endpoint
        mockMvc.perform(get("/api/v1/test-security/platform/hospitals")
                        .header("Authorization", "Bearer " + hospitalAAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Hospital A admin allowed on Hospital A workspace
        mockMvc.perform(get("/api/v1/test-security/hospitals/{hospitalId}/workspace", hospitalA.getId())
                        .header("Authorization", "Bearer " + hospitalAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(hospitalA.getId()));

        // Hospital A admin blocked on Hospital B workspace even with forged client header
        mockMvc.perform(get("/api/v1/test-security/hospitals/{hospitalId}/workspace", hospitalB.getId())
                        .header("Authorization", "Bearer " + hospitalAAdminToken)
                        .header("X-Hospital-Id", hospitalB.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Platform super admin lacks hospital tenant binding and HOSPITAL_WORKSPACE_READ
        mockMvc.perform(get("/api/v1/test-security/hospitals/{hospitalId}/workspace", hospitalA.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Repository tenant scoping prevents cross-tenant lookup
        assertThat(userRepository.findByIdAndHospitalIdAndDeletedFalse(userHospitalB.getId(), hospitalA.getId())).isEmpty();
        assertThat(userRepository.findByIdAndHospitalIdAndDeletedFalse(userHospitalB.getId(), hospitalB.getId())).isPresent();
    }

    private String loginAndExtractToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
    }
}
