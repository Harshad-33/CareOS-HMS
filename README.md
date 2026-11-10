# CareOS-HMS

CareOS-HMS is a modular Hospital Management System built with Angular, Spring Boot, and MySQL 8.

## Technology Stack

- **Frontend:** Angular 21, TypeScript, Angular Material, npm
- **Backend:** Java 21, Spring Boot 3.5.x, Spring Web, Spring Data JPA / Hibernate, Bean Validation, Maven
- **Database & Migrations:** MySQL 8, Flyway
- **Containerization:** Docker, Docker Compose
- **Version Control:** Git & GitHub

## Project Structure

```text
CareOS/
├── frontend/               # Angular + Angular Material application
├── backend/                # Spring Boot Maven API service
├── database/
│   └── migrations/         # Flyway SQL migration scripts
├── docker/                 # Development Dockerfiles
├── docs/                   # Project & architecture documentation
├── .env.example            # Environment variable template
├── .gitignore
├── docker-compose.yml      # Local MySQL and full-stack container setup
└── README.md
```

## Prerequisites

- **Java:** JDK 21+
- **Maven:** Apache Maven 3.9+
- **Node.js & npm:** Node.js 24+ / npm 11+
- **Angular CLI:** `@angular/cli` 21+
- **Database:** MySQL 8.0+ (local installation or Docker)
- **Containerization (optional):** Docker & Docker Compose

## Environment Configuration

Copy `.env.example` to `.env` and update the credentials for your local environment:

```bash
cp .env.example .env
```

Never commit `.env` or real credentials to Git.

## MySQL Configuration

1. Ensure MySQL 8 is running (locally or via Docker Compose).
2. Create the database schema `careos_hms` if running MySQL outside Docker:

   ```sql
   CREATE DATABASE IF NOT EXISTS careos_hms
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_unicode_ci;
   ```

3. Set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` in `.env` (or export them as environment variables).
4. Flyway is enabled by default and reads migrations from `database/migrations/` and `classpath:db/migration`.

## Backend Setup (`backend/`)

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Verify the health endpoint once the server is running:

```bash
curl http://localhost:8080/api/v1/health
```

## Frontend Setup (`frontend/`)

```bash
cd frontend
npm install
npm start
```

The Angular development server runs at `http://localhost:4200`.

## Docker Setup

Start only MySQL 8 for local development (recommended when running backend and frontend natively):

```bash
docker compose up -d mysql
```

Or run the full stack (MySQL, backend, and frontend) in containers:

```bash
docker compose --profile full up --build
```

Stop containers:

```bash
docker compose down
```

## GitHub Repository

- **Repository URL:** https://github.com/Harshad-33/CareOS-HMS.git
- **Default Branch:** `main`
