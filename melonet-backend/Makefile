.PHONY: run test test-integration smoke tidy migrate-up migrate-down docker-up docker-down seed seed-offline docker-seed docker-seed-offline sync-dev-url adb-reverse print-lan-ip pull-base-images download-voice-models

LAN_IP := $(shell ./scripts/detect-lan-ip.sh)
PUBLIC_BASE_URL ?= http://$(LAN_IP):8080
ANDROID_LOCAL_PROPERTIES ?= ../melonet-android/local.properties

print-lan-ip:
	@echo "$(LAN_IP)"

# Detect current Wi‑Fi/LAN IP and write it into backend `.env` (PUBLIC_BASE_URL).
# Android uses Gradle auto-detect by default (`melonet.devApiBaseUrl=auto`), so a
# Rebuild/Sync after changing Wi‑Fi is enough — no manual IP edits.
# Run this (or `make docker-up`) whenever you change networks so media URLs stay correct.
sync-dev-url:
	@chmod +x scripts/detect-lan-ip.sh
	@IP="$$(./scripts/detect-lan-ip.sh)"; \
	URL="http://$${IP}:8080"; \
	echo "Detected LAN IP: $${IP}"; \
	echo "PUBLIC_BASE_URL=$${URL}"; \
	if [ -f .env ]; then \
		if grep -q '^PUBLIC_BASE_URL=' .env; then \
			sed -i "s|^PUBLIC_BASE_URL=.*|PUBLIC_BASE_URL=$${URL}|" .env; \
		else \
			printf '\nPUBLIC_BASE_URL=%s\n' "$${URL}" >> .env; \
		fi; \
	else \
		cp .env.example .env; \
		sed -i "s|^PUBLIC_BASE_URL=.*|PUBLIC_BASE_URL=$${URL}|" .env; \
	fi; \
	if [ -f "$(ANDROID_LOCAL_PROPERTIES)" ]; then \
		if grep -q '^melonet.devApiBaseUrl=' "$(ANDROID_LOCAL_PROPERTIES)"; then \
			sed -i "s|^melonet.devApiBaseUrl=.*|melonet.devApiBaseUrl=auto|" "$(ANDROID_LOCAL_PROPERTIES)"; \
		else \
			printf '\n# auto = Gradle picks current LAN IP at build time\nmelonet.devApiBaseUrl=auto\n' >> "$(ANDROID_LOCAL_PROPERTIES)"; \
		fi; \
		echo "Android local.properties → melonet.devApiBaseUrl=auto (build-time detect)"; \
	else \
		echo "Skipped Android local.properties (not found at $(ANDROID_LOCAL_PROPERTIES))"; \
	fi; \
	echo "Done. Rebuild/Sync the Android app so BuildConfig picks up $${URL}/"

# USB debugging: phone talks to laptop backend regardless of Wi‑Fi.
# After this, set melonet.devApiBaseUrl=http://127.0.0.1:8080/ and rebuild,
# or use: make sync-dev-url-usb
adb-reverse:
	adb reverse tcp:8080 tcp:8080
	@echo "adb reverse ok — phone can reach backend at http://127.0.0.1:8080/"

sync-dev-url-usb: adb-reverse
	@URL="http://127.0.0.1:8080"; \
	if [ -f .env ]; then \
		if grep -q '^PUBLIC_BASE_URL=' .env; then \
			sed -i "s|^PUBLIC_BASE_URL=.*|PUBLIC_BASE_URL=$${URL}|" .env; \
		else \
			printf '\nPUBLIC_BASE_URL=%s\n' "$${URL}" >> .env; \
		fi; \
	fi; \
	if [ -f "$(ANDROID_LOCAL_PROPERTIES)" ]; then \
		if grep -q '^melonet.devApiBaseUrl=' "$(ANDROID_LOCAL_PROPERTIES)"; then \
			sed -i "s|^melonet.devApiBaseUrl=.*|melonet.devApiBaseUrl=$${URL}/|" "$(ANDROID_LOCAL_PROPERTIES)"; \
		else \
			printf '\nmelonet.devApiBaseUrl=%s/\n' "$${URL}" >> "$(ANDROID_LOCAL_PROPERTIES)"; \
		fi; \
	fi; \
	echo "Pinned Android + PUBLIC_BASE_URL to $${URL}/ — rebuild the app."

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

docker-up: sync-dev-url
	PUBLIC_BASE_URL="$(PUBLIC_BASE_URL)" docker compose up --build -d

# When Docker Hub TLS times out (common in IR), pull base images via a mirror
# and retag them to official names first:
#   make pull-base-images
#   make docker-up
pull-base-images:
	@chmod +x scripts/pull-base-images.sh
	./scripts/pull-base-images.sh

# Download community RVC packs into data/voice-models/<slug>/model.pth
download-voice-models:
	@chmod +x scripts/download-voice-models.sh
	./scripts/download-voice-models.sh

docker-down:
	docker compose down

docker-seed:
	docker compose --profile seed build seed
	PUBLIC_BASE_URL="$(PUBLIC_BASE_URL)" docker compose --profile seed run --rm seed

# Generates valid silent audio locally without any downloads.
# Use this when GitHub / SoundHelix are blocked or too slow.
docker-seed-offline:
	docker compose --profile seed build seed
	PUBLIC_BASE_URL="$(PUBLIC_BASE_URL)" docker compose --profile seed run --rm -e SEED_AUDIO_MODE=synthetic seed
