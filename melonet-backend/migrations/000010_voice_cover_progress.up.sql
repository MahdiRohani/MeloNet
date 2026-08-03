-- Progress fields for live voice-cover UX (poll-friendly).

ALTER TABLE voice_covers
    ADD COLUMN IF NOT EXISTS progress_pct INTEGER NOT NULL DEFAULT 0
        CHECK (progress_pct >= 0 AND progress_pct <= 100),
    ADD COLUMN IF NOT EXISTS progress_stage TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS eta_seconds INTEGER NOT NULL DEFAULT 0
        CHECK (eta_seconds >= 0);
