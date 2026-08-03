#!/usr/bin/env bash
# Download community RVC models into data/voice-models/<slug>/
# Sources are public HuggingFace uploads (quality varies; personal/local use).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODELS_DIR="${VOICE_MODELS_DIR:-$ROOT/data/voice-models}"
TMP="${TMPDIR:-/tmp}/melonet-rvc-dl"
mkdir -p "$MODELS_DIR" "$TMP"

hf_file() {
  # hf_file <repo> <path> <out>
  local repo="$1" path="$2" out="$3"
  local url="https://huggingface.co/${repo}/resolve/main/${path}"
  echo "↓ ${repo}/${path}"
  curl -fL --retry 5 --retry-delay 2 -o "$out" "$url"
}

ensure_dir() {
  mkdir -p "$MODELS_DIR/$1"
}

extract_model_zip() {
  # extract_model_zip <zip> <slug>
  local zip="$1"
  local slug="$2"
  local dest="$MODELS_DIR/$slug"
  ensure_dir "$slug"
  local work="$TMP/extract-$slug"
  rm -rf "$work"
  mkdir -p "$work"
  unzip -qo "$zip" -d "$work"
  # Prefer largest .pth and any .index
  local pth
  pth="$(find "$work" -type f -iname '*.pth' ! -iname 'D_*' ! -iname 'G_*' -printf '%s %p\n' | sort -nr | head -1 | cut -d' ' -f2- || true)"
  if [[ -z "${pth}" ]]; then
    echo "ERROR: no .pth in $zip" >&2
    return 1
  fi
  cp -f "$pth" "$dest/model.pth"
  local idx
  idx="$(find "$work" -type f \( -iname '*.index' -o -iname '*added*.index' \) | head -1 || true)"
  if [[ -n "${idx}" ]]; then
    cp -f "$idx" "$dest/index.index"
  fi
  rm -rf "$work"
  echo "✓ $slug ← $(basename "$pth")"
}

write_meta() {
  local slug="$1" name="$2" pitch="${3:-0}"
  cat >"$MODELS_DIR/$slug/meta.json" <<EOF
{
  "slug": "$slug",
  "display_name": "$name",
  "pitch_default": $pitch,
  "source": "community-rvc"
}
EOF
}

echo "Models dir: $MODELS_DIR"

# --- Hayedeh (direct pth) ---
ensure_dir hayedeh
if [[ ! -f "$MODELS_DIR/hayedeh/model.pth" ]]; then
  hf_file "PersianSingers/RVC-models" "hayedeh/hayedeh-200.pth" "$MODELS_DIR/hayedeh/model.pth"
fi
write_meta hayedeh "هایده" 0

# --- Morteza Pashaei ---
if [[ ! -f "$MODELS_DIR/morteza-pashaei/model.pth" ]]; then
  hf_file "momomelodi/pashaee2" "pashaei.zip" "$TMP/pashaei.zip"
  extract_model_zip "$TMP/pashaei.zip" "morteza-pashaei"
fi
write_meta morteza-pashaei "مرتضی پاشایی" 0

# --- Shadmehr ---
if [[ ! -f "$MODELS_DIR/shadmehr/model.pth" ]]; then
  hf_file "frixy/ShadmehrAghili" "Shadmehr-Aghili.zip" "$TMP/shadmehr.zip"
  extract_model_zip "$TMP/shadmehr.zip" "shadmehr"
fi
write_meta shadmehr "شادمهر" 0

# --- Mahasti ---
if [[ ! -f "$MODELS_DIR/mahasti/model.pth" ]]; then
  hf_file "momomelodi/mahasti2" "mahasti2.zip" "$TMP/mahasti.zip"
  extract_model_zip "$TMP/mahasti.zip" "mahasti"
fi
write_meta mahasti "مهستی" 0

# --- Mohsen Chavoshi (large ~250MB zip) ---
if [[ ! -f "$MODELS_DIR/mohsen-chavoshi/model.pth" ]]; then
  hf_file "samad321kk/chavoshi" "chavoshi.pth.zip" "$TMP/chavoshi.zip"
  extract_model_zip "$TMP/chavoshi.zip" "mohsen-chavoshi"
fi
write_meta mohsen-chavoshi "محسن چاوشی" 0

# --- Ebi (ABIY community pack — likely ابی) ---
if [[ ! -f "$MODELS_DIR/ebi/model.pth" ]]; then
  ensure_dir ebi
  hf_file "RO-Rtechs/RVC_ABIY" "ABIY_MODEL.pth" "$MODELS_DIR/ebi/model.pth"
  hf_file "RO-Rtechs/RVC_ABIY" "ABIY.index" "$MODELS_DIR/ebi/index.index" || true
fi
write_meta ebi "ابی" 0

# Placeholders for artists without a known public pack yet.
for pair in \
  "mohsen-yeganeh|محسن یگانه" \
  "googoosh|گوگوش" \
  "moein|معین" \
  "siavash-ghomayshi|سیاوش قمیشی"
do
  slug="${pair%%|*}"
  name="${pair##*|}"
  ensure_dir "$slug"
  write_meta "$slug" "$name" 0
  if [[ ! -f "$MODELS_DIR/$slug/model.pth" ]]; then
    echo "… $slug: no public model yet — drop model.pth into $MODELS_DIR/$slug/"
  fi
done

echo
echo "Ready models:"
find "$MODELS_DIR" -name model.pth -printf '%h\n' | sed "s|$MODELS_DIR/||" | sort
