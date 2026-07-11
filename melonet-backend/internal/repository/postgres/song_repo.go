package postgres

import (
	"context"
	"fmt"

	"melonet-backend/internal/domain/db"
)

const songSelectColumns = `
	s.id,
	s.artist_id,
	s.album_id,
	s.genre_id,
	s.title,
	s.slug,
	s.cover_object_key,
	s.audio_object_key,
	s.cover_url,
	s.audio_url,
	s.category,
	s.lyrics,
	s.duration_sec,
	s.play_count,
	s.published_at,
	s.created_at,
	s.updated_at,
	a.name AS artist_name,
	COALESCE(g.name, s.category) AS genre_name,
	al.title AS album_title
`

const songFromClause = `
	FROM songs AS s
	INNER JOIN artists AS a ON a.id = s.artist_id
	LEFT JOIN genres AS g ON g.id = s.genre_id
	LEFT JOIN albums AS al ON al.id = s.album_id
`

type SongRepository struct {
	db *DB
}

func NewSongRepository(db *DB) *SongRepository {
	return &SongRepository{db: db}
}

func (r *SongRepository) List(ctx context.Context, category string, page, limit int) ([]db.Song, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 20
	}
	offset := (page - 1) * limit

	whereClause := ""
	countArgs := []any{}
	if category != "" {
		whereClause = " WHERE s.category = $1"
		countArgs = append(countArgs, category)
	}

	var total int
	countQuery := "SELECT COUNT(*) " + songFromClause + whereClause
	if err := r.db.Pool.QueryRow(ctx, countQuery, countArgs...).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count songs: %w", err)
	}

	listArgs := append(countArgs, limit, offset)
	listQuery := `
		SELECT` + songSelectColumns + songFromClause + whereClause + `
		ORDER BY s.id ASC
		LIMIT $` + fmt.Sprintf("%d", len(countArgs)+1) + `
		OFFSET $` + fmt.Sprintf("%d", len(countArgs)+2)

	rows, err := r.db.Pool.Query(ctx, listQuery, listArgs...)
	if err != nil {
		return nil, 0, fmt.Errorf("list songs: %w", err)
	}
	defer rows.Close()

	songs, err := scanSongs(rows)
	if err != nil {
		return nil, 0, err
	}

	return songs, total, nil
}

func (r *SongRepository) Search(ctx context.Context, query string, page, limit int) ([]db.Song, int, error) {
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
	`+songFromClause+`
		WHERE s.title ILIKE $1 OR a.name ILIKE $1
	`, pattern).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count search results: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT`+songSelectColumns+ songFromClause+ `
		WHERE s.title ILIKE $1 OR a.name ILIKE $1
		ORDER BY s.id ASC
		LIMIT $2 OFFSET $3
	`, pattern, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("search songs: %w", err)
	}
	defer rows.Close()

	songs, err := scanSongs(rows)
	if err != nil {
		return nil, 0, err
	}

	return songs, total, nil
}

type songScanner interface {
	Next() bool
	Scan(dest ...any) error
	Err() error
}

func scanSongs(rows songScanner) ([]db.Song, error) {
	songs := make([]db.Song, 0)
	for rows.Next() {
		song, err := scanSongRow(rows.Scan)
		if err != nil {
			return nil, err
		}
		songs = append(songs, song)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate songs: %w", err)
	}
	return songs, nil
}

func scanSongRow(scan func(dest ...any) error) (db.Song, error) {
	var song db.Song
	if err := scan(
		&song.ID,
		&song.ArtistID,
		&song.AlbumID,
		&song.GenreID,
		&song.Title,
		&song.Slug,
		&song.CoverObjectKey,
		&song.AudioObjectKey,
		&song.CoverURL,
		&song.AudioURL,
		&song.Category,
		&song.Lyrics,
		&song.DurationSec,
		&song.PlayCount,
		&song.PublishedAt,
		&song.CreatedAt,
		&song.UpdatedAt,
		&song.ArtistName,
		&song.GenreName,
		&song.AlbumTitle,
	); err != nil {
		return db.Song{}, fmt.Errorf("scan song: %w", err)
	}
	return song, nil
}
