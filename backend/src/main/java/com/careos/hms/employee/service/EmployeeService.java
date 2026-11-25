package com.careos.hms.employee.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.authentication.entity.TokenRevocationReason;
import com.careos.hms.authentication.repository.RefreshTokenRepository;
import com.careos.hms.authentication.validator.PasswordPolicyValidator;
import com.careos.hms.branch.entity.HospitalBranch;
import com.careos.hms.branch.repository.HospitalBranchRepository;
import com.careos.hms.common.exception.DuplicateResourceException;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.department.entity.Department;
import com.careos.hms.department.entity.DepartmentStatus;
import com.careos.hms.department.repository.DepartmentRepository;
import com.careos.hms.employee.dto.CreateEmployeeRequest;
import com.careos.hms.employee.dto.EmployeeLifecycleEventResponse;
import com.careos.hms.employee.dto.EmployeeResponse;
import com.careos.hms.employee.dto.EmployeeSummaryResponse;
import com.careos.hms.employee.dto.LinkUserAccountRequest;
import com.careos.hms.employee.dto.UpdateEmployeeRequest;
import com.careos.hms.employee.dto.UpdateEmployeeStatusRequest;
import com.careos.hms.employee.entity.Employee;
import com.careos.hms.employee.entity.EmployeeLifecycleEvent;
import com.careos.hms.employee.entity.EmployeeStatus;
import com.careos.hms.employee.entity.EmploymentType;
import com.careos.hms.employee.repository.EmployeeLifecycleEventRepository;
import com.careos.hms.employee.repository.EmployeeRepository;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.role.entity.Role;
import com.careos.hms.role.entity.RoleScope;
import com.careos.hms.role.entity.UserRole;
import com.careos.hms.role.repository.RoleRepository;
import com.careos.hms.role.repository.UserRoleRepository;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import com.careos.hms.storage.DocumentStorageService;
import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeLifecycleEventRepository employeeLifecycleEventRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalBranchRepository hospitalBranchRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final DocumentStorageService documentStorageService;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;
    private final EmployeeCodeGeneratorService employeeCodeGeneratorService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            EmployeeLifecycleEventRepository employeeLifecycleEventRepository,
            DepartmentRepository departmentRepository,
            HospitalBranchRepository hospitalBranchRepository,
            HospitalRepository hospitalRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            DocumentStorageService documentStorageService,
            AuditLogService auditLogService,
            TenantContext tenantContext,
            EmployeeCodeGeneratorService employeeCodeGeneratorService
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeLifecycleEventRepository = employeeLifecycleEventRepository;
        this.departmentRepository = departmentRepository;
        this.hospitalBranchRepository = hospitalBranchRepository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.documentStorageService = documentStorageService;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
        this.employeeCodeGeneratorService = employeeCodeGeneratorService;
    }

    @Transactional
    public EmployeeResponse createEmployee(Long hospitalId, CreateEmployeeRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);

        // 1. Verify hospital existence
        Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));

        // 2. Validate department belongs to same hospital and is ACTIVE
        Department department = departmentRepository.findByPublicIdAndHospitalIdAndDeletedFalse(
                request.departmentPublicId(), hospitalId
        ).orElseThrow(() -> new ResourceNotFoundException("Department", request.departmentPublicId()));

        if (department.getStatus() != DepartmentStatus.ACTIVE) {
            throw new InvalidRequestException("Cannot assign employee to an inactive department");
        }

        // 3. Validate branch if provided
        HospitalBranch branch = null;
        if (request.branchPublicId() != null && !request.branchPublicId().isBlank()) {
            branch = hospitalBranchRepository.findByPublicIdAndHospitalIdAndDeletedFalse(
                    request.branchPublicId(), hospitalId
            ).orElseThrow(() -> new ResourceNotFoundException("HospitalBranch", request.branchPublicId()));
        }

        // 4. Validate reporting manager if provided
        Employee reportingManager = null;
        if (request.reportingManagerPublicId() != null && !request.reportingManagerPublicId().isBlank()) {
            reportingManager = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(
                    request.reportingManagerPublicId(), hospitalId
            ).orElseThrow(() -> new ResourceNotFoundException("Reporting Manager", request.reportingManagerPublicId()));
        }

        // 5. Validate unique email within hospital
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (employeeRepository.existsByHospitalIdAndEmailIgnoreCaseAndDeletedFalse(hospitalId, email)) {
            throw new DuplicateResourceException("Employee with email '" + request.email() + "' already exists in this hospital");
        }

        // 6. Determine employee number (auto-generate or use manual)
        String employeeNumber;
        if (request.employeeNumber() != null && !request.employeeNumber().isBlank()) {
            employeeNumber = request.employeeNumber().trim();
            if (employeeRepository.existsByHospitalIdAndEmployeeNumberIgnoreCaseAndDeletedFalse(hospitalId, employeeNumber)) {
                throw new DuplicateResourceException("Employee number '" + employeeNumber + "' already exists in this hospital");
            }
        } else {
            employeeNumber = employeeCodeGeneratorService.generateNextEmployeeNumber(hospitalId);
        }

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();

        // 7. Optional User account provisioning
        User linkedUser = null;
        if (Boolean.TRUE.equals(request.createUserAccount())) {
            if (request.username() == null || request.username().isBlank()) {
                throw new InvalidRequestException("Username is required when creating a user account");
            }
            if (request.password() == null || request.password().isBlank()) {
                throw new InvalidRequestException("Password is required when creating a user account");
            }

            String username = request.username().trim().toLowerCase(Locale.ROOT);
            passwordPolicyValidator.validatePasswordStrength(request.password(), username);

            if (userRepository.existsByUsernameIgnoreCaseOrEmailIgnoreCase(username, email)) {
                throw new DuplicateResourceException("User with username '" + username + "' or email '" + email + "' already exists");
            }

            linkedUser = new User(
                    hospital,
                    UserScope.HOSPITAL,
                    username,
                    email,
                    passwordEncoder.encode(request.password()),
                    request.firstName().trim(),
                    request.lastName().trim()
            );
            linkedUser = userRepository.saveAndFlush(linkedUser);

            // Assign roles
            if (request.roleCodes() != null && !request.roleCodes().isEmpty()) {
                for (String roleCode : request.roleCodes()) {
                    Role role = roleRepository.findByCodeAndDeletedFalseAndActiveTrue(roleCode.trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Role", roleCode));
                    if (role.getScope() == RoleScope.PLATFORM) {
                        throw new ForbiddenOperationException("FORBIDDEN", "Cannot assign platform-scoped role to hospital employee");
                    }
                    userRoleRepository.save(new UserRole(linkedUser, role, principal.getUsername()));
                }
            }
        }

        // 8. Create Employee entity
        Employee employee = new Employee(
                hospitalId,
                employeeNumber,
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender(),
                email,
                request.phoneNumber().trim(),
                request.designation().trim(),
                department,
                request.joiningDate()
        );

        if (request.middleName() != null && !request.middleName().isBlank()) {
            employee.setMiddleName(request.middleName().trim());
        }
        employee.setBloodGroup(request.bloodGroup() != null ? request.bloodGroup().trim() : null);
        employee.setAlternatePhoneNumber(request.alternatePhoneNumber() != null ? request.alternatePhoneNumber().trim() : null);
        employee.setAddressLine1(request.addressLine1() != null ? request.addressLine1().trim() : null);
        employee.setAddressLine2(request.addressLine2() != null ? request.addressLine2().trim() : null);
        employee.setCity(request.city() != null ? request.city().trim() : null);
        employee.setDistrict(request.district() != null ? request.district().trim() : null);
        employee.setState(request.state() != null ? request.state().trim() : null);
        if (request.country() != null && !request.country().isBlank()) {
            employee.setCountry(request.country().trim());
        }
        employee.setPinCode(request.pinCode() != null ? request.pinCode().trim() : null);
        employee.setEmergencyContactName(request.emergencyContactName() != null ? request.emergencyContactName().trim() : null);
        employee.setEmergencyContactRelation(request.emergencyContactRelation() != null ? request.emergencyContactRelation().trim() : null);
        employee.setEmergencyContactPhone(request.emergencyContactPhone() != null ? request.emergencyContactPhone().trim() : null);

        employee.setBranch(branch);
        employee.setEmploymentType(request.employmentType());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setReportingManager(reportingManager);
        employee.setUser(linkedUser);

        Employee saved = employeeRepository.saveAndFlush(employee);

        // 9. Initial Lifecycle Event
        EmployeeLifecycleEvent initialEvent = new EmployeeLifecycleEvent(
                hospitalId,
                saved,
                null,
                EmployeeStatus.ACTIVE,
                "Initial employee registration",
                saved.getJoiningDate(),
                principal.getUsername()
        );
        employeeLifecycleEventRepository.saveAndFlush(initialEvent);

        // 10. Audit Log
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.EMPLOYEE_REGISTERED,
                "Employee",
                saved.getPublicId(),
                "Registered employee: " + saved.getFullName() + " (" + saved.getEmployeeNumber() + ")",
                null,
                clientIp
        );

        return getEmployeeByPublicId(hospitalId, saved.getPublicId());
    }

    @Transactional
    public EmployeeResponse createMyEmployee(CreateEmployeeRequest request, String clientIp) {
        return createEmployee(requireCurrentHospitalId(), request, clientIp);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> getEmployees(
            Long hospitalId,
            String search,
            String departmentPublicId,
            String branchPublicId,
            String designation,
            EmploymentType employmentType,
            EmployeeStatus status,
            Pageable pageable
    ) {
        assertHospitalAccess(hospitalId);

        Long departmentId = null;
        if (departmentPublicId != null && !departmentPublicId.isBlank()) {
            departmentId = departmentRepository.findByPublicIdAndHospitalIdAndDeletedFalse(departmentPublicId, hospitalId)
                    .map(Department::getId)
                    .orElse(null);
        }

        Long branchId = null;
        if (branchPublicId != null && !branchPublicId.isBlank()) {
            branchId = hospitalBranchRepository.findByPublicIdAndHospitalIdAndDeletedFalse(branchPublicId, hospitalId)
                    .map(HospitalBranch::getId)
                    .orElse(null);
        }

        Specification<Employee> spec = EmployeeSpecification.filter(
                hospitalId,
                search,
                departmentId,
                branchId,
                designation,
                employmentType,
                status
        );

        return employeeRepository.findAll(spec, pageable).map(EmployeeSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> getMyEmployees(
            String search,
            String departmentPublicId,
            String branchPublicId,
            String designation,
            EmploymentType employmentType,
            EmployeeStatus status,
            Pageable pageable
    ) {
        return getEmployees(
                requireCurrentHospitalId(),
                search,
                departmentPublicId,
                branchPublicId,
                designation,
                employmentType,
                status,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByPublicId(Long hospitalId, String publicId) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findWithDetailsByPublicIdAndHospitalId(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));
        return EmployeeResponse.from(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getMyEmployeeByPublicId(String publicId) {
        return getEmployeeByPublicId(requireCurrentHospitalId(), publicId);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long hospitalId, String publicId, UpdateEmployeeRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        // Check duplicate email
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (employeeRepository.existsByHospitalIdAndEmailIgnoreCaseAndIdNotAndDeletedFalse(hospitalId, email, employee.getId())) {
            throw new DuplicateResourceException("Employee with email '" + request.email() + "' already exists in this hospital");
        }

        // Validate department
        Department department = departmentRepository.findByPublicIdAndHospitalIdAndDeletedFalse(
                request.departmentPublicId(), hospitalId
        ).orElseThrow(() -> new ResourceNotFoundException("Department", request.departmentPublicId()));

        // Validate branch
        HospitalBranch branch = null;
        if (request.branchPublicId() != null && !request.branchPublicId().isBlank()) {
            branch = hospitalBranchRepository.findByPublicIdAndHospitalIdAndDeletedFalse(
                    request.branchPublicId(), hospitalId
            ).orElseThrow(() -> new ResourceNotFoundException("HospitalBranch", request.branchPublicId()));
        }

        // Validate reporting manager
        Employee reportingManager = null;
        if (request.reportingManagerPublicId() != null && !request.reportingManagerPublicId().isBlank()) {
            if (request.reportingManagerPublicId().equals(publicId)) {
                throw new InvalidRequestException("An employee cannot be their own reporting manager");
            }
            reportingManager = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(
                    request.reportingManagerPublicId(), hospitalId
            ).orElseThrow(() -> new ResourceNotFoundException("Reporting Manager", request.reportingManagerPublicId()));
        }

        employee.setFirstName(request.firstName().trim());
        employee.setMiddleName(request.middleName() != null ? request.middleName().trim() : null);
        employee.setLastName(request.lastName().trim());
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setGender(request.gender());
        employee.setBloodGroup(request.bloodGroup() != null ? request.bloodGroup().trim() : null);
        employee.setEmail(email);
        employee.setPhoneNumber(request.phoneNumber().trim());
        employee.setAlternatePhoneNumber(request.alternatePhoneNumber() != null ? request.alternatePhoneNumber().trim() : null);
        employee.setAddressLine1(request.addressLine1() != null ? request.addressLine1().trim() : null);
        employee.setAddressLine2(request.addressLine2() != null ? request.addressLine2().trim() : null);
        employee.setCity(request.city() != null ? request.city().trim() : null);
        employee.setDistrict(request.district() != null ? request.district().trim() : null);
        employee.setState(request.state() != null ? request.state().trim() : null);
        if (request.country() != null && !request.country().isBlank()) {
            employee.setCountry(request.country().trim());
        }
        employee.setPinCode(request.pinCode() != null ? request.pinCode().trim() : null);
        employee.setEmergencyContactName(request.emergencyContactName() != null ? request.emergencyContactName().trim() : null);
        employee.setEmergencyContactRelation(request.emergencyContactRelation() != null ? request.emergencyContactRelation().trim() : null);
        employee.setEmergencyContactPhone(request.emergencyContactPhone() != null ? request.emergencyContactPhone().trim() : null);

        employee.setDesignation(request.designation().trim());
        employee.setDepartment(department);
        employee.setBranch(branch);
        employee.setEmploymentType(request.employmentType());
        employee.setJoiningDate(request.joiningDate());
        employee.setLeavingDate(request.leavingDate());
        employee.setReportingManager(reportingManager);

        Employee saved = employeeRepository.saveAndFlush(employee);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.EMPLOYEE_UPDATED,
                "Employee",
                saved.getPublicId(),
                "Updated employee: " + saved.getFullName() + " (" + saved.getEmployeeNumber() + ")",
                null,
                clientIp
        );

        return getEmployeeByPublicId(hospitalId, saved.getPublicId());
    }

    @Transactional
    public EmployeeResponse updateMyEmployee(String publicId, UpdateEmployeeRequest request, String clientIp) {
        return updateEmployee(requireCurrentHospitalId(), publicId, request, clientIp);
    }

    @Transactional
    public EmployeeResponse updateEmployeeStatus(Long hospitalId, String publicId, UpdateEmployeeStatusRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        EmployeeStatus currentStatus = employee.getStatus();
        EmployeeStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            return EmployeeResponse.from(employee);
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidRequestException(
                    "Invalid employee status transition from " + currentStatus + " to " + targetStatus
            );
        }

        LocalDate effectiveDate = request.effectiveDate() != null ? request.effectiveDate() : LocalDate.now();

        employee.setStatus(targetStatus);
        if (targetStatus == EmployeeStatus.RESIGNED || targetStatus == EmployeeStatus.TERMINATED) {
            employee.setLeavingDate(effectiveDate);
        } else if (targetStatus == EmployeeStatus.ACTIVE && currentStatus != EmployeeStatus.ACTIVE) {
            employee.setLeavingDate(null);
        }

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();

        // Manage linked user account status & session revocation
        User linkedUser = employee.getUser();
        if (linkedUser != null) {
            if (targetStatus == EmployeeStatus.SUSPENDED) {
                linkedUser.setStatus(AccountStatus.SUSPENDED);
                userRepository.saveAndFlush(linkedUser);
                refreshTokenRepository.revokeAllActiveTokensForUser(
                        linkedUser.getId(), TokenRevocationReason.ACCOUNT_DISABLED, Instant.now()
                );
            } else if (targetStatus == EmployeeStatus.INACTIVE || targetStatus == EmployeeStatus.RESIGNED || targetStatus == EmployeeStatus.TERMINATED) {
                linkedUser.setStatus(AccountStatus.INACTIVE);
                userRepository.saveAndFlush(linkedUser);
                refreshTokenRepository.revokeAllActiveTokensForUser(
                        linkedUser.getId(), TokenRevocationReason.ACCOUNT_DISABLED, Instant.now()
                );
            } else if (targetStatus == EmployeeStatus.ACTIVE && linkedUser.getStatus() != AccountStatus.ACTIVE) {
                linkedUser.setStatus(AccountStatus.ACTIVE);
                userRepository.saveAndFlush(linkedUser);
            }
        }

        Employee saved = employeeRepository.saveAndFlush(employee);

        // Record lifecycle event
        EmployeeLifecycleEvent lifecycleEvent = new EmployeeLifecycleEvent(
                hospitalId,
                saved,
                currentStatus,
                targetStatus,
                request.reason().trim(),
                effectiveDate,
                principal.getUsername()
        );
        employeeLifecycleEventRepository.saveAndFlush(lifecycleEvent);

        // Audit log
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.EMPLOYEE_STATUS_CHANGED,
                "Employee",
                saved.getPublicId(),
                "Changed employee " + saved.getEmployeeNumber() + " status from " + currentStatus + " to " + targetStatus + ". Reason: " + request.reason().trim(),
                null,
                clientIp
        );

        return getEmployeeByPublicId(hospitalId, saved.getPublicId());
    }

    @Transactional
    public EmployeeResponse updateMyEmployeeStatus(String publicId, UpdateEmployeeStatusRequest request, String clientIp) {
        return updateEmployeeStatus(requireCurrentHospitalId(), publicId, request, clientIp);
    }

    @Transactional(readOnly = true)
    public List<EmployeeLifecycleEventResponse> getLifecycleHistory(Long hospitalId, String publicId) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        return employeeLifecycleEventRepository.findByHospitalIdAndEmployeeIdOrderByCreatedAtDesc(hospitalId, employee.getId())
                .stream()
                .map(EmployeeLifecycleEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeLifecycleEventResponse> getMyEmployeeLifecycleHistory(String publicId) {
        return getLifecycleHistory(requireCurrentHospitalId(), publicId);
    }

    @Transactional
    public EmployeeResponse linkUserAccount(Long hospitalId, String publicId, LinkUserAccountRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        if (employee.getUser() != null) {
            throw new InvalidRequestException("Employee is already linked to a user account");
        }

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        User linkedUser;

        if (request.userPublicId() != null && !request.userPublicId().isBlank()) {
            linkedUser = userRepository.findActiveGraphByPublicId(request.userPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.userPublicId()));

            if (!Objects.equals(linkedUser.getHospitalId(), hospitalId)) {
                throw new ForbiddenOperationException("FORBIDDEN", "User does not belong to the current hospital tenant");
            }

            if (employeeRepository.findByUserIdAndDeletedFalse(linkedUser.getId()).isPresent()) {
                throw new DuplicateResourceException("This user account is already linked to another employee");
            }
        } else {
            // Create a new user account
            if (request.username() == null || request.username().isBlank()) {
                throw new InvalidRequestException("Username is required to create a user account");
            }
            if (request.password() == null || request.password().isBlank()) {
                throw new InvalidRequestException("Password is required to create a user account");
            }

            String username = request.username().trim().toLowerCase(Locale.ROOT);
            passwordPolicyValidator.validatePasswordStrength(request.password(), username);

            if (userRepository.existsByUsernameIgnoreCaseOrEmailIgnoreCase(username, employee.getEmail())) {
                throw new DuplicateResourceException("User with username '" + username + "' or email '" + employee.getEmail() + "' already exists");
            }

            Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(hospitalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));

            linkedUser = new User(
                    hospital,
                    UserScope.HOSPITAL,
                    username,
                    employee.getEmail(),
                    passwordEncoder.encode(request.password()),
                    employee.getFirstName(),
                    employee.getLastName()
            );
            linkedUser = userRepository.saveAndFlush(linkedUser);

            if (request.roleCodes() != null && !request.roleCodes().isEmpty()) {
                for (String roleCode : request.roleCodes()) {
                    Role role = roleRepository.findByCodeAndDeletedFalseAndActiveTrue(roleCode.trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Role", roleCode));
                    if (role.getScope() == RoleScope.PLATFORM) {
                        throw new ForbiddenOperationException("FORBIDDEN", "Cannot assign platform-scoped role to hospital employee");
                    }
                    userRoleRepository.save(new UserRole(linkedUser, role, principal.getUsername()));
                }
            }
        }

        employee.setUser(linkedUser);
        Employee saved = employeeRepository.saveAndFlush(employee);

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.EMPLOYEE_USER_LINKED,
                "Employee",
                saved.getPublicId(),
                "Linked employee " + saved.getEmployeeNumber() + " to user account: " + linkedUser.getUsername(),
                null,
                clientIp
        );

        return getEmployeeByPublicId(hospitalId, saved.getPublicId());
    }

    @Transactional
    public EmployeeResponse linkMyEmployeeUserAccount(String publicId, LinkUserAccountRequest request, String clientIp) {
        return linkUserAccount(requireCurrentHospitalId(), publicId, request, clientIp);
    }

    @Transactional
    public EmployeeResponse unlinkUserAccount(Long hospitalId, String publicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findWithDetailsByPublicIdAndHospitalId(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        User user = employee.getUser();
        if (user == null) {
            return EmployeeResponse.from(employee);
        }

        String unlinkedUsername = user.getUsername();
        Long unlinkedUserId = user.getId();

        employee.setUser(null);
        Employee saved = employeeRepository.saveAndFlush(employee);

        // Revoke active sessions for unlinked user
        refreshTokenRepository.revokeAllActiveTokensForUser(
                unlinkedUserId, TokenRevocationReason.ACCOUNT_DISABLED, Instant.now()
        );

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.EMPLOYEE_USER_UNLINKED,
                "Employee",
                saved.getPublicId(),
                "Unlinked employee " + saved.getEmployeeNumber() + " from user account: " + unlinkedUsername,
                null,
                clientIp
        );

        return getEmployeeByPublicId(hospitalId, saved.getPublicId());
    }

    @Transactional
    public EmployeeResponse unlinkMyEmployeeUserAccount(String publicId, String clientIp) {
        return unlinkUserAccount(requireCurrentHospitalId(), publicId, clientIp);
    }

    @Transactional
    public EmployeeResponse uploadProfilePhoto(Long hospitalId, String publicId, MultipartFile file, String clientIp) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        if (file.isEmpty()) {
            throw new InvalidRequestException("Profile photo file cannot be empty");
        }

        Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));

        try {
            // Delete previous photo if exists
            if (employee.getProfilePhotoKey() != null && !employee.getProfilePhotoKey().isBlank()) {
                documentStorageService.deleteFile(employee.getProfilePhotoKey());
            }

            String photoKey = documentStorageService.storeFile(
                    hospital.getPublicId(),
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType()
            );

            employee.setProfilePhotoKey(photoKey);
            Employee saved = employeeRepository.saveAndFlush(employee);

            CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
            auditLogService.record(
                    principal.getUserId(),
                    principal.getUsername(),
                    hospitalId,
                    AuditAction.EMPLOYEE_PHOTO_UPDATED,
                    "Employee",
                    saved.getPublicId(),
                    "Updated profile photo for employee " + saved.getEmployeeNumber(),
                    null,
                    clientIp
            );

            return getEmployeeByPublicId(hospitalId, saved.getPublicId());
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to read profile photo file: " + e.getMessage());
        }
    }

    @Transactional
    public EmployeeResponse uploadMyEmployeeProfilePhoto(String publicId, MultipartFile file, String clientIp) {
        return uploadProfilePhoto(requireCurrentHospitalId(), publicId, file, clientIp);
    }

    @Transactional(readOnly = true)
    public byte[] loadProfilePhoto(Long hospitalId, String publicId) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        if (employee.getProfilePhotoKey() == null || employee.getProfilePhotoKey().isBlank()) {
            throw new ResourceNotFoundException("Profile photo not found for employee", publicId);
        }

        return documentStorageService.loadFile(employee.getProfilePhotoKey());
    }

    @Transactional(readOnly = true)
    public byte[] loadMyEmployeeProfilePhoto(String publicId) {
        return loadProfilePhoto(requireCurrentHospitalId(), publicId);
    }

    @Transactional
    public void deleteEmployee(Long hospitalId, String publicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", publicId));

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        employee.markDeleted(principal.getUsername());

        // Revoke linked user's active tokens if any
        if (employee.getUser() != null) {
            refreshTokenRepository.revokeAllActiveTokensForUser(
                    employee.getUser().getId(), TokenRevocationReason.ACCOUNT_DISABLED, Instant.now()
            );
        }

        employeeRepository.saveAndFlush(employee);

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.EMPLOYEE_DELETED,
                "Employee",
                employee.getPublicId(),
                "Deleted employee: " + employee.getFullName() + " (" + employee.getEmployeeNumber() + ")",
                null,
                clientIp
        );
    }

    @Transactional
    public void deleteMyEmployee(String publicId, String clientIp) {
        deleteEmployee(requireCurrentHospitalId(), publicId, clientIp);
    }

    private Long requireCurrentHospitalId() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException("TENANT_REQUIRED", "Hospital context is required");
        }
        return principal.getHospitalId();
    }

    private void assertHospitalAccess(Long hospitalId) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (principal.isPlatformUser()) {
            return;
        }
        if (!Objects.equals(principal.getHospitalId(), hospitalId)) {
            throw new ForbiddenOperationException("FORBIDDEN", "Cannot access employees of another hospital");
        }
    }
}
