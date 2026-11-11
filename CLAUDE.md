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
make reset       # delete the DB volume (prompts); schema reloads on next `make up`
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

- `spring.jpa.hibernate.ddl-auto=none`, so Hibernate never touches the schema. The source of truth is `database/schema` (plain SQL with no extension).
- `docker/mysql/init/01-schema.sh` loads `database/schema` on first volume creation only, skipping its `CREATE DATABASE` line because the image creates the DB itself. Schema changes need `make reset`.
- `database/DummyData` is **not** loaded automatically. Load it only when the user asks.
- Data model: `projects` → `feature_flags` → `feature_flag_config` (per environment, with `enabled` and `rollout_percentage`) → `rules` (attribute/operator/value targeting).

## Code architecture

The package is `com.devjuice.flagman`, with a conventional layered structure: `controller` → `service` → `repository` (Spring Data `JpaRepository`) → `entity`.

- Controllers are thin and use constructor injection. Most endpoints return JPA entities directly. Only `Project` creation uses request/response DTOs (`dto/project/request`, `dto/project/response`), which is the pattern being moved toward.
- Entities map `@ManyToOne` relationships up the hierarchy (`FeatureFlag.project`, `FeatureFlagConfig.featureFlag`, `Rule.featureFlagConfig`). Lookups by parent use derived queries such as `findByFeatureFlag(FeatureFlag)`.
- Current endpoints:
  - `GET/POST /projects`
  - `GET /projects/{projectId}/flags`
  - `GET /featureFlags`
  - `GET /featureFlagConfigs`
  - `GET /flags/{featureId}/configs`
  - `GET /rules`

Known quirks in the current code:
- `Project.createdOn` and the DTO's `createdOn` are `String`s, and inserts send `null`, which overrides the column's `DEFAULT CURRENT_TIMESTAMP`.
- `FeatureFlagConfig.enabled` is an `Integer` mapped to a `BOOLEAN` column.
- `Rule.ruleId` has no `@GeneratedValue`.
- `FeatureFlagConfigService.getFeatureFlagConfigById` uses a bare `orElseThrow()`, so an unknown flag ID returns a 500 instead of a 404.
