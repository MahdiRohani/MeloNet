#!/usr/bin/env bash
# Download hubert/rmvpe base weights used by rvc-python (once), with resume.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="${1:-$ROOT/data/rvc-base}"
mkdir -p "$DEST"

BASE_URL="${RVC_BASE_URL:-https://huggingface.co/Daswer123/RVC_Base/resolve/main}"

download() {
  local name="$1"
  local out="$DEST/$name"
  local partial="$out.partial"
  if [[ -f "$out" ]]; then
    local size
    size="$(stat -c%s "$out" 2>/dev/null || echo 0)"
    if [[ "$size" -gt 100000000 ]]; then
      echo "ok $name ($(numfmt --to=iec "$size" 2>/dev/null || echo "$size"))"
      return 0
    fi
    echo "replacing undersized $name ($size bytes)"
    rm -f "$out"
  fi
  echo "downloading $name ..."
  curl -fL --retry 8 --retry-all-errors --retry-delay 2 \
    -C - --connect-timeout 30 --max-time 900 \
    -o "$partial" "$BASE_URL/$name"
  local size
  size="$(stat -c%s "$partial")"
  if [[ "$size" -lt 100000000 ]]; then
    echo "download incomplete for $name ($size bytes)" >&2
    exit 1
  fi
  mv "$partial" "$out"
  echo "saved $name ($(numfmt --to=iec "$size" 2>/dev/null || echo "$size"))"
}

download hubert_base.pt
download rmvpe.pt
# onnx is optional; skip if flaky
if [[ "${RVC_DOWNLOAD_ONNX:-0}" == "1" ]]; then
  download rmvpe.onnx || true
fi

echo "RVC base models ready in $DEST"
