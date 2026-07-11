package postgres

import (
	"context"
	"fmt"

	"melonet-backend/internal/domain/db"
)

type LikeRepository struct {
	db *DB
}

func NewLikeRepository(db *DB) *LikeRepository {
	return &LikeRepository{db: db}
}

func (r *LikeRepository) Like(ctx context.Context, userID, songID int64) error {
	_, err := r.db.Pool.Exec(ctx, `
		INSERT INTO likes (user_id, song_id)
		VALUES ($1, $2)
		ON CONFLICT DO NOTHING
	`, userID, songID)
	if err != nil {
		return fmt.Errorf("like song: %w", err)
	}
	return nil
}

func (r *LikeRepository) Unlike(ctx context.Context, userID, songID int64) error {
	_, err := r.db.Pool.Exec(ctx, `
		DELETE FROM likes
		WHERE user_id = $1 AND song_id = $2
	`, userID, songID)
	if err != nil {
		return fmt.Errorf("unlike song: %w", err)
	}
	return nil
}

func (r *LikeRepository) IsLiked(ctx context.Context, userID, songID int64) (bool, error) {
	var exists bool
	err := r.db.Pool.QueryRow(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM likes WHERE user_id = $1 AND song_id = $2
		)
	`, userID, songID).Scan(&exists)
	if err != nil {
		return false, fmt.Errorf("check like: %w", err)
	}
	return exists, nil
}

func (r *LikeRepository) ListLikedSongs(ctx context.Context, userID int64, page, limit int) ([]db.Song, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM likes AS l
		WHERE l.user_id = $1
	`, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count liked songs: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT`+songSelectColumns+`
		FROM likes AS l
		INNER JOIN songs AS s ON s.id = l.song_id
		INNER JOIN artists AS a ON a.id = s.artist_id
		LEFT JOIN genres AS g ON g.id = s.genre_id
		LEFT JOIN albums AS al ON al.id = s.album_id
		WHERE l.user_id = $1
		ORDER BY l.created_at DESC, s.id ASC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list liked songs: %w", err)
	}
	defer rows.Close()

	songs, err := scanSongs(rows)
	if err != nil {
		return nil, 0, err
	}
	return songs, total, nil
}
