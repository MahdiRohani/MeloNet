package auth

import (
	"errors"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

var ErrInvalidToken = errors.New("invalid token")

type TokenManager struct {
	secret     []byte
	accessTTL  time.Duration
	refreshTTL time.Duration
}

type Claims struct {
	UserID   uint   `json:"uid"`
	Username string `json:"username"`
	TokenUse string `json:"token_use"`
	jwt.RegisteredClaims
}

func NewTokenManager(secret string, accessTTL, refreshTTL time.Duration) *TokenManager {
	return &TokenManager{
		secret:     []byte(secret),
		accessTTL:  accessTTL,
		refreshTTL: refreshTTL,
	}
}

func (m *TokenManager) GenerateAccessToken(userID uint, username string) (string, time.Time, error) {
	return m.generateToken(userID, username, "access", m.accessTTL)
}

func (m *TokenManager) GenerateRefreshToken(userID uint, username string) (string, time.Time, error) {
	return m.generateToken(userID, username, "refresh", m.refreshTTL)
}

func (m *TokenManager) ParseAccessToken(token string) (*Claims, error) {
	claims, err := m.parseToken(token)
	if err != nil {
		return nil, err
	}
	if claims.TokenUse != "access" {
		return nil, ErrInvalidToken
	}
	return claims, nil
}

func (m *TokenManager) ParseRefreshToken(token string) (*Claims, error) {
	claims, err := m.parseToken(token)
	if err != nil {
		return nil, err
	}
	if claims.TokenUse != "refresh" {
		return nil, ErrInvalidToken
	}
	return claims, nil
}

func (m *TokenManager) RefreshTTL() time.Duration {
	return m.refreshTTL
}

func (m *TokenManager) generateToken(userID uint, username, tokenUse string, ttl time.Duration) (string, time.Time, error) {
	expiresAt := time.Now().UTC().Add(ttl)
	claims := Claims{
		UserID:   userID,
		Username: username,
		TokenUse: tokenUse,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(expiresAt),
			IssuedAt:  jwt.NewNumericDate(time.Now().UTC()),
			Subject:   fmt.Sprintf("%d", userID),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	signed, err := token.SignedString(m.secret)
	if err != nil {
		return "", time.Time{}, fmt.Errorf("sign token: %w", err)
	}

	return signed, expiresAt, nil
}

func (m *TokenManager) parseToken(tokenString string) (*Claims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (any, error) {
		if token.Method != jwt.SigningMethodHS256 {
			return nil, ErrInvalidToken
		}
		return m.secret, nil
	})
	if err != nil {
		return nil, ErrInvalidToken
	}

	claims, ok := token.Claims.(*Claims)
	if !ok || !token.Valid {
		return nil, ErrInvalidToken
	}

	return claims, nil
}
