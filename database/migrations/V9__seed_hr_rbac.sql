-- CareOS-HMS Phase 5 HR RBAC Permissions Seed
-- Seeds permissions for Leave Management, Attendance, Shifts, Holidays, HR Documents, and Employee Self-Service.

INSERT INTO permissions (code, name, module_name, scope, description, created_by, updated_by)
VALUES
    ('PLATFORM_HR_READ', 'Platform Read HR Oversight', 'hr', 'PLATFORM', 'Platform-level read-only oversight of hospital HR metrics and workforce distributions.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_HR_SELF_SERVICE', 'Employee Self-Service Access', 'hr', 'HOSPITAL', 'Access personal Employee Self-Service portal (leaves, punch clock, personal roster, and documents).', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_LEAVES_APPLY', 'Apply for Leave', 'hr', 'HOSPITAL', 'Submit personal leave applications, track balance ledgers, and cancel pending requests.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_LEAVES_READ', 'Read Leave Applications', 'hr', 'HOSPITAL', 'View leave requests, balance distributions, and team leave calendars.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_LEAVES_APPROVE', 'Approve Leave Applications', 'hr', 'HOSPITAL', 'Perform multi-level review, approval, or rejection with mandatory reason on leave requests.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_LEAVES_MANAGE', 'Manage Leave Policies', 'hr', 'HOSPITAL', 'Configure hospital leave types, entitlement rules, accrual schemes, and approval workflows.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_ATTENDANCE_RECORD', 'Record Daily Attendance', 'hr', 'HOSPITAL', 'Perform web check-in/out, view personal attendance timesheets, and raise correction requests.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_ATTENDANCE_READ', 'Read Attendance Registers', 'hr', 'HOSPITAL', 'View daily attendance registers, biometric logs, and monthly timesheet matrices.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_ATTENDANCE_APPROVE', 'Approve Attendance Corrections', 'hr', 'HOSPITAL', 'Review, approve, or reject attendance regularisation and correction requests.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_ATTENDANCE_MANAGE', 'Manage Attendance & Biometrics', 'hr', 'HOSPITAL', 'Manage attendance settings, manual overrides, and biometric device integration syncs.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_SHIFTS_READ', 'Read Shift Rosters', 'hr', 'HOSPITAL', 'View hospital shift definitions, duty schedules, and personal/department rotas.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_SHIFTS_MANAGE', 'Manage Shift Schedules', 'hr', 'HOSPITAL', 'Define shift timings, configure grace periods, publish rosters, and approve shift swaps.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_SHIFTS_SWAP', 'Request Shift Swaps', 'hr', 'HOSPITAL', 'Propose and accept peer-to-peer clinical shift swap requests.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_HOLIDAYS_READ', 'Read Hospital Holidays', 'hr', 'HOSPITAL', 'View statutory public holidays, branch calendars, and weekly-off schedules.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_HOLIDAYS_MANAGE', 'Manage Hospital Holidays', 'hr', 'HOSPITAL', 'Declare hospital holidays, manage branch exceptions, and set weekly-off policies.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_HR_DOCUMENTS_READ', 'Read HR Documents', 'hr', 'HOSPITAL', 'View and download employee HR dossiers, credentialing files, and contracts.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_HR_DOCUMENTS_MANAGE', 'Manage HR Documents', 'hr', 'HOSPITAL', 'Upload, verify, and govern employee contracts, experience letters, and track expiries.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_HR_REPORTS_READ', 'Read HR Reports & Analytics', 'hr', 'HOSPITAL', 'Access staff presence KPIs, leave utilization analytics, and shift coverage reports.', 'SYSTEM', 'SYSTEM');

-- Assign platform permission to PLATFORM_SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code = 'PLATFORM_HR_READ'
WHERE r.code = 'PLATFORM_SUPER_ADMIN';

-- Assign all hospital HR permissions to HOSPITAL_ADMIN
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code IN (
    'HOSPITAL_HR_SELF_SERVICE',
    'HOSPITAL_LEAVES_APPLY',
    'HOSPITAL_LEAVES_READ',
    'HOSPITAL_LEAVES_APPROVE',
    'HOSPITAL_LEAVES_MANAGE',
    'HOSPITAL_ATTENDANCE_RECORD',
    'HOSPITAL_ATTENDANCE_READ',
    'HOSPITAL_ATTENDANCE_APPROVE',
    'HOSPITAL_ATTENDANCE_MANAGE',
    'HOSPITAL_SHIFTS_READ',
    'HOSPITAL_SHIFTS_MANAGE',
    'HOSPITAL_SHIFTS_SWAP',
    'HOSPITAL_HOLIDAYS_READ',
    'HOSPITAL_HOLIDAYS_MANAGE',
    'HOSPITAL_HR_DOCUMENTS_READ',
    'HOSPITAL_HR_DOCUMENTS_MANAGE',
    'HOSPITAL_HR_REPORTS_READ'
)
WHERE r.code = 'HOSPITAL_ADMIN';
