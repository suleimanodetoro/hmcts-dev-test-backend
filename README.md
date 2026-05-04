# Tasks backend

A small Spring Boot service for caseworker task management. It exposes a JSON API for creating, listing, retrieving, status-updating and deleting tasks, persisted in PostgreSQL.

## Endpoints

| Method | Path                  | Description                          |
|--------|-----------------------|--------------------------------------|
| POST   | `/tasks`              | Create a task                        |
| GET    | `/tasks`              | List all tasks (ordered by due date) |
| GET    | `/tasks/{id}`         | Retrieve a task by id                |
| PATCH  | `/tasks/{id}/status`  | Update the status of a task          |
| DELETE | `/tasks/{id}`         | Delete a task                        |

OpenAPI documentation is served at:

- JSON: `http://localhost:4000/v3/api-docs`
- Swagger UI: `http://localhost:4000/swagger-ui.html`

A health endpoint is available at `http://localhost:4000/health`.

## Prerequisites

- JDK 17 or newer (Gradle is pinned to a Java 17 toolchain)
- Docker (for the local PostgreSQL container)

## Run the database

```sh
docker compose up -d
```

This starts PostgreSQL on `localhost:5433` with database, user and password all set to `tasks`. Host port `5433` is used in preference to `5432` so that the container does not clash with a developer machine that already runs PostgreSQL locally. Flyway applies the schema migration automatically when the application starts.

To stop and remove the volume:

```sh
docker compose down -v
```

## Run the service

```sh
./gradlew bootRun
```

The service listens on port `4000` by default. Override the port with `SERVER_PORT`. Database connection details can be overridden via `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER_NAME`, `DB_PASSWORD` and `DB_OPTIONS`.

Build a runnable jar:

```sh
./gradlew bootJar
java -jar build/libs/test-backend.jar
```

## Run the tests

Unit and web layer tests:

```sh
./gradlew test
```

Integration tests (run against an in-memory H2 database in PostgreSQL mode):

```sh
./gradlew integration
```

The full check (tests, integration tests, checkstyle):

```sh
./gradlew check
```

## Project layout

- `controllers/`: REST controllers
- `services/`: transactional business logic
- `repositories/`: Spring Data JPA repositories
- `models/`: JPA entities and the `TaskStatus` enum
- `dto/`: request and response payloads
- `exceptions/`: domain exceptions and the global exception handler
- `db/migration/`: Flyway SQL migrations

## Assumptions

- Statuses are limited to `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. Any status can transition to any other status; callers decide the workflow.
- `dueDateTime` is required and stored as a local timestamp; the service is intended to run in a single timezone.
- `title` is required, capped at 200 characters; `description` is optional, capped at 2000 characters.
- The service is currently unauthenticated. In production it would sit behind the existing HMCTS authentication and authorisation patterns.
