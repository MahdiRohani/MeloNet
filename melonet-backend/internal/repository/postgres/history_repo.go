package postgres

import (
	"context"
	"fmt"
	"time"

	"melonet-backend/internal/domain/db"
)

type HistoryRepository struct {
	db *DB
}

func NewHistoryRepository(db *DB) *HistoryRepository {
	return &HistoryRepository{db: db}
}

type PlayRecord struct {
	SongID            int64
	PlayedAt          time.Time
	DurationPlayedSec int
}

func (r *HistoryRepository) RecordPlay(ctx context.Context, userID, songID int64, durationSec int, source string) (time.Time, error) {
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

	_, err = tx.Exec(ctx, `
		UPDATE songs
		SET play_count = play_count + 1,
		    updated_at = NOW()
		WHERE id = $1
	`, songID)
	if err != nil {
		return time.Time{}, fmt.Errorf("increment play count: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return time.Time{}, fmt.Errorf("commit play record: %w", err)
	}

	return playedAt, nil
}

func (r *HistoryRepository) GetPlayCount(ctx context.Context, songID int64) (int, error) {
	var count int
	err := r.db.Pool.QueryRow(ctx, `
		SELECT play_count FROM songs WHERE id = $1
	`, songID).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("get play count: %w", err)
	}
	return count, nil
}

func (r *HistoryRepository) ListRecentSongs(ctx context.Context, userID int64, page, limit int) ([]db.Song, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(DISTINCT song_id)
		FROM play_history
		WHERE user_id = $1
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
		SELECT`+songSelectColumns+`
		FROM recent AS r
		INNER JOIN songs AS s ON s.id = r.song_id
		INNER JOIN artists AS a ON a.id = s.artist_id
		LEFT JOIN genres AS g ON g.id = s.genre_id
		LEFT JOIN albums AS al ON al.id = s.album_id
		ORDER BY r.last_played DESC, s.id ASC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list recent songs: %w", err)
	}
	defer rows.Close()

	songs, err := scanSongs(rows)
	if err != nil {
		return nil, 0, err
	}
	return songs, total, nil
}
