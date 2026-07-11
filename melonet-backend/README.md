# MeloNet Backend

بک‌اند Go/Gin برای اپ استریم موسیقی MeloNet — شامل auth، catalog، library، social، chat real-time و media storage.

## پیش‌نیازها

- Go 1.22+
- Docker & Docker Compose (پیشنهادی)
- `curl` و `jq` (برای smoke test)

## راه‌اندازی سریع (Docker)

```bash
cd melonet-backend

# 1. سرویس‌ها را بالا بیاور
make docker-up

# 2. صبر کن تا healthy شوند، بعد seed واقعی ۵۰ آهنگ (اختیاری ولی توصیه می‌شود)
make docker-seed

# 3. تست خودکار end-to-end
chmod +x scripts/smoke-test.sh
./scripts/smoke-test.sh
```

API روی `http://localhost:8080` در دسترس است.

MinIO console: `http://localhost:9001` (user/pass: `melonet` / `melonetsecret`)

## راه‌اندازی بدون Docker

PostgreSQL، Redis و MinIO باید جداگانه در حال اجرا باشند.

```bash
cp .env.example .env
# DATABASE_URL را به localhost تغییر بده:
# postgres://melonet:melonet@localhost:5432/melonet?sslmode=disable
# REDIS_URL=redis://localhost:6379/0
# STORAGE_ENDPOINT=localhost:9000

make migrate-up
make run          # در یک ترمینال
make seed         # در ترمینال دیگر (دانلود آهنگ‌ها — نیاز به اینترنت)
```

## تست‌ها

### 1. Unit tests (همیشه — بدون Docker)

```bash
make test
# یا
go test ./...
```

### 2. Integration tests (نیاز به PostgreSQL + Redis)

```bash
make docker-up   # اگر هنوز بالا نیست

export INTEGRATION_TEST=1
export DATABASE_URL=postgres://melonet:melonet@localhost:5432/melonet?sslmode=disable
export REDIS_URL=redis://localhost:6379/0

make test-integration
```

پوشش: auth، IDOR playlist، دسترسی مکالمه چت.

### 3. Smoke test دستی (توصیه‌شده بعد از هر deploy)

```bash
./scripts/smoke-test.sh
```

این اسکریپت health، login، catalog، library، playlist، social و chat را یکجا تست می‌کند.

### 4. تست WebSocket

بعد از login، token بگیر:

```bash
TOKEN=$(curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"mahdi","password":"melonet123"}' | jq -r '.data.access_token')

# با wscat (npm i -g wscat)
wscat -c "ws://localhost:8080/ws/chat?token=${TOKEN}"
```

بفرست:

```json
{"event":"ping"}
```

باید `pong` برگردد.

برای ارسال پیام (به `student_test` با id از API):

```json
{"event":"message.send","data":{"receiver_id":2,"content":"سلام","msg_type":"text","client_id":"test-1"}}
```

### 5. تست Media / Streaming

بعد از `make docker-seed`:

```bash
curl -I -H "Range: bytes=0-1023" "http://localhost:8080/api/media/catalog/audio/1/..."
```

باید `206 Partial Content` و `Accept-Ranges: bytes` ببینی.

## کاربران Demo

| Username | Password | Premium |
|----------|----------|---------|
| `mahdi` | `melonet123` | بله |
| `student_test` | `melonet123` | خیر |

## وضعیت فازهای بک‌اند

| فاز | موضوع | وضعیت |
|-----|-------|--------|
| 1 | زیرساخت، Docker، health | ✅ |
| 2 | Schema و migration | ✅ |
| 3 | Auth، JWT، premium | ✅ |
| 4 | Catalog، search، home | ✅ |
| 5 | Media storage، seed ۵۰ آهنگ | ✅ |
| 6 | Playlist، like، history | ✅ |
| 7 | Social، follow، notifications | ✅ |
| 8 | Chat WebSocket | ✅ |
| 9 | امنیت، rate limit، تست | ✅ |
| 10 | مستندات API، smoke test | ✅ |

**خارج از scope فعلی بک‌اند:** API دانلود آفلاین (جدول DB هست، endpoint نیست)، اتصال Google Play/Bazaar billing، deploy production.

## مستندات

- [API Reference](docs/API.md)
- [`.env.example`](.env.example) — متغیرهای محیطی

## دستورات Makefile

| دستور | کار |
|-------|-----|
| `make docker-up` | بالا آوردن همه سرویس‌ها |
| `make docker-down` | خاموش کردن |
| `make docker-seed` | seed ۵۰ آهنگ واقعی |
| `make test` | unit tests |
| `make test-integration` | integration tests |
| `make smoke` | smoke test end-to-end |
| `make run` | اجرای API لوکال |
