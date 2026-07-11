FROM golang:1.22-alpine AS builder

WORKDIR /src

RUN apk add --no-cache git ca-certificates

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /bin/melonet-api ./cmd/api

FROM alpine:3.21

WORKDIR /app

RUN apk add --no-cache ca-certificates curl \
    && addgroup -S melonet \
    && adduser -S melonet -G melonet

COPY --from=builder /bin/melonet-api /app/melonet-api
COPY migrations /app/migrations

RUN mkdir -p /app/data \
    && chown -R melonet:melonet /app

USER melonet

ENV MIGRATIONS_PATH=/app/migrations
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD curl -fsS http://localhost:8080/health/live || exit 1

ENTRYPOINT ["/app/melonet-api"]
