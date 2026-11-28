package com.careos.hms.hr.leave;

import com.careos.hms.authentication.SecurityTestDataHelper;
import com.careos.hms.authentication.dto.LoginRequest;
import com.careos.hms.branch.dto.HospitalBranchRequest;
import com.careos.hms.branch.entity.BranchStatus;
import com.careos.hms.department.dto.DepartmentRequest;
import com.careos.hms.department.entity.DepartmentStatus;
import com.careos.hms.employee.dto.CreateEmployeeRequest;
import com.careos.hms.employee.dto.LinkUserAccountRequest;
import com.careos.hms.employee.entity.EmployeeGender;
import com.careos.hms.employee.entity.EmploymentType;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hr.leave.dto.AdjustLeaveBalanceRequest;
import com.careos.hms.hr.leave.dto.AllocateLeaveBalanceRequest;
import com.careos.hms.hr.leave.dto.ApplyLeaveRequest;
import com.careos.hms.hr.leave.dto.ApproveLeaveStepRequest;
import com.careos.hms.hr.leave.dto.CreateLeaveTypeRequest;
import com.careos.hms.hr.leave.dto.RejectLeaveStepRequest;
import com.careos.hms.hr.leave.dto.UpdateLeaveTypeRequest;
import com.careos.hms.hr.leave.entity.HalfDayPeriod;
import com.careos.hms.hr.leave.entity.LeaveApprovalWorkflowType;
import com.careos.hms.hr.leave.entity.LeaveBalance;
import com.careos.hms.hr.leave.repository.LeaveBalanceRepository;
import com.careos.hms.permission.entity.PermissionCode;
import com.careos.hms.role.entity.Role;
import com.careos.hms.role.entity.RoleScope;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class LeaveManagementIntegrationTest {

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
    private LeaveBalanceRepository leaveBalanceRepository;

    private SecurityTestDataHelper.SeededRbac seededRbac;
    private Hospital hospitalA;
    private Hospital hospitalB;
    private String adminTokenA;
    private String adminTokenB;
    private String managerToken;
    private String employeeToken;
    private String deptPublicIdA;
    private String branchPublicIdA;

    private String managerEmployeePubId;
    private String staffEmployeePubId;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiterService.resetAll();
        testDataHelper.resetSecurityTables();
        seededRbac = testDataHelper.seedRbacCatalog();
        SecurityTestDataHelper.SeededRbac rbac = seededRbac;

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

        HospitalBranchRequest branchReq = new HospitalBranchRequest("MAIN", "Main Facility", "+919876543210", "main@hospital.local", "123 Health Ave", "Metropolis", "District", "State", "India", "110001", BranchStatus.ACTIVE);
        MvcResult branchRes = mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(branchReq)))
                .andExpect(status().isCreated())
                .andReturn();
        branchPublicIdA = objectMapper.readTree(branchRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Create Manager Employee with User Account
        CreateEmployeeRequest managerReq = new CreateEmployeeRequest(
                "Alice", null, "Manager",
                LocalDate.of(1985, 5, 20), EmployeeGender.FEMALE, "O_POSITIVE",
                "alice.manager@careos.local", "+919876543211", null,
                "Street 1", null, "City", "District", "State", "India", "110001",
                "Bob Manager", "Spouse", "+919876543212",
                "Lead Clinician", deptPublicIdA, branchPublicIdA, EmploymentType.FULL_TIME,
                LocalDate.of(2020, 1, 10), null, null,
                true, "manager.alice", TEST_PASSWORD, Set.of("HOSPITAL_ADMIN")
        );
        MvcResult mgrRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        managerEmployeePubId = objectMapper.readTree(mgrRes.getResponse().getContentAsString()).path("data").path("publicId").asText();
        managerToken = loginAndExtractToken("manager.alice");

        // Create Staff Employee reporting to Manager with User Account
        CreateEmployeeRequest staffReq = new CreateEmployeeRequest(
                "John", null, "Doe",
                LocalDate.of(1992, 8, 15), EmployeeGender.MALE, "B_POSITIVE",
                "john.doe@careos.local", "+919876543220", null,
                "Street 2", null, "City", "District", "State", "India", "110001",
                "Jane Doe", "Sister", "+919876543221",
                "Registered Nurse", deptPublicIdA, branchPublicIdA, EmploymentType.FULL_TIME,
                LocalDate.of(2022, 3, 1), null, managerEmployeePubId,
                true, "staff.john", TEST_PASSWORD, Set.of("HOSPITAL_ADMIN")
        );
        MvcResult staffRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isCreated())
                .andReturn();
        staffEmployeePubId = objectMapper.readTree(staffRes.getResponse().getContentAsString()).path("data").path("publicId").asText();
        employeeToken = loginAndExtractToken("staff.john");
    }

    private String loginAndExtractToken(String username) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, TEST_PASSWORD, false);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    void testLeaveTypeCrudAndDuplicatePrevention() throws Exception {
        CreateLeaveTypeRequest req = new CreateLeaveTypeRequest(
                "CASUAL", "Casual Leave", "Standard paid casual leave",
                12, true, 5, false, 0, true,
                LeaveApprovalWorkflowType.MANAGER_THEN_HR, 2
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("CASUAL"))
                .andExpect(jsonPath("$.data.annualAllowance").value(12))
                .andExpect(jsonPath("$.data.approvalLevelsCount").value(2))
                .andReturn();

        String leaveTypePubId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Duplicate code rejection in same hospital
        mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Update leave type
        UpdateLeaveTypeRequest updateReq = new UpdateLeaveTypeRequest(
                "Casual Leave Updated", "Updated description",
                14, true, 6, false, 0, true,
                LeaveApprovalWorkflowType.MANAGER_THEN_HR, 2
        );
        mockMvc.perform(put("/api/v1/hr/management/leaves/types/" + leaveTypePubId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Casual Leave Updated"))
                .andExpect(jsonPath("$.data.annualAllowance").value(14));

        // Deactivate and Activate
        mockMvc.perform(patch("/api/v1/hr/management/leaves/types/" + leaveTypePubId + "/deactivate")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/v1/hr/management/leaves/types/" + leaveTypePubId + "/activate")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testBalanceAllocationAndManualAdjustment() throws Exception {
        // Create leave type
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "SICK", "Sick Leave", "Paid sick leave",
                10, false, 0, true, 0, true,
                LeaveApprovalWorkflowType.HR_ONLY, 1
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Allocate 10 days to staff employee
        int currentYear = LocalDate.now().getYear();
        AllocateLeaveBalanceRequest allocReq = new AllocateLeaveBalanceRequest(
                staffEmployeePubId, leaveTypePubId, currentYear, BigDecimal.valueOf(10.0), "Annual allocation"
        );
        MvcResult allocRes = mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAllocated").value(10.0))
                .andExpect(jsonPath("$.data.availableBalance").value(10.0))
                .andReturn();

        String balancePubId = objectMapper.readTree(allocRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Adjust +2 days manually
        AdjustLeaveBalanceRequest adjReq = new AdjustLeaveBalanceRequest(BigDecimal.valueOf(2.0), "Bonus performance leave");
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/" + balancePubId + "/adjust")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAllocated").value(12.0))
                .andExpect(jsonPath("$.data.availableBalance").value(12.0));

        // Negative adjustment exceeding available -> rejected (prevent negative balance)
        AdjustLeaveBalanceRequest invalidAdj = new AdjustLeaveBalanceRequest(BigDecimal.valueOf(-20.0), "Deduction error");
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/" + balancePubId + "/adjust")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAdj)))
                .andExpect(status().isBadRequest());

        // Check transaction ledger
        mockMvc.perform(get("/api/v1/hr/management/leaves/balances/employees/" + staffEmployeePubId + "/transactions")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void testFullAndHalfDayApplicationsWithValidation() throws Exception {
        // Create leave type
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "EARNED", "Earned Leave", "Annual earned leave",
                15, true, 10, false, 0, true,
                LeaveApprovalWorkflowType.MANAGER_THEN_HR, 2
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Allocate balance
        int year = LocalDate.now().getYear();
        AllocateLeaveBalanceRequest allocReq = new AllocateLeaveBalanceRequest(
                staffEmployeePubId, leaveTypePubId, year, BigDecimal.valueOf(15.0), "Earned balance"
        );
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk());

        // Invalid: startDate after endDate
        ApplyLeaveRequest invalidDatesReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 7, 10), LocalDate.of(year, 7, 5),
                false, null, "Vacation", null, null
        );
        mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDatesReq)))
                .andExpect(status().isBadRequest());

        // Invalid: half-day across multiple dates
        ApplyLeaveRequest invalidHalfDayReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 7, 5), LocalDate.of(year, 7, 6),
                true, HalfDayPeriod.FIRST_HALF, "Half day error", null, null
        );
        mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHalfDayReq)))
                .andExpect(status().isBadRequest());

        // Valid half-day application
        ApplyLeaveRequest validHalfDay = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 7, 5), LocalDate.of(year, 7, 5),
                true, HalfDayPeriod.SECOND_HALF, "Afternoon appointment", "+919876543220", null
        );
        mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validHalfDay)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.halfDay").value(true))
                .andExpect(jsonPath("$.data.totalDays").value(0.5))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // Overlapping application detection
        ApplyLeaveRequest overlappingReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 7, 5), LocalDate.of(year, 7, 7),
                false, null, "Overlapping trip", null, null
        );
        mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingReq)))
                .andExpect(status().isBadRequest());

        // Insufficient balance detection
        ApplyLeaveRequest hugeReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 9, 1), LocalDate.of(year, 10, 30),
                false, null, "Huge vacation", null, null
        );
        mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hugeReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testManagerThenHrMultiLevelApprovalWorkflow() throws Exception {
        // Create leave type with MANAGER_THEN_HR
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "ANNUAL", "Annual Leave", "Standard annual leave",
                20, false, 0, false, 0, true,
                LeaveApprovalWorkflowType.MANAGER_THEN_HR, 2
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Allocate 20 days
        int year = LocalDate.now().getYear();
        AllocateLeaveBalanceRequest allocReq = new AllocateLeaveBalanceRequest(
                staffEmployeePubId, leaveTypePubId, year, BigDecimal.valueOf(20.0), "Allocation"
        );
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk());

        // Staff applies for 3 days
        ApplyLeaveRequest applyReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 8, 10), LocalDate.of(year, 8, 12),
                false, null, "Family trip", null, null
        );
        MvcResult applyRes = mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currentStepNumber").value(1))
                .andExpect(jsonPath("$.data.requiredStepsCount").value(2))
                .andReturn();
        String appPubId = objectMapper.readTree(applyRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Staff cannot approve own leave
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Out-of-order approval rejected (cannot approve step 2 before step 1)
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/2/approve")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isBadRequest());

        // Step 1: Manager approves
        ApproveLeaveStepRequest step1Req = new ApproveLeaveStepRequest("Manager approved, coverage ensured");
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step1Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.currentStepNumber").value(2));

        // Step 2: HR Admin approves (Final Step)
        ApproveLeaveStepRequest step2Req = new ApproveLeaveStepRequest("HR verified, approved");
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/2/approve")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step2Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Verify balance after final approval (used = 3, available = 17, pending = 0)
        mockMvc.perform(get("/api/v1/hr/self/leaves/balances")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].usedDays").value(3.0))
                .andExpect(jsonPath("$.data[0].availableBalance").value(17.0))
                .andExpect(jsonPath("$.data[0].pendingApprovalDays").value(0.0));
    }

    @Test
    void testRejectionReleasesPendingBalanceWithMandatoryReason() throws Exception {
        // Create HR_ONLY leave type
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "COMP_OFF", "Compensatory Off", "Compensatory time off",
                5, false, 0, false, 0, true,
                LeaveApprovalWorkflowType.HR_ONLY, 1
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        int year = LocalDate.now().getYear();
        AllocateLeaveBalanceRequest allocReq = new AllocateLeaveBalanceRequest(
                staffEmployeePubId, leaveTypePubId, year, BigDecimal.valueOf(5.0), "Comp off allocation"
        );
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk());

        ApplyLeaveRequest applyReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 11, 1), LocalDate.of(year, 11, 2),
                false, null, "Comp off request", null, null
        );
        MvcResult applyRes = mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String appPubId = objectMapper.readTree(applyRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Rejection without reason is rejected
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/reject")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"\"}"))
                .andExpect(status().isBadRequest());

        // Valid rejection
        RejectLeaveStepRequest rejectReq = new RejectLeaveStepRequest("Critical surgery scheduled on this day", "Please reschedule");
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/reject")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.finalRejectionReason").value("Critical surgery scheduled on this day"));

        // Pending balance released: available = 5, pending = 0
        mockMvc.perform(get("/api/v1/hr/self/leaves/balances")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].availableBalance").value(5.0))
                .andExpect(jsonPath("$.data[0].pendingApprovalDays").value(0.0));
    }

    @Test
    void testApprovedLeaveCancellationReversesBalance() throws Exception {
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "STUDY", "Study Leave", "Exam and study leave",
                8, false, 0, false, 0, true,
                LeaveApprovalWorkflowType.HR_ONLY, 1
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        int year = LocalDate.now().getYear() + 1; // future year to guarantee leave has not commenced
        AllocateLeaveBalanceRequest allocReq = new AllocateLeaveBalanceRequest(
                staffEmployeePubId, leaveTypePubId, year, BigDecimal.valueOf(8.0), "Future study leave"
        );
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk());

        ApplyLeaveRequest applyReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 2, 1), LocalDate.of(year, 2, 3),
                false, null, "Medical exam", null, null
        );
        MvcResult applyRes = mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String appPubId = objectMapper.readTree(applyRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // HR approves
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Staff cancels approved future leave
        mockMvc.perform(post("/api/v1/hr/self/leaves/applications/" + appPubId + "/cancel")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Used balance reversed, available restored to 8
        mockMvc.perform(get("/api/v1/hr/self/leaves/balances?year=" + year)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].usedDays").value(0.0))
                .andExpect(jsonPath("$.data[0].availableBalance").value(8.0));
    }

    @Test
    void testTenantIsolationAndSelfServiceBoundary() throws Exception {
        // Staff A in Hospital A applies for leave
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "WELLNESS", "Wellness Leave", "Health and wellness",
                5, false, 0, false, 0, true,
                LeaveApprovalWorkflowType.HR_ONLY, 1
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        int year = LocalDate.now().getYear();
        AllocateLeaveBalanceRequest allocReq = new AllocateLeaveBalanceRequest(
                staffEmployeePubId, leaveTypePubId, year, BigDecimal.valueOf(5.0), "Wellness allocation"
        );
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk());

        ApplyLeaveRequest applyReq = new ApplyLeaveRequest(
                null, leaveTypePubId,
                LocalDate.of(year, 12, 10), LocalDate.of(year, 12, 11),
                false, null, "Wellness retreat", null, null
        );
        MvcResult applyRes = mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String appPubId = objectMapper.readTree(applyRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // Hospital B Admin CANNOT access Hospital A's leave application
        mockMvc.perform(get("/api/v1/hr/management/leaves/applications/" + appPubId)
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // Hospital B Admin CANNOT approve Hospital A's leave step
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // Hospital B Admin CANNOT view Hospital A's employee balances
        mockMvc.perform(get("/api/v1/hr/management/leaves/balances/employees/" + staffEmployeePubId)
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // Hospital B Admin CANNOT download Hospital A's leave document
        mockMvc.perform(get("/api/v1/hr/management/leaves/applications/" + appPubId + "/document")
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDuplicateStepApprovalAndRepeatedFinalApprovalCannotDoubleDebitBalance() throws Exception {
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "DUP_TEST", "Annual Leave", "Two-step leave",
                10, false, 0, false, 0, true,
                LeaveApprovalWorkflowType.MANAGER_THEN_HR, 2
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        int year = LocalDate.now().getYear();
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AllocateLeaveBalanceRequest(
                                staffEmployeePubId, leaveTypePubId, year, BigDecimal.valueOf(10.0), "Init"
                        ))))
                .andExpect(status().isOk());

        MvcResult applyRes = mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplyLeaveRequest(
                                null, leaveTypePubId,
                                LocalDate.of(year, 6, 10), LocalDate.of(year, 6, 11),
                                false, null, "Duplicate approval check", null, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String appPubId = objectMapper.readTree(applyRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        // HR cannot override assigned reporting manager on Step 1
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isForbidden());

        // Manager inbox returns full LeaveApprovalInboxItemResponse including applicationPublicId
        mockMvc.perform(get("/api/v1/hr/management/leaves/approvals/inbox")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].applicationPublicId").value(appPubId))
                .andExpect(jsonPath("$.data.content[0].stepNumber").value(1))
                .andExpect(jsonPath("$.data.content[0].requiredStepsCount").value(2))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("John Doe"));

        // Step 1 approved by reporting manager
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveLeaveStepRequest("Step 1 OK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStepNumber").value(2));

        // Duplicate approval of Step 1 is rejected (400)
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveLeaveStepRequest("Duplicate step 1"))))
                .andExpect(status().isBadRequest());

        // Step 2 (final) approved by HR Admin
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/2/approve")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveLeaveStepRequest("Final OK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Repeated final approval of Step 2 is rejected (400) and does NOT debit balance twice
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appPubId + "/steps/2/approve")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveLeaveStepRequest("Duplicate final"))))
                .andExpect(status().isBadRequest());

        // Verify balance debited exactly once (used = 2.0, available = 8.0, pending = 0.0)
        mockMvc.perform(get("/api/v1/hr/self/leaves/balances?year=" + year)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].usedDays").value(2.0))
                .andExpect(jsonPath("$.data[0].availableBalance").value(8.0))
                .andExpect(jsonPath("$.data[0].pendingApprovalDays").value(0.0));

        // Verify ledger has exactly 2 transactions: 1 ALLOCATION + 1 DEBIT_APPROVED_LEAVE
        mockMvc.perform(get("/api/v1/hr/self/leaves/balances/transactions")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].transactionType").value("DEBIT_APPROVED_LEAVE"))
                .andExpect(jsonPath("$.data.content[1].transactionType").value("ALLOCATION"));
    }

    @Test
    void testSameHospitalEmployeeSelfServiceAndDocumentIsolationAndUnauthorizedAccess() throws Exception {
        // Seed non-admin roles matching V9 RBAC
        Role staffRole = testDataHelper.createCustomRole(
                "STAFF_SELF_SERVICE",
                "Staff Self Service",
                RoleScope.HOSPITAL,
                List.of(
                        seededRbac.permissions().get(PermissionCode.HOSPITAL_HR_SELF_SERVICE),
                        seededRbac.permissions().get(PermissionCode.HOSPITAL_LEAVES_APPLY),
                        seededRbac.permissions().get(PermissionCode.HOSPITAL_WORKSPACE_READ)
                )
        );
        Role viewerRole = testDataHelper.createCustomRole(
                "WORKSPACE_VIEWER",
                "Workspace Viewer",
                RoleScope.HOSPITAL,
                List.of(seededRbac.permissions().get(PermissionCode.HOSPITAL_WORKSPACE_READ))
        );
        Role approverOnlyRole = testDataHelper.createCustomRole(
                "APPROVER_ONLY",
                "Approver Only",
                RoleScope.HOSPITAL,
                List.of(
                        seededRbac.permissions().get(PermissionCode.HOSPITAL_HR_SELF_SERVICE),
                        seededRbac.permissions().get(PermissionCode.HOSPITAL_LEAVES_APPLY),
                        seededRbac.permissions().get(PermissionCode.HOSPITAL_LEAVES_APPROVE)
                )
        );

        // Create Employee A and Employee B in Hospital A with STAFF_SELF_SERVICE only (NO admin role)
        CreateEmployeeRequest empAReq = new CreateEmployeeRequest(
                "Priya", null, "Sharma",
                LocalDate.of(1993, 4, 12), EmployeeGender.FEMALE, "A_POSITIVE",
                "priya.sharma@careos.local", "+919876543230", null,
                "Addr A", null, "City", "Dist", "State", "India", "110001",
                "Contact A", "Spouse", "+919876543231",
                "Lab Tech", deptPublicIdA, branchPublicIdA, EmploymentType.FULL_TIME,
                LocalDate.of(2023, 1, 15), null, managerEmployeePubId,
                true, "emp.priya", TEST_PASSWORD, Set.of(staffRole.getCode())
        );
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empAReq)))
                .andExpect(status().isCreated());
        String empAToken = loginAndExtractToken("emp.priya");

        CreateEmployeeRequest empBReq = new CreateEmployeeRequest(
                "Rahul", null, "Verma",
                LocalDate.of(1991, 9, 18), EmployeeGender.MALE, "O_POSITIVE",
                "rahul.verma@careos.local", "+919876543240", null,
                "Addr B", null, "City", "Dist", "State", "India", "110001",
                "Contact B", "Parent", "+919876543241",
                "Pharmacist", deptPublicIdA, branchPublicIdA, EmploymentType.FULL_TIME,
                LocalDate.of(2022, 6, 1), null, managerEmployeePubId,
                true, "emp.rahul", TEST_PASSWORD, Set.of(staffRole.getCode())
        );
        MvcResult empBRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empBReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String empBPubId = objectMapper.readTree(empBRes.getResponse().getContentAsString()).path("data").path("publicId").asText();
        String empBToken = loginAndExtractToken("emp.rahul");

        // Unrelated manager with HOSPITAL_LEAVES_APPROVE (not Rahul's reporting manager)
        CreateEmployeeRequest otherMgrReq = new CreateEmployeeRequest(
                "Other", null, "Supervisor",
                LocalDate.of(1982, 2, 10), EmployeeGender.MALE, "B_POSITIVE",
                "other.mgr@careos.local", "+919876543250", null,
                "Addr C", null, "City", "Dist", "State", "India", "110001",
                "Contact C", "Spouse", "+919876543251",
                "Supervisor", deptPublicIdA, branchPublicIdA, EmploymentType.FULL_TIME,
                LocalDate.of(2021, 1, 1), null, null,
                true, "other.mgr", TEST_PASSWORD, Set.of(approverOnlyRole.getCode())
        );
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherMgrReq)))
                .andExpect(status().isCreated());
        String otherMgrToken = loginAndExtractToken("other.mgr");

        // Viewer user with zero leave permissions
        testDataHelper.createUser(
                hospitalA,
                UserScope.HOSPITAL,
                "viewer.alpha",
                "viewer.alpha@careos.local",
                TEST_PASSWORD,
                viewerRole
        );
        String viewerToken = loginAndExtractToken("viewer.alpha");

        // Admin creates Medical Leave type and allocates balance to Employee B
        CreateLeaveTypeRequest medTypeReq = new CreateLeaveTypeRequest(
                "MED_ISO", "Medical Leave", "Requires cert",
                12, false, 0, true, 0, true,
                LeaveApprovalWorkflowType.MANAGER_THEN_HR, 2
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medTypeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        int year = LocalDate.now().getYear();
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AllocateLeaveBalanceRequest(
                                empBPubId, leaveTypePubId, year, BigDecimal.valueOf(12.0), "Annual"
                        ))))
                .andExpect(status().isOk());

        // Employee B uploads medical certificate via self-service
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "medical_cert.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF-CONTENT".getBytes(StandardCharsets.UTF_8)
        );
        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/hr/self/leaves/upload-document")
                        .file(file)
                        .header("Authorization", "Bearer " + empBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentStorageKey").isNotEmpty())
                .andReturn();
        String docKey = objectMapper.readTree(uploadRes.getResponse().getContentAsString())
                .path("data").path("documentStorageKey").asText();

        // Employee B submits 3-day medical leave with uploaded document
        MvcResult applyRes = mockMvc.perform(post("/api/v1/hr/self/leaves/apply")
                        .header("Authorization", "Bearer " + empBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplyLeaveRequest(
                                null, leaveTypePubId,
                                LocalDate.of(year, 5, 10), LocalDate.of(year, 5, 12),
                                false, null, "Surgery recovery", "+919876543240", docKey
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String appBPubId = objectMapper.readTree(applyRes.getResponse().getContentAsString())
                .path("data").path("publicId").asText();

        // Employee B CAN read own application and download own document
        mockMvc.perform(get("/api/v1/hr/self/leaves/applications/" + appBPubId)
                        .header("Authorization", "Bearer " + empBToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/hr/self/leaves/applications/" + appBPubId + "/document")
                        .header("Authorization", "Bearer " + empBToken))
                .andExpect(status().isOk());

        // Employee A (same hospital) CANNOT view, cancel, or download Employee B's leave or document
        mockMvc.perform(get("/api/v1/hr/self/leaves/applications/" + appBPubId)
                        .header("Authorization", "Bearer " + empAToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/hr/self/leaves/applications/" + appBPubId + "/cancel")
                        .header("Authorization", "Bearer " + empAToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/hr/self/leaves/applications/" + appBPubId + "/document")
                        .header("Authorization", "Bearer " + empAToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/hr/management/leaves/applications/" + appBPubId + "/document")
                        .header("Authorization", "Bearer " + empAToken))
                .andExpect(status().isForbidden());

        // Unauthorized staff & viewer users CANNOT access management/approval APIs
        mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + empAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medTypeReq)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + empAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AllocateLeaveBalanceRequest(
                                empBPubId, leaveTypePubId, year, BigDecimal.ONE, "Unauthorized"
                        ))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/hr/management/leaves/approvals/inbox")
                        .header("Authorization", "Bearer " + empAToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appBPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + empAToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appBPubId + "/steps/1/reject")
                        .header("Authorization", "Bearer " + empAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectLeaveStepRequest("No", null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/hr/self/leaves/balances")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // Unassigned manager with HOSPITAL_LEAVES_APPROVE CANNOT approve Step 1 assigned to manager.alice
        mockMvc.perform(post("/api/v1/hr/management/leaves/applications/" + appBPubId + "/steps/1/approve")
                        .header("Authorization", "Bearer " + otherMgrToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLeaveBalanceOptimisticLockingProtectsConcurrentUpdates() throws Exception {
        CreateLeaveTypeRequest typeReq = new CreateLeaveTypeRequest(
                "LOCK_TEST", "Lock Test Leave", "Concurrency test",
                10, false, 0, false, 0, true,
                LeaveApprovalWorkflowType.HR_ONLY, 1
        );
        MvcResult typeRes = mockMvc.perform(post("/api/v1/hr/management/leaves/types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String leaveTypePubId = objectMapper.readTree(typeRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        int year = LocalDate.now().getYear();
        MvcResult allocRes = mockMvc.perform(post("/api/v1/hr/management/leaves/balances/allocate")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AllocateLeaveBalanceRequest(
                                staffEmployeePubId, leaveTypePubId, year, BigDecimal.valueOf(10.0), "Init"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn();
        String balancePubId = objectMapper.readTree(allocRes.getResponse().getContentAsString()).path("data").path("publicId").asText();

        LeaveBalance staleSnapshot = leaveBalanceRepository.findByPublicIdAndHospitalId(balancePubId, hospitalA.getId())
                .orElseThrow();

        // Advance version in DB via legitimate adjustment (version increments on transaction commit/flush)
        mockMvc.perform(post("/api/v1/hr/management/leaves/balances/" + balancePubId + "/adjust")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustLeaveBalanceRequest(
                                BigDecimal.valueOf(2.0), "Concurrent update"
                        ))))
                .andExpect(status().isOk());

        LeaveBalance committed = leaveBalanceRepository.findByPublicIdAndHospitalId(balancePubId, hospitalA.getId())
                .orElseThrow();
        assertThat(committed.getVersion()).isEqualTo(1L);

        // Attempting to persist stale snapshot must fail with optimistic locking exception
        staleSnapshot.setPendingApprovalDays(BigDecimal.ONE);
        assertThatThrownBy(() -> leaveBalanceRepository.saveAndFlush(staleSnapshot))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
