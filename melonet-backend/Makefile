.PHONY: run test tidy migrate-up migrate-down docker-up docker-down seed

run:
	go run ./cmd/api

test:
	go test ./...

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

docker-up:
	docker compose up --build -d

docker-down:
	docker compose down

docker-seed:
	docker compose --profile seed run --rm seed
