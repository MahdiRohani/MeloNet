# MeloNet API Reference

Base URL (local): `http://localhost:8080`

همه پاسخ‌های REST در envelope زیر هستند:

```json
{
  "data": {},
  "error": null,
  "meta": { "page": 1, "limit": 20, "total": 100, "has_more": true }
}
```

خطا:

```json
{
  "data": null,
  "error": { "code": "unauthorized", "message": "authentication required" }
}
```

## Auth

| Method | Path | Auth | Body |
|--------|------|------|------|
| POST | `/api/auth/register` | No | `username`, `email`, `password`, `display_name` |
| POST | `/api/auth/login` | No | `login`, `password` |
| POST | `/api/auth/refresh` | No | `refresh_token` |
| POST | `/api/auth/logout` | No | `refresh_token` |
| GET | `/api/auth/me` | Yes | — |
| PATCH | `/api/auth/me` | Yes | `display_name`, `bio`, `email` |
| POST | `/api/auth/me/avatar` | Yes | multipart `avatar` |

کاربران demo (بعد از migration):

- `mahdi` / `melonet123` (premium)
- `student_test` / `melonet123`

## Catalog & Search

| Method | Path | Query |
|--------|------|-------|
| GET | `/api/songs` | `page`, `limit`, `genre`, `sort` |
| GET | `/api/songs/:id` | — |
| GET | `/api/search` | `q`, `type` (`song\|artist\|user\|all`), `page`, `limit` |
| GET | `/api/home` | — |
| GET | `/api/catalog/popular` | `page`, `limit` |
| GET | `/api/catalog/new` | `page`, `limit` |
| GET | `/api/catalog/trending` | `page`, `limit` |
| GET | `/api/artists` | `q`, `page`, `limit` |
| GET | `/api/artists/:id` | — |
| GET | `/api/artists/:id/songs` | `page`, `limit` |
| GET | `/api/albums/:id` | — |
| GET | `/api/albums/:id/songs` | `page`, `limit` |
| GET | `/api/genres` | — |
| GET | `/api/genres/:id` | — |
| GET | `/api/genres/:id/songs` | `page`, `limit` |

## Library & Playlists

| Method | Path | Body |
|--------|------|------|
| GET | `/api/library/liked` | — |
| GET | `/api/library/recent` | — |
| POST | `/api/songs/:id/like` | — |
| DELETE | `/api/songs/:id/like` | — |
| POST | `/api/songs/:id/play` | `duration_played_sec`, `source` |
| GET | `/api/playlists` | `scope` = `mine\|system\|friends\|all` |
| POST | `/api/playlists` | `title`, `visibility`, `description` |
| GET/PATCH/DELETE | `/api/playlists/:id` | — |
| GET | `/api/playlists/:id/songs` | — |
| POST | `/api/playlists/:id/songs` | `song_id` |
| PUT | `/api/playlists/:id/songs/reorder` | `song_ids` |
| DELETE | `/api/playlists/:id/songs/:songId` | — |

## Social

| Method | Path |
|--------|------|
| GET | `/api/users/search?q=` |
| GET | `/api/users/:id` |
| GET | `/api/users/:id/playlists` |
| GET | `/api/users/:id/followers` |
| GET | `/api/users/:id/following` |
| POST/DELETE | `/api/users/:id/follow` |
| GET | `/api/notifications` |
| PATCH | `/api/notifications/:id/read` |
| POST | `/api/notifications/read-all` |

## Chat (REST)

| Method | Path | Body |
|--------|------|------|
| GET | `/api/conversations` | — |
| POST | `/api/conversations` | `user_id` |
| GET | `/api/conversations/unread-count` | — |
| GET | `/api/conversations/:id` | — |
| GET | `/api/conversations/:id/messages` | `page`, `limit` |
| POST | `/api/conversations/:id/read` | `message_ids` |
| GET | `/api/chat/history` | `with_id` (legacy) |

## WebSocket

`GET /ws/chat?token=<access_token>` یا header `Authorization: Bearer <token>`

Envelope:

```json
{ "event": "message.send", "data": { ... } }
```

Events: `ping`, `pong`, `message.send`, `message.ack`, `message.new`, `message.delivered`, `message.read`, `typing.start`, `typing.stop`, `song.share`

## Media

`GET /api/media/<object_key>` — بدون auth، با پشتیبانی `Range: bytes=`

## Health

- `GET /health/live` — liveness
- `GET /health/ready` — database + redis + storage

## Android Base URLs

| محیط | URL |
|------|-----|
| Emulator | `http://10.0.2.2:8080` |
| دستگاه واقعی (همان WiFi) | `http://<IP-کامپیوتر>:8080` |
| Docker روی host | `http://localhost:8080` |
