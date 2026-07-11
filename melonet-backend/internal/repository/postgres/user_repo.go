package postgres

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

type UserRepository struct {
	db *DB
}

func NewUserRepository(db *DB) *UserRepository {
	return &UserRepository{db: db}
}

const userSelectColumns = `
	id, username, email, password_hash, display_name, bio,
	avatar_url, avatar_object_key, is_premium, premium_until,
	created_at, updated_at
`

func (r *UserRepository) Create(ctx context.Context, user db.User) (db.User, error) {
	err := r.db.Pool.QueryRow(ctx, `
		INSERT INTO users (username, email, password_hash, display_name, bio, avatar_url, is_premium)
		VALUES ($1, $2, $3, $4, $5, '', FALSE)
		RETURNING `+userSelectColumns+`
	`, user.Username, user.Email, user.PasswordHash, user.DisplayName, user.Bio).Scan(
		&user.ID,
		&user.Username,
		&user.Email,
		&user.PasswordHash,
		&user.DisplayName,
		&user.Bio,
		&user.AvatarURL,
		&user.AvatarObjectKey,
		&user.IsPremium,
		&user.PremiumUntil,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if err != nil {
		return db.User{}, fmt.Errorf("create user: %w", err)
	}
	return user, nil
}

func (r *UserRepository) GetByID(ctx context.Context, userID int64) (db.User, error) {
	var user db.User
	err := r.db.Pool.QueryRow(ctx, `
		SELECT `+userSelectColumns+`
		FROM users
		WHERE id = $1
	`, userID).Scan(
		&user.ID,
		&user.Username,
		&user.Email,
		&user.PasswordHash,
		&user.DisplayName,
		&user.Bio,
		&user.AvatarURL,
		&user.AvatarObjectKey,
		&user.IsPremium,
		&user.PremiumUntil,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.User{}, ErrNotFound
		}
		return db.User{}, fmt.Errorf("get user by id: %w", err)
	}
	return user, nil
}

func (r *UserRepository) GetByUsername(ctx context.Context, username string) (db.User, error) {
	var user db.User
	err := r.db.Pool.QueryRow(ctx, `
		SELECT `+userSelectColumns+`
		FROM users
		WHERE lower(username) = lower($1)
	`, username).Scan(
		&user.ID,
		&user.Username,
		&user.Email,
		&user.PasswordHash,
		&user.DisplayName,
		&user.Bio,
		&user.AvatarURL,
		&user.AvatarObjectKey,
		&user.IsPremium,
		&user.PremiumUntil,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.User{}, ErrNotFound
		}
		return db.User{}, fmt.Errorf("get user by username: %w", err)
	}
	return user, nil
}

func (r *UserRepository) GetByEmail(ctx context.Context, email string) (db.User, error) {
	var user db.User
	err := r.db.Pool.QueryRow(ctx, `
		SELECT `+userSelectColumns+`
		FROM users
		WHERE lower(email) = lower($1)
	`, email).Scan(
		&user.ID,
		&user.Username,
		&user.Email,
		&user.PasswordHash,
		&user.DisplayName,
		&user.Bio,
		&user.AvatarURL,
		&user.AvatarObjectKey,
		&user.IsPremium,
		&user.PremiumUntil,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.User{}, ErrNotFound
		}
		return db.User{}, fmt.Errorf("get user by email: %w", err)
	}
	return user, nil
}

func (r *UserRepository) GetByLogin(ctx context.Context, login string) (db.User, error) {
	login = strings.TrimSpace(login)
	if login == "" {
		return db.User{}, ErrNotFound
	}
	if strings.Contains(login, "@") {
		return r.GetByEmail(ctx, login)
	}
	return r.GetByUsername(ctx, login)
}

func (r *UserRepository) UpdateProfile(ctx context.Context, userID int64, displayName, bio string, email *string) (db.User, error) {
	var user db.User
	err := r.db.Pool.QueryRow(ctx, `
		UPDATE users
		SET display_name = $2,
		    bio = $3,
		    email = COALESCE($4, email),
		    updated_at = NOW()
		WHERE id = $1
		RETURNING `+userSelectColumns+`
	`, userID, displayName, bio, email).Scan(
		&user.ID,
		&user.Username,
		&user.Email,
		&user.PasswordHash,
		&user.DisplayName,
		&user.Bio,
		&user.AvatarURL,
		&user.AvatarObjectKey,
		&user.IsPremium,
		&user.PremiumUntil,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.User{}, ErrNotFound
		}
		return db.User{}, fmt.Errorf("update profile: %w", err)
	}
	return user, nil
}

func (r *UserRepository) UpdateAvatar(ctx context.Context, userID int64, objectKey, avatarURL string) (db.User, error) {
	var user db.User
	err := r.db.Pool.QueryRow(ctx, `
		UPDATE users
		SET avatar_object_key = $2,
		    avatar_url = $3,
		    updated_at = NOW()
		WHERE id = $1
		RETURNING `+userSelectColumns+`
	`, userID, objectKey, avatarURL).Scan(
		&user.ID,
		&user.Username,
		&user.Email,
		&user.PasswordHash,
		&user.DisplayName,
		&user.Bio,
		&user.AvatarURL,
		&user.AvatarObjectKey,
		&user.IsPremium,
		&user.PremiumUntil,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.User{}, ErrNotFound
		}
		return db.User{}, fmt.Errorf("update avatar: %w", err)
	}
	return user, nil
}

func (r *UserRepository) GetActivePremiumEntitlement(ctx context.Context, userID int64) (db.PremiumEntitlement, error) {
	var entitlement db.PremiumEntitlement
	err := r.db.Pool.QueryRow(ctx, `
		SELECT id, user_id, source, granted_at, expires_at, revoked_at, metadata
		FROM premium_entitlements
		WHERE user_id = $1
		  AND revoked_at IS NULL
		  AND (expires_at IS NULL OR expires_at > NOW())
		ORDER BY granted_at DESC
		LIMIT 1
	`, userID).Scan(
		&entitlement.ID,
		&entitlement.UserID,
		&entitlement.Source,
		&entitlement.GrantedAt,
		&entitlement.ExpiresAt,
		&entitlement.RevokedAt,
		&entitlement.Metadata,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.PremiumEntitlement{}, nil
		}
		return db.PremiumEntitlement{}, fmt.Errorf("get premium entitlement: %w", err)
	}
	return entitlement, nil
}

func (r *UserRepository) GrantPremium(ctx context.Context, userID int64, source string, expiresAt *time.Time) error {
	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin premium tx: %w", err)
	}
	defer tx.Rollback(ctx)

	if _, err := tx.Exec(ctx, `
		INSERT INTO premium_entitlements (user_id, source, expires_at)
		VALUES ($1, $2, $3)
	`, userID, source, expiresAt); err != nil {
		return fmt.Errorf("insert premium entitlement: %w", err)
	}

	if _, err := tx.Exec(ctx, `
		UPDATE users
		SET is_premium = TRUE,
		    premium_until = $2,
		    updated_at = NOW()
		WHERE id = $1
	`, userID, expiresAt); err != nil {
		return fmt.Errorf("update user premium flag: %w", err)
	}

	return tx.Commit(ctx)
}

var ErrNotFound = errors.New("not found")
