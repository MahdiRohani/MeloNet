CREATE TABLE messages_legacy (
    id SERIAL PRIMARY KEY,
    sender_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content TEXT NOT NULL DEFAULT '',
    msg_type VARCHAR(50) NOT NULL DEFAULT 'text',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO messages_legacy (sender_id, receiver_id, content, msg_type, created_at)
SELECT
    m.sender_id,
    other_member.user_id,
    m.content,
    m.msg_type::text,
    m.created_at
FROM messages AS m
JOIN LATERAL (
    SELECT cm.user_id
    FROM conversation_members AS cm
    WHERE cm.conversation_id = m.conversation_id
      AND cm.user_id <> m.sender_id
    LIMIT 1
) AS other_member ON TRUE;

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS message_receipts;
DROP TABLE IF EXISTS messages;

ALTER TABLE messages_legacy RENAME TO messages;

CREATE INDEX idx_messages_participants ON messages (sender_id, receiver_id, created_at);

DROP TABLE IF EXISTS conversation_members;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS download_entitlements;
DROP TABLE IF EXISTS play_history;
DROP TABLE IF EXISTS follows;
DROP TABLE IF EXISTS likes;
DROP TABLE IF EXISTS playlist_songs;
DROP TABLE IF EXISTS playlists;

ALTER TABLE songs ADD COLUMN IF NOT EXISTS artist VARCHAR(255);

UPDATE songs AS s
SET artist = a.name
FROM artists AS a
WHERE a.id = s.artist_id;

ALTER TABLE songs
    DROP COLUMN IF EXISTS published_at,
    DROP COLUMN IF EXISTS play_count,
    DROP COLUMN IF EXISTS audio_object_key,
    DROP COLUMN IF EXISTS cover_object_key,
    DROP COLUMN IF EXISTS slug,
    DROP COLUMN IF EXISTS genre_id,
    DROP COLUMN IF EXISTS album_id,
    DROP COLUMN IF EXISTS artist_id;

DROP INDEX IF EXISTS idx_songs_search;
DROP INDEX IF EXISTS idx_songs_play_count;
DROP INDEX IF EXISTS idx_songs_published_at;
DROP INDEX IF EXISTS idx_songs_genre_id;
DROP INDEX IF EXISTS idx_songs_album_id;
DROP INDEX IF EXISTS idx_songs_artist_id;

CREATE INDEX IF NOT EXISTS idx_songs_artist ON songs (artist);

DROP TABLE IF EXISTS albums;
DROP TABLE IF EXISTS genres;
DROP TABLE IF EXISTS artists;
DROP TABLE IF EXISTS refresh_tokens;

ALTER TABLE users
    DROP COLUMN IF EXISTS premium_until,
    DROP COLUMN IF EXISTS avatar_object_key,
    DROP COLUMN IF EXISTS bio,
    DROP COLUMN IF EXISTS display_name,
    DROP COLUMN IF EXISTS password_hash,
    DROP COLUMN IF EXISTS email;

DROP INDEX IF EXISTS idx_users_email;

DROP TYPE IF EXISTS conversation_type;
DROP TYPE IF EXISTS playlist_visibility;
DROP TYPE IF EXISTS notification_type;
DROP TYPE IF EXISTS message_content_type;
DROP TYPE IF EXISTS message_delivery_status;
