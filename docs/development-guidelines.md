# CareOS-HMS — Development & Testing Guidelines

## 1. General Engineering Standards

- **Strict Scope Discipline:** Implement only the active phase's approved requirements. Keep secrets out of source control (`.env` is git-ignored; update `.env.example` with placeholders only).
- **Constructor Injection:** Always use constructor injection in Spring components and `inject()` in Angular standalone classes.
- **No Sensitive Logging:** Never log passwords, JWT tokens, connection strings with credentials, or patient PHI.
- **Immutability & Type Safety:** Use Java `record` DTOs on the backend and TypeScript strict mode (`strict: true`, `strictTemplates: true`) on the frontend.

## 2. Backend Testing Strategy (`backend/src/test/`)

| Test Category | Annotation / Tooling | Purpose |
|---|---|---|
| Unit Tests | JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) | Test services, validators, pagination, and mapping logic in isolation |
| Controller / Web Slice | `@WebMvcTest` + `MockMvc` | Verify HTTP routing, DTO validation (`@Valid`), status codes, and `GlobalExceptionHandler` responses |
| Repository / Auditing | `@DataJpaTest` + H2 (`MODE=MySQL`, `test` profile) | Verify JPA entity mappings, queries, `BaseEntity` auditing, and soft-delete fields |
| Integration Tests | `@SpringBootTest` + `AutoConfigureMockMvc` | Verify full application context, filters, OpenAPI spec generation, and database readiness |

Run backend tests:

```bash
cd backend
mvn clean test
```

## 3. Frontend Testing Strategy (`frontend/src/app/`)

| Test Category | Tooling | Purpose |
|---|---|---|
| Component Tests | Angular `TestBed` + Vitest | Verify inputs, outputs, accessible templates, and Material integration |
| Service & Interceptor Tests | `provideHttpClientTesting()` + `HttpTestingController` | Verify API URL resolution, correlation headers, loading state tracking, and error normalization |
| Routing Tests | `provideRouter(routes)` + `Location` | Verify lazy-loaded feature navigation, redirects, and breadcrumb updates |

Run frontend tests and production build:

```bash
cd frontend
npx ng test --watch=false
npm run build
```

## 4. Database Migration Workflow

1. Create a new file `database/migrations/V<next>__<description>.sql`.
2. Test locally against MySQL 8 (`careos_hms`) by starting the backend (`mvn spring-boot:run`) and verifying `flyway_schema_history`.
3. Never modify an already committed `V*.sql` migration.

## 5. Git Workflow

- Create clear, atomic commits scoped to the task (`feat(...)`, `fix(...)`, `chore(...)`, `docs(...)`).
- Never commit `.env`, `node_modules/`, `target/`, `dist/`, or IDE files.
- Always run backend tests, frontend tests, and production builds before pushing to `main`.
