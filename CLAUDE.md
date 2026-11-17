# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

FlagMan is a feature flag management REST API with no frontend. It uses Spring Boot 4.0.x, Java 21, Maven (via the `./mvnw` wrapper), Spring Data JPA, and MySQL 8.4. It's a solo project developed on both macOS (Apple Silicon) and Windows/WSL2, so Docker setup must stay platform-neutral (no `platform:` overrides).

## Commands

Local dev runs through Docker Compose and the Makefile. Copy `.env.example` to `.env` first.

```sh
make up          # build app image, start mysql + app, wait for health
make db          # start only MySQL (to run the app from the IDE / ./mvnw)
make down        # stop containers, keep data
make logs        # follow logs
make ps          # status
make mysql       # MySQL shell as the app user
make seed        # load mock data (database/DummyData) into a fresh DB
make reset       # delete the DB volume (prompts); tables recreated by Flyway on next `make up`
```

- The API listens on http://localhost:8081, and MySQL is exposed on host port **3307** (it listens on 3306 inside the Compose network).
- `make up` only waits for the app container to be running. There is no app healthcheck or Actuator, and Spring takes about 6s more before requests succeed.
- Code changes need `make up` again to rebuild the app image.

Running on the host (requires JDK 21, which is not installed in the WSL environment):

```sh
./mvnw spring-boot:run
./mvnw test
./mvnw test -Dtest=FlagManApplicationTests#contextLoads
```

The only test is a `@SpringBootTest` context-load test. It needs a reachable MySQL (`make db`) and `.env`, because it boots the full JPA context.

## Configuration

- `application.properties` reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. Its defaults target the Compose MySQL on `localhost:3307`.
- `spring.config.import=optional:file:.env[.properties]` makes host runs pick up `.env` from the project root. In Compose, real env vars override it, with `DB_HOST=mysql` and `DB_PORT=3306`.
- The app connects as the non-root `flagman` user, which the MySQL container creates from `.env`.

## Database

- The schema is managed by **Flyway**. Migrations live in `src/main/resources/db/migration/` (`V1__initial_schema.sql`, `V2__...`) and run on app startup. `spring.jpa.hibernate.ddl-auto=none`, so Hibernate never touches the schema.
- **Never edit an applied migration** (it causes a checksum failure on startup). Schema changes are a new `V<n+1>__description.sql`.
- `database/DummyData` holds mock data (2 projects, 4 flags, configs for every environment, a few rules). It is loaded only by `make seed`, never by migrations.
- Constraints: `flag_key` is unique per project, `(flag_id, environment)` is unique, and `environment` is limited to `DEV`, `STAGING`, `PREPROD`, `PROD` (the `Environment` enum, stored with `@Enumerated(STRING)`).
- Data model: `projects` → `feature_flags` → `feature_flag_config` (per environment, with `enabled` and `rollout_percentage`) → `rules` (attribute/operator/value targeting).

## Code architecture

The package is `com.devjuice.flagman`, with a conventional layered structure: `controller` → `service` → `repository` (Spring Data `JpaRepository`) → `entity`.

- Controllers are thin and use constructor injection. Project and `/projects/{projectId}/flags` endpoints use DTOs (`dto/<feature>/request`, `dto/<feature>/response`), built in the service by a private `toResponse(entity)` helper. A missing project throws `ResponseStatusException(NOT_FOUND)`. `/featureFlags`, the config and rule endpoints still return entities.
- Entities map `@ManyToOne` relationships up the hierarchy (`FeatureFlag.project`, `FeatureFlagConfig.featureFlag`, `Rule.featureFlagConfig`). Lookups by parent use derived queries such as `findByFeatureFlag(FeatureFlag)`.
- Current endpoints:
  - `GET/POST /projects`, `GET /projects/{projectId}`
  - `GET/POST /projects/{projectId}/flags` (POST returns 201, 409 on duplicate `flagKey`, and auto-creates a disabled 0% config per environment)
  - `GET/PATCH /projects/{projectId}/flags/{flagKey}/configs/{environment}` (PATCH changes only the fields sent; `rolloutPercentage` must be 0–100, else 400; environment must be uppercase, else 400)
  - `DELETE /projects/{projectId}/flags/{flagKey}` (204; deletes its rules and configs first, in one transaction)
  - `GET /featureFlags`
  - `GET /featureFlagConfigs`
  - `GET /flags/{featureId}/configs`
  - `GET /rules`

Known quirks in the current code:
- `Rule.ruleId` has no `@GeneratedValue`.
- `FeatureFlagConfigService.getFeatureFlagConfigById` uses a bare `orElseThrow()`, so an unknown ID returns a 500 instead of a 404.
