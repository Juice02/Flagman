# FlagMan

A feature flag management service (Spring Boot 4, Java 21, MySQL).

## Docker setup

Runs MySQL 8.4 and the app in Docker. Works the same on Apple Silicon (arm64) and Windows/WSL2 (amd64).

### Prerequisites

- **macOS:** Docker Desktop.
- **Windows:** Docker Desktop with WSL integration enabled for your distro
  (Settings → Resources → WSL integration), or Docker Engine installed inside WSL2.
- `make` (preinstalled on macOS; on Ubuntu/WSL: `sudo apt install make`).
- Java 21 is only needed if you run the app outside Docker.

### First-time setup

```sh
cp .env.example .env   # then change the passwords in .env
make up                # builds the app, starts MySQL, waits until healthy
```

The API is then at http://localhost:8081 (try `curl localhost:8081/projects`).
MySQL is reachable from the host on port **3307**.

On first start, MySQL creates the database and app user from `.env` and loads `database/schema`.
Dummy data is **not** loaded automatically.

### Daily workflow

| Command      | What it does                                            |
|--------------|---------------------------------------------------------|
| `make up`    | Start everything (rebuilds the app image if code changed) |
| `make down`  | Stop containers; data is kept                           |
| `make logs`  | Follow logs                                             |
| `make ps`    | Show status and health                                  |
| `make mysql` | MySQL shell as the app user                             |
| `make reset` | Delete the database volume (asks first); schema reloads on next `make up` |

**Running the app from your IDE instead:** run `make db` to start only MySQL, then start the app
with `./mvnw spring-boot:run` or from the IDE, with the project root as the working directory. The app
reads `.env` automatically, so it connects to `localhost:3307`. If the app container is already
running, stop it first (`docker compose stop app`) so port 8081 is free.

### Troubleshooting

- **"port is already allocated" / "address already in use":** something else is using 3307 or 8081.
  Change `MYSQL_PORT` (and `DB_PORT` to match) or `APP_PORT` in `.env`, then `make up`.
- **"permission denied … docker.sock" (Linux/WSL with Docker Engine):** add yourself to the docker group with
  `sudo usermod -aG docker $USER`, then run `wsl --shutdown` from Windows and reopen the terminal.
- **"docker: command not found" or "Cannot connect to the Docker daemon" in WSL with Docker Desktop:**
  Docker Desktop isn't running, or WSL integration isn't enabled for your distro (see Prerequisites).
- **Schema changes not showing up / login fails after changing passwords in `.env`:** the init
  scripts and credentials only apply when the volume is first created. Run `make reset`, then `make up`.
