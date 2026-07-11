package postgres

import (
	"context"
	"fmt"
	"strings"
)

type SongMediaState struct {
	SongID int64
	Seeded bool
}

type SongMediaUpdate struct {
	SongID         int64
	Title          string
	Slug           string
	ArtistID       int64
	GenreID        int64
	Category       string
	AudioObjectKey string
	CoverObjectKey string
	AudioURL       string
	CoverURL       string
	DurationSec    int
}

type SeedRepository struct {
	db *DB
}

func NewSeedRepository(db *DB) *SeedRepository {
	return &SeedRepository{db: db}
}

func (r *SeedRepository) GetSongMediaState(ctx context.Context, songID int64) (SongMediaState, error) {
	var audioKey string
	err := r.db.Pool.QueryRow(ctx, `
		SELECT audio_object_key
		FROM songs
		WHERE id = $1
	`, songID).Scan(&audioKey)
	if err != nil {
		return SongMediaState{}, fmt.Errorf("get song media state: %w", err)
	}

	return SongMediaState{
		SongID: songID,
		Seeded: strings.HasPrefix(audioKey, "catalog/"),
	}, nil
}

func (r *SeedRepository) EnsureArtist(ctx context.Context, name string) (int64, error) {
	slug := slugify(name)
	if slug == "" {
		return 0, fmt.Errorf("invalid artist name %q", name)
	}

	var id int64
	err := r.db.Pool.QueryRow(ctx, `
		INSERT INTO artists (name, slug)
		VALUES ($1, $2)
		ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name
		RETURNING id
	`, name, slug).Scan(&id)
	if err != nil {
		return 0, fmt.Errorf("ensure artist: %w", err)
	}
	return id, nil
}

func (r *SeedRepository) EnsureGenre(ctx context.Context, name string) (int64, error) {
	slug := slugify(name)
	if slug == "" {
		return 0, fmt.Errorf("invalid genre name %q", name)
	}

	var id int64
	err := r.db.Pool.QueryRow(ctx, `
		INSERT INTO genres (name, slug)
		VALUES ($1, $2)
		ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name
		RETURNING id
	`, name, slug).Scan(&id)
	if err != nil {
		return 0, fmt.Errorf("ensure genre: %w", err)
	}
	return id, nil
}

func (r *SeedRepository) UpdateSongMedia(ctx context.Context, update SongMediaUpdate) error {
	_, err := r.db.Pool.Exec(ctx, `
		UPDATE songs
		SET title = $2,
		    slug = $3,
		    artist_id = $4,
		    genre_id = $5,
		    category = $6,
		    audio_object_key = $7,
		    cover_object_key = $8,
		    audio_url = $9,
		    cover_url = $10,
		    duration_sec = $11,
		    updated_at = NOW()
		WHERE id = $1
	`,
		update.SongID,
		update.Title,
		update.Slug,
		update.ArtistID,
		update.GenreID,
		update.Category,
		update.AudioObjectKey,
		update.CoverObjectKey,
		update.AudioURL,
		update.CoverURL,
		update.DurationSec,
	)
	if err != nil {
		return fmt.Errorf("update song media: %w", err)
	}
	return nil
}

func (r *SeedRepository) RefreshAlbumCovers(ctx context.Context) error {
	_, err := r.db.Pool.Exec(ctx, `
		UPDATE albums AS al
		SET cover_object_key = s.cover_object_key,
		    cover_url = s.cover_url,
		    updated_at = NOW()
		FROM songs AS s
		WHERE s.album_id = al.id
		  AND s.cover_object_key LIKE 'catalog/%'
		  AND s.id = (
		      SELECT MIN(s2.id)
		      FROM songs AS s2
		      WHERE s2.album_id = al.id
		        AND s2.cover_object_key LIKE 'catalog/%'
		  )
	`)
	if err != nil {
		return fmt.Errorf("refresh album covers: %w", err)
	}

	_, err = r.db.Pool.Exec(ctx, `
		UPDATE playlists
		SET cover_url = '/api/media/catalog/covers/1/cover.jpg',
		    updated_at = NOW()
		WHERE is_system = TRUE
		  AND cover_url LIKE '/static/%'
	`)
	if err != nil {
		return fmt.Errorf("refresh playlist covers: %w", err)
	}

	return nil
}

func slugify(value string) string {
	var b []byte
	lastDash := false
	for i := 0; i < len(value); i++ {
		c := value[i]
		if (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') {
			b = append(b, c)
			lastDash = false
			continue
		}
		if c >= 'A' && c <= 'Z' {
			b = append(b, c+('a'-'A'))
			lastDash = false
			continue
		}
		if !lastDash && len(b) > 0 {
			b = append(b, '-')
			lastDash = true
		}
	}
	for len(b) > 0 && b[len(b)-1] == '-' {
		b = b[:len(b)-1]
	}
	return string(b)
}
