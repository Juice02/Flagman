# FlagMan

A self-hosted feature flag service: a REST API (Spring Boot 4, Java 21, MySQL 8.4) and a web dashboard
(React, TypeScript, Vite) for managing projects, flags, environment configs and rules.

Shipping a feature usually means deploying it to everyone at once. Feature flags separate the
deploy from the release: code ships switched off, then gets turned on per environment, for users
who match targeting rules, or for a percentage of users, without a redeploy.

FlagMan stores flags per project, keeps a separate config for each environment (`DEV`, `STAGING`,
`PREPROD`, `PROD`), and answers one question for an app: *is this flag on for this user?*

## How evaluation works

`POST /evaluate` checks three things in order and stops at the first that fails:

1. **Enabled:** if the flag's config for that environment is off → `FLAG_DISABLED`.
2. **Rules:** every rule on the config must match the user's attributes (AND) → else `RULES_NOT_MATCHED`.
   A missing attribute or unknown operator never matches.
3. **Rollout:** the user is in if `CRC32(flagKey + ":" + userId) % 100 < rolloutPercentage`
   → `IN_ROLLOUT` or `OUT_OF_ROLLOUT`.

The rollout is deterministic: the same user always gets the same answer for a flag, and raising the
percentage only adds users. Including the flag key means each flag picks a different set of users.

```sh
curl -X POST localhost:8081/evaluate -H 'Content-Type: application/json' -d '{
  "projectId": 6767, "flagKey": "hide-non-fiction", "environment": "PROD",
  "userId": "u-1", "attributes": {"author": "Tejas", "country": "IN"}
}'
# {"enabled":true,"reason":"IN_ROLLOUT"}
```

## API

Base URL: `http://localhost:8081`. Request and response bodies are JSON. Environment names in paths
and bodies are uppercase.

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/projects` | List projects |
| `POST` | `/projects` | Create a project |
| `GET` | `/projects/{projectId}` | One project |
| `GET` | `/projects/{projectId}/flags` | Flags in a project |
| `POST` | `/projects/{projectId}/flags` | Create a flag (`flagKey` required, unique per project). Also creates a disabled 0% config for every environment. 201, or 409 if the key exists |
| `DELETE` | `/projects/{projectId}/flags/{flagKey}` | Delete a flag with its configs and rules. 204 |
| `GET` | `/projects/{projectId}/flags/{flagKey}/configs/{environment}` | A flag's config in one environment |
| `PATCH` | `/projects/{projectId}/flags/{flagKey}/configs/{environment}` | Change `enabled` and/or `rolloutPercentage` (0–100); fields not sent are kept |
| `GET` | `/projects/{projectId}/flags/{flagKey}/configs/{environment}/rules` | Rules on that config |
| `POST` | `/projects/{projectId}/flags/{flagKey}/configs/{environment}/rules` | Add a rule. 201 |
| `DELETE` | `/projects/{projectId}/flags/{flagKey}/configs/{environment}/rules/{ruleId}` | Remove a rule. 204 |
| `POST` | `/evaluate` | Evaluate a flag for a user (see above) |

A rule compares one user attribute with a value:

```json
{"attribute": "country", "operator": "IN", "value": "IN,US,GB"}
```

| Operator | Matches when the attribute… |
|----------|-----------------------------|
| `EQUALS` | equals the value |
| `NOT_EQUALS` | differs from the value |
| `IN` | equals one of the comma-separated values |
| `CONTAINS` | contains the value (e.g. `@company.com` in an email) |

Comparisons are case-sensitive; the operator name is not. `attribute` is up to 100 characters,
`value` up to 255.

### Errors

Errors use the [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem detail format:

```json
{"title": "Not Found", "status": 404, "detail": "Project 999 not found", "instance": "/projects/999"}
```

`400` means the request is invalid (missing field, bad value, malformed JSON), `404` that a
project, flag, config or rule doesn't exist, and `409` that it conflicts with existing data.

### CORS

Browser apps can call the API only from the origins in `CORS_ALLOWED_ORIGINS` in `.env`
(comma-separated, no trailing slash; default `http://localhost:5173`). Run `make up` after changing it.

## Docker setup

