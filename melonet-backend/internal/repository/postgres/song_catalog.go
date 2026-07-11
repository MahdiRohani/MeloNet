package postgres

import (
	"context"
	"errors"
	"fmt"

	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

// paginationPlaceholders returns the 1-based positional placeholder numbers for
// the LIMIT and OFFSET arguments that are appended after argCount WHERE args.
func paginationPlaceholders(argCount int) (limitPos, offsetPos int) {
	return argCount + 1, argCount + 2
}

// buildSongListQuery assembles the paginated song list query. It keeps the
// WHERE and ORDER BY fragments separated by spaces so they never fuse into
// invalid SQL (e.g. "$1ORDER BY").
func buildSongListQuery(whereClause, orderClause string, limitPos, offsetPos int) string {
	return fmt.Sprintf(`
		SELECT%s%s %s %s
		LIMIT $%d OFFSET $%d
	`, songSelectColumns, songFromClause, whereClause, orderClause, limitPos, offsetPos)
}

func (r *SongRepository) ListFiltered(ctx context.Context, filter SongFilter) ([]db.Song, int, error) {
	filter = filter.normalized()
	offset := (filter.Page - 1) * filter.Limit

	whereClause, args := filter.whereClause(1)

	var total int
	countQuery := "SELECT COUNT(*) " + songFromClause + whereClause
	if err := r.db.Pool.QueryRow(ctx, countQuery, args...).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count songs: %w", err)
	}

	listArgs := append(args, filter.Limit, offset)
	limitPos, offsetPos := paginationPlaceholders(len(args))
	listQuery := buildSongListQuery(whereClause, filter.orderClause(), limitPos, offsetPos)

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

func (r *SongRepository) GetByID(ctx context.Context, songID int64) (db.Song, error) {
	var song db.Song
	err := r.db.Pool.QueryRow(ctx, `
		SELECT`+songSelectColumns+ songFromClause+ `
		WHERE s.id = $1
	`, songID).Scan(
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
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Song{}, ErrNotFound
		}
		return db.Song{}, fmt.Errorf("get song: %w", err)
	}
	return song, nil
}

func (r *SongRepository) ListPopular(ctx context.Context, page, limit int) ([]db.Song, int, error) {
	return r.ListFiltered(ctx, SongFilter{Sort: "popular", Page: page, Limit: limit})
}

func (r *SongRepository) ListNewest(ctx context.Context, page, limit int) ([]db.Song, int, error) {
	return r.ListFiltered(ctx, SongFilter{Sort: "newest", Page: page, Limit: limit})
}

func (r *SongRepository) ListTrending(ctx context.Context, page, limit int) ([]db.Song, int, error) {
	return r.ListFiltered(ctx, SongFilter{Sort: "trending", Page: page, Limit: limit})
}

func (r *SongRepository) SearchSongs(ctx context.Context, query string, page, limit int) ([]db.Song, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 20
	}
	offset := (page - 1) * limit
	pattern := "%" + query + "%"
	prefix := query + "%"

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
	`+songFromClause+`
		WHERE s.title ILIKE $1 OR a.name ILIKE $1 OR al.title ILIKE $1
	`, pattern).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count search songs: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT`+songSelectColumns+ songFromClause+ `
		WHERE s.title ILIKE $1 OR a.name ILIKE $1 OR al.title ILIKE $1
		ORDER BY
			CASE
				WHEN s.title ILIKE $2 THEN 0
				WHEN a.name ILIKE $2 THEN 1
				ELSE 2
			END,
			s.play_count DESC,
			s.id ASC
		LIMIT $3 OFFSET $4
	`, pattern, prefix, limit, offset)
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
