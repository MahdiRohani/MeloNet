ALTER TABLE voice_covers
    DROP COLUMN IF EXISTS eta_seconds,
    DROP COLUMN IF EXISTS progress_stage,
    DROP COLUMN IF EXISTS progress_pct;
