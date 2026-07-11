package postgres

import (
	"context"
	"fmt"

	"melonet-backend/internal/domain"
)

type SongRepository struct {
	db *DB
}

func NewSongRepository(db *DB) *SongRepository {
	return &SongRepository{db: db}
}

func (r *SongRepository) List(ctx context.Context, category string, page, limit int) ([]domain.Song, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 20
	}
	offset := (page - 1) * limit

	var total int
	countQuery := `SELECT COUNT(*) FROM songs`
	countArgs := []any{}
	whereClause := ""

	if category != "" {
		whereClause = ` WHERE category = $1`
		countArgs = append(countArgs, category)
	}

	if err := r.db.Pool.QueryRow(ctx, countQuery+whereClause, countArgs...).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count songs: %w", err)
	}

	listQuery := `
		SELECT id, title, artist, cover_url, audio_url, category, lyrics, duration_sec
		FROM songs` + whereClause + `
		ORDER BY id ASC
		LIMIT $` + fmt.Sprintf("%d", len(countArgs)+1) + ` OFFSET $` + fmt.Sprintf("%d", len(countArgs)+2)

	listArgs := append(countArgs, limit, offset)
	rows, err := r.db.Pool.Query(ctx, listQuery, listArgs...)
	if err != nil {
		return nil, 0, fmt.Errorf("list songs: %w", err)
	}
	defer rows.Close()

	songs := make([]domain.Song, 0)
	for rows.Next() {
		var song domain.Song
		if err := rows.Scan(
			&song.ID,
			&song.Title,
			&song.Artist,
			&song.CoverURL,
			&song.AudioURL,
			&song.Category,
			&song.Lyrics,
			&song.DurationSec,
		); err != nil {
			return nil, 0, fmt.Errorf("scan song: %w", err)
		}
		songs = append(songs, song)
	}

	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate songs: %w", err)
	}

	return songs, total, nil
}

func (r *SongRepository) Search(ctx context.Context, query string, page, limit int) ([]domain.Song, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 20
	}
	offset := (page - 1) * limit
	pattern := "%" + query + "%"

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM songs
		WHERE title ILIKE $1 OR artist ILIKE $1
	`, pattern).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count search results: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT id, title, artist, cover_url, audio_url, category, lyrics, duration_sec
		FROM songs
		WHERE title ILIKE $1 OR artist ILIKE $1
		ORDER BY id ASC
		LIMIT $2 OFFSET $3
	`, pattern, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("search songs: %w", err)
	}
	defer rows.Close()

	songs := make([]domain.Song, 0)
	for rows.Next() {
		var song domain.Song
		if err := rows.Scan(
			&song.ID,
			&song.Title,
			&song.Artist,
			&song.CoverURL,
			&song.AudioURL,
			&song.Category,
			&song.Lyrics,
			&song.DurationSec,
		); err != nil {
			return nil, 0, fmt.Errorf("scan song: %w", err)
		}
		songs = append(songs, song)
	}

	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate search results: %w", err)
	}

	return songs, total, nil
}
