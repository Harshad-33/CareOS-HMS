package com.careos.hms.authentication;

import com.careos.hms.authentication.entity.RefreshToken;
import com.careos.hms.authentication.entity.TokenRevocationReason;
import com.careos.hms.authentication.repository.RefreshTokenRepository;
import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.permission.entity.PermissionCode;
import com.careos.hms.security.CareOsSecurityProperties;
import com.careos.hms.security.LoginRateLimiterService;
import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    private static final String TEST_PASSWORD = "Care0s!Strong#Password1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityTestDataHelper testDataHelper;

    @Autowired
    private CareOsSecurityProperties securityProperties;

    @Autowired
    private LoginRateLimiterService rateLimiterService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private SecurityTestDataHelper.SeededRbac rbac;
    private Hospital activeHospital;
    private User hospitalAdmin;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        rateLimiterService.resetAll();
        testDataHelper.resetSecurityTables();
        rbac = testDataHelper.seedRbacCatalog();
        activeHospital = testDataHelper.createHospital("CGH-01", "CareOS General Hospital");
        hospitalAdmin = testDataHelper.createUser(
                activeHospital,
                UserScope.HOSPITAL,
                "admin.cgh",
                "admin@cgh.careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );
        inactiveUser = testDataHelper.createUser(
                activeHospital,
                UserScope.HOSPITAL,
                "disabled.cgh",
                "disabled@cgh.careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );
        inactiveUser.setStatus(AccountStatus.INACTIVE);
        userRepository.saveAndFlush(inactiveUser);
    }

    @Test
    void shouldAllowHealthPubliclyAndRejectUnauthenticatedAuthEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldLoginWithUsernameOrEmailSetHttpOnlyCookiesAndLoadCurrentUser() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", TEST_PASSWORD,
                                "rememberMe", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.csrfToken").isString())
                .andExpect(jsonPath("$.data.user.username").value("admin.cgh"))
                .andExpect(jsonPath("$.data.user.userScope").value("HOSPITAL"))
                .andExpect(jsonPath("$.data.user.hospitalId").value(activeHospital.getId()))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie(securityProperties.getCookie().getRefreshTokenName());
        Cookie csrfCookie = loginResult.getResponse().getCookie(securityProperties.getCookie().getCsrfTokenName());
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isTrue();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = root.at("/data/accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin.cgh"))
                .andExpect(jsonPath("$.data.roles[0]").value("HOSPITAL_ADMIN"));

        mockMvc.perform(get("/api/v1/auth/permissions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions", org.hamcrest.Matchers.hasItem(PermissionCode.HOSPITAL_WORKSPACE_READ)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin@cgh.careos.local",
                                "password", TEST_PASSWORD,
                                "rememberMe", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    void shouldRejectInvalidCredentialsInactiveUserAndSuspendedHospital() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "unknown.user",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", "WrongPassword123!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "disabled.cgh",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        activeHospital.setStatus(HospitalStatus.SUSPENDED);
        hospitalRepository.saveAndFlush(activeHospital);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldLockAccountAfterMaxFailedAttempts() throws Exception {
        int maxAttempts = securityProperties.getLogin().getMaxFailedAttempts();
        for (int i = 1; i < maxAttempts; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "usernameOrEmail", "admin.cgh",
                                    "password", "WrongAttempt!" + i
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", "WrongAttemptFinal!"
                        ))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void shouldEnforceCsrfRotateRefreshTokensAndDetectReuse() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie initialRefreshCookie = loginResult.getResponse().getCookie(securityProperties.getCookie().getRefreshTokenName());
        Cookie initialCsrfCookie = loginResult.getResponse().getCookie(securityProperties.getCookie().getCsrfTokenName());
        String initialCsrfValue = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/csrfToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:4200")
                        .cookie(initialRefreshCookie, initialCsrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "https://evil.example.com")
                        .header(ApiConstants.CSRF_TOKEN_HEADER, initialCsrfValue)
                        .cookie(initialRefreshCookie, initialCsrfCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Referer", "https://evil.example.com/attack")
                        .header(ApiConstants.CSRF_TOKEN_HEADER, initialCsrfValue)
                        .cookie(initialRefreshCookie, initialCsrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_ORIGIN_DENIED"));

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:4200")
                        .header(ApiConstants.CSRF_TOKEN_HEADER, initialCsrfValue)
                        .cookie(initialRefreshCookie, initialCsrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        Cookie rotatedRefreshCookie = refreshResult.getResponse().getCookie(securityProperties.getCookie().getRefreshTokenName());
        Cookie rotatedCsrfCookie = refreshResult.getResponse().getCookie(securityProperties.getCookie().getCsrfTokenName());
        String rotatedCsrfValue = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .at("/data/csrfToken").asText();

        assertThat(rotatedRefreshCookie).isNotNull();
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(initialRefreshCookie.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:4200")
                        .header(ApiConstants.CSRF_TOKEN_HEADER, rotatedCsrfValue)
                        .cookie(initialRefreshCookie, rotatedCsrfCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSE_DETECTED"));

        List<RefreshToken> familyTokens = refreshTokenRepository.findAll();
        assertThat(familyTokens).hasSize(2);
        assertThat(familyTokens).allMatch(RefreshToken::isRevoked);
        assertThat(familyTokens)
                .extracting(RefreshToken::getRevokedReason)
                .contains(TokenRevocationReason.REUSE_DETECTED);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:4200")
                        .header(ApiConstants.CSRF_TOKEN_HEADER, rotatedCsrfValue)
                        .cookie(rotatedRefreshCookie, rotatedCsrfCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSE_DETECTED"));
    }

    @Test
    void shouldLogoutAndLogoutAllAndInvalidateOldJwtOnChangePassword() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
        String csrfToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/csrfToken").asText();
        Cookie refreshCookie = loginResult.getResponse().getCookie(securityProperties.getCookie().getRefreshTokenName());
        Cookie csrfCookie = loginResult.getResponse().getCookie(securityProperties.getCookie().getCsrfTokenName());

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongCurr3nt!1",
                                "newPassword", "NewStr0ng!Pass#22",
                                "confirmPassword", "NewStr0ng!Pass#22"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", TEST_PASSWORD,
                                "newPassword", "NewStr0ng!Pass#22",
                                "confirmPassword", "NewStr0ng!Pass#22"
                        ))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        // Verify previous JWT access token is immediately rejected after password change
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REVOKED"));

        List<RefreshToken> tokensAfterPwdChange = refreshTokenRepository.findAll();
        assertThat(tokensAfterPwdChange).isNotEmpty();
        assertThat(tokensAfterPwdChange).allMatch(RefreshToken::isRevoked);
        assertThat(tokensAfterPwdChange)
                .extracting(RefreshToken::getRevokedReason)
                .containsOnly(TokenRevocationReason.PASSWORD_CHANGED);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        MvcResult newLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin.cgh",
                                "password", "NewStr0ng!Pass#22"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie newRefreshCookie = newLogin.getResponse().getCookie(securityProperties.getCookie().getRefreshTokenName());
        Cookie newCsrfCookie = newLogin.getResponse().getCookie(securityProperties.getCookie().getCsrfTokenName());
        String newCsrf = objectMapper.readTree(newLogin.getResponse().getContentAsString()).at("/data/csrfToken").asText();
        String newAccessToken = objectMapper.readTree(newLogin.getResponse().getContentAsString()).at("/data/accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());

        assertThat(refreshTokenRepository.findAll().stream()
                .filter(t -> t.getRevokedReason() == TokenRevocationReason.LOGOUT_ALL)
                .count()).isEqualTo(1);

        // Regular cookie logout still succeeds idempotently
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", "http://localhost:4200")
                        .header(ApiConstants.CSRF_TOKEN_HEADER, newCsrf)
                        .cookie(newRefreshCookie, newCsrfCookie))
                .andExpect(status().isOk());
    }
}
