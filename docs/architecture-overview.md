# CareOS-HMS — Phase 0 Environment & Architecture Notes

## Repository Layout

- `frontend/` — Angular 21 + TypeScript + Angular Material SPA shell
- `backend/` — Spring Boot 3.5.x (Java 21, Maven) REST API
- `database/migrations/` — Flyway SQL migrations (versioned `V*.sql` scripts)
- `docker/` — Development Dockerfiles for backend and frontend
- `docs/` — Architecture and developer documentation

## Configuration Contract

All runtime secrets and environment-specific settings are supplied via environment variables (or a local git-ignored `.env` file modeled after `.env.example`).

| Variable | Purpose | Default |
|---|---|---|
| `APP_NAME` | Spring Boot application name | `careos-hms-backend` |
| `BACKEND_PORT` | Backend HTTP port | `8080` |
| `FRONTEND_PORT` | Frontend dev server port | `4200` |
| `DB_HOST` | MySQL hostname | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | MySQL schema name | `careos_hms` |
| `DB_USERNAME` | MySQL username | *(required)* |
| `DB_PASSWORD` | MySQL password | *(required)* |
| `DB_ROOT_PASSWORD` | MySQL root password (Docker Compose) | *(required for Compose)* |
| `DB_USE_SSL` | MySQL JDBC SSL toggle | `false` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins for `/api/**` | `http://localhost:4200` |
| `FLYWAY_ENABLED` | Enable/disable Flyway migrations | `true` |
| `FLYWAY_LOCATIONS` | Comma-separated Flyway locations | `classpath:db/migration,filesystem:../database/migrations` |

## Health Check Endpoint

`GET /api/v1/health`

Returns HTTP `200 OK` when the API and MySQL connection are healthy, or `503 Service Unavailable` when the database cannot be reached:

```json
{
  "status": "UP",
  "service": "careos-hms-backend",
  "version": "0.1.0-SNAPSHOT",
  "database": "UP",
  "timestamp": "2026-11-10T09:00:00Z"
}
```
