package com.careos.hms.hospital;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.branch.dto.HospitalBranchRequest;
import com.careos.hms.branch.entity.BranchStatus;
import com.careos.hms.department.dto.DepartmentRequest;
import com.careos.hms.department.entity.DepartmentStatus;
import com.careos.hms.hospital.dto.HospitalContactRequest;
import com.careos.hms.hospital.dto.UpdateHospitalProfileRequest;
import com.careos.hms.hospital.entity.ContactType;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalType;
import com.careos.hms.security.LoginRateLimiterService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase3TenantIsolationIntegrationTest {

    private static final String TEST_PASSWORD = "Care0s!Strong#Password1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityTestDataHelper testDataHelper;

    @Autowired
    private LoginRateLimiterService rateLimiterService;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private String adminTokenA;
    private String adminTokenB;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiterService.resetAll();
        testDataHelper.resetSecurityTables();
        SecurityTestDataHelper.SeededRbac rbac = testDataHelper.seedRbacCatalog();

        hospitalA = testDataHelper.createHospital("HOSP-A", "Hospital Alpha");
        hospitalB = testDataHelper.createHospital("HOSP-B", "Hospital Beta");

        testDataHelper.createUser(
                hospitalA,
                UserScope.HOSPITAL,
                "admin.alpha",
                "admin.alpha@careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );
        testDataHelper.createUser(
                hospitalB,
                UserScope.HOSPITAL,
                "admin.beta",
                "admin.beta@careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );

        adminTokenA = loginAndExtractToken("admin.alpha");
        adminTokenB = loginAndExtractToken("admin.beta");
    }

    @Test
    void shouldPreventHospitalBFromMutatingHospitalADepartmentsBranchesContactsAndProfile() throws Exception {
        // 1. Hospital A creates a department
        MvcResult deptResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentRequest(
                                "ONCO", "Oncology", "Cancer Care", DepartmentStatus.ACTIVE
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String deptPublicIdA = objectMapper.readTree(deptResult.getResponse().getContentAsString())
                .at("/data/publicId")
                .asText();

        // 2. Hospital B attempts to update Hospital A's department -> 404
        mockMvc.perform(put("/api/v1/departments/{publicId}", deptPublicIdA)
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentRequest(
                                "ONCO", "Malicious Update", "Description", DepartmentStatus.ACTIVE
                        ))))
                .andExpect(status().isNotFound());

        // 3. Hospital B attempts to delete Hospital A's department -> 404
        mockMvc.perform(delete("/api/v1/departments/{publicId}", deptPublicIdA)
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // 4. Hospital A creates a branch
        MvcResult branchResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HospitalBranchRequest(
                                "EAST-WING", "East Wing", "+919876543210", "east@hospa.com",
                                "East Ave", "Pune", "Pune", "MH", "India", "411001", BranchStatus.ACTIVE
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String branchPublicIdA = objectMapper.readTree(branchResult.getResponse().getContentAsString())
                .at("/data/publicId")
                .asText();

        // 5. Hospital B attempts to update Hospital A's branch -> 404
        mockMvc.perform(put("/api/v1/branches/{publicId}", branchPublicIdA)
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HospitalBranchRequest(
                                "EAST-WING", "Hacked Branch", "+919876543210", "hacked@hospa.com",
                                "East Ave", "Pune", "Pune", "MH", "India", "411001", BranchStatus.ACTIVE
                        ))))
                .andExpect(status().isNotFound());

        // 6. Hospital A creates a contact
        MvcResult contactResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/hospitals/me/contacts")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HospitalContactRequest(
                                ContactType.EMERGENCY, "Dr. Emergency", "Head of ER", "+919876543000", "er@hospa.com", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String contactPublicIdA = objectMapper.readTree(contactResult.getResponse().getContentAsString())
                .at("/data/publicId")
                .asText();

        // 7. Hospital B attempts to delete Hospital A's contact -> 404
        mockMvc.perform(delete("/api/v1/hospitals/me/contacts/{contactPublicId}", contactPublicIdA)
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // 8. Hospital B attempts to update Hospital A's profile via publicId -> 403 Forbidden
        UpdateHospitalProfileRequest attackProfile = new UpdateHospitalProfileRequest(
                "Hospital A Defaced", HospitalType.GENERAL, "defaced@hospa.com", "+919876543210",
                null, null, "123 Main Rd", null, "Pune", "Pune", "Maharashtra", "India", "411001", null
        );
        mockMvc.perform(put("/api/v1/hospitals/{publicId}", hospitalA.getPublicId())
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attackProfile)))
                .andExpect(status().isForbidden());
    }

    private String loginAndExtractToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
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