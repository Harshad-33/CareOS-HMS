# CareOS-HMS — System Architecture Overview

## 1. Architectural Style: Modular Monolith

CareOS-HMS is designed as a **modular monolith** with strict feature-based boundaries on both the Spring Boot backend (`com.careos.hms.<module>`) and the Angular frontend (`src/app/features/<module>`). This avoids premature microservice operational overhead while preserving clean domain boundaries, independent testability, and straightforward future extraction if scale ever demands it.

## 2. Runtime Topology

- **Frontend SPA (`frontend/`):** Angular 21 standalone components + Angular Material shell communicating over HTTP/JSON (`/api/v1/**`) with correlation IDs (`X-Correlation-Id`).
- **Backend API (`backend/`):** Spring Boot 3.5.x REST API running on Java 21, exposing versioned endpoints under `/api/v1/`, OpenAPI 3 documentation, Jakarta Bean Validation, centralized exception mapping, and JPA auditing.
- **Database (`database/migrations/`):** MySQL 8.0 (`careos_hms`) managed exclusively through versioned Flyway SQL migrations.

## 3. Environment & Configuration Strategy

| Profile | Config File | Purpose |
|---|---|---|
| Shared defaults | `application.yml` | Base datasource, Flyway, OpenAPI paths, CORS, default profile (`dev`) |
| Development | `application-dev.yml` | Formatted SQL diagnostics, `DEBUG` app logs, Swagger UI enabled |
| Production | `application-prod.yml` | Strict env-var MySQL SSL connection, bounded Hikari pool, `WARN`/`INFO` logs, Swagger UI disabled by default |
| Test | `src/test/resources/application-test.yml` | Isolated H2 MySQL-mode database for fast automated test suites |

All credentials and environment-specific values are injected via environment variables (documented in `.env.example`).

---

## 4. Future Multi-Hospital (Multi-Tenant) Architecture

CareOS-HMS is designed from Phase 1 to support multiple hospitals within the same platform deployment. Actual tenant authentication, authorization guards, and tenant administration UI are intentionally deferred to designated future phases.

### 4.1 Hospital Identification Strategy
- Every hospital organization will receive an immutable surrogate primary key (`hospital_id BIGINT`) and a unique external business code/slug (`hospital_code VARCHAR(32)`).
- Authenticated requests in future phases will carry hospital context inside the verified security principal / JWT claims (and/or explicit `X-Hospital-Id` header validated against user-hospital memberships), never trusted blindly from client-supplied request bodies.
- Frontend core services will store active hospital context centrally in `core/` so HTTP interceptors and navigation remain hospital-aware without rewriting feature views.

### 4.2 Tenant-Aware Database Relationships
- Operational tables (patients, appointments, employees, doctors, admissions, clinical records, labs, pharmacy, inventory, billing, reports) will carry a non-null `hospital_id BIGINT` foreign key referencing `hospitals(id)`.
- [`TenantAwareEntity`](../backend/src/main/java/com/careos/hms/common/audit/TenantAwareEntity.java) establishes the reusable `@MappedSuperclass` contract (`hospital_id` + soft delete + auditing columns) so future domain entities inherit tenant ownership consistently.
- Composite indexes across all tenant-scoped tables must lead with `hospital_id` (e.g., `idx_patients_hospital_mrn (hospital_id, mrn)`) to keep tenant queries fast and prevent cross-tenant uniqueness collisions.

### 4.3 Hospital-Level Data Ownership
- Every clinical and operational record is owned by exactly one hospital (`hospital_id`).
- Uniqueness constraints (such as Medical Record Numbers, invoice numbers, or employee codes) will be scoped per hospital (`UNIQUE (hospital_id, code)`) rather than globally unless explicitly platform-wide.
- Soft-deletes, audit trails (`created_by`, `updated_by`, `created_at`, `updated_at`), and reporting aggregations respect `hospital_id` ownership boundaries.

### 4.4 Future Tenant Isolation Strategy
- **Discriminator / Shared-Schema Isolation:** Shared MySQL database (`careos_hms`) with mandatory `hospital_id` discriminator columns and indexed tenant predicates.
- **Service & Repository Enforcement:** In future security phases, a `TenantContext` populated from the authenticated security context will feed repository queries / Hibernate `@Filter` or Spring Data specifications, preventing cross-hospital data leakage even if a controller omits a parameter.
- **No Trust in Client Tenant IDs:** Path/body IDs will always be verified against the caller's authorized `hospital_id` in the service layer.

### 4.5 Platform-Level vs. Hospital-Level Responsibilities
| Layer | Scope | Examples |
|---|---|---|
| **Platform-Level** | Global / Cross-tenant administration | Hospital onboarding & lifecycle, global subscription/feature flags, platform super-admin accounts, shared reference catalogs (optional), system health & observability |
| **Hospital-Level** | Tenant-scoped (`hospital_id`) | Hospital profile & branches, hospital users/staff/doctors, RBAC role assignments within the hospital, patients, appointments, admissions, EMR/clinical, laboratory, pharmacy, inventory, billing, hospital reports |
