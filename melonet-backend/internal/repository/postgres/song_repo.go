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
	COALESCE(al.title, '') AS album_title
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
	return r.ListFiltered(ctx, SongFilter{
		Category: category,
		Sort:     "newest",
		Page:     page,
		Limit:    limit,
	})
}

func (r *SongRepository) Search(ctx context.Context, query string, page, limit int) ([]db.Song, int, error) {
	return r.SearchSongs(ctx, query, page, limit)
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
