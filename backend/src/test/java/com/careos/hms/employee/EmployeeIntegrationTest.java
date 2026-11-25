package com.careos.hms.employee;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.authentication.dto.LoginRequest;
import com.careos.hms.branch.dto.HospitalBranchRequest;
import com.careos.hms.branch.entity.BranchStatus;
import com.careos.hms.department.dto.DepartmentRequest;
import com.careos.hms.department.entity.DepartmentStatus;
import com.careos.hms.employee.dto.CreateEmployeeRequest;
import com.careos.hms.employee.dto.LinkUserAccountRequest;
import com.careos.hms.employee.dto.UpdateEmployeeRequest;
import com.careos.hms.employee.dto.UpdateEmployeeStatusRequest;
import com.careos.hms.employee.entity.EmployeeGender;
import com.careos.hms.employee.entity.EmployeeStatus;
import com.careos.hms.employee.entity.EmploymentType;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.security.LoginRateLimiterService;
import com.careos.hms.user.entity.UserScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeIntegrationTest {

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
    private String platformAdminToken;
    private String deptPublicIdA;
    private String branchPublicIdA;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiterService.resetAll();
        testDataHelper.resetSecurityTables();
        SecurityTestDataHelper.SeededRbac rbac = testDataHelper.seedRbacCatalog();

        hospitalA = testDataHelper.createHospital("HOSP-ALPHA", "Hospital Alpha");
        hospitalB = testDataHelper.createHospital("HOSP-BETA", "Hospital Beta");

        testDataHelper.createUser(
                null,
                UserScope.PLATFORM,
                "platform.superadmin",
                "superadmin@careos.local",
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
        testDataHelper.createUser(
                hospitalB,
                UserScope.HOSPITAL,
                "admin.beta",
                "admin.beta@careos.local",
                TEST_PASSWORD,
                rbac.hospitalAdminRole()
        );

        platformAdminToken = loginAndExtractToken("platform.superadmin");
        adminTokenA = loginAndExtractToken("admin.alpha");
        adminTokenB = loginAndExtractToken("admin.beta");

        // Create Department and Branch in Hospital A
        DepartmentRequest deptReq = new DepartmentRequest("CARDIO", "Cardiology", "Heart department", DepartmentStatus.ACTIVE);
        MvcResult deptRes = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isCreated())
                .andReturn();
        deptPublicIdA = objectMapper.readTree(deptRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        HospitalBranchRequest branchReq = new HospitalBranchRequest("MAIN", "Main Branch", "+1-555-123-4567", "branch@careos.local", "100 Main St", "Metropolis", "Metro", "NY", "USA", "10001", BranchStatus.ACTIVE);
        MvcResult branchRes = mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(branchReq)))
                .andExpect(status().isCreated())
                .andReturn();
        branchPublicIdA = objectMapper.readTree(branchRes.getResponse().getContentAsString()).path("data").path("publicId").asText();
    }

    @Test
    void shouldRegisterEmployeeWithAutoGeneratedCodeAndOptionalUserAccount() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Jane",
                "M",
                "Doe",
                LocalDate.of(1990, 5, 15),
                EmployeeGender.FEMALE,
                "O_POSITIVE",
                "jane.doe@alphahospital.com",
                "+1-555-019-2834",
                null,
                "456 Oak Avenue",
                "Apt 2B",
                "Metropolis",
                "Metro",
                "NY",
                "USA",
                "10001",
                "John Doe",
                "Spouse",
                "+1-555-019-9999",
                "Senior Cardiologist",
                deptPublicIdA,
                branchPublicIdA,
                EmploymentType.FULL_TIME,
                LocalDate.of(2026, 1, 1),
                null, // auto-generate code
                null,
                true, // create user account
                "dr.janedoe",
                "Care0s!Jane#Pass123",
                Set.of("HOSPITAL_ADMIN")
        );

        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.hasUserAccount").value(true))
                .andExpect(jsonPath("$.data.userAccount.username").value("dr.janedoe"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String empPublicId = json.path("data").path("publicId").asText();
        String generatedCode = json.path("data").path("employeeNumber").asText();
        assertThat(generatedCode).matches("^EMP-\\d{4}-\\d{4}$");

        // Verify newly created user account can authenticate
        String doctorToken = loginAndExtractToken("dr.janedoe", "Care0s!Jane#Pass123");
        assertThat(doctorToken).isNotBlank();

        // Check Lifecycle History
        mockMvc.perform(get("/api/v1/employees/" + empPublicId + "/lifecycle-history")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].newStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].reason").value("Initial employee registration"));
    }

    @Test
    void shouldEnforceUniqueEmailAndEmployeeNumberPerHospitalAndCrossTenantIsolation() throws Exception {
        CreateEmployeeRequest request1 = new CreateEmployeeRequest(
                "Robert",
                null,
                "Smith",
                LocalDate.of(1985, 3, 20),
                EmployeeGender.MALE,
                "A_POSITIVE",
                "robert.smith@careos.local",
                "+1-555-444-1111",
                null,
                null, null, null, null, null, null, null,
                null, null, null,
                "General Physician",
                deptPublicIdA,
                null,
                EmploymentType.FULL_TIME,
                LocalDate.of(2025, 6, 1),
                "EMP-FIXED-001",
                null,
                false,
                null, null, null
        );

        // Hospital A registers Robert
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Duplicate employee number in Hospital A -> Conflict
        CreateEmployeeRequest dupNumberReq = new CreateEmployeeRequest(
                "Another", null, "Person", LocalDate.of(1992, 1, 1),
                EmployeeGender.MALE, null, "another@careos.local", "+1-555-444-2222",
                null, null, null, null, null, null, null, null, null, null, null,
                "Resident", deptPublicIdA, null, EmploymentType.FULL_TIME,
                LocalDate.of(2026, 1, 1), "EMP-FIXED-001", null, false, null, null, null
        );
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupNumberReq)))
                .andExpect(status().isConflict());

        // Duplicate email in Hospital A -> Conflict
        CreateEmployeeRequest dupEmailReq = new CreateEmployeeRequest(
                "Another", null, "Person", LocalDate.of(1992, 1, 1),
                EmployeeGender.MALE, null, "robert.smith@careos.local", "+1-555-444-2222",
                null, null, null, null, null, null, null, null, null, null, null,
                "Resident", deptPublicIdA, null, EmploymentType.FULL_TIME,
                LocalDate.of(2026, 1, 1), "EMP-FIXED-002", null, false, null, null, null
        );
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupEmailReq)))
                .andExpect(status().isConflict());

        // Hospital B creates its own department
        DepartmentRequest deptBReq = new DepartmentRequest("SURG", "Surgery", "Surgical suite", DepartmentStatus.ACTIVE);
        MvcResult deptBRes = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptBReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String deptPublicIdB = objectMapper.readTree(deptBRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Hospital B can register an employee with same number "EMP-FIXED-001" (multi-tenant isolation)
        CreateEmployeeRequest hospBReq = new CreateEmployeeRequest(
                "HospitalB", null, "Staff", LocalDate.of(1988, 8, 8),
                EmployeeGender.FEMALE, null, "staff.b@careos.local", "+1-555-999-0000",
                null, null, null, null, null, null, null, null, null, null, null,
                "Surgeon", deptPublicIdB, null, EmploymentType.FULL_TIME,
                LocalDate.of(2026, 2, 1), "EMP-FIXED-001", null, false, null, null, null
        );
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospBReq)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldExecuteStatusTransitionsAndRevokeTokensOnSuspension() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Alice",
                null,
                "Walker",
                LocalDate.of(1991, 7, 10),
                EmployeeGender.FEMALE,
                "B_POSITIVE",
                "alice.walker@careos.local",
                "+1-555-777-8888",
                null,
                null, null, null, null, null, null, null,
                null, null, null,
                "Charge Nurse",
                deptPublicIdA,
                null,
                EmploymentType.FULL_TIME,
                LocalDate.of(2025, 4, 1),
                null,
                null,
                true,
                "alice.walker",
                "Care0s!Alice#Pass456",
                Set.of("HOSPITAL_ADMIN")
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String empPublicId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Login as Alice successfully
        loginAndExtractToken("alice.walker", "Care0s!Alice#Pass456");

        // 1. Suspend employee
        UpdateEmployeeStatusRequest suspendReq = new UpdateEmployeeStatusRequest(
                EmployeeStatus.SUSPENDED,
                "Internal investigation pending",
                LocalDate.of(2026, 6, 1)
        );
        mockMvc.perform(patch("/api/v1/employees/" + empPublicId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.userAccount.status").value("SUSPENDED"));

        // Suspended user cannot login
        LoginRequest loginAttempt = new LoginRequest("alice.walker", "Care0s!Alice#Pass456", false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isUnauthorized());

        // 2. Reinstate to ACTIVE
        UpdateEmployeeStatusRequest reinstateReq = new UpdateEmployeeStatusRequest(
                EmployeeStatus.ACTIVE,
                "Investigation cleared",
                LocalDate.of(2026, 6, 15)
        );
        mockMvc.perform(patch("/api/v1/employees/" + empPublicId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reinstateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.userAccount.status").value("ACTIVE"));

        // Now Alice can log in again
        loginAndExtractToken("alice.walker", "Care0s!Alice#Pass456");

        // 3. Resign employee
        UpdateEmployeeStatusRequest resignReq = new UpdateEmployeeStatusRequest(
                EmployeeStatus.RESIGNED,
                "Relocating to another city",
                LocalDate.of(2026, 11, 30)
        );
        mockMvc.perform(patch("/api/v1/employees/" + empPublicId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESIGNED"))
                .andExpect(jsonPath("$.data.leavingDate").value("2026-11-30"))
                .andExpect(jsonPath("$.data.userAccount.status").value("INACTIVE"));

        // 4. Invalid transition RESIGNED -> SUSPENDED must be rejected
        UpdateEmployeeStatusRequest invalidReq = new UpdateEmployeeStatusRequest(
                EmployeeStatus.SUSPENDED,
                "Cannot suspend resigned staff",
                LocalDate.now()
        );
        mockMvc.perform(patch("/api/v1/employees/" + empPublicId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateEmployeeProfileAndManageUserAccountLinking() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Mark",
                null,
                "Ruffalo",
                LocalDate.of(1982, 11, 22),
                EmployeeGender.MALE,
                "AB_POSITIVE",
                "mark.ruffalo@careos.local",
                "+1-555-888-9999",
                null,
                "12 Gamma St",
                null,
                "Metropolis",
                null,
                "NY",
                "USA",
                "10001",
                null, null, null,
                "Biochemist",
                deptPublicIdA,
                null,
                EmploymentType.CONTRACT,
                LocalDate.of(2026, 3, 1),
                null,
                null,
                false, // no user account yet
                null, null, null
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.hasUserAccount").value(false))
                .andReturn();
        String empPublicId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Update employee details
        UpdateEmployeeRequest updateReq = new UpdateEmployeeRequest(
                "Mark",
                "Alan",
                "Ruffalo",
                LocalDate.of(1982, 11, 22),
                EmployeeGender.MALE,
                "AB_POSITIVE",
                "mark.alan.ruffalo@careos.local",
                "+1-555-888-0000",
                null,
                "14 Gamma St",
                "Suite 100",
                "Metropolis",
                null,
                "NY",
                "USA",
                "10001",
                "Laura Ruffalo",
                "Spouse",
                "+1-555-888-1111",
                "Lead Biochemist",
                deptPublicIdA,
                branchPublicIdA,
                EmploymentType.FULL_TIME,
                LocalDate.of(2026, 3, 1),
                null,
                null
        );

        mockMvc.perform(put("/api/v1/employees/" + empPublicId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.designation").value("Lead Biochemist"))
                .andExpect(jsonPath("$.data.email").value("mark.alan.ruffalo@careos.local"))
                .andExpect(jsonPath("$.data.middleName").value("Alan"));

        // Link a new user account
        LinkUserAccountRequest linkReq = new LinkUserAccountRequest(
                null,
                "mark.ruffalo",
                "Care0s!Mark#Secret99",
                Set.of("HOSPITAL_ADMIN")
        );
        mockMvc.perform(post("/api/v1/employees/" + empPublicId + "/user-account")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasUserAccount").value(true))
                .andExpect(jsonPath("$.data.userAccount.username").value("mark.ruffalo"));

        // Unlink user account
        mockMvc.perform(delete("/api/v1/employees/" + empPublicId + "/user-account")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasUserAccount").value(false));
    }

    @Test
    void shouldUploadAndDownloadProfilePhoto() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Diana",
                null,
                "Prince",
                LocalDate.of(1989, 4, 12),
                EmployeeGender.FEMALE,
                "O_POSITIVE",
                "diana.prince@careos.local",
                "+1-555-222-3333",
                null,
                null, null, null, null, null, null, null,
                null, null, null,
                "Surgeon",
                deptPublicIdA,
                null,
                EmploymentType.FULL_TIME,
                LocalDate.of(2025, 9, 1),
                null, null, false, null, null, null
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String empPublicId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Upload photo
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "dummy-image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/employees/" + empPublicId + "/photo")
                        .file(file)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profilePhotoKey").isNotEmpty());

        // Download photo
        mockMvc.perform(get("/api/v1/employees/" + empPublicId + "/photo")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(content().bytes("dummy-image-content".getBytes()));
    }

    @Test
    void shouldAllowPlatformAdminToViewAnyHospitalEmployees() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Barry",
                null,
                "Allen",
                LocalDate.of(1993, 10, 5),
                EmployeeGender.MALE,
                "A_POSITIVE",
                "barry.allen@careos.local",
                "+1-555-111-2222",
                null,
                null, null, null, null, null, null, null,
                null, null, null,
                "Lab Technician",
                deptPublicIdA,
                null,
                EmploymentType.FULL_TIME,
                LocalDate.of(2026, 1, 10),
                null, null, false, null, null, null
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String empPublicId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Hospital B cannot view Hospital A employee (Cross-tenant forbidden)
        mockMvc.perform(get("/api/v1/employees/" + empPublicId)
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // Platform Admin can view Hospital A employees
        mockMvc.perform(get("/api/v1/hospitals/" + hospitalA.getPublicId() + "/employees")
                        .header("Authorization", "Bearer " + platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].firstName").value("Barry"));

        // Platform Admin can view single employee profile
        mockMvc.perform(get("/api/v1/hospitals/" + hospitalA.getPublicId() + "/employees/" + empPublicId)
                        .header("Authorization", "Bearer " + platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Barry"));
    }

    private String loginAndExtractToken(String username) throws Exception {
        return loginAndExtractToken(username, TEST_PASSWORD);
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password, false);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }
}
