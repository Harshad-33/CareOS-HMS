# CareOS-HMS — API Standards & Versioning

## 1. Versioning Strategy

- All REST APIs are exposed under `/api/v1/` (centralized in `ApiConstants.API_V1_BASE_PATH`).
- Breaking contract changes in future releases must introduce `/api/v2/` rather than breaking existing `/api/v1/` consumers.

## 2. Endpoint Naming & HTTP Methods

- Use lowercase, plural kebab-case resource nouns: `/api/v1/patients`, `/api/v1/lab-orders`.
- Avoid verbs in URI paths except for clear sub-resource state transitions (e.g., `POST /api/v1/admissions/{id}/discharge`).

| Method | Semantics | Typical Status Codes |
|---|---|---|
| `GET` | Retrieve single resource or paginated collection | `200 OK`, `404 Not Found` |
| `POST` | Create a new resource or execute a non-idempotent action | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `PUT` | Full replacement update | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `PATCH` | Partial update / status transition | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `DELETE` | Soft-delete or removal | `200 OK` / `204 No Content`, `404 Not Found` |

## 3. Standard Response Envelopes

### Success Envelope (`ApiResponse<T>`)
Used for structured API payloads where envelope metadata adds clarity:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-11-10T18:30:00Z",
  "path": "/api/v1/example",
  "correlationId": "a1b2c3d4-..."
}
```

Lightweight operational probes such as `GET /api/v1/health` may return a direct DTO (`status`, `service`, `version`, `database`, `timestamp`) alongside standard HTTP status codes (`200 OK` / `503 Service Unavailable`).

### Error Envelope (`ErrorResponse`)
Returned consistently by `GlobalExceptionHandler` for all 4xx/5xx failures without internal stack traces or raw SQL:

```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/v1/example",
  "correlationId": "a1b2c3d4-...",
  "timestamp": "2026-11-10T18:30:00Z",
  "fieldErrors": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

## 4. Pagination, Sorting & Filtering Conventions

- Standard query parameters handled via `PaginationUtils`:
  - `page` (0-indexed, default `0`)
  - `size` (default `20`, maximum `100`)
  - `sortBy` (validated against an explicit allowlist per endpoint, default `id`)
  - `sortDirection` (`ASC` or `DESC`, default `ASC`)
  - Optional domain filters as explicit query parameters (e.g., `?status=ACTIVE&search=term`).
- Paginated responses wrap results using `PagedResponse<T>` (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`).

## 5. Correlation IDs & OpenAPI Documentation

- Clients may supply `X-Correlation-Id`; otherwise the backend generates a UUID and echoes `X-Correlation-Id` on every response and log line.
- Interactive OpenAPI documentation (enabled in `dev` profile, disabled by default in `prod`):
  - Swagger UI: `/swagger-ui.html`
  - OpenAPI JSON (`v1` group): `/v3/api-docs/v1`
