package com.careos.hms.authentication;

import com.careos.hms.hr.attendance.repository.AttendanceCorrectionRepository;
import com.careos.hms.hr.attendance.repository.AttendanceRepository;
import com.careos.hms.hr.document.repository.HrDocumentRepository;
import com.careos.hms.hr.holiday.repository.HolidayRepository;
import com.careos.hms.hr.leave.repository.LeaveApplicationRepository;
import com.careos.hms.hr.leave.repository.LeaveApprovalRepository;
import com.careos.hms.hr.leave.repository.LeaveBalanceRepository;
import com.careos.hms.hr.leave.repository.LeaveBalanceTransactionRepository;
import com.careos.hms.hr.leave.repository.LeaveTypeRepository;
import com.careos.hms.hr.shift.repository.ShiftRepository;
import com.careos.hms.hr.shift.repository.ShiftRosterRepository;
import com.careos.hms.hr.shift.repository.ShiftSwapRequestRepository;
import com.careos.hms.audit.repository.AuditLogRepository;
import com.careos.hms.authentication.repository.RefreshTokenRepository;
import com.careos.hms.branch.repository.HospitalBranchRepository;
import com.careos.hms.department.repository.DepartmentRepository;
import com.careos.hms.employee.repository.EmployeeLifecycleEventRepository;
import com.careos.hms.employee.repository.EmployeeRepository;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.repository.HospitalContactRepository;
import com.careos.hms.hospital.repository.HospitalDocumentRepository;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.hospital.repository.HospitalSettingsRepository;
import com.careos.hms.permission.entity.Permission;
import com.careos.hms.permission.entity.PermissionCode;
import com.careos.hms.permission.entity.PermissionScope;
import com.careos.hms.permission.entity.RolePermission;
import com.careos.hms.permission.repository.PermissionRepository;
import com.careos.hms.permission.repository.RolePermissionRepository;
import com.careos.hms.role.entity.Role;
import com.careos.hms.role.entity.RoleScope;
import com.careos.hms.role.entity.SystemRoleCode;
import com.careos.hms.role.entity.UserRole;
import com.careos.hms.role.repository.RoleRepository;
import com.careos.hms.role.repository.UserRoleRepository;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SecurityTestDataHelper {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final HospitalSettingsRepository hospitalSettingsRepository;
    private final HospitalDocumentRepository hospitalDocumentRepository;
    private final HospitalContactRepository hospitalContactRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalBranchRepository hospitalBranchRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmployeeLifecycleEventRepository employeeLifecycleEventRepository;
    private final EmployeeRepository employeeRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    // HR Repositories
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveBalanceTransactionRepository leaveBalanceTransactionRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AttendanceCorrectionRepository attendanceCorrectionRepository;
    private final AttendanceRepository attendanceRepository;
    private final ShiftSwapRequestRepository shiftSwapRequestRepository;
    private final ShiftRosterRepository shiftRosterRepository;
    private final ShiftRepository shiftRepository;
    private final HolidayRepository holidayRepository;
    private final HrDocumentRepository hrDocumentRepository;

    public SecurityTestDataHelper(
            RefreshTokenRepository refreshTokenRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            HospitalSettingsRepository hospitalSettingsRepository,
            HospitalDocumentRepository hospitalDocumentRepository,
            HospitalContactRepository hospitalContactRepository,
            DepartmentRepository departmentRepository,
            HospitalBranchRepository hospitalBranchRepository,
            AuditLogRepository auditLogRepository,
            EmployeeLifecycleEventRepository employeeLifecycleEventRepository,
            EmployeeRepository employeeRepository,
            HospitalRepository hospitalRepository,
            PasswordEncoder passwordEncoder,
            LeaveApprovalRepository leaveApprovalRepository,
            LeaveApplicationRepository leaveApplicationRepository,
            LeaveBalanceTransactionRepository leaveBalanceTransactionRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveTypeRepository leaveTypeRepository,
            AttendanceCorrectionRepository attendanceCorrectionRepository,
            AttendanceRepository attendanceRepository,
            ShiftSwapRequestRepository shiftSwapRequestRepository,
            ShiftRosterRepository shiftRosterRepository,
            ShiftRepository shiftRepository,
            HolidayRepository holidayRepository,
            HrDocumentRepository hrDocumentRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.hospitalSettingsRepository = hospitalSettingsRepository;
        this.hospitalDocumentRepository = hospitalDocumentRepository;
        this.hospitalContactRepository = hospitalContactRepository;
        this.departmentRepository = departmentRepository;
        this.hospitalBranchRepository = hospitalBranchRepository;
        this.auditLogRepository = auditLogRepository;
        this.employeeLifecycleEventRepository = employeeLifecycleEventRepository;
        this.employeeRepository = employeeRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaveApprovalRepository = leaveApprovalRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveBalanceTransactionRepository = leaveBalanceTransactionRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.attendanceCorrectionRepository = attendanceCorrectionRepository;
        this.attendanceRepository = attendanceRepository;
        this.shiftSwapRequestRepository = shiftSwapRequestRepository;
        this.shiftRosterRepository = shiftRosterRepository;
        this.shiftRepository = shiftRepository;
        this.holidayRepository = holidayRepository;
        this.hrDocumentRepository = hrDocumentRepository;
    }

    @Transactional
    public void resetSecurityTables() {
        hrDocumentRepository.deleteAllInBatch();
        holidayRepository.deleteAllInBatch();
        attendanceCorrectionRepository.deleteAllInBatch();
        attendanceRepository.deleteAllInBatch();
        shiftSwapRequestRepository.deleteAllInBatch();
        shiftRosterRepository.deleteAllInBatch();
        shiftRepository.deleteAllInBatch();
        leaveApprovalRepository.deleteAllInBatch();
        leaveApplicationRepository.deleteAllInBatch();
        leaveBalanceTransactionRepository.deleteAllInBatch();
        leaveBalanceRepository.deleteAllInBatch();
        leaveTypeRepository.deleteAllInBatch();
        employeeLifecycleEventRepository.deleteAllInBatch();
        employeeRepository.deleteAllInBatch();
        auditLogRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
        hospitalBranchRepository.deleteAllInBatch();
        hospitalContactRepository.deleteAllInBatch();
        hospitalDocumentRepository.deleteAllInBatch();
        hospitalSettingsRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        rolePermissionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
        permissionRepository.deleteAllInBatch();
        hospitalRepository.deleteAllInBatch();
    }

    @Transactional
    public SeededRbac seedRbacCatalog() {
        Role superAdminRole = roleRepository.saveAndFlush(new Role(
                SystemRoleCode.PLATFORM_SUPER_ADMIN,
                "Platform Super Administrator",
                "Global platform owner role",
                RoleScope.PLATFORM,
                true
        ));
        Role hospitalAdminRole = roleRepository.saveAndFlush(new Role(
                SystemRoleCode.HOSPITAL_ADMIN,
                "Hospital Administrator",
                "Hospital tenant administrator role",
                RoleScope.HOSPITAL,
                true
        ));

        Map<String, Permission> permissions = new LinkedHashMap<>();
        permissions.put(PermissionCode.AUTH_ME_READ, createPerm(PermissionCode.AUTH_ME_READ, "Read Current User", "authentication", PermissionScope.COMMON));
        permissions.put(PermissionCode.AUTH_PERMISSIONS_READ, createPerm(PermissionCode.AUTH_PERMISSIONS_READ, "Read User Permissions", "authentication", PermissionScope.COMMON));
        permissions.put(PermissionCode.AUTH_PASSWORD_CHANGE, createPerm(PermissionCode.AUTH_PASSWORD_CHANGE, "Change Own Password", "authentication", PermissionScope.COMMON));
        permissions.put(PermissionCode.SYSTEM_HEALTH_VIEW, createPerm(PermissionCode.SYSTEM_HEALTH_VIEW, "View System Health", "health", PermissionScope.COMMON));
        permissions.put(PermissionCode.PLATFORM_OVERVIEW_READ, createPerm(PermissionCode.PLATFORM_OVERVIEW_READ, "Read Platform Overview", "platform", PermissionScope.PLATFORM));
        permissions.put(PermissionCode.PLATFORM_HOSPITALS_MANAGE, createPerm(PermissionCode.PLATFORM_HOSPITALS_MANAGE, "Manage Hospitals", "platform", PermissionScope.PLATFORM));
        permissions.put(PermissionCode.PLATFORM_AUDIT_READ, createPerm(PermissionCode.PLATFORM_AUDIT_READ, "Read Platform Audit", "platform", PermissionScope.PLATFORM));
        permissions.put(PermissionCode.PLATFORM_DOCUMENTS_VERIFY, createPerm(PermissionCode.PLATFORM_DOCUMENTS_VERIFY, "Verify Compliance Documents", "document", PermissionScope.PLATFORM));

        permissions.put(PermissionCode.HOSPITAL_WORKSPACE_READ, createPerm(PermissionCode.HOSPITAL_WORKSPACE_READ, "Read Hospital Workspace", "hospital", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_USERS_MANAGE, createPerm(PermissionCode.HOSPITAL_USERS_MANAGE, "Manage Hospital Users", "users", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_ROLES_READ, createPerm(PermissionCode.HOSPITAL_ROLES_READ, "Read Hospital Roles", "roles", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_SETTINGS_MANAGE, createPerm(PermissionCode.HOSPITAL_SETTINGS_MANAGE, "Manage Hospital Settings", "hospital", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_DEPARTMENTS_MANAGE, createPerm(PermissionCode.HOSPITAL_DEPARTMENTS_MANAGE, "Manage Departments", "department", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_BRANCHES_MANAGE, createPerm(PermissionCode.HOSPITAL_BRANCHES_MANAGE, "Manage Branches", "branch", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_DOCUMENTS_MANAGE, createPerm(PermissionCode.HOSPITAL_DOCUMENTS_MANAGE, "Manage Documents", "document", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_CONTACTS_MANAGE, createPerm(PermissionCode.HOSPITAL_CONTACTS_MANAGE, "Manage Contacts", "contact", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_PROFILE_UPDATE, createPerm(PermissionCode.HOSPITAL_PROFILE_UPDATE, "Update Hospital Profile", "hospital", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_AUDIT_READ, createPerm(PermissionCode.HOSPITAL_AUDIT_READ, "Read Hospital Audit", "audit", PermissionScope.HOSPITAL));

        // Phase 4 Employee Management Permissions
        permissions.put(PermissionCode.HOSPITAL_EMPLOYEES_READ, createPerm(PermissionCode.HOSPITAL_EMPLOYEES_READ, "Read Hospital Employees", "employee", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_EMPLOYEES_MANAGE, createPerm(PermissionCode.HOSPITAL_EMPLOYEES_MANAGE, "Manage Hospital Employees", "employee", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_EMPLOYEES_STATUS, createPerm(PermissionCode.HOSPITAL_EMPLOYEES_STATUS, "Manage Employee Status", "employee", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_EMPLOYEES_ACCOUNT_LINK, createPerm(PermissionCode.HOSPITAL_EMPLOYEES_ACCOUNT_LINK, "Link Employee Account", "employee", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.PLATFORM_EMPLOYEES_READ, createPerm(PermissionCode.PLATFORM_EMPLOYEES_READ, "Read Platform Employees", "employee", PermissionScope.PLATFORM));

        // Phase 5 HR Management Permissions
        permissions.put(PermissionCode.PLATFORM_HR_READ, createPerm(PermissionCode.PLATFORM_HR_READ, "Read Platform HR Overview", "hr", PermissionScope.PLATFORM));
        permissions.put(PermissionCode.HOSPITAL_HR_SELF_SERVICE, createPerm(PermissionCode.HOSPITAL_HR_SELF_SERVICE, "Access Employee HR Self-Service", "hr", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_LEAVES_APPLY, createPerm(PermissionCode.HOSPITAL_LEAVES_APPLY, "Apply For Leaves", "leave", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_LEAVES_READ, createPerm(PermissionCode.HOSPITAL_LEAVES_READ, "Read Hospital Leaves", "leave", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_LEAVES_APPROVE, createPerm(PermissionCode.HOSPITAL_LEAVES_APPROVE, "Approve or Reject Leaves", "leave", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_LEAVES_MANAGE, createPerm(PermissionCode.HOSPITAL_LEAVES_MANAGE, "Manage Leave Policies and Balances", "leave", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_ATTENDANCE_RECORD, createPerm(PermissionCode.HOSPITAL_ATTENDANCE_RECORD, "Record Own Attendance", "attendance", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_ATTENDANCE_READ, createPerm(PermissionCode.HOSPITAL_ATTENDANCE_READ, "Read Hospital Attendance", "attendance", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_ATTENDANCE_APPROVE, createPerm(PermissionCode.HOSPITAL_ATTENDANCE_APPROVE, "Approve Attendance Corrections", "attendance", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_ATTENDANCE_MANAGE, createPerm(PermissionCode.HOSPITAL_ATTENDANCE_MANAGE, "Manage Attendance and Ingestion", "attendance", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_SHIFTS_READ, createPerm(PermissionCode.HOSPITAL_SHIFTS_READ, "Read Shift Rosters", "shift", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_SHIFTS_MANAGE, createPerm(PermissionCode.HOSPITAL_SHIFTS_MANAGE, "Manage Shifts and Rosters", "shift", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_SHIFTS_SWAP, createPerm(PermissionCode.HOSPITAL_SHIFTS_SWAP, "Request and Approve Shift Swaps", "shift", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_HOLIDAYS_READ, createPerm(PermissionCode.HOSPITAL_HOLIDAYS_READ, "Read Holidays", "holiday", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_HOLIDAYS_MANAGE, createPerm(PermissionCode.HOSPITAL_HOLIDAYS_MANAGE, "Manage Holiday Calendar", "holiday", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_HR_DOCUMENTS_READ, createPerm(PermissionCode.HOSPITAL_HR_DOCUMENTS_READ, "Read HR Documents", "document", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_HR_DOCUMENTS_MANAGE, createPerm(PermissionCode.HOSPITAL_HR_DOCUMENTS_MANAGE, "Manage HR Documents", "document", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_HR_REPORTS_READ, createPerm(PermissionCode.HOSPITAL_HR_REPORTS_READ, "Read HR Reports & Analytics", "report", PermissionScope.HOSPITAL));

        for (String code : List.of(
                PermissionCode.AUTH_ME_READ,
                PermissionCode.AUTH_PERMISSIONS_READ,
                PermissionCode.AUTH_PASSWORD_CHANGE,
                PermissionCode.SYSTEM_HEALTH_VIEW,
                PermissionCode.PLATFORM_OVERVIEW_READ,
                PermissionCode.PLATFORM_HOSPITALS_MANAGE,
                PermissionCode.PLATFORM_AUDIT_READ,
                PermissionCode.PLATFORM_DOCUMENTS_VERIFY,
                PermissionCode.PLATFORM_EMPLOYEES_READ,
                PermissionCode.PLATFORM_HR_READ
        )) {
            rolePermissionRepository.saveAndFlush(new RolePermission(superAdminRole, permissions.get(code)));
        }

        for (String code : List.of(
                PermissionCode.AUTH_ME_READ,
                PermissionCode.AUTH_PERMISSIONS_READ,
                PermissionCode.AUTH_PASSWORD_CHANGE,
                PermissionCode.SYSTEM_HEALTH_VIEW,
                PermissionCode.HOSPITAL_WORKSPACE_READ,
                PermissionCode.HOSPITAL_USERS_MANAGE,
                PermissionCode.HOSPITAL_ROLES_READ,
                PermissionCode.HOSPITAL_SETTINGS_MANAGE,
                PermissionCode.HOSPITAL_DEPARTMENTS_MANAGE,
                PermissionCode.HOSPITAL_BRANCHES_MANAGE,
                PermissionCode.HOSPITAL_DOCUMENTS_MANAGE,
                PermissionCode.HOSPITAL_CONTACTS_MANAGE,
                PermissionCode.HOSPITAL_PROFILE_UPDATE,
                PermissionCode.HOSPITAL_AUDIT_READ,
                PermissionCode.HOSPITAL_EMPLOYEES_READ,
                PermissionCode.HOSPITAL_EMPLOYEES_MANAGE,
                PermissionCode.HOSPITAL_EMPLOYEES_STATUS,
                PermissionCode.HOSPITAL_EMPLOYEES_ACCOUNT_LINK,
                PermissionCode.HOSPITAL_HR_SELF_SERVICE,
                PermissionCode.HOSPITAL_LEAVES_APPLY,
                PermissionCode.HOSPITAL_LEAVES_READ,
                PermissionCode.HOSPITAL_LEAVES_APPROVE,
                PermissionCode.HOSPITAL_LEAVES_MANAGE,
                PermissionCode.HOSPITAL_ATTENDANCE_RECORD,
                PermissionCode.HOSPITAL_ATTENDANCE_READ,
                PermissionCode.HOSPITAL_ATTENDANCE_APPROVE,
                PermissionCode.HOSPITAL_ATTENDANCE_MANAGE,
                PermissionCode.HOSPITAL_SHIFTS_READ,
                PermissionCode.HOSPITAL_SHIFTS_MANAGE,
                PermissionCode.HOSPITAL_SHIFTS_SWAP,
                PermissionCode.HOSPITAL_HOLIDAYS_READ,
                PermissionCode.HOSPITAL_HOLIDAYS_MANAGE,
                PermissionCode.HOSPITAL_HR_DOCUMENTS_READ,
                PermissionCode.HOSPITAL_HR_DOCUMENTS_MANAGE,
                PermissionCode.HOSPITAL_HR_REPORTS_READ
        )) {
            rolePermissionRepository.saveAndFlush(new RolePermission(hospitalAdminRole, permissions.get(code)));
        }

        return new SeededRbac(superAdminRole, hospitalAdminRole, permissions);
    }

    @Transactional
    public Role createCustomRole(String code, String name, RoleScope scope, List<Permission> perms) {
        Role role = roleRepository.saveAndFlush(new Role(code, name, name, scope, false));
        for (Permission p : perms) {
            rolePermissionRepository.saveAndFlush(new RolePermission(role, p));
        }
        return role;
    }

    @Transactional
    public Hospital createHospital(String code, String name) {
        return hospitalRepository.saveAndFlush(new Hospital(code, name));
    }

    @Transactional
    public User createUser(
            Hospital hospital,
            UserScope scope,
            String username,
            String email,
            String rawPassword,
            Role role
    ) {
        User user = userRepository.saveAndFlush(new User(
                hospital,
                scope,
                username,
                email,
                passwordEncoder.encode(rawPassword),
                "Test",
                "User"
        ));
        if (role != null) {
            userRoleRepository.saveAndFlush(new UserRole(user, role, "TEST_SEED"));
        }
        return user;
    }

    private Permission createPerm(String code, String name, String moduleName, PermissionScope scope) {
        return permissionRepository.saveAndFlush(new Permission(code, name, moduleName, scope, name));
    }

    public record SeededRbac(
            Role platformSuperAdminRole,
            Role hospitalAdminRole,
            Map<String, Permission> permissions
    ) {
    }
}