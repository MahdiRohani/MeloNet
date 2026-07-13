-- Follows for curated (Audius-sourced) artists. Artists are not real DB rows,
-- so we store a synthetic artist_id plus a denormalized name/image snapshot.
CREATE TABLE artist_follows (
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    artist_id BIGINT NOT NULL,
    artist_name TEXT NOT NULL,
    image_url TEXT NOT NULL DEFAULT '',
    region TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, artist_id)
);

CREATE INDEX idx_artist_follows_user ON artist_follows (user_id, created_at DESC);
