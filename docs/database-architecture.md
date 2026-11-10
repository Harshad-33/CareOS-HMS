# CareOS-HMS — Database Architecture & Standards

## 1. Engine & Migration Foundation

- **DBMS:** MySQL 8.0 (`careos_hms`)
- **Character Set & Collation:** `utf8mb4` / `utf8mb4_unicode_ci`
- **ORM:** Spring Data JPA / Hibernate (`spring.jpa.hibernate.ddl-auto=validate` in `dev` and `prod`)
- **Migrations:** Flyway (`database/migrations/`), ensuring schema changes are strictly version-controlled and reproducible.
- **No Hibernate Auto-DDL in Non-Test Environments:** Hibernate never alters production/dev tables; all DDL runs through Flyway.

## 2. Naming Conventions

| Object | Standard | Example |
|---|---|---|
| Tables | Lowercase `snake_case`, plural nouns | `hospitals`, `patients`, `lab_orders` |
| Columns | Lowercase `snake_case` | `hospital_id`, `created_at`, `is_deleted` |
| Primary Keys | `id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` (`BIGINT` in JPA `Long`) | `id` |
| Foreign Keys | `fk_<child_table>_<parent_table>[_<role>]` | `fk_patients_hospitals` |
| Indexes | `idx_<table>_<col1>_<col2>` | `idx_patients_hospital_last_name` |
| Unique Constraints | `uk_<table>_<col1>_<col2>` | `uk_patients_hospital_mrn` |
| Boolean Columns | `TINYINT(1)` / `BOOLEAN` prefixed with `is_` or `has_` | `is_deleted`, `is_active` |

## 3. Auditing & Base Entity Standards

All tables include standard audit columns managed through Spring Data JPA auditing (`JpaAuditingConfig`) and [`BaseEntity`](../backend/src/main/java/com/careos/hms/common/audit/BaseEntity.java):

| Column | SQL Type | Java Field / Type | Notes |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT PRIMARY KEY` | `Long id` | Surrogate primary key |
| `created_at` | `DATETIME(6) NOT NULL` | `Instant createdAt` | Stored in UTC (`hibernate.jdbc.time_zone=UTC`) |
| `updated_at` | `DATETIME(6) NOT NULL` | `Instant updatedAt` | Automatically updated on modification |
| `created_by` | `VARCHAR(100) NOT NULL` | `String createdBy` | `"SYSTEM"` placeholder in Phase 1; authenticated user identity from Phase 2 |
| `updated_by` | `VARCHAR(100) NOT NULL` | `String updatedBy` | Updated automatically on save |

## 4. Soft-Delete Guidelines

Entities inheriting [`SoftDeletableEntity`](../backend/src/main/java/com/careos/hms/common/audit/SoftDeletableEntity.java) maintain historical clinical/financial traceability instead of physical `DELETE`:

- Columns: `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`, `deleted_at DATETIME(6) NULL`, `deleted_by VARCHAR(100) NULL`.
- Clinical records, invoices, prescriptions, and audit logs must never be hard-deleted.
- Unique constraints on soft-deletable entities must consider soft-delete semantics (e.g., nullable archive token or explicit active-record checks) before enforcing strict unique indexes.

## 5. Multi-Hospital Relationship Standards

Entities inheriting [`TenantAwareEntity`](../backend/src/main/java/com/careos/hms/common/audit/TenantAwareEntity.java) carry:

- `hospital_id BIGINT NOT NULL` with a foreign key `fk_<table>_hospitals` to `hospitals(id)` (once the `hospitals` table is introduced in its designated phase).
- Every query index on tenant-owned tables leads with `hospital_id` so multi-hospital isolation remains performant.

## 6. Transaction Management Guidelines

- Annotate service-layer query methods with `@Transactional(readOnly = true)` to reduce lock contention and prevent dirty-checking overhead.
- Annotate state-mutating service methods with `@Transactional` (rolling back on unchecked exceptions by default).
- Never open database transactions inside controllers.

## 7. Flyway Migration Naming Conventions

- Versioned migrations: `V<number>__<short_snake_case_description>.sql` (double underscore `__`), e.g., `V1__init_baseline.sql`, `V2__create_hospitals_table.sql`.
- Never edit or delete a migration script once it has been applied to any shared environment or committed to `main`. Always add a new sequential `V<n+1>__...sql` migration.
