# CareOS-HMS Authorization & RBAC Architecture (Phase 2)

## 1. Permission-Centric RBAC Model
CareOS-HMS uses a **Role-Based Access Control (RBAC)** model backed by granular **permission authorities**:
- `User -> UserRole -> Role -> RolePermission -> Permission`
- `CareOsUserPrincipal` maps:
  - Roles to `ROLE_<ROLE_CODE>` authorities
  - Permissions to raw permission strings (e.g., `HOSPITAL_WORKSPACE_READ`, `PLATFORM_HOSPITALS_MANAGE`)
- Backend controllers and services authorize primarily against **fine-grained permissions** (`@PreAuthorize("hasAuthority('...')")`) combined with tenant scope checks (`@tenantGuard`). Role codes identify functional personas without coupling business rules to rigid role names.

## 2. Phase 2 Seeded Roles (`V3__seed_initial_rbac.sql`)

| Role Code | Scope | Purpose |
| :--- | :--- | :--- |
| `PLATFORM_SUPER_ADMIN` | `PLATFORM` | Global platform owner with permission to manage platform overview, audit visibility, and future hospital onboarding; intentionally excluded from hospital operational/clinical permissions unless explicitly audited. |
| `HOSPITAL_ADMIN` | `HOSPITAL` | Primary tenant administrator scoped to a single `hospital_id`; owns hospital workspace access, user administration, role lookup, and hospital settings. |

### Reserved Future Clinical & Operational Roles (`SystemRoleCode`)
Defined as documented constants for upcoming phases (not seeded or assigned in Phase 2):
- `DOCTOR`, `NURSE`, `RECEPTIONIST`, `LAB_TECHNICIAN`, `PHARMACIST`, `ACCOUNTANT`, `INVENTORY_MANAGER`

## 3. Phase 2 Baseline Permissions

| Permission Code | Scope | Module | Assigned Roles |
| :--- | :--- | :--- | :--- |
| `AUTH_ME_READ` | `COMMON` | `authentication` | `PLATFORM_SUPER_ADMIN`, `HOSPITAL_ADMIN` |
| `AUTH_PERMISSIONS_READ` | `COMMON` | `authentication` | `PLATFORM_SUPER_ADMIN`, `HOSPITAL_ADMIN` |
| `AUTH_PASSWORD_CHANGE` | `COMMON` | `authentication` | `PLATFORM_SUPER_ADMIN`, `HOSPITAL_ADMIN` |
| `SYSTEM_HEALTH_VIEW` | `COMMON` | `health` | `PLATFORM_SUPER_ADMIN`, `HOSPITAL_ADMIN` |
| `PLATFORM_OVERVIEW_READ` | `PLATFORM` | `platform` | `PLATFORM_SUPER_ADMIN` |
| `PLATFORM_HOSPITALS_MANAGE` | `PLATFORM` | `platform` | `PLATFORM_SUPER_ADMIN` |
| `PLATFORM_AUDIT_READ` | `PLATFORM` | `platform` | `PLATFORM_SUPER_ADMIN` |
| `HOSPITAL_WORKSPACE_READ` | `HOSPITAL` | `hospital` | `HOSPITAL_ADMIN` |
| `HOSPITAL_USERS_MANAGE` | `HOSPITAL` | `users` | `HOSPITAL_ADMIN` |
| `HOSPITAL_ROLES_READ` | `HOSPITAL` | `roles` | `HOSPITAL_ADMIN` |
| `HOSPITAL_SETTINGS_MANAGE` | `HOSPITAL` | `hospital` | `HOSPITAL_ADMIN` |

## 4. Frontend Authorization & UI Gating
- **Auth & Permission Constants:** Defined in [permissions.constants.ts](file:///F:/Engineering/CareOS/frontend/src/app/core/constants/permissions.constants.ts) matching backend `PermissionCode` / `SystemRoleCode`.
- **Route Guards ([auth.guard.ts](file:///F:/Engineering/CareOS/frontend/src/app/core/guards/auth.guard.ts)):**
  - `authGuard`: Prevents unauthenticated access to the shell, redirecting to `/login?returnUrl=...`.
  - `guestGuard`: Redirects already-authenticated users from `/login` to `/dashboard`.
  - `permissionGuard`: Validates `route.data.requiredPermissions` and optional `requiredScope` via `AuthStateService.canAccess(...)`, routing unauthorized attempts to `/unauthorized`.
- **Navigation Filtering:** `AppShell.navGroups` dynamically filters sidebar items by current user permissions and scope so users only see accessible areas (UI hiding is strictly a UX layer; backend `@PreAuthorize` remains authoritative).
