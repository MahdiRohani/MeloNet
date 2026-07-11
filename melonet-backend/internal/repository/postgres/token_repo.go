package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

type TokenRepository struct {
	db *DB
}

func NewTokenRepository(db *DB) *TokenRepository {
	return &TokenRepository{db: db}
}

func (r *TokenRepository) Store(ctx context.Context, token db.RefreshToken) error {
	_, err := r.db.Pool.Exec(ctx, `
		INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
		VALUES ($1, $2, $3)
	`, token.UserID, token.TokenHash, token.ExpiresAt)
	if err != nil {
		return fmt.Errorf("store refresh token: %w", err)
	}
	return nil
}

func (r *TokenRepository) GetValid(ctx context.Context, tokenHash string) (db.RefreshToken, error) {
	var token db.RefreshToken
	err := r.db.Pool.QueryRow(ctx, `
		SELECT id, user_id, token_hash, expires_at, revoked_at, created_at
		FROM refresh_tokens
		WHERE token_hash = $1
		  AND revoked_at IS NULL
		  AND expires_at > NOW()
	`, tokenHash).Scan(
		&token.ID,
		&token.UserID,
		&token.TokenHash,
		&token.ExpiresAt,
		&token.RevokedAt,
		&token.CreatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.RefreshToken{}, ErrNotFound
		}
		return db.RefreshToken{}, fmt.Errorf("get refresh token: %w", err)
	}
	return token, nil
}

func (r *TokenRepository) Revoke(ctx context.Context, tokenHash string) error {
	_, err := r.db.Pool.Exec(ctx, `
		UPDATE refresh_tokens
		SET revoked_at = NOW()
		WHERE token_hash = $1 AND revoked_at IS NULL
	`, tokenHash)
	if err != nil {
		return fmt.Errorf("revoke refresh token: %w", err)
	}
	return nil
}

func (r *TokenRepository) RevokeAllForUser(ctx context.Context, userID int64) error {
	_, err := r.db.Pool.Exec(ctx, `
		UPDATE refresh_tokens
		SET revoked_at = NOW()
		WHERE user_id = $1 AND revoked_at IS NULL
	`, userID)
	if err != nil {
		return fmt.Errorf("revoke user refresh tokens: %w", err)
	}
	return nil
}

func (r *TokenRepository) CleanupExpired(ctx context.Context, before time.Time) error {
	_, err := r.db.Pool.Exec(ctx, `
		DELETE FROM refresh_tokens
		WHERE expires_at < $1
	`, before)
	if err != nil {
		return fmt.Errorf("cleanup refresh tokens: %w", err)
	}
	return nil
}
