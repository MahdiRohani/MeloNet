package auth

import (
	"context"
	"errors"
)

type contextKey string

const userIDKey contextKey = "auth_user_id"
const usernameKey contextKey = "auth_username"

var ErrUnauthorized = errors.New("unauthorized")

func WithUserID(ctx context.Context, userID uint) context.Context {
	return context.WithValue(ctx, userIDKey, userID)
}

func WithUsername(ctx context.Context, username string) context.Context {
	return context.WithValue(ctx, usernameKey, username)
}

func UserIDFromContext(ctx context.Context) (uint, error) {
	value, ok := ctx.Value(userIDKey).(uint)
	if !ok || value == 0 {
		return 0, ErrUnauthorized
	}
	return value, nil
}

func UsernameFromContext(ctx context.Context) string {
	value, _ := ctx.Value(usernameKey).(string)
	return value
}

func UserIDFromGin(c interface {
	Get(string) (any, bool)
}) (uint, error) {
	value, ok := c.Get(string(userIDKey))
	if !ok {
		return 0, ErrUnauthorized
	}
	userID, ok := value.(uint)
	if !ok || userID == 0 {
		return 0, ErrUnauthorized
	}
	return userID, nil
}

const GinUserIDKey = "auth_user_id"
const GinUsernameKey = "auth_username"
