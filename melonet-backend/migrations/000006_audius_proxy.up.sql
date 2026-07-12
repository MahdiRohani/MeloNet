-- Switch song references from local integer IDs to Audius string IDs.
-- Dev data in likes/playlists/history is reset because old numeric IDs
-- do not map to Audius track identifiers.

TRUNCATE TABLE likes, play_history, playlist_songs, download_entitlements RESTART IDENTITY CASCADE;
UPDATE messages SET song_id = NULL WHERE song_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS song_cache (
    audius_id     TEXT PRIMARY KEY,
    title         TEXT NOT NULL DEFAULT '',
    artist_name   TEXT NOT NULL DEFAULT '',
    cover_url     TEXT NOT NULL DEFAULT '',
    audio_url     TEXT NOT NULL DEFAULT '',
    duration_sec  INTEGER NOT NULL DEFAULT 0,
    genre         TEXT NOT NULL DEFAULT '',
    play_count    INTEGER NOT NULL DEFAULT 0,
    cached_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE likes DROP CONSTRAINT IF EXISTS likes_song_id_fkey;
ALTER TABLE likes ALTER COLUMN song_id TYPE TEXT USING song_id::TEXT;

ALTER TABLE play_history DROP CONSTRAINT IF EXISTS play_history_song_id_fkey;
ALTER TABLE play_history ALTER COLUMN song_id TYPE TEXT USING song_id::TEXT;

ALTER TABLE playlist_songs DROP CONSTRAINT IF EXISTS playlist_songs_song_id_fkey;
ALTER TABLE playlist_songs ALTER COLUMN song_id TYPE TEXT USING song_id::TEXT;

ALTER TABLE download_entitlements DROP CONSTRAINT IF EXISTS download_entitlements_song_id_fkey;
ALTER TABLE download_entitlements ALTER COLUMN song_id TYPE TEXT USING song_id::TEXT;

ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_song_id_fkey;
ALTER TABLE messages ALTER COLUMN song_id TYPE TEXT USING song_id::TEXT;

CREATE INDEX IF NOT EXISTS idx_song_cache_cached_at ON song_cache (cached_at DESC);
