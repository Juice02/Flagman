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

There are three tests. `SecurityPiecesTest` covers `ClientIp`, `RateLimiter` and the sandbox rule allowlist without a database. `FlagManApplicationTests` is a `@SpringBootTest` context-load test; it needs a reachable MySQL (`make db`) and `.env`, because it boots the full JPA context. `EvaluatorTest` runs `src/test/resources/evaluation-cases.json` against `Evaluator` and needs no database: `./mvnw test -Dtest=EvaluatorTest`. Without JDK 21 on the host, run it in the build image: `docker run --rm -v "$PWD":/src -v flagman-m2:/root/.m2 -w /src eclipse-temurin:21.0.13_9-jdk-noble ./mvnw -B -q test -Dtest=EvaluatorTest`.

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
  - `GET/POST /projects/{projectId}/flags/{flagKey}/configs/{environment}/rules` (POST returns 201; `attribute` ≤100 chars, `value` ≤255 chars, `operator` one of `Evaluator.OPERATORS` (case-insensitive, stored uppercase); else 400)
  - `DELETE /projects/{projectId}/flags/{flagKey}/configs/{environment}/rules/{ruleId}` (204; 404 if the rule isn't on that config)
  - `GET /featureFlags`
  - `GET /featureFlagConfigs`
  - `GET /flags/{featureId}/configs`
  - `GET /rules`
  - `GET /projects/{projectId}/environments/{environment}/snapshot`: every flag in that environment as `FlagState` (`flagKey, enabled, rolloutPercentage, rules`), plus the project's `revision`. 404 for an unknown project.
  - `GET /projects/{projectId}/environments/{environment}/stream`: the same snapshot over Server-Sent Events (`SseEmitter`), on connect and after every committed change, with a `: ping` comment every 25 s. `?revisionOnly=true` sends a `change` event with only `{projectId, environment, revision}` instead (StreamHub keeps those emitters in `revisionOnly`).
- Evaluation: `service/Evaluator` is pure (no database) and is the single implementation; `EvaluationService` only loads the config and rules and calls it. `EvaluatorTest` runs `evaluation-cases.json`; a change to the evaluation order, an operator or the bucket formula needs a new case. `POST /evaluate/flags` (`evaluateFlags`) runs it for up to 20 flags for one user; flagman-demo's devices use it, so they no longer evaluate in the browser.
- Live updates (`stream/`): every write to a flag, config or rule is `@Transactional` and calls `FlagChangeNotifier.changed(projectId)`, which bumps `projects.revision` (V4 migration) and publishes `FlagsChanged`. `StreamHub` listens with `@TransactionalEventListener(AFTER_COMMIT)`, builds one snapshot per subscribed environment with `SnapshotService` (three queries, `REQUIRES_NEW` because the original transaction has finished) and sends it to every open emitter. A new write path must call the notifier, or streams miss the change. Open emitters live in memory, so this only works with one app instance.
- Errors: `controller/ApiExceptionHandler` (extends `ResponseEntityExceptionHandler`) returns RFC 9457 problem details `{title, status, detail, instance}`. Services throw `ResponseStatusException` with a readable reason, which becomes `detail`. Unreadable JSON or a wrong field type gives 400 naming the field; a `DataIntegrityViolationException` (e.g. a duplicate-flag race) gives 409.
- Access control (`security/`): `AccessInterceptor` runs before every request. Changes need `X-Admin-Key` = `FLAGMAN_ADMIN_KEY` (constant-time compare; 10 wrong keys per IP lock admin requests for 15 min), except the three sandbox routes (config PATCH, rule POST/DELETE) with an `X-Sandbox-Token` for that sandbox's own project, `flagman.sandbox.environment` and `flagman.sandbox.flags`; `RuleController.createRule` then checks the rule against `flagman.sandbox.allowed-rules`. Reads, `/evaluate`, `/sandboxes` and streams need no key; `/admin/**` always needs it. An empty `FLAGMAN_ADMIN_KEY` leaves changes open (local dev); a set one must be ≥32 chars. A new write endpoint is denied to tokens by default; add it to `SANDBOX_ROUTES` only if visitors should use it. Rate limits use `RateLimiter` (in memory, per IP from `ClientIp`, which takes the Nth `X-Forwarded-For` entry from the right for `FLAGMAN_TRUSTED_PROXIES` = N).
- Sandboxes (`sandbox/`, V5 `sandboxes` table): `POST /sandboxes` creates a project with the configured flags and returns a token once; only its SHA-256 is stored. `SandboxService.deleteIdle` (hourly) deletes sandboxes idle longer than `flagman.sandbox.idle-timeout`, and only rows listed in `sandboxes`. Writes and stream connects call `touch` (at most one update an hour). `GET /projects` hides sandboxes. `StreamHub` caps open streams per IP (`flagman.max-streams-per-ip`) and in total (500).
- `spring.jpa.open-in-view=false` on purpose: with it on, each open stream held a database connection and ten streams exhausted the pool. Load what a response needs inside a service.
- CORS: `config/CorsConfig` allows the origins in `CORS_ALLOWED_ORIGINS` (comma-separated, default `http://localhost:5173`) for GET/POST/PATCH/DELETE. Set it in `.env`; Compose passes it to the app.

Known quirks in the current code:
- `Rule.operator` is a plain String, matched case-insensitively in `Evaluator`: `EQUALS`, `NOT_EQUALS`, `IN` (comma-separated value, each part trimmed), `CONTAINS`. An unknown operator or a missing attribute never matches. `Evaluator.OPERATORS` and the `matches()` switch must change together.

## Dashboard

- `dashboard/`: Vite + React 19 + TypeScript, no router or UI library. Hash routes (`#/projects/{id}/flags/{key}`) live in `src/routes.ts`; pages are `ProjectsPage`, `ProjectPage`, `FlagPage`; the typed API client is `src/api.ts`, and `useLoad` is in `src/lib.ts`.
- It calls `/api/*`. `vite.config.ts` (dev, port 5173, target `FLAGMAN_API_URL` or `http://localhost:8081`) and `dashboard/nginx.conf` (Compose service `dashboard`, proxy to `app:8081`) strip `/api` and forward, so the dashboard needs no CORS. CORS is for other browser apps.
- `npm run build` type-checks (`tsc -b`) and builds; `npx oxlint src` lints.
- Look: palette `soot-pale-lime` and type pairing `instrument` (Instrument Sans + DM Mono), dealt by the `design-taste-frontend` skill's `start.mjs` and copied as tokens into `src/index.css`. Only its look and motion values are used; its website rules and finish gate don't apply to an admin app. The skill is installed per project in `.claude/skills/` (not committed).

