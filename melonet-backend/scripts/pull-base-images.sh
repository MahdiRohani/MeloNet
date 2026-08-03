#!/usr/bin/env bash
# Pull MeloNet Docker base images via an Iran-reachable Hub mirror, then retag
# them to the official names so existing Dockerfiles keep working.
#
# Usage:
#   ./scripts/pull-base-images.sh
#   MIRROR=docker.iranserver.com/library ./scripts/pull-base-images.sh
set -euo pipefail

MIRROR="${MIRROR:-docker.arvancloud.ir/library}"

images=(
  "golang:1.22-alpine"
  "alpine:3.21"
  "python:3.11-slim"
)

echo "Using mirror prefix: ${MIRROR}"
for img in "${images[@]}"; do
  src="${MIRROR}/${img}"
  echo "→ pulling ${src}"
  docker pull "${src}"
  echo "→ tagging as ${img}"
  docker tag "${src}" "${img}"
done

echo "Done. Official tags are ready for docker compose build."
