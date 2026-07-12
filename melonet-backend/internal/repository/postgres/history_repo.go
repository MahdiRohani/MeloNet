package postgres

import (
	"context"
	"fmt"
	"time"
)

type HistoryRepository struct {
	db *DB
}

func NewHistoryRepository(db *DB) *HistoryRepository {
	return &HistoryRepository{db: db}
}

func (r *HistoryRepository) RecordPlay(ctx context.Context, userID int64, songID string, durationSec int, source string) (time.Time, error) {
	if source == "" {
		source = "player"
	}

	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return time.Time{}, fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	var playedAt time.Time
	err = tx.QueryRow(ctx, `
		INSERT INTO play_history (user_id, song_id, duration_played_sec, source)
		VALUES ($1, $2, $3, $4)
		RETURNING played_at
	`, userID, songID, durationSec, source).Scan(&playedAt)
	if err != nil {
		return time.Time{}, fmt.Errorf("insert play history: %w", err)
	}

	var playCount int
	err = tx.QueryRow(ctx, `
		UPDATE song_cache
		SET play_count = play_count + 1
		WHERE audius_id = $1
		RETURNING play_count
	`, songID).Scan(&playCount)
	if err != nil {
		return time.Time{}, fmt.Errorf("increment play count: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return time.Time{}, fmt.Errorf("commit play record: %w", err)
	}

	return playedAt, nil
}

func (r *HistoryRepository) GetPlayCount(ctx context.Context, songID string) (int, error) {
	var count int
	err := r.db.Pool.QueryRow(ctx, `
		SELECT play_count FROM song_cache WHERE audius_id = $1
	`, songID).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("get play count: %w", err)
	}
	return count, nil
}

func (r *HistoryRepository) ListRecentSongs(ctx context.Context, userID int64, page, limit int) ([]CachedSong, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(DISTINCT ph.song_id)
		FROM play_history AS ph
		INNER JOIN song_cache AS sc ON sc.audius_id = ph.song_id
		WHERE ph.user_id = $1
	`, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count recent songs: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		WITH recent AS (
			SELECT song_id, MAX(played_at) AS last_played
			FROM play_history
			WHERE user_id = $1
			GROUP BY song_id
		)
		SELECT`+cachedSongSelectColumns+`
		FROM recent AS r
		INNER JOIN song_cache AS sc ON sc.audius_id = r.song_id
		ORDER BY r.last_played DESC, sc.audius_id ASC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list recent songs: %w", err)
	}
	defer rows.Close()

	songs, err := scanCachedSongs(rows)
	if err != nil {
		return nil, 0, err
	}
	return songs, total, nil
}