Runs MySQL 8.4 and the app in Docker. Works the same on Apple Silicon (arm64) and Windows/WSL2 (amd64).

### Prerequisites

- **macOS:** Docker Desktop (`brew install --cask docker-desktop`), running.
- **Windows:** Docker Desktop with WSL integration enabled for your distro
  (Settings → Resources → WSL integration), or Docker Engine installed inside WSL2.
- `make` (preinstalled on macOS; on Ubuntu/WSL: `sudo apt install make`).
- Java 21 is only needed if you run the app outside Docker.

### First-time setup

```sh
cp .env.example .env   # then change the passwords in .env
make up                # builds the app, starts MySQL, waits until healthy
```

The API is then at http://localhost:8081 (try `curl localhost:8081/projects`) and the dashboard at
http://localhost:8080. `make up` returns when the containers start; Spring needs a few more seconds
before requests succeed.
MySQL is reachable from the host on port **3307**.

On first start, MySQL creates the database and app user from `.env`. When the app starts, Flyway
creates or updates the tables (see [Database migrations](#database-migrations)).
Mock data is **not** loaded automatically: run `make seed` once on a fresh database.

### Daily workflow

| Command      | What it does                                            |
|--------------|---------------------------------------------------------|
| `make up`    | Start everything (rebuilds the app image if code changed) |
| `make down`  | Stop containers; data is kept                           |
| `make logs`  | Follow logs                                             |
| `make ps`    | Show status and health                                  |
| `make mysql` | MySQL shell as the app user                             |
| `make seed`  | Load mock data from `database/DummyData` (once, on a fresh database) |
| `make reset` | Delete the database volume (asks first); tables are recreated on next `make up` |

**Running the app from your IDE instead:** run `make db` to start only MySQL, then start the app
with `./mvnw spring-boot:run` or from the IDE, with the project root as the working directory. The app
reads `.env` automatically, so it connects to `localhost:3307`. If the app container is already
running, stop it first (`docker compose stop app`) so port 8081 is free.

### Troubleshooting

- **"port is already allocated" / "address already in use":** something else is using 3307, 8081 or 8080.
  Change `MYSQL_PORT` (and `DB_PORT` to match), `APP_PORT` or `DASHBOARD_PORT` in `.env`, then `make up`.
- **"permission denied … docker.sock" (Linux/WSL with Docker Engine):** add yourself to the docker group with
  `sudo usermod -aG docker $USER`, then run `wsl --shutdown` from Windows and reopen the terminal.
- **"docker: command not found" or "Cannot connect to the Docker daemon" in WSL with Docker Desktop:**
  Docker Desktop isn't running, or WSL integration isn't enabled for your distro (see Prerequisites).
- **Login fails after changing passwords in `.env`:** MySQL only applies credentials when the volume
  is first created. Run `make reset`, then `make up` (and `make seed` if you want mock data back).
- **App won't start with a Flyway "checksum mismatch" error:** a migration that already ran was edited.
  Undo the edit and put the change in a new migration file instead.

## Dashboard

`dashboard/` is a React + TypeScript app built with Vite. It lists projects and their flags, and per
environment switches a flag on or off, sets its rollout percentage and edits its rules.

It calls the API under `/api`: the Vite dev server and the dashboard container (nginx) both forward
`/api/*` to the API, so the browser stays on one origin and the dashboard needs no CORS setting.

- **In Docker:** `make up` builds and serves it at http://localhost:8080 (`DASHBOARD_PORT` in `.env`).
- **While working on it** (Node 22+): start the API with `make up`, then
  ```sh
  cd dashboard
  npm install
  npm run dev        # http://localhost:5173, reloads on save
  ```
  Set `FLAGMAN_API_URL` to point the dev proxy at an API other than `http://localhost:8081`.

## Database migrations

The schema is managed by [Flyway](https://documentation.red-gate.com/fd). Migrations live in
`src/main/resources/db/migration/` and run automatically when the app starts. Only migrations a
database hasn't seen yet are applied, so existing data is kept.

To change the schema, add a new file with the next version number, e.g.
`V4__add_flag_tags.sql` (capital `V`, **two** underscores). Then run `make up`.
Never edit a migration that has already been applied; add a new one instead.
