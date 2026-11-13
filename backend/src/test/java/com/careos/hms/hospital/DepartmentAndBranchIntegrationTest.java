package com.careos.hms.hospital;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.branch.dto.HospitalBranchRequest;
import com.careos.hms.branch.entity.BranchStatus;
import com.careos.hms.department.dto.DepartmentRequest;
import com.careos.hms.department.entity.DepartmentStatus;
import com.careos.hms.hospital.entity.Hospital;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentAndBranchIntegrationTest {

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
    void shouldPerformDepartmentCrudAndEnforceTenantScopedCodeUniqueness() throws Exception {
        DepartmentRequest req = new DepartmentRequest(
                "CARDIO",
                "Cardiology",
                "Cardiology and Cardiovascular Surgery",
                DepartmentStatus.ACTIVE
        );

        // 1. Hospital A creates CARDIO
        MvcResult createResult = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.departmentCode").value("CARDIO"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        String deptPublicId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/publicId")
                .asText();

        // 2. Duplicate code in same Hospital A is rejected
        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        // 3. Same code in Hospital B is allowed (tenant-scoped uniqueness)
        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.departmentCode").value("CARDIO"));

        // 4. Update department in Hospital A
        DepartmentRequest updateReq = new DepartmentRequest(
                "CARDIO",
                "Department of Cardiology",
                "Updated description",
                DepartmentStatus.ACTIVE
        );
        mockMvc.perform(put("/api/v1/departments/{publicId}", deptPublicId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentName").value("Department of Cardiology"));

        // 5. Toggle status to INACTIVE
        mockMvc.perform(patch("/api/v1/departments/{publicId}/status", deptPublicId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // 6. Delete department
        mockMvc.perform(delete("/api/v1/departments/{publicId}", deptPublicId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk());

        // 7. Verify listing in Hospital A shows 0 active departments
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldPerformBranchCrudAndEnforceTenantScopedCodeUniqueness() throws Exception {
        HospitalBranchRequest req = new HospitalBranchRequest(
                "MAIN-CAMPUS",
                "Main City Campus",
                "+919876543301",
                "main@alphahospital.com",
                "10 Health City",
                "Mumbai",
                "Mumbai",
                "Maharashtra",
                "India",
                "400001",
                BranchStatus.ACTIVE
        );

        // 1. Hospital A creates branch
        MvcResult createResult = mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.branchCode").value("MAIN-CAMPUS"))
                .andReturn();

        String branchPublicId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/publicId")
                .asText();

        // 2. Duplicate branch code in same Hospital A is rejected
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        // 3. Same branch code in Hospital B is allowed
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // 4. Update branch
        HospitalBranchRequest updateReq = new HospitalBranchRequest(
                "MAIN-CAMPUS",
                "Updated Main Campus",
                "+919876543399",
                "updated@alphahospital.com",
                "10 Health City",
                "Mumbai",
                "Mumbai",
                "Maharashtra",
                "India",
                "400001",
                BranchStatus.ACTIVE
        );
        mockMvc.perform(put("/api/v1/branches/{publicId}", branchPublicId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branchName").value("Updated Main Campus"));

        // 5. Toggle branch status
        mockMvc.perform(patch("/api/v1/branches/{publicId}/status", branchPublicId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // 6. Delete branch
        mockMvc.perform(delete("/api/v1/branches/{publicId}", branchPublicId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk());

        // 7. Verify listing
        mockMvc.perform(get("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
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