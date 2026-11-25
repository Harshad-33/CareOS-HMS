-- CareOS-HMS Phase 4 RBAC Permissions Seed
-- Adds fine-grained permissions for employee registry, profile editing, lifecycle changes, and user linking.

INSERT INTO permissions (code, name, module_name, scope, description, created_by, updated_by)
VALUES
    ('HOSPITAL_EMPLOYEES_READ', 'Read Employee Directory', 'employee', 'HOSPITAL', 'View hospital staff directory, demographic information, and professional assignments.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_EMPLOYEES_MANAGE', 'Manage Employees', 'employee', 'HOSPITAL', 'Register new hospital staff, update profiles, addresses, and department assignments.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_EMPLOYEES_STATUS', 'Manage Employee Status', 'employee', 'HOSPITAL', 'Govern employee lifecycle transitions (activate, suspend, mark resigned/terminated).', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_EMPLOYEES_ACCOUNT_LINK', 'Link Employee User Account', 'employee', 'HOSPITAL', 'Provision, link, or unlink login user credentials with employee records.', 'SYSTEM', 'SYSTEM'),
    ('PLATFORM_EMPLOYEES_READ', 'Platform Read Employees', 'employee', 'PLATFORM', 'Platform-level read-only oversight of employee directories across hospitals.', 'SYSTEM', 'SYSTEM');

-- Assign platform permission to PLATFORM_SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code = 'PLATFORM_EMPLOYEES_READ'
WHERE r.code = 'PLATFORM_SUPER_ADMIN';

-- Assign hospital permissions to HOSPITAL_ADMIN
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code IN (
    'HOSPITAL_EMPLOYEES_READ',
    'HOSPITAL_EMPLOYEES_MANAGE',
    'HOSPITAL_EMPLOYEES_STATUS',
    'HOSPITAL_EMPLOYEES_ACCOUNT_LINK'
)
WHERE r.code = 'HOSPITAL_ADMIN';
