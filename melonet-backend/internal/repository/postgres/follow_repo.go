package postgres

import (
	"context"
	"fmt"

	"melonet-backend/internal/domain/db"
)

type FollowRepository struct {
	db *DB
}

func NewFollowRepository(db *DB) *FollowRepository {
	return &FollowRepository{db: db}
}

func (r *FollowRepository) Follow(ctx context.Context, followerID, followingID int64) (bool, error) {
	if followerID == followingID {
		return false, fmt.Errorf("cannot follow yourself")
	}

	tag, err := r.db.Pool.Exec(ctx, `
		INSERT INTO follows (follower_id, following_id)
		VALUES ($1, $2)
		ON CONFLICT DO NOTHING
	`, followerID, followingID)
	if err != nil {
		return false, fmt.Errorf("follow user: %w", err)
	}
	return tag.RowsAffected() > 0, nil
}

func (r *FollowRepository) Unfollow(ctx context.Context, followerID, followingID int64) error {
	_, err := r.db.Pool.Exec(ctx, `
		DELETE FROM follows
		WHERE follower_id = $1 AND following_id = $2
	`, followerID, followingID)
	if err != nil {
		return fmt.Errorf("unfollow user: %w", err)
	}
	return nil
}

func (r *FollowRepository) IsFollowing(ctx context.Context, followerID, followingID int64) (bool, error) {
	var exists bool
	err := r.db.Pool.QueryRow(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM follows
			WHERE follower_id = $1 AND following_id = $2
		)
	`, followerID, followingID).Scan(&exists)
	if err != nil {
		return false, fmt.Errorf("check follow: %w", err)
	}
	return exists, nil
}

func (r *FollowRepository) CountFollowers(ctx context.Context, userID int64) (int, error) {
	var count int
	err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM follows WHERE following_id = $1
	`, userID).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("count followers: %w", err)
	}
	return count, nil
}

func (r *FollowRepository) CountFollowing(ctx context.Context, userID int64) (int, error) {
	var count int
	err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM follows WHERE follower_id = $1
	`, userID).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("count following: %w", err)
	}
	return count, nil
}

func (r *FollowRepository) ListFollowers(ctx context.Context, userID int64, page, limit int) ([]db.UserSummary, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM follows WHERE following_id = $1
	`, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count followers: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT u.id, u.username, u.display_name, u.avatar_url, u.bio, u.is_premium
		FROM follows AS f
		INNER JOIN users AS u ON u.id = f.follower_id
		WHERE f.following_id = $1
		ORDER BY f.created_at DESC, u.id ASC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list followers: %w", err)
	}
	defer rows.Close()

	return scanUserSummaries(rows, total)
}

func (r *FollowRepository) ListFollowing(ctx context.Context, userID int64, page, limit int) ([]db.UserSummary, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM follows WHERE follower_id = $1
	`, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count following: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT u.id, u.username, u.display_name, u.avatar_url, u.bio, u.is_premium
		FROM follows AS f
		INNER JOIN users AS u ON u.id = f.following_id
		WHERE f.follower_id = $1
		ORDER BY f.created_at DESC, u.id ASC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list following: %w", err)
	}
	defer rows.Close()

	return scanUserSummaries(rows, total)
}

type userSummaryScanner interface {
	Next() bool
	Scan(dest ...any) error
	Err() error
}

func scanUserSummaries(rows userSummaryScanner, total int) ([]db.UserSummary, int, error) {
	users := make([]db.UserSummary, 0)
	for rows.Next() {
		var user db.UserSummary
		if err := rows.Scan(
			&user.ID,
			&user.Username,
			&user.DisplayName,
			&user.AvatarURL,
			&user.Bio,
			&user.IsPremium,
		); err != nil {
			return nil, 0, fmt.Errorf("scan user summary: %w", err)
		}
		users = append(users, user)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate user summaries: %w", err)
	}
	return users, total, nil
}
