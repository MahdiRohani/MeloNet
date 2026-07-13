package postgres

import (
	"context"
	"fmt"
)

type ArtistFollow struct {
	ArtistID   int64
	ArtistName string
	ImageURL   string
	Region     string
}

type ArtistFollowRepository struct {
	db *DB
}

func NewArtistFollowRepository(db *DB) *ArtistFollowRepository {
	return &ArtistFollowRepository{db: db}
}

func (r *ArtistFollowRepository) Follow(ctx context.Context, userID, artistID int64, name, imageURL, region string) (bool, error) {
	tag, err := r.db.Pool.Exec(ctx, `
		INSERT INTO artist_follows (user_id, artist_id, artist_name, image_url, region)
		VALUES ($1, $2, $3, $4, $5)
		ON CONFLICT (user_id, artist_id)
		DO UPDATE SET artist_name = EXCLUDED.artist_name, image_url = EXCLUDED.image_url, region = EXCLUDED.region
	`, userID, artistID, name, imageURL, region)
	if err != nil {
		return false, fmt.Errorf("follow artist: %w", err)
	}
	return tag.RowsAffected() > 0, nil
}

func (r *ArtistFollowRepository) Unfollow(ctx context.Context, userID, artistID int64) error {
	_, err := r.db.Pool.Exec(ctx, `
		DELETE FROM artist_follows WHERE user_id = $1 AND artist_id = $2
	`, userID, artistID)
	if err != nil {
		return fmt.Errorf("unfollow artist: %w", err)
	}
	return nil
}

func (r *ArtistFollowRepository) IsFollowing(ctx context.Context, userID, artistID int64) (bool, error) {
	var exists bool
	err := r.db.Pool.QueryRow(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM artist_follows WHERE user_id = $1 AND artist_id = $2
		)
	`, userID, artistID).Scan(&exists)
	if err != nil {
		return false, fmt.Errorf("check artist follow: %w", err)
	}
	return exists, nil
}

func (r *ArtistFollowRepository) ListFollowing(ctx context.Context, userID int64, page, limit int) ([]ArtistFollow, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM artist_follows WHERE user_id = $1
	`, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count artist follows: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT artist_id, artist_name, image_url, region
		FROM artist_follows
		WHERE user_id = $1
		ORDER BY created_at DESC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list artist follows: %w", err)
	}
	defer rows.Close()

	out := make([]ArtistFollow, 0)
	for rows.Next() {
		var a ArtistFollow
		if err := rows.Scan(&a.ArtistID, &a.ArtistName, &a.ImageURL, &a.Region); err != nil {
			return nil, 0, fmt.Errorf("scan artist follow: %w", err)
		}
		out = append(out, a)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate artist follows: %w", err)
	}
	return out, total, nil
}
