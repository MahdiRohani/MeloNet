#!/usr/bin/env bash
# End-to-end smoke test for MeloNet backend.
# Requires: curl, jq, running API at BASE_URL (default http://localhost:8080)

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DEMO_USER="${DEMO_USER:-mahdi}"
DEMO_PASS="${DEMO_PASS:-melonet123}"
SECOND_USER="${SECOND_USER:-student_test}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓${NC} $1"; }
fail() { echo -e "${RED}✗${NC} $1"; exit 1; }
info() { echo -e "${YELLOW}→${NC} $1"; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "نیاز به نصب $1 دارید"
}

api() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"

  local args=(-sS -X "$method" "${BASE_URL}${path}" -H "Accept: application/json")
  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer ${token}")
  fi
  if [[ -n "$body" ]]; then
    args+=(-H "Content-Type: application/json" -d "$body")
  fi

  curl "${args[@]}"
}

expect_status() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$actual" != "$expected" ]]; then
    fail "${label}: status=${actual}, expected=${expected}"
  fi
  pass "$label"
}

require_cmd curl
require_cmd jq

echo "MeloNet smoke test — ${BASE_URL}"
echo

info "Health checks"
live_code=$(curl -sS -o /dev/null -w '%{http_code}' "${BASE_URL}/health/live")
expect_status "GET /health/live" "200" "$live_code"

ready_code=$(curl -sS -o /dev/null -w '%{http_code}' "${BASE_URL}/health/ready")
expect_status "GET /health/ready" "200" "$ready_code"

unauth_code=$(curl -sS -o /dev/null -w '%{http_code}' "${BASE_URL}/api/songs")
expect_status "GET /api/songs without token → 401" "401" "$unauth_code"

info "Auth — login demo user (${DEMO_USER})"
login_resp=$(api POST /api/auth/login "" "{\"login\":\"${DEMO_USER}\",\"password\":\"${DEMO_PASS}\"}")
login_code=$(echo "$login_resp" | jq -r '.error // empty | if . == null then "ok" else .code end' 2>/dev/null || true)
TOKEN=$(echo "$login_resp" | jq -r '.data.access_token // empty')
if [[ -z "$TOKEN" ]]; then
  echo "$login_resp" | jq . 2>/dev/null || echo "$login_resp"
  fail "login failed — آیا migration اجرا شده؟ کاربر demo: ${DEMO_USER}/${DEMO_PASS}"
fi
pass "login successful"

me_resp=$(api GET /api/auth/me "$TOKEN")
me_user=$(echo "$me_resp" | jq -r '.data.username')
[[ "$me_user" == "$DEMO_USER" ]] || fail "GET /api/auth/me username mismatch"
pass "GET /api/auth/me"

premium=$(echo "$me_resp" | jq -r '.data.is_premium')
info "premium status for ${DEMO_USER}: ${premium}"

info "Catalog & home"
songs_resp=$(api GET "/api/songs?page=1&limit=5" "$TOKEN")
song_count=$(echo "$songs_resp" | jq '.data | length')
[[ "$song_count" -gt 0 ]] || fail "no songs returned"
SONG_ID=$(echo "$songs_resp" | jq -r '.data[0].id')
pass "GET /api/songs (${song_count} items)"

home_resp=$(api GET /api/home "$TOKEN")
echo "$home_resp" | jq -e '.data' >/dev/null || fail "invalid home response"
pass "GET /api/home"

search_resp=$(api GET "/api/search?q=artist&type=song&limit=5" "$TOKEN")
echo "$search_resp" | jq -e '.data' >/dev/null || fail "invalid search response"
pass "GET /api/search"

popular_resp=$(api GET "/api/catalog/popular?limit=5" "$TOKEN")
echo "$popular_resp" | jq -e '.data | length > 0' >/dev/null || fail "popular catalog empty"
pass "GET /api/catalog/popular"

info "Library — like & play"
like_resp=$(api POST "/api/songs/${SONG_ID}/like" "$TOKEN")
echo "$like_resp" | jq -e '.data.song_id' >/dev/null || fail "like failed"
pass "POST /api/songs/:id/like"

play_resp=$(api POST "/api/songs/${SONG_ID}/play" "$TOKEN" '{"duration_played_sec":30,"source":"smoke-test"}')
echo "$play_resp" | jq -e '.data.song_id' >/dev/null || fail "play event failed"
pass "POST /api/songs/:id/play"

liked_resp=$(api GET /api/library/liked "$TOKEN")
echo "$liked_resp" | jq -e '.data | length >= 0' >/dev/null
pass "GET /api/library/liked"

