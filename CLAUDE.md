# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

FlagMan is a feature flag management REST API plus a web dashboard (`dashboard/`). The API uses Spring Boot 4.0.x, Java 21, Maven (via the `./mvnw` wrapper), Spring Data JPA, and MySQL 8.4. It's a solo project developed on both macOS (Apple Silicon) and Windows/WSL2, so Docker setup must stay platform-neutral (no `platform:` overrides).

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

- The API listens on http://localhost:8081, the dashboard container on http://localhost:8080, and MySQL is exposed on host port **3307** (it listens on 3306 inside the Compose network).
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
- `database/DummyData` holds mock data (2 projects, 4 flags, configs for every environment, and 5 rules covering every operator). It is loaded only by `make seed`, never by migrations.
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
  - `POST /evaluate`: the body is `{projectId, flagKey, environment, userId, attributes}` and the response is `{enabled, reason}`. The order is: a disabled flag gives `FLAG_DISABLED`; then every rule must match (AND), else `RULES_NOT_MATCHED`; then the user is in if `CRC32(flagKey + ":" + userId) % 100 < rolloutPercentage` (`IN_ROLLOUT` / `OUT_OF_ROLLOUT`). Changing that formula reshuffles every user. A missing field or a bad environment gives 400; an unknown project, flag or config gives 404.
  - `GET/POST /projects/{projectId}/flags/{flagKey}/configs/{environment}/rules` (POST returns 201; `attribute` ≤100 chars, `value` ≤255 chars, `operator` one of `EvaluationService.OPERATORS` (case-insensitive, stored uppercase); else 400)
  - `DELETE /projects/{projectId}/flags/{flagKey}/configs/{environment}/rules/{ruleId}` (204; 404 if the rule isn't on that config)
  - `GET /featureFlags`
  - `GET /featureFlagConfigs`
  - `GET /flags/{featureId}/configs`
  - `GET /rules`
- Errors: `controller/ApiExceptionHandler` (extends `ResponseEntityExceptionHandler`) returns RFC 9457 problem details `{title, status, detail, instance}`. Services throw `ResponseStatusException` with a readable reason, which becomes `detail`. Unreadable JSON or a wrong field type gives 400 naming the field; a `DataIntegrityViolationException` (e.g. a duplicate-flag race) gives 409.
- CORS: `config/CorsConfig` allows the origins in `CORS_ALLOWED_ORIGINS` (comma-separated, default `http://localhost:5173`) for GET/POST/PATCH/DELETE. Set it in `.env`; Compose passes it to the app.

Known quirks in the current code:
- `Rule.operator` is a plain String, matched case-insensitively in `EvaluationService`: `EQUALS`, `NOT_EQUALS`, `IN` (comma-separated value), `CONTAINS`. An unknown operator or a missing attribute never matches. `EvaluationService.OPERATORS` and the `matches()` switch must change together.

## Dashboard

- `dashboard/`: Vite + React 19 + TypeScript, no router or UI library. Hash routes (`#/projects/{id}/flags/{key}`) live in `src/routes.ts`; pages are `ProjectsPage`, `ProjectPage`, `FlagPage`; the typed API client is `src/api.ts`, and `useLoad` is in `src/lib.ts`.
- It calls `/api/*`. `vite.config.ts` (dev, port 5173, target `FLAGMAN_API_URL` or `http://localhost:8081`) and `dashboard/nginx.conf` (Compose service `dashboard`, proxy to `app:8081`) strip `/api` and forward, so the dashboard needs no CORS. CORS is for other browser apps.
- `npm run build` type-checks (`tsc -b`) and builds; `npx oxlint src` lints.
- Look: palette `soot-pale-lime` and type pairing `instrument` (Instrument Sans + DM Mono), dealt by the `design-taste-frontend` skill's `start.mjs` and copied as tokens into `src/index.css`. Only its look and motion values are used; its website rules and finish gate don't apply to an admin app. The skill is installed per project in `.claude/skills/` (not committed).

