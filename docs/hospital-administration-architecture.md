# CareOS-HMS — Hospital Administration Architecture (Phase 3)

## 1. Overview
The Hospital Administration module in CareOS-HMS establishes the operational lifecycle of healthcare institutions on the platform. It bridges multi-tenant isolation, organization configuration, department structures, branch networks, compliance document verification, and administrative audit logging.

---

## 2. Core Concepts and Data Model

### 2.1 Hospital Onboarding & Lifecycle
Each healthcare institution is represented by the `hospitals` entity with an immutable `public_id` (UUID) and an operational `hospital_code` (e.g., `HOSP-METRO`).
The hospital progresses through a defined state machine:

```
[ PENDING ] ──(Platform Admin Approves)──> [ ACTIVE ]
     │                                        │   ▲
     │                                        ▼   │
     └──(Rejection/Deactivation)─────────> [ SUSPENDED / INACTIVE ]
```

- **PENDING**: Initial state upon self-registration or registration by platform staff. A default operational configuration (`hospital_settings`) and initial admin account are atomically provisioned.
- **ACTIVE**: Fully functional; hospital staff can log in, manage departments, branches, contacts, and documents.
- **SUSPENDED**: Suspended by Platform Admin (e.g., billing, compliance expiration, policy violation). All active refresh tokens of the hospital's users are immediately revoked (`ACCOUNT_DISABLED`), cutting off active sessions.
- **INACTIVE**: Decommissioned hospital. Users cannot authenticate.

### 2.2 Operational Settings (`hospital_settings`)
Encapsulates hospital-specific localization and operational defaults:
- Localization: `timezone` (e.g. `Asia/Kolkata`), `currency` (`INR`), `date_format` (`YYYY-MM-DD`), `time_format` (`24_HOUR`).
- Scheduling policies: `working_days`, `working_hours_start`, `working_hours_end`, `appointment_slot_duration_minutes`, `max_advance_booking_days`.
- Hospital contact pointers: `emergency_contact_number`, `support_email`.

### 2.3 Departments (`departments`)
Departments represent clinical, diagnostic, and administrative units within a hospital.
- Extends `TenantAwareEntity` (automatic assignment of `hospital_id` via current user's session context).
- Multi-tenant uniqueness: `UNIQUE KEY uq_dept_hospital_code (hospital_id, department_code)`. Different hospitals can share the same code (e.g., `CARDIO`, `EMERGENCY`).

### 2.4 Hospital Branches (`hospital_branches`)
Represents physical campus facilities and satellite centers.
- Extends `TenantAwareEntity`.
- Scoped uniqueness: `UNIQUE KEY uq_branch_hospital_code (hospital_id, branch_code)`.
- Includes address, contact person, status (`ACTIVE`, `INACTIVE`).

### 2.5 Compliance Documents (`hospital_documents`)
Tracks institutional regulatory compliance files (Registration Certificate, NABH, Clinical Establishment License, Fire Safety NOC, Pollution Control NOC, Tax ID).
- Storage Abstraction: Managed via `DocumentStorageService` (local secure filesystem storage with path traversal defense, file type whitelisting, 10MB limit, and non-guessable disk paths).
- Document Verification Status:
  - `PENDING`: Uploaded by hospital, pending review.
  - `VERIFIED`: Approved by Platform Super Admin.
  - `REJECTED`: Rejected by Platform Super Admin with documented reason.

### 2.6 Key Contacts (`hospital_contacts`)
Structured emergency and administrative contacts (General, Emergency, Billing, Administrative, Technical).

### 2.7 Audit Logs (`audit_logs`)
Captures all critical administrative actions:
- Fields: `hospital_id`, `actor_user_id`, `actor_username`, `action`, `resource_type`, `resource_id`, `details_json`, `ip_address`, `created_at`.
- Scoping: Platform administrators can view system-wide audit records; Hospital administrators can view only their own hospital audit records.

---

## 3. Tenant Isolation & Security Rules

1. **Context Resolution**: The user's authenticated JWT carries `scope` (`PLATFORM` or `HOSPITAL`) and `hospitalId`.
2. **Access Control**:
   - `PLATFORM` scope users have administrative oversight across all hospitals (`PLATFORM_HOSPITALS_MANAGE`, `PLATFORM_DOCUMENTS_VERIFY`).
   - `HOSPITAL` scope users are strictly bound to their assigned `hospitalId`. Any attempt to access or modify resources belonging to another hospital results in a 403 `ForbiddenOperationException` or 404 `ResourceNotFoundException`.
3. **Session Invalidation on Suspension**: Transitioning hospital status to `SUSPENDED` or `INACTIVE` automatically revokes all active refresh tokens for hospital users, preventing ongoing access.