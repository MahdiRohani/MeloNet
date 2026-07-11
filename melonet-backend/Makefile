.PHONY: run test tidy migrate-up migrate-down docker-up docker-down

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

docker-up:
	docker compose up --build -d

docker-down:
	docker compose down
