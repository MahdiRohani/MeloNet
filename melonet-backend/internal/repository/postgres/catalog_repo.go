package postgres

import (
	"context"
	"errors"
	"fmt"

	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

type CatalogRepository struct {
	db *DB
}

func NewCatalogRepository(db *DB) *CatalogRepository {
	return &CatalogRepository{db: db}
}

func (r *CatalogRepository) ListArtists(ctx context.Context, query string, page, limit int) ([]db.Artist, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 20
	}
	offset := (page - 1) * limit

	whereClause := ""
	args := []any{}
	if query != "" {
		whereClause = " WHERE a.name ILIKE $1 OR a.slug ILIKE $1"
		args = append(args, "%"+query+"%")
	}

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM artists AS a`+whereClause, args...).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count artists: %w", err)
	}

	listArgs := append(args, limit, offset)
	limitPos := len(args) - 1
	offsetPos := len(args)
	rows, err := r.db.Pool.Query(ctx, fmt.Sprintf(`
		SELECT
			a.id, a.name, a.slug, a.bio, a.image_object_key, a.image_url,
			a.created_at, a.updated_at,
			COUNT(s.id) AS song_count
		FROM artists AS a
		LEFT JOIN songs AS s ON s.artist_id = a.id
		%s
		GROUP BY a.id
		ORDER BY a.name ASC
		LIMIT $%d OFFSET $%d
	`, whereClause, limitPos, offsetPos), listArgs...)
	if err != nil {
		return nil, 0, fmt.Errorf("list artists: %w", err)
	}
	defer rows.Close()

	return scanArtists(rows, total)
}

func (r *CatalogRepository) GetArtistByID(ctx context.Context, artistID int64) (db.Artist, error) {
	var artist db.Artist
	var songCount int
	err := r.db.Pool.QueryRow(ctx, `
		SELECT
			a.id, a.name, a.slug, a.bio, a.image_object_key, a.image_url,
			a.created_at, a.updated_at,
			COUNT(s.id) AS song_count
		FROM artists AS a
		LEFT JOIN songs AS s ON s.artist_id = a.id
		WHERE a.id = $1
		GROUP BY a.id
	`, artistID).Scan(
		&artist.ID,
		&artist.Name,
		&artist.Slug,
		&artist.Bio,
		&artist.ImageObjectKey,
		&artist.ImageURL,
		&artist.CreatedAt,
		&artist.UpdatedAt,
		&songCount,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Artist{}, ErrNotFound
		}
		return db.Artist{}, fmt.Errorf("get artist: %w", err)
	}
	artist.SongCount = songCount
	return artist, nil
}

func (r *CatalogRepository) SearchArtists(ctx context.Context, query string, page, limit int) ([]db.Artist, int, error) {
	return r.ListArtists(ctx, query, page, limit)
}

func (r *CatalogRepository) GetAlbumByID(ctx context.Context, albumID int64) (db.Album, error) {
	var album db.Album
	var artistName string
	var songCount int
	err := r.db.Pool.QueryRow(ctx, `
		SELECT
			al.id, al.artist_id, al.title, al.slug, al.cover_object_key, al.cover_url,
			al.release_date, al.created_at, al.updated_at,
			a.name,
			COUNT(s.id) AS song_count
		FROM albums AS al
		INNER JOIN artists AS a ON a.id = al.artist_id
		LEFT JOIN songs AS s ON s.album_id = al.id
		WHERE al.id = $1
		GROUP BY al.id, a.name
	`, albumID).Scan(
		&album.ID,
		&album.ArtistID,
		&album.Title,
		&album.Slug,
		&album.CoverObjectKey,
		&album.CoverURL,
		&album.ReleaseDate,
		&album.CreatedAt,
		&album.UpdatedAt,
		&artistName,
		&songCount,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Album{}, ErrNotFound
		}
		return db.Album{}, fmt.Errorf("get album: %w", err)
	}
	return albumFromScan(album, artistName, songCount), nil
}

func (r *CatalogRepository) SearchAlbums(ctx context.Context, query string, page, limit int) ([]db.Album, int, error) {
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
		FROM albums AS al
		INNER JOIN artists AS a ON a.id = al.artist_id
		WHERE al.title ILIKE $1 OR a.name ILIKE $1
	`, pattern).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count albums: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT
			al.id, al.artist_id, al.title, al.slug, al.cover_object_key, al.cover_url,
			al.release_date, al.created_at, al.updated_at,
			a.name,
			COUNT(s.id) AS song_count
		FROM albums AS al
		INNER JOIN artists AS a ON a.id = al.artist_id
		LEFT JOIN songs AS s ON s.album_id = al.id
		WHERE al.title ILIKE $1 OR a.name ILIKE $1
		GROUP BY al.id, a.name
		ORDER BY
			CASE WHEN al.title ILIKE $2 THEN 0 ELSE 1 END,
			al.title ASC
		LIMIT $3 OFFSET $4
	`, pattern, prefix, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("search albums: %w", err)
	}
	defer rows.Close()

	albums := make([]db.Album, 0)
	for rows.Next() {
		var album db.Album
		var artistName string
		var songCount int
		if err := rows.Scan(
			&album.ID,
			&album.ArtistID,
			&album.Title,
			&album.Slug,
			&album.CoverObjectKey,
			&album.CoverURL,
			&album.ReleaseDate,
			&album.CreatedAt,
			&album.UpdatedAt,
			&artistName,
			&songCount,
		); err != nil {
			return nil, 0, fmt.Errorf("scan album: %w", err)
		}
		albums = append(albums, albumFromScan(album, artistName, songCount))
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate albums: %w", err)
	}

	return albums, total, nil
}

func albumFromScan(album db.Album, artistName string, songCount int) db.Album {
	album.ArtistName = artistName
	album.SongCount = songCount
	return album
}

func (r *CatalogRepository) ListGenres(ctx context.Context, page, limit int) ([]db.Genre, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 50
	}
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `SELECT COUNT(*) FROM genres`).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count genres: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT g.id, g.name, g.slug, g.created_at, COUNT(s.id) AS song_count
		FROM genres AS g
		LEFT JOIN songs AS s ON s.genre_id = g.id
		GROUP BY g.id
		ORDER BY g.name ASC
		LIMIT $1 OFFSET $2
	`, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list genres: %w", err)
	}
	defer rows.Close()

	genres := make([]db.Genre, 0)
	for rows.Next() {
		var genre db.Genre
		if err := rows.Scan(&genre.ID, &genre.Name, &genre.Slug, &genre.CreatedAt, &genre.SongCount); err != nil {
			return nil, 0, fmt.Errorf("scan genre: %w", err)
		}
		genres = append(genres, genre)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate genres: %w", err)
	}

	return genres, total, nil
}

