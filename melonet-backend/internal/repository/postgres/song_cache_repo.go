package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"melonet-backend/internal/domain/api"

	"github.com/jackc/pgx/v5"
)

type CachedSong struct {
	AudiusID    string
	Title       string
	ArtistName  string
	CoverURL    string
	AudioURL    string
	DurationSec int
	Genre       string
	PlayCount   int
	CachedAt    time.Time
}

type SongCacheRepository struct {
	db *DB
}

func NewSongCacheRepository(db *DB) *SongCacheRepository {
	return &SongCacheRepository{db: db}
}

func (r *SongCacheRepository) Upsert(ctx context.Context, song CachedSong) error {
	_, err := r.db.Pool.Exec(ctx, `
		INSERT INTO song_cache (
			audius_id, title, artist_name, cover_url, audio_url,
			duration_sec, genre, play_count, cached_at
		)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
		ON CONFLICT (audius_id) DO UPDATE SET
			title = EXCLUDED.title,
			artist_name = EXCLUDED.artist_name,
			cover_url = EXCLUDED.cover_url,
			audio_url = EXCLUDED.audio_url,
			duration_sec = EXCLUDED.duration_sec,
			genre = EXCLUDED.genre,
			cached_at = NOW()
	`,
		song.AudiusID,
		song.Title,
		song.ArtistName,
		song.CoverURL,
		song.AudioURL,
		song.DurationSec,
		song.Genre,
		song.PlayCount,
	)
	if err != nil {
		return fmt.Errorf("upsert song cache: %w", err)
	}
	return nil
}

func (r *SongCacheRepository) GetByID(ctx context.Context, audiusID string) (CachedSong, error) {
	row := r.db.Pool.QueryRow(ctx, `
		SELECT audius_id, title, artist_name, cover_url, audio_url,
		       duration_sec, genre, play_count, cached_at
		FROM song_cache
		WHERE audius_id = $1
	`, audiusID)

	song, err := scanCachedSong(row.Scan)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return CachedSong{}, ErrNotFound
		}
		return CachedSong{}, fmt.Errorf("get song cache: %w", err)
	}
	return song, nil
}

func (r *SongCacheRepository) IncrementPlayCount(ctx context.Context, audiusID string) (int, error) {
	var count int
	err := r.db.Pool.QueryRow(ctx, `
		UPDATE song_cache
		SET play_count = play_count + 1,
		    cached_at = cached_at
		WHERE audius_id = $1
		RETURNING play_count
	`, audiusID).Scan(&count)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return 0, ErrNotFound
		}
		return 0, fmt.Errorf("increment play count: %w", err)
	}
	return count, nil
}

func CachedSongToAPI(song CachedSong) api.SongResponse {
	return api.SongResponse{
		ID:            song.AudiusID,
		Title:         song.Title,
		Artist:        song.ArtistName,
		ArtistName:    song.ArtistName,
		CoverURL:      song.CoverURL,
		CoverImageURL: song.CoverURL,
		AudioURL:      song.AudioURL,
		Category:      song.Genre,
		Genre:         song.Genre,
		DurationSec:   song.DurationSec,
		PlayCount:     song.PlayCount,
	}
}

func scanCachedSong(scan func(dest ...any) error) (CachedSong, error) {
	var song CachedSong
	if err := scan(
		&song.AudiusID,
		&song.Title,
		&song.ArtistName,
		&song.CoverURL,
		&song.AudioURL,
		&song.DurationSec,
		&song.Genre,
		&song.PlayCount,
		&song.CachedAt,
	); err != nil {
		return CachedSong{}, err
	}
	return song, nil
}

const cachedSongSelectColumns = `
	sc.audius_id,
	sc.title,
	sc.artist_name,
	sc.cover_url,
	sc.audio_url,
	sc.duration_sec,
	sc.genre,
	sc.play_count,
	sc.cached_at
`

func scanCachedSongs(rows pgx.Rows) ([]CachedSong, error) {
	songs := make([]CachedSong, 0)
	for rows.Next() {
		song, err := scanCachedSong(rows.Scan)
		if err != nil {
			return nil, fmt.Errorf("scan cached song: %w", err)
		}
		songs = append(songs, song)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate cached songs: %w", err)
	}
	return songs, nil
}

func cachedSongsToAPI(songs []CachedSong) []api.SongResponse {
	out := make([]api.SongResponse, 0, len(songs))
	for _, song := range songs {
		out = append(out, CachedSongToAPI(song))
	}
	return out
}
