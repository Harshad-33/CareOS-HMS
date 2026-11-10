# CareOS-HMS — Backend Architecture

## 1. Modular Monolith Package Layout

```text
com.careos.hms/
├── CareOsHmsApplication.java
├── config/
│   ├── JpaAuditingConfig.java       # @EnableJpaAuditing + AuditorAware ("SYSTEM" placeholder until Phase 2)
│   ├── OpenApiConfig.java           # SpringDoc OpenAPI / Swagger UI v1 group
│   ├── RequestLoggingFilter.java    # X-Correlation-Id MDC + structured request timing logs
│   └── WebConfig.java               # CORS policy under /api/v1/**
├── common/
│   ├── audit/
│   │   ├── BaseEntity.java          # id, createdAt, updatedAt, createdBy, updatedBy
│   │   ├── SoftDeletableEntity.java # deleted, deletedAt, deletedBy
│   │   └── TenantAwareEntity.java   # hospitalId tenant ownership contract
│   ├── constants/
│   │   └── ApiConstants.java        # /api/v1 base path, headers, pagination limits
│   ├── dto/
│   │   ├── ErrorResponse.java       # Standardized error envelope
│   │   ├── PagedResponse.java       # Standardized page metadata wrapper
│   │   └── ValidationErrorDetail.java
│   ├── exception/
│   │   ├── CareOsException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── DuplicateResourceException.java
│   │   ├── InvalidRequestException.java
│   │   └── GlobalExceptionHandler.java
│   ├── response/
│   │   └── ApiResponse.java         # Standard success response envelope
│   └── validation/
│       ├── PaginationUtils.java
│       ├── TrimmedNotBlank.java
│       └── TrimmedNotBlankValidator.java
├── health/                          # System & database readiness (/api/v1/health)
└── [future domain modules]          # Documented boundaries via package-info.java:
    hospital, user, authentication, role, permission, patient,
    appointment, employee, doctor, admission, clinical, laboratory,
    pharmacy, inventory, billing, reporting
```

## 2. Layering Rules

Each domain module follows strict separation of concerns using constructor-based dependency injection:

1. **Controller (`controller/`):** Handles HTTP routing, request DTO `@Valid` validation, and HTTP status mapping. Contains zero business logic or direct repository access.
2. **Service (`service/`):** Owns business rules, `@Transactional` boundaries (`readOnly = true` for queries), tenant ownership checks, and domain exceptions.
3. **Repository (`repository/`):** Spring Data JPA interfaces with explicit query methods and pagination support.
4. **Entity (`entity/` or `domain/`):** JPA entities extending `BaseEntity`, `SoftDeletableEntity`, or `TenantAwareEntity`. Never exposed directly as API payloads.
5. **DTO (`dto/`):** Immutable Java `record` types for requests and responses.
6. **Mapper (`mapper/`):** Dedicated mapping components converting between Entities and DTOs.
7. **Validator (`validator/`):** Custom domain validation rules complementing Jakarta Bean Validation annotations.

## 3. Exception Handling & Logging Standards

- `GlobalExceptionHandler` maps domain, validation (`MethodArgumentNotValidException`, `ConstraintViolationException`), HTTP payload/parameter errors, and database exceptions (`DataIntegrityViolationException`, `DataAccessException`) to [`ErrorResponse`](../backend/src/main/java/com/careos/hms/common/dto/ErrorResponse.java) without leaking SQL statements, internal class names, or stack traces.
- `logback-spring.xml` includes `[corrId=%X{correlationId:-SYSTEM}]` on every log line via `RequestLoggingFilter` (`X-Correlation-Id` header).
- Passwords, JWT tokens, credentials, and sensitive patient clinical data must never be logged.

## 4. Adding a New Backend Module (Future Phases)

1. Create subpackages inside `com.careos.hms.<module>`: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `validator`.
2. Add a Flyway SQL migration `V<n>__create_<module>_tables.sql` in `database/migrations/` (never modify already-applied migrations).
3. Extend `TenantAwareEntity` for hospital-scoped tables or `SoftDeletableEntity`/`BaseEntity` for platform-level tables.
4. Annotate controllers with `@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/<resources>")` and SpringDoc `@Tag` / `@Operation`.
5. Add unit tests for service/validator, `@WebMvcTest` for controller, and `@DataJpaTest` for repository queries.
