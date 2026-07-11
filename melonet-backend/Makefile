.PHONY: run test test-integration smoke tidy migrate-up migrate-down docker-up docker-down seed seed-offline docker-seed docker-seed-offline

run:
	go run ./cmd/api

test:
	go test ./...

test-integration:
	INTEGRATION_TEST=1 go test -tags=integration ./internal/integration/...

smoke:
	@chmod +x scripts/smoke-test.sh
	@./scripts/smoke-test.sh

tidy:
	go mod tidy

migrate-up:
	go run ./cmd/migrate up

migrate-down:
	go run ./cmd/migrate down

seed:
	go run ./cmd/seed

seed-force:
	go run ./cmd/seed -force

seed-offline:
	go run ./cmd/seed -audio synthetic

docker-up:
	docker compose up --build -d

docker-down:
	docker compose down

docker-seed:
	docker compose --profile seed build seed
	docker compose --profile seed run --rm seed

# Generates valid silent audio locally without any downloads.
# Use this when GitHub / SoundHelix are blocked or too slow.
docker-seed-offline:
	docker compose --profile seed build seed
	docker compose --profile seed run --rm -e SEED_AUDIO_MODE=synthetic seed