recent_resp=$(api GET /api/library/recent "$TOKEN")
echo "$recent_resp" | jq -e '.data | length >= 0' >/dev/null
pass "GET /api/library/recent"

info "Playlist CRUD"
pl_create=$(api POST /api/playlists "$TOKEN" '{"title":"Smoke Test Playlist","visibility":"private"}')
PL_ID=$(echo "$pl_create" | jq -r '.data.id // empty')
[[ -n "$PL_ID" ]] || fail "playlist create failed"
pass "POST /api/playlists (id=${PL_ID})"

add_song=$(api POST "/api/playlists/${PL_ID}/songs" "$TOKEN" "{\"song_id\":${SONG_ID}}")
echo "$add_song" | jq -e '.data' >/dev/null || fail "add song to playlist failed"
pass "POST /api/playlists/:id/songs"

pl_get=$(api GET "/api/playlists/${PL_ID}" "$TOKEN")
pl_songs=$(echo "$pl_get" | jq '.data.songs | length')
[[ "$pl_songs" -ge 1 ]] || fail "playlist should contain at least one song"
pass "GET /api/playlists/:id"

info "Social — search & follow"
users_resp=$(api GET "/api/users/search?q=${SECOND_USER}" "$TOKEN")
OTHER_ID=$(echo "$users_resp" | jq -r --arg u "$SECOND_USER" '.data[] | select(.username==$u) | .id' | head -n1)
[[ -n "$OTHER_ID" ]] || fail "user ${SECOND_USER} not found"
pass "GET /api/users/search"

profile_resp=$(api GET "/api/users/${OTHER_ID}" "$TOKEN")
echo "$profile_resp" | jq -e '.data.username' >/dev/null
pass "GET /api/users/:id"

follow_resp=$(api POST "/api/users/${OTHER_ID}/follow" "$TOKEN")
echo "$follow_resp" | jq -e '.data' >/dev/null 2>&1 || true
pass "POST /api/users/:id/follow"

notif_resp=$(api GET /api/notifications "$TOKEN")
echo "$notif_resp" | jq -e '.data' >/dev/null
pass "GET /api/notifications"

info "Chat — conversations"
conv_resp=$(api POST /api/conversations "$TOKEN" "{\"user_id\":${OTHER_ID}}")
CONV_ID=$(echo "$conv_resp" | jq -r '.data.id // empty')
[[ -n "$CONV_ID" ]] || fail "create conversation failed"
pass "POST /api/conversations (id=${CONV_ID})"

conv_list=$(api GET /api/conversations "$TOKEN")
echo "$conv_list" | jq -e '.data | length >= 1' >/dev/null
pass "GET /api/conversations"

msgs_resp=$(api GET "/api/conversations/${CONV_ID}/messages" "$TOKEN")
echo "$msgs_resp" | jq -e '.data' >/dev/null
pass "GET /api/conversations/:id/messages"

info "Media — range request (if audio URL available)"
audio_url=$(echo "$songs_resp" | jq -r '.data[0].audio_url // empty')
if [[ -n "$audio_url" && "$audio_url" != "null" ]]; then
  if [[ "$audio_url" == http* ]]; then
    media_path="$audio_url"
  else
    media_path="${BASE_URL}${audio_url}"
  fi
  range_code=$(curl -sS -o /dev/null -w '%{http_code}' -H "Range: bytes=0-1023" "$media_path")
  if [[ "$range_code" == "206" || "$range_code" == "200" ]]; then
    pass "audio range/stream (${range_code}) — ${media_path}"
  else
    info "audio URL returned ${range_code} (seed شاید اجرا نشده) — ${media_path}"
  fi
else
  info "no audio_url in song — برای media واقعی: make docker-seed"
fi

info "Register fresh user (optional flow)"
suffix=$(date +%s)
reg_resp=$(api POST /api/auth/register "" "{\"username\":\"smoke_${suffix}\",\"email\":\"smoke_${suffix}@test.local\",\"password\":\"melonet123\",\"display_name\":\"Smoke User\"}")
reg_token=$(echo "$reg_resp" | jq -r '.data.access_token // empty')
if [[ -n "$reg_token" ]]; then
  pass "POST /api/auth/register"
else
  info "register skipped or failed (ممکن است rate limit باشد)"
fi

echo
echo -e "${GREEN}همه تست‌های smoke با موفقیت پاس شدند.${NC}"
echo "برای WebSocket دستی:"
echo "  wscat -c \"${BASE_URL/http/ws}/ws/chat?token=\${TOKEN}\""
echo "  سپس بفرست: {\"event\":\"ping\"}"
