package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
)

type VoiceArtist struct {
	ID           int64
	Slug         string
	DisplayName  string
	ModelPath    string
	AvatarURL    string
	PitchDefault int
	Enabled      bool
	SortOrder    int
}

type VoiceCover struct {
	ID               int64
	SourceSongID     string
	TargetArtistID   int64
	TargetArtistSlug string
	TargetArtistName string
	Status           string
	AudioObjectKey   string
	CoverURL         string
	SourceTitle      string
	SourceArtist     string
	Error            string
	ProgressPct      int
	ProgressStage    string
	EtaSeconds       int
	AttemptCount     int
	RequestedBy      *int64
	CreatedAt        time.Time
	UpdatedAt        time.Time
	ReadyAt          *time.Time
}

type VoiceCoverRepository struct {
	db *DB
}

func NewVoiceCoverRepository(db *DB) *VoiceCoverRepository {
	return &VoiceCoverRepository{db: db}
}

func (r *VoiceCoverRepository) ListArtists(ctx context.Context, enabledOnly bool) ([]VoiceArtist, error) {
	query := `
		SELECT id, slug, display_name, model_path, avatar_url, pitch_default, enabled, sort_order
		FROM voice_artists
	`
	if enabledOnly {
		query += ` WHERE enabled = TRUE`
	}
	query += ` ORDER BY sort_order ASC, id ASC`

	rows, err := r.db.Pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("list voice artists: %w", err)
	}
	defer rows.Close()

	out := make([]VoiceArtist, 0)
	for rows.Next() {
		var a VoiceArtist
		if err := rows.Scan(
			&a.ID, &a.Slug, &a.DisplayName, &a.ModelPath, &a.AvatarURL,
			&a.PitchDefault, &a.Enabled, &a.SortOrder,
		); err != nil {
			return nil, fmt.Errorf("scan voice artist: %w", err)
		}
		out = append(out, a)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate voice artists: %w", err)
	}
	return out, nil
}

func (r *VoiceCoverRepository) GetArtistBySlug(ctx context.Context, slug string) (VoiceArtist, error) {
	row := r.db.Pool.QueryRow(ctx, `
		SELECT id, slug, display_name, model_path, avatar_url, pitch_default, enabled, sort_order
		FROM voice_artists
		WHERE slug = $1
	`, slug)

	var a VoiceArtist
	err := row.Scan(
		&a.ID, &a.Slug, &a.DisplayName, &a.ModelPath, &a.AvatarURL,
		&a.PitchDefault, &a.Enabled, &a.SortOrder,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return VoiceArtist{}, ErrNotFound
		}
		return VoiceArtist{}, fmt.Errorf("get voice artist: %w", err)
	}
	return a, nil
}

func (r *VoiceCoverRepository) GetByID(ctx context.Context, id int64) (VoiceCover, error) {
	row := r.db.Pool.QueryRow(ctx, `
		SELECT
			c.id, c.source_song_id, c.target_artist_id, a.slug, a.display_name,
			c.status, c.audio_object_key, c.cover_url, c.source_title, c.source_artist,
			c.error, c.progress_pct, c.progress_stage, c.eta_seconds,
			c.attempt_count, c.requested_by, c.created_at, c.updated_at, c.ready_at
		FROM voice_covers c
		JOIN voice_artists a ON a.id = c.target_artist_id
		WHERE c.id = $1
	`, id)

	cover, err := scanVoiceCover(row.Scan)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return VoiceCover{}, ErrNotFound
		}
		return VoiceCover{}, fmt.Errorf("get voice cover: %w", err)
	}
	return cover, nil
}

func (r *VoiceCoverRepository) GetBySongAndArtist(ctx context.Context, songID string, artistID int64) (VoiceCover, error) {
	row := r.db.Pool.QueryRow(ctx, `
		SELECT
			c.id, c.source_song_id, c.target_artist_id, a.slug, a.display_name,
			c.status, c.audio_object_key, c.cover_url, c.source_title, c.source_artist,
			c.error, c.progress_pct, c.progress_stage, c.eta_seconds,
			c.attempt_count, c.requested_by, c.created_at, c.updated_at, c.ready_at
		FROM voice_covers c
		JOIN voice_artists a ON a.id = c.target_artist_id
		WHERE c.source_song_id = $1 AND c.target_artist_id = $2
	`, songID, artistID)

	cover, err := scanVoiceCover(row.Scan)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return VoiceCover{}, ErrNotFound
		}
		return VoiceCover{}, fmt.Errorf("get voice cover by song/artist: %w", err)
	}
	return cover, nil
}

