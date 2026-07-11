# MeloNet Backend

Go/Gin backend for the MeloNet music streaming app. It provides authentication, catalog, library, playlists, social graph, real-time chat, and media storage.

## Prerequisites

| Tool | Required for | Install |
|------|----------------|---------|
| **Go 1.22+** | Running the API | [go.dev/dl](https://go.dev/dl/) |
| **Docker & Compose** | Easiest full stack (optional) | See [Docker setup](#option-a-docker-recommended) below |
| **PostgreSQL 16** | Database | `sudo apt install postgresql` |
| **Redis** | Cache, chat presence, rate limits | `sudo apt install redis-server` |
| **MinIO** | Audio/cover file storage | See [native setup](#option-b-native-linux-no-docker) |
| **curl + jq** | Smoke tests | `sudo apt install curl jq` |

---

## `make docker-up` fails: `docker: No such file or directory`

Docker is **not installed** on your machine. You have two choices:

### Choice 1 — Install Docker (recommended if you want the simplest workflow)

On Ubuntu / Linux Mint / Pop!_OS:

```bash
# Official Docker install script (or use your distro's package manager)
curl -fsSL https://get.docker.com | sh

# Allow your user to run docker without sudo (log out/in after this)
sudo usermod -aG docker $USER

# Install Compose plugin (often included with Docker Desktop / modern docker.io)
sudo apt install docker-compose-plugin

# Verify
docker --version
docker compose version
```

Then retry:

```bash
cd ~/AndroidStudioProjects/MeloNet/melonet-backend
make docker-up
make docker-seed   # optional: 50 real royalty-free tracks
make smoke
```

### Choice 2 — Run without Docker (works on your machine right now)

Install and start PostgreSQL, Redis, and MinIO locally, then run the Go API directly. Full steps are in [Option B: Native Linux](#option-b-native-linux-no-docker) below.

You can still run unit tests **without any services**:

```bash
cd ~/AndroidStudioProjects/MeloNet/melonet-backend
make test
```

---

## Quick start

### Option A: Docker (recommended)

```bash
cd melonet-backend

make docker-up      # PostgreSQL, Redis, MinIO, API
make docker-seed    # optional: download & upload 50 real tracks (needs internet)
make smoke          # end-to-end API smoke test
```

- API: `http://localhost:8080`
- MinIO console: `http://localhost:9001` (user `melonet`, password `melonetsecret`)

### Option B: Native Linux (no Docker)

#### 1. Install services

```bash
sudo apt update
sudo apt install -y postgresql postgresql-contrib redis-server curl jq

# Start services
sudo systemctl enable --now postgresql redis-server
```

#### 2. Create the database

```bash
sudo -u postgres psql -c "CREATE USER melonet WITH PASSWORD 'melonet';"
sudo -u postgres psql -c "CREATE DATABASE melonet OWNER melonet;"
```

#### 3. Install and run MinIO

```bash
# Download MinIO server binary
curl -fsSL https://dl.min.io/server/minio/release/linux-amd64/minio -o /tmp/minio
chmod +x /tmp/minio
mkdir -p ~/melonet-minio-data

# Run in a separate terminal (keep it open)
/tmp/minio server ~/melonet-minio-data --console-address ":9001"
```

Default MinIO credentials are `minioadmin` / `minioadmin`. Create a bucket named `melonet-media` via the console at `http://localhost:9001`, **or** let the API create it on first connect (the client usually auto-creates the bucket).

To match the project's expected credentials, you can start MinIO with:

```bash
MINIO_ROOT_USER=melonet MINIO_ROOT_PASSWORD=melonetsecret \
  /tmp/minio server ~/melonet-minio-data --console-address ":9001"
```

#### 4. Configure environment

```bash
cd ~/AndroidStudioProjects/MeloNet/melonet-backend
cp .env.example .env
```

Edit `.env` and point everything to `localhost`:

```env
DATABASE_URL=postgres://melonet:melonet@localhost:5432/melonet?sslmode=disable
REDIS_URL=redis://localhost:6379/0
STORAGE_ENDPOINT=localhost:9000
STORAGE_ACCESS_KEY=melonet
STORAGE_SECRET_KEY=melonetsecret
MIGRATIONS_PATH=migrations
SEED_CACHE_DIR=./data/.cache
```

Load env vars before running commands:

```bash
set -a && source .env && set +a
```

#### 5. Migrate, run, seed

```bash
# Terminal 1 — API
make migrate-up
make run

# Terminal 2 — optional real media seed (needs internet)
make seed
```

#### 6. Verify

```bash
make smoke
```

---

## Testing

### 1. Unit tests (no Docker, no database)

```bash
make test
```

Runs all Go unit tests. Safe to run anytime.

### 2. Smoke test (API must be running)

```bash
make smoke
# or
./scripts/smoke-test.sh
```

Checks health, auth, catalog, library, playlists, social, and chat in one pass.

### 3. Integration tests (PostgreSQL + Redis required)

```bash
export INTEGRATION_TEST=1
export DATABASE_URL=postgres://melonet:melonet@localhost:5432/melonet?sslmode=disable
export REDIS_URL=redis://localhost:6379/0

make test-integration
```

Covers auth flows, playlist IDOR protection, and chat conversation permissions.

### 4. WebSocket (manual)

```bash
TOKEN=$(curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"mahdi","password":"melonet123"}' | jq -r '.data.access_token')

# Install wscat once: npm i -g wscat
wscat -c "ws://localhost:8080/ws/chat?token=${TOKEN}"
```

Send:

```json
{"event":"ping"}
```

You should receive `pong`.

### 5. Media streaming (after `make seed` or `make docker-seed`)

```bash
curl -I -H "Range: bytes=0-1023" \
  "http://localhost:8080/api/media/catalog/audio/1/<slug>.mp3"
```

Expect `206 Partial Content` and `Accept-Ranges: bytes`.

---

## Demo users

Created by migrations (`000005_auth_premium`):

| Username | Password | Premium |
|----------|----------|---------|
| `mahdi` | `melonet123` | Yes |
| `student_test` | `melonet123` | No |

---

## Android client base URLs

| Environment | Base URL |
|-------------|----------|
| Emulator | `http://10.0.2.2:8080` |
| Physical device (same Wi‑Fi) | `http://<your-pc-ip>:8080` |
| Local API on host | `http://localhost:8080` |

---

## Backend roadmap status

| Phase | Topic | Status |
|-------|-------|--------|
| 1 | Infrastructure, Docker, health | Done |
| 2 | Schema & migrations | Done |
| 3 | Auth, JWT, premium | Done |
| 4 | Catalog, search, home | Done |
| 5 | Media storage, 50-track seed | Done |
| 6 | Playlists, likes, history | Done |
| 7 | Social graph, notifications | Done |
| 8 | Real-time WebSocket chat | Done |
| 9 | Security, rate limits, tests | Done |
| 10 | API docs, smoke test | Done |

**Not in current backend scope:** offline download API (DB table exists, no REST endpoint yet), Google Play / Bazaar billing, production deployment.

---

## Documentation

- [API Reference](docs/API.md)
- [Environment variables](.env.example)

---

## Makefile commands

| Command | Description |
|---------|-------------|
| `make docker-up` | Start all services via Docker Compose |
| `make docker-down` | Stop Docker stack |
| `make docker-seed` | Seed 50 real tracks (Docker profile) |
| `make migrate-up` | Apply database migrations |
| `make migrate-down` | Roll back last migration |
| `make run` | Run API locally with Go |
| `make seed` | Seed media locally (needs DB + MinIO) |
| `make test` | Unit tests |
| `make test-integration` | Integration tests (`INTEGRATION_TEST=1`) |
| `make smoke` | End-to-end smoke test |
| `make tidy` | `go mod tidy` |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `docker: No such file or directory` | Install Docker ([Choice 1](#choice-1--install-docker-recommended-if-you-want-the-simplest-workflow)) or use [native setup](#option-b-native-linux-no-docker) |
| `go mod download ... 403 Forbidden` from `proxy.golang.org` | The default Go proxy is blocked in your region. The Dockerfiles now default to `GOPROXY=https://goproxy.cn,https://goproxy.io,direct`. For native builds run `go env -w GOPROXY=https://goproxy.cn,direct GOSUMDB=off` once, then retry. Override the Docker proxy with `docker compose build --build-arg GOPROXY=<url>`. |
| `connection refused` on `:8080` | API not running — `make run` or `make docker-up` |
| Login fails / empty users | Run migrations: `make migrate-up` |
| Media 404 | Run seed: `make seed` or `make docker-seed` |
| `jq: command not found` | `sudo apt install jq` |
| PostgreSQL auth failed | Check `DATABASE_URL` user/password and that DB exists |
| MinIO connection failed | Ensure MinIO is running on port 9000 and credentials match `.env` |
