# CareOS-HMS

CareOS-HMS is a modular Hospital Management System built with Angular 21, Spring Boot 3.5.x (Java 21), and MySQL 8.

## Technology Stack

- **Frontend:** Angular 21, TypeScript (strict mode), Angular Material 21, Vitest, npm
- **Backend:** Java 21, Spring Boot 3.5.x, Spring Security 6, JJWT (HS256), Spring Web, Spring Data JPA / Hibernate 6, Bean Validation, SpringDoc OpenAPI (Swagger UI), Maven
- **Database & Migrations:** MySQL 8, Flyway
- **Containerization:** Docker, Docker Compose
- **Version Control:** Git & GitHub

## Project Structure

```text
CareOS/
├── frontend/               # Modular Angular + Angular Material application
├── backend/                # Modular monolith Spring Boot Maven API service
├── database/
│   └── migrations/         # Flyway SQL migration scripts (V*.sql)
├── docker/                 # Development Dockerfiles
├── docs/                   # Architecture, API, DB, and engineering guidelines
├── .env.example            # Environment variable template
├── .dockerignore
├── .gitignore
├── docker-compose.yml      # Isolated MySQL and full-stack container setup
└── README.md
```

## Documentation Index

- [Architecture Overview & Multi-Hospital Strategy](docs/architecture-overview.md)
- [Backend Architecture](docs/backend-architecture.md)
- [Frontend Architecture](docs/frontend-architecture.md)
- [Database Architecture & Auditing Standards](docs/database-architecture.md)
- [API Standards & Versioning](docs/api-standards.md)
- [Development & Testing Guidelines](docs/development-guidelines.md)
- [Authentication Architecture](docs/authentication-architecture.md)
- [Security Architecture](docs/security-architecture.md)
- [Authorization & RBAC](docs/authorization-and-rbac.md)
- [Multi-Tenant Security](docs/multi-tenant-security.md)
- [Authentication API Reference](docs/authentication-api.md)

## Prerequisites

- **Java:** JDK 21+
- **Maven:** Apache Maven 3.9+
- **Node.js & npm:** Node.js 24+ / npm 11+
- **Angular CLI:** `@angular/cli` 21+
- **Database:** MySQL 8.0+ (local installation or Docker)
- **Containerization (optional):** Docker & Docker Compose

## Environment Configuration

Copy `.env.example` to `.env` and set local credentials:

```bash
cp .env.example .env
```

Never commit `.env` or real credentials to Git. Spring Boot reads environment variables from the OS environment and optional `.env` files, with profile-specific overrides in `application-dev.yml` and `application-prod.yml`.

## Backend Setup (`backend/`)

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Useful local endpoints (default `dev` profile):
- **Health Check:** `http://localhost:8080/api/v1/health`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec:** `http://localhost:8080/v3/api-docs/v1`

## Frontend Setup (`frontend/`)

```bash
cd frontend
npm install
npm test -- --watch=false
npm run build
npm start
```

The Angular development server runs at `http://localhost:4200`.

## Docker Setup

Docker Compose uses dedicated default host ports (`3308` for MySQL, `8081` for Backend, `4201` for Frontend) bound to `127.0.0.1` so it coexists safely alongside local `MySQL80` (`3306`) or other stacks.

Start only MySQL 8 for local native development:

```bash
docker compose up -d mysql
```

Or run the full containerized stack (MySQL, backend, and frontend):

```bash
docker compose --profile full up -d --build
```

When running the Dockerized full stack:
- **Frontend:** `http://localhost:4201`
- **Backend Health:** `http://localhost:8081/api/v1/health`
- **Swagger UI:** `http://localhost:8081/swagger-ui.html`

Stop CareOS-HMS containers:

```bash
docker compose --profile full down
```

## GitHub Repository

- **Repository URL:** https://github.com/Harshad-33/CareOS-HMS.git
- **Default Branch:** `main`