func (r *CatalogRepository) GetGenreByID(ctx context.Context, genreID int64) (db.Genre, error) {
	var genre db.Genre
	err := r.db.Pool.QueryRow(ctx, `
		SELECT g.id, g.name, g.slug, g.created_at, COUNT(s.id) AS song_count
		FROM genres AS g
		LEFT JOIN songs AS s ON s.genre_id = g.id
		WHERE g.id = $1
		GROUP BY g.id
	`, genreID).Scan(&genre.ID, &genre.Name, &genre.Slug, &genre.CreatedAt, &genre.SongCount)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Genre{}, ErrNotFound
		}
		return db.Genre{}, fmt.Errorf("get genre: %w", err)
	}
	return genre, nil
}

func (r *CatalogRepository) SearchGenres(ctx context.Context, query string, page, limit int) ([]db.Genre, int, error) {
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
		SELECT COUNT(*) FROM genres WHERE name ILIKE $1 OR slug ILIKE $1
	`, pattern).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count genres: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT g.id, g.name, g.slug, g.created_at, COUNT(s.id) AS song_count
		FROM genres AS g
		LEFT JOIN songs AS s ON s.genre_id = g.id
		WHERE g.name ILIKE $1 OR g.slug ILIKE $1
		GROUP BY g.id
		ORDER BY g.name ASC
		LIMIT $2 OFFSET $3
	`, pattern, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("search genres: %w", err)
	}
	defer rows.Close()

	genres := make([]db.Genre, 0)
	for rows.Next() {
		var genre db.Genre
		if err := rows.Scan(&genre.ID, &genre.Name, &genre.Slug, &genre.CreatedAt, &genre.SongCount); err != nil {
			return nil, 0, fmt.Errorf("scan genre: %w", err)
		}
		genres = append(genres, genre)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate genres: %w", err)
	}

	return genres, total, nil
}

type artistScanner interface {
	Next() bool
	Scan(dest ...any) error
	Err() error
}

func scanArtists(rows artistScanner, total int) ([]db.Artist, int, error) {
	artists := make([]db.Artist, 0)
	for rows.Next() {
		var artist db.Artist
		var songCount int
		if err := rows.Scan(
			&artist.ID,
			&artist.Name,
			&artist.Slug,
			&artist.Bio,
			&artist.ImageObjectKey,
			&artist.ImageURL,
			&artist.CreatedAt,
			&artist.UpdatedAt,
			&songCount,
		); err != nil {
			return nil, 0, fmt.Errorf("scan artist: %w", err)
		}
		artist.SongCount = songCount
		artists = append(artists, artist)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate artists: %w", err)
	}
	return artists, total, nil
}
