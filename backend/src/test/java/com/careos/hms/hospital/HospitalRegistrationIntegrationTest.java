package com.careos.hms.hospital;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.hospital.dto.HospitalRegistrationRequest;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalSettings;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.hospital.entity.HospitalType;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.hospital.repository.HospitalSettingsRepository;
import com.careos.hms.security.LoginRateLimiterService;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalRegistrationIntegrationTest {

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

    @Autowired
    private HospitalSettingsRepository settingsRepository;

    @Autowired
    private UserRepository userRepository;

    private String platformSuperAdminToken;

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

        platformSuperAdminToken = loginAndExtractToken("platform.super", TEST_PASSWORD);
    }

    @Test
    void shouldRegisterNewHospitalTenantAtomically() throws Exception {
        HospitalRegistrationRequest req = new HospitalRegistrationRequest(
                "City Care Hospital",
                "CITY-CARE",
                "REG-MH-2026-001",
                HospitalType.MULTI_SPECIALTY,
                "contact@citycare.com",
                "+919876543210",
                "+919876543211",
                "https://citycare.example.com",
                "123 Healthcare Blvd",
                "Opposite Central Park",
                "Pune",
                "Pune",
                "Maharashtra",
                "India",
                "411001",
                LocalDate.of(2015, 6, 15),
                "cityadmin",
                "admin@citycare.com",
                "Admin!Secret#2026",
                "Rajesh",
                "Sharma"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/hospitals")
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("City Care Hospital"))
                .andExpect(jsonPath("$.data.hospitalCode").value("CITY-CARE"))
                .andExpect(jsonPath("$.data.registrationNumber").value("REG-MH-2026-001"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        String hospitalPublicId = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/publicId")
                .asText();

        // 1. Verify Hospital persistence
        Optional<Hospital> hospitalOpt = hospitalRepository.findByPublicIdAndDeletedFalse(hospitalPublicId);
        assertThat(hospitalOpt).isPresent();
        Hospital hospital = hospitalOpt.get();
        assertThat(hospital.getName()).isEqualTo("City Care Hospital");

        // 2. Verify default settings created
        Optional<HospitalSettings> settingsOpt = settingsRepository.findByHospitalId(hospital.getId());
        assertThat(settingsOpt).isPresent();
        assertThat(settingsOpt.get().getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(settingsOpt.get().getCurrency()).isEqualTo("INR");

        // 3. Verify admin user created and can log in immediately
        String adminToken = loginAndExtractToken("cityadmin", "Admin!Secret#2026");
        assertThat(adminToken).isNotBlank();

        // 4. Verify admin user profile via /api/v1/auth/me has hospital association
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hospitalId").value(hospital.getId()))
                .andExpect(jsonPath("$.data.hospitalCode").value("CITY-CARE"))
                .andExpect(jsonPath("$.data.roles[0]").value("HOSPITAL_ADMIN"));
    }

    @Test
    void shouldAutoGenerateHospitalCodeWhenNotProvided() throws Exception {
        HospitalRegistrationRequest req = new HospitalRegistrationRequest(
                "Apollo Diagnostic Center",
                null, // code not supplied
                "REG-MH-2026-002",
                HospitalType.DIAGNOSTIC_CENTER,
                "info@apollo-diag.com",
                "+919876543220",
                null,
                null,
                "45 Station Road",
                null,
                "Mumbai",
                "Mumbai Suburban",
                "Maharashtra",
                "India",
                "400001",
                null,
                "apolloadmin",
                "admin@apollo-diag.com",
                "Apollo!Secure#2026",
                "Suresh",
                "Patil"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/hospitals")
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String generatedCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/hospitalCode")
                .asText();

        assertThat(generatedCode).isNotBlank();
        assertThat(generatedCode).startsWith("APOL-");
    }

    @Test
    void shouldRejectDuplicateRegistrationNumber() throws Exception {
        HospitalRegistrationRequest req1 = new HospitalRegistrationRequest(
                "Hospital One",
                "HOSP-001",
                "REG-DUPLICATE-999",
                HospitalType.GENERAL,
                "hosp1@careos.local",
                "+919876543201",
                null, null, "Street 1", null, "Delhi", "Delhi", "Delhi", "India", "110001", null,
                "admin1", "admin1@careos.local", "Admin!Secret#1", "Admin", "One"
        );

        mockMvc.perform(post("/api/v1/hospitals")
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second registration with same registrationNumber
        HospitalRegistrationRequest req2 = new HospitalRegistrationRequest(
                "Hospital Two",
                "HOSP-002",
                "REG-DUPLICATE-999",
                HospitalType.GENERAL,
                "hosp2@careos.local",
                "+919876543202",
                null, null, "Street 2", null, "Delhi", "Delhi", "Delhi", "India", "110002", null,
                "admin2", "admin2@careos.local", "Admin!Secret#2", "Admin", "Two"
        );

        mockMvc.perform(post("/api/v1/hospitals")
                        .header("Authorization", "Bearer " + platformSuperAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
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