-- Target voice artists (fixed whitelist of 10 singers) and cached cover jobs.

CREATE TABLE voice_artists (
    id SERIAL PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    model_path TEXT NOT NULL DEFAULT '',
    avatar_url TEXT NOT NULL DEFAULT '',
    pitch_default INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE voice_covers (
    id BIGSERIAL PRIMARY KEY,
    source_song_id TEXT NOT NULL,
    target_artist_id INTEGER NOT NULL REFERENCES voice_artists (id) ON DELETE RESTRICT,
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'processing', 'ready', 'failed')),
    audio_object_key TEXT NOT NULL DEFAULT '',
    cover_url TEXT NOT NULL DEFAULT '',
    source_title TEXT NOT NULL DEFAULT '',
    source_artist TEXT NOT NULL DEFAULT '',
    error TEXT NOT NULL DEFAULT '',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    requested_by INTEGER REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ready_at TIMESTAMPTZ,
    UNIQUE (source_song_id, target_artist_id)
);

CREATE INDEX idx_voice_covers_status_updated
    ON voice_covers (status, updated_at DESC);

CREATE INDEX idx_voice_covers_ready
    ON voice_covers (updated_at DESC)
    WHERE status = 'ready';

INSERT INTO voice_artists (slug, display_name, model_path, pitch_default, sort_order) VALUES
    ('shadmehr', 'شادمهر', 'voice-models/shadmehr/model.pth', 0, 1),
    ('morteza-pashaei', 'مرتضی پاشایی', 'voice-models/morteza-pashaei/model.pth', 0, 2),
    ('mohsen-yeganeh', 'محسن یگانه', 'voice-models/mohsen-yeganeh/model.pth', 0, 3),
    ('ebi', 'ابی', 'voice-models/ebi/model.pth', 0, 4),
    ('mahasti', 'مهستی', 'voice-models/mahasti/model.pth', 0, 5),
    ('hayedeh', 'هایده', 'voice-models/hayedeh/model.pth', 0, 6),
    ('googoosh', 'گوگوش', 'voice-models/googoosh/model.pth', 0, 7),
    ('moein', 'معین', 'voice-models/moein/model.pth', 0, 8),
    ('mohsen-chavoshi', 'محسن چاوشی', 'voice-models/mohsen-chavoshi/model.pth', 0, 9),
    ('siavash-ghomayshi', 'سیاوش قمیشی', 'voice-models/siavash-ghomayshi/model.pth', 0, 10);