func (r *VoiceCoverRepository) ListReady(ctx context.Context, page, limit int) ([]VoiceCover, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM voice_covers WHERE status = 'ready'
	`).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count ready voice covers: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT
			c.id, c.source_song_id, c.target_artist_id, a.slug, a.display_name,
			c.status, c.audio_object_key, c.cover_url, c.source_title, c.source_artist,
			c.error, c.progress_pct, c.progress_stage, c.eta_seconds,
			c.attempt_count, c.requested_by, c.created_at, c.updated_at, c.ready_at
		FROM voice_covers c
		JOIN voice_artists a ON a.id = c.target_artist_id
		WHERE c.status = 'ready'
		ORDER BY COALESCE(c.ready_at, c.updated_at) DESC
		LIMIT $1 OFFSET $2
	`, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list ready voice covers: %w", err)
	}
	defer rows.Close()

	out := make([]VoiceCover, 0)
	for rows.Next() {
		cover, err := scanVoiceCover(rows.Scan)
		if err != nil {
			return nil, 0, fmt.Errorf("scan voice cover: %w", err)
		}
		out = append(out, cover)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate voice covers: %w", err)
	}
	return out, total, nil
}

// CreatePending inserts a new pending cover, or returns the existing row for the same cache key.
// requeue is true when a new job should be enqueued (new row, or previous failed).
func (r *VoiceCoverRepository) CreatePending(
	ctx context.Context,
	songID string,
	artistID int64,
	title, artist, coverURL string,
	requestedBy *int64,
) (cover VoiceCover, requeue bool, err error) {
	existing, getErr := r.GetBySongAndArtist(ctx, songID, artistID)
	if getErr == nil {
		if existing.Status == "failed" {
			_, err = r.db.Pool.Exec(ctx, `
				UPDATE voice_covers
				SET status = 'pending',
				    error = '',
				    audio_object_key = '',
				    progress_pct = 0,
				    progress_stage = '',
				    eta_seconds = 0,
				    attempt_count = 0,
				    source_title = $2,
				    source_artist = $3,
				    cover_url = $4,
				    requested_by = COALESCE($5, requested_by),
				    updated_at = NOW(),
				    ready_at = NULL
				WHERE id = $1
			`, existing.ID, title, artist, coverURL, requestedBy)
			if err != nil {
				return VoiceCover{}, false, fmt.Errorf("reset failed voice cover: %w", err)
			}
			updated, err := r.GetByID(ctx, existing.ID)
			if err != nil {
				return VoiceCover{}, false, err
			}
			return updated, true, nil
		}
		return existing, false, nil
	}
	if !errors.Is(getErr, ErrNotFound) {
		return VoiceCover{}, false, getErr
	}

	var id int64
	err = r.db.Pool.QueryRow(ctx, `
		INSERT INTO voice_covers (
			source_song_id, target_artist_id, status,
			cover_url, source_title, source_artist, requested_by
		)
		VALUES ($1, $2, 'pending', $3, $4, $5, $6)
		RETURNING id
	`, songID, artistID, coverURL, title, artist, requestedBy).Scan(&id)
	if err != nil {
		// Race: another request inserted the same cache key.
		if cover, getErr := r.GetBySongAndArtist(ctx, songID, artistID); getErr == nil {
			return cover, false, nil
		}
		return VoiceCover{}, false, fmt.Errorf("insert voice cover: %w", err)
	}

	created, err := r.GetByID(ctx, id)
	if err != nil {
		return VoiceCover{}, false, err
	}
	return created, true, nil
}

// Delete removes a voice cover row and returns the audio object key (if any) for storage cleanup.
func (r *VoiceCoverRepository) Delete(ctx context.Context, id int64) (audioObjectKey string, err error) {
	err = r.db.Pool.QueryRow(ctx, `
		DELETE FROM voice_covers
		WHERE id = $1
		RETURNING audio_object_key
	`, id).Scan(&audioObjectKey)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return "", ErrNotFound
		}
		return "", fmt.Errorf("delete voice cover: %w", err)
	}
	return audioObjectKey, nil
}

func scanVoiceCover(scan func(dest ...any) error) (VoiceCover, error) {
	var c VoiceCover
	err := scan(
		&c.ID, &c.SourceSongID, &c.TargetArtistID, &c.TargetArtistSlug, &c.TargetArtistName,
		&c.Status, &c.AudioObjectKey, &c.CoverURL, &c.SourceTitle, &c.SourceArtist,
		&c.Error, &c.ProgressPct, &c.ProgressStage, &c.EtaSeconds,
		&c.AttemptCount, &c.RequestedBy, &c.CreatedAt, &c.UpdatedAt, &c.ReadyAt,
	)
	return c, err
}
