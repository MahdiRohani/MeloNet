-- Product schema: normalized catalog, social graph, chat, and entitlements.

CREATE TYPE message_delivery_status AS ENUM ('sent', 'delivered', 'read');
CREATE TYPE message_content_type AS ENUM ('text', 'song', 'image', 'system');
CREATE TYPE notification_type AS ENUM ('follow', 'like', 'message', 'playlist_share', 'system');
CREATE TYPE playlist_visibility AS ENUM ('private', 'public');
CREATE TYPE conversation_type AS ENUM ('direct');

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_hash TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS bio TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS avatar_object_key TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS premium_until TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email) WHERE email IS NOT NULL;

UPDATE users
SET display_name = username
WHERE display_name = '';

CREATE TABLE refresh_tokens (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE artists (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    bio TEXT NOT NULL DEFAULT '',
    image_object_key TEXT NOT NULL DEFAULT '',
    image_url TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_artists_name ON artists (name);

CREATE TABLE genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE albums (
    id SERIAL PRIMARY KEY,
    artist_id INTEGER NOT NULL REFERENCES artists (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    cover_object_key TEXT NOT NULL DEFAULT '',
    cover_url TEXT NOT NULL DEFAULT '',
    release_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (artist_id, slug)
);

CREATE INDEX idx_albums_artist_id ON albums (artist_id);
CREATE INDEX idx_albums_title ON albums (title);

INSERT INTO artists (name, slug)
SELECT DISTINCT
    artist,
    lower(regexp_replace(regexp_replace(trim(artist), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'))
FROM songs
WHERE trim(artist) <> ''
ON CONFLICT (slug) DO NOTHING;

INSERT INTO genres (name, slug)
SELECT DISTINCT
    category,
    lower(regexp_replace(regexp_replace(trim(category), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'))
FROM songs
WHERE trim(category) <> ''
ON CONFLICT (slug) DO NOTHING;

ALTER TABLE songs
    ADD COLUMN IF NOT EXISTS artist_id INTEGER REFERENCES artists (id) ON DELETE RESTRICT,
    ADD COLUMN IF NOT EXISTS album_id INTEGER REFERENCES albums (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS genre_id INTEGER REFERENCES genres (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cover_object_key TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS audio_object_key TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS play_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;

UPDATE songs AS s
SET artist_id = a.id
FROM artists AS a
WHERE a.name = s.artist
  AND s.artist_id IS NULL;

UPDATE songs AS s
SET genre_id = g.id
FROM genres AS g
WHERE g.name = s.category
  AND s.genre_id IS NULL;

UPDATE songs
SET slug = lower(regexp_replace(regexp_replace(trim(title), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'))
    || '-' || id::text
WHERE slug IS NULL;

UPDATE songs
SET published_at = created_at
WHERE published_at IS NULL;

ALTER TABLE songs
    ALTER COLUMN artist_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_songs_artist_id ON songs (artist_id);
CREATE INDEX IF NOT EXISTS idx_songs_album_id ON songs (album_id);
CREATE INDEX IF NOT EXISTS idx_songs_genre_id ON songs (genre_id);
CREATE INDEX IF NOT EXISTS idx_songs_published_at ON songs (published_at DESC);
CREATE INDEX IF NOT EXISTS idx_songs_play_count ON songs (play_count DESC);
CREATE INDEX IF NOT EXISTS idx_songs_search ON songs USING gin (to_tsvector('simple', title));

ALTER TABLE songs DROP COLUMN IF EXISTS artist;

CREATE TABLE playlists (
    id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    visibility playlist_visibility NOT NULL DEFAULT 'private',
    cover_object_key TEXT NOT NULL DEFAULT '',
    cover_url TEXT NOT NULL DEFAULT '',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_playlists_owner_id ON playlists (owner_id);
CREATE INDEX idx_playlists_visibility ON playlists (visibility);

CREATE TABLE playlist_songs (
    playlist_id INTEGER NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    song_id INTEGER NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (playlist_id, song_id)
);

CREATE INDEX idx_playlist_songs_song_id ON playlist_songs (song_id);
CREATE INDEX idx_playlist_songs_position ON playlist_songs (playlist_id, position);

CREATE TABLE likes (
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    song_id INTEGER NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, song_id)
);

CREATE INDEX idx_likes_song_id ON likes (song_id);

CREATE TABLE follows (
    follower_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    following_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);

CREATE INDEX idx_follows_following_id ON follows (following_id);

CREATE TABLE play_history (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    song_id INTEGER NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    played_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    duration_played_sec INTEGER NOT NULL DEFAULT 0,
    source VARCHAR(50) NOT NULL DEFAULT 'player'
);

CREATE INDEX idx_play_history_user_played_at ON play_history (user_id, played_at DESC);
CREATE INDEX idx_play_history_song_id ON play_history (song_id);

CREATE TABLE download_entitlements (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    song_id INTEGER NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    UNIQUE (user_id, song_id)
);

CREATE INDEX idx_download_entitlements_user_id ON download_entitlements (user_id);

CREATE TABLE conversations (
    id SERIAL PRIMARY KEY,
    type conversation_type NOT NULL DEFAULT 'direct',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE conversation_members (
    conversation_id INTEGER NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conversation_members_user_id ON conversation_members (user_id);

CREATE TEMP TABLE _direct_conversation_map (
    user_a INTEGER NOT NULL,
    user_b INTEGER NOT NULL,
    conversation_id INTEGER NOT NULL,
    PRIMARY KEY (user_a, user_b)
) ON COMMIT DROP;

DO $$
DECLARE
    legacy RECORD;
    conv_id INTEGER;
    user_a INTEGER;
    user_b INTEGER;
BEGIN
    FOR legacy IN
        SELECT DISTINCT sender_id, receiver_id
        FROM messages
    LOOP
        user_a := LEAST(legacy.sender_id, legacy.receiver_id);
        user_b := GREATEST(legacy.sender_id, legacy.receiver_id);

        SELECT conversation_id
        INTO conv_id
        FROM _direct_conversation_map
        WHERE _direct_conversation_map.user_a = user_a
          AND _direct_conversation_map.user_b = user_b;

        IF conv_id IS NULL THEN
            INSERT INTO conversations (type)
            VALUES ('direct')
            RETURNING id INTO conv_id;

            INSERT INTO _direct_conversation_map (user_a, user_b, conversation_id)
            VALUES (user_a, user_b, conv_id);

            INSERT INTO conversation_members (conversation_id, user_id)
            VALUES (conv_id, user_a), (conv_id, user_b);
        END IF;
    END LOOP;
END $$;

ALTER TABLE messages RENAME TO messages_legacy;

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id INTEGER NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    msg_type message_content_type NOT NULL DEFAULT 'text',
    content TEXT NOT NULL DEFAULT '',
    song_id INTEGER REFERENCES songs (id) ON DELETE SET NULL,
    delivery_status message_delivery_status NOT NULL DEFAULT 'sent',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation_created_at ON messages (conversation_id, created_at);
CREATE INDEX idx_messages_sender_id ON messages (sender_id);

INSERT INTO messages (conversation_id, sender_id, msg_type, content, delivery_status, created_at, updated_at)
SELECT
    map.conversation_id,
    legacy.sender_id,
    CASE
        WHEN legacy.msg_type IN ('text', 'song', 'image', 'system') THEN legacy.msg_type::message_content_type
        ELSE 'text'::message_content_type
    END,
    legacy.content,
    'sent'::message_delivery_status,
    legacy.created_at,
    legacy.created_at
FROM messages_legacy AS legacy
JOIN _direct_conversation_map AS map
    ON map.user_a = LEAST(legacy.sender_id, legacy.receiver_id)
   AND map.user_b = GREATEST(legacy.sender_id, legacy.receiver_id);

CREATE TABLE message_receipts (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status message_delivery_status NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (message_id, user_id)
);

CREATE INDEX idx_message_receipts_user_id ON message_receipts (user_id);

INSERT INTO message_receipts (message_id, user_id, status, updated_at)
SELECT
    m.id,
    CASE
        WHEN m.sender_id = map.user_a THEN map.user_b
        ELSE map.user_a
    END,
    'delivered'::message_delivery_status,
    m.created_at
FROM messages AS m
JOIN _direct_conversation_map AS map
    ON map.conversation_id = m.conversation_id;

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type notification_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL DEFAULT '',
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_created_at ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;

DROP TABLE messages_legacy;

DROP INDEX IF EXISTS idx_messages_participants;
DROP INDEX IF EXISTS idx_songs_artist;
