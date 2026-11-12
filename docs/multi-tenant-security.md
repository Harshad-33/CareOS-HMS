# CareOS-HMS Multi-Tenant Security Architecture (Phase 2)

## 1. Platform vs. Hospital Scope Isolation
CareOS-HMS enforces strict separation between **Platform-level operations** and **Hospital-tenant operations**:

| Aspect | `PLATFORM` Scope (`UserScope.PLATFORM`) | `HOSPITAL` Scope (`UserScope.HOSPITAL`) |
| :--- | :--- | :--- |
| **`users.hospital_id`** | Must be `NULL` (enforced by JPA `@PrePersist`/`@PreUpdate` and SQL check discipline) | Must reference an existing `hospitals.id` (`NOT NULL`) |
| **Role Scope (`roles.scope`)** | `PLATFORM` roles (e.g., `PLATFORM_SUPER_ADMIN`) | `HOSPITAL` roles (e.g., `HOSPITAL_ADMIN`, future clinical staff) |
| **Login Preconditions** | User must be active, unlocked, and not soft-deleted | User active/unlocked **AND** associated `Hospital` must not be deleted and must have `status = ACTIVE` |
| **Access Boundary** | Permitted only on platform-scoped endpoints | Permitted only on endpoints and entities owned by their server-resolved `hospital_id` |

## 2. Server-Resolved Tenant Authority (Anti-Spoofing)
- **Zero Trust on Client Headers/Bodies:** CareOS-HMS **never** trusts client-supplied `X-Hospital-Id` headers, query parameters, or request body `hospitalId` overrides for authorization decisions.
- Even though JWT claims embed `scope` and `hospital_id` for diagnostics/auditing, `JwtAuthenticationFilter` loads authoritative state via `CareOsUserDetailsService.loadPrincipalByPublicId(...)` from the database into `CareOsUserPrincipal` (`SecurityContextHolder`).
- `TenantContext` exposes the authenticated `CareOsUserPrincipal` and current `hospitalId`.

## 3. Enforcement Components
1. **Controller / Method Layer (`@PreAuthorize` + `TenantAccessGuard`):**
   - `@Component("tenantGuard")` ([TenantAccessGuard.java](file:///F:/Engineering/CareOS/backend/src/main/java/com/careos/hms/security/TenantAccessGuard.java)) provides SpEL-callable helpers:
     - `@tenantGuard.canAccessHospital(#hospitalId)` — returns `true` only if the principal is `HOSPITAL`-scoped and `principal.hospitalId.equals(targetHospitalId)`.
     - `@tenantGuard.canAccessPlatform()` — returns `true` only if the principal is `PLATFORM`-scoped with `hospitalId == null`.
2. **Service Layer (`TenantAccessGuard`):**
   - `tenantGuard.requireHospitalId()` extracts the mandatory `hospitalId` for hospital-scoped queries or throws `403 TENANT_CONTEXT_REQUIRED`.
   - `tenantGuard.assertSameHospital(targetHospitalId)` throws `403 CROSS_TENANT_ACCESS_DENIED` and logs cross-tenant access attempts without leaking sensitive entity details.
   - `tenantGuard.assertPlatformScope()` verifies `PLATFORM` scope or throws `403 PLATFORM_SCOPE_REQUIRED`.
3. **Repository / Database Layer:**
   - All tenant-owned entities extend `TenantAwareEntity` (indexed `hospital_id`).
   - Tenant-scoped queries explicitly filter by `:hospitalId` and `deleted = false` (e.g., `UserRepository.findByIdAndHospitalIdAndDeletedFalse` and `findAllByHospitalIdAndDeletedFalse`), ensuring even accidental ID tampering returns empty / 403 rather than cross-tenant records.
