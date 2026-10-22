# CyclingApp

Cycling training application.

## Repository layout

| Directory   | Description                                   |
|-------------|-----------------------------------------------|
| `backend/`  | REST API — Java 25, Spring Boot 4, Maven      |
| `frontend/` | Web client — Angular (not created yet)        |

## Backend

### Requirements

- JDK 25
- Docker (local PostgreSQL and integration tests)

### Running locally

```bash
cd backend
./mvnw spring-boot:run
```

PostgreSQL is started automatically from `backend/compose.yaml` via Spring Boot Docker Compose support.
Health check: <http://localhost:8080/actuator/health>

### Tests

```bash
cd backend
./mvnw test
```

Integration tests start a throwaway PostgreSQL container with Testcontainers.

### Database migrations

Schema changes are Flyway migrations in `backend/src/main/resources/db/migration`
(`V<version>__<description>.sql`). Hibernate only validates the schema.

### Production profile

Run with `SPRING_PROFILES_ACTIVE=prod` and provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
