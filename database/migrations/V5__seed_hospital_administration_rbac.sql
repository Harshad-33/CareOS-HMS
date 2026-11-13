-- CareOS-HMS Phase 3 RBAC Permissions Seed
-- Adds administration permissions for hospital profile, departments, branches, documents, contacts, and audits.

INSERT INTO permissions (code, name, module_name, scope, description, created_by, updated_by)
VALUES
    ('HOSPITAL_DEPARTMENTS_MANAGE', 'Manage Hospital Departments', 'department', 'HOSPITAL', 'Create, update, activate/deactivate, and delete departments within own hospital.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_BRANCHES_MANAGE', 'Manage Hospital Branches', 'branch', 'HOSPITAL', 'Create, update, activate/deactivate, and delete branches within own hospital.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_DOCUMENTS_MANAGE', 'Manage Hospital Documents', 'document', 'HOSPITAL', 'Upload, download, and manage legal/accreditation documents for own hospital.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_CONTACTS_MANAGE', 'Manage Hospital Contacts', 'contact', 'HOSPITAL', 'Create, update, and manage official contacts for own hospital.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_PROFILE_UPDATE', 'Update Hospital Profile', 'hospital', 'HOSPITAL', 'Update profile and contact details for own hospital.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_AUDIT_READ', 'Read Hospital Audit Logs', 'audit', 'HOSPITAL', 'View administrative and operational audit trail for own hospital.', 'SYSTEM', 'SYSTEM'),
    ('PLATFORM_DOCUMENTS_VERIFY', 'Verify Hospital Documents', 'document', 'PLATFORM', 'Platform-level review, approval, and rejection of hospital compliance documents.', 'SYSTEM', 'SYSTEM');

-- Assign new platform permissions to PLATFORM_SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code IN (
    'PLATFORM_DOCUMENTS_VERIFY'
)
WHERE r.code = 'PLATFORM_SUPER_ADMIN';

-- Assign new hospital administration permissions to HOSPITAL_ADMIN
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code IN (
    'HOSPITAL_DEPARTMENTS_MANAGE',
    'HOSPITAL_BRANCHES_MANAGE',
    'HOSPITAL_DOCUMENTS_MANAGE',
    'HOSPITAL_CONTACTS_MANAGE',
    'HOSPITAL_PROFILE_UPDATE',
    'HOSPITAL_AUDIT_READ'
)
WHERE r.code = 'HOSPITAL_ADMIN';
