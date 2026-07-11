package service

import (
	"context"
	"errors"
	"fmt"
	"io"
	"mime"
	"path/filepath"
	"regexp"
	"strings"
	"time"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
	"melonet-backend/internal/repository/postgres"
)

var (
	ErrInvalidCredentials = errors.New("invalid credentials")
	ErrUserExists         = errors.New("user already exists")
	ErrInvalidInput       = errors.New("invalid input")
	ErrUnauthorized       = errors.New("unauthorized")
)

type AvatarUploader interface {
	UploadAvatar(ctx context.Context, userID int64, filename string, reader io.Reader, size int64, contentType string) (objectKey, publicURL string, err error)
}

type AuthService struct {
	users      *postgres.UserRepository
	tokens     *postgres.TokenRepository
	tokenMgr   *auth.TokenManager
	uploader   AvatarUploader
	publicBase string
}

func NewAuthService(
	users *postgres.UserRepository,
	tokens *postgres.TokenRepository,
	tokenMgr *auth.TokenManager,
	uploader AvatarUploader,
	publicBase string,
) *AuthService {
	return &AuthService{
		users:      users,
		tokens:     tokens,
		tokenMgr:   tokenMgr,
		uploader:   uploader,
		publicBase: strings.TrimRight(publicBase, "/"),
	}
}

func (s *AuthService) Register(ctx context.Context, req api.RegisterRequest) (api.AuthTokenResponse, error) {
	req.Username = strings.TrimSpace(req.Username)
	req.Email = strings.TrimSpace(strings.ToLower(req.Email))
	req.DisplayName = strings.TrimSpace(req.DisplayName)

	if err := validateRegister(req); err != nil {
		return api.AuthTokenResponse{}, err
	}

	if _, err := s.users.GetByUsername(ctx, req.Username); err == nil {
		return api.AuthTokenResponse{}, ErrUserExists
	} else if !errors.Is(err, postgres.ErrNotFound) {
		return api.AuthTokenResponse{}, err
	}

	if _, err := s.users.GetByEmail(ctx, req.Email); err == nil {
		return api.AuthTokenResponse{}, ErrUserExists
	} else if !errors.Is(err, postgres.ErrNotFound) {
		return api.AuthTokenResponse{}, err
	}

	passwordHash, err := auth.HashPassword(req.Password)
	if err != nil {
		return api.AuthTokenResponse{}, err
	}

	displayName := req.DisplayName
	if displayName == "" {
		displayName = req.Username
	}

	email := req.Email
	user, err := s.users.Create(ctx, db.User{
		Username:     req.Username,
		Email:        &email,
		PasswordHash: passwordHash,
		DisplayName:  displayName,
	})
	if err != nil {
		return api.AuthTokenResponse{}, err
	}

	return s.issueTokens(ctx, user)
}

func (s *AuthService) Login(ctx context.Context, req api.LoginRequest) (api.AuthTokenResponse, error) {
	req.Login = strings.TrimSpace(req.Login)
	if req.Login == "" || req.Password == "" {
		return api.AuthTokenResponse{}, ErrInvalidInput
	}

	user, err := s.users.GetByLogin(ctx, req.Login)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.AuthTokenResponse{}, ErrInvalidCredentials
		}
		return api.AuthTokenResponse{}, err
	}

	if err := auth.ComparePassword(user.PasswordHash, req.Password); err != nil {
		return api.AuthTokenResponse{}, ErrInvalidCredentials
	}

	return s.issueTokens(ctx, user)
}

func (s *AuthService) Refresh(ctx context.Context, refreshToken string) (api.AuthTokenResponse, error) {
	refreshToken = strings.TrimSpace(refreshToken)
	if refreshToken == "" {
		return api.AuthTokenResponse{}, ErrInvalidInput
	}

	claims, err := s.tokenMgr.ParseRefreshToken(refreshToken)
	if err != nil {
		return api.AuthTokenResponse{}, ErrUnauthorized
	}

	stored, err := s.tokens.GetValid(ctx, auth.HashToken(refreshToken))
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.AuthTokenResponse{}, ErrUnauthorized
		}
		return api.AuthTokenResponse{}, err
	}

	if stored.UserID != int64(claims.UserID) {
		return api.AuthTokenResponse{}, ErrUnauthorized
	}

	user, err := s.users.GetByID(ctx, stored.UserID)
	if err != nil {
		return api.AuthTokenResponse{}, err
	}

	if err := s.tokens.Revoke(ctx, auth.HashToken(refreshToken)); err != nil {
		return api.AuthTokenResponse{}, err
	}

	return s.issueTokens(ctx, user)
}

func (s *AuthService) Logout(ctx context.Context, refreshToken string) error {
	refreshToken = strings.TrimSpace(refreshToken)
	if refreshToken == "" {
		return ErrInvalidInput
	}

	if _, err := s.tokenMgr.ParseRefreshToken(refreshToken); err != nil {
		return ErrUnauthorized
	}

	return s.tokens.Revoke(ctx, auth.HashToken(refreshToken))
}

func (s *AuthService) Me(ctx context.Context, userID uint) (api.UserResponse, error) {
	user, err := s.users.GetByID(ctx, int64(userID))
	if err != nil {
		return api.UserResponse{}, err
	}
	return s.buildUserResponse(ctx, user)
}

func (s *AuthService) UpdateProfile(ctx context.Context, userID uint, req api.UpdateProfileRequest) (api.UserResponse, error) {
	user, err := s.users.GetByID(ctx, int64(userID))
	if err != nil {
		return api.UserResponse{}, err
	}

	displayName := user.DisplayName
	if req.DisplayName != nil {
		displayName = strings.TrimSpace(*req.DisplayName)
		if displayName == "" {
			return api.UserResponse{}, ErrInvalidInput
		}
	}

	bio := user.Bio
	if req.Bio != nil {
		bio = strings.TrimSpace(*req.Bio)
	}

	var email *string
	if req.Email != nil {
		normalized := strings.TrimSpace(strings.ToLower(*req.Email))
		if normalized == "" || !strings.Contains(normalized, "@") {
			return api.UserResponse{}, ErrInvalidInput
		}
		if existing, err := s.users.GetByEmail(ctx, normalized); err == nil && existing.ID != user.ID {
			return api.UserResponse{}, ErrUserExists
		} else if err != nil && !errors.Is(err, postgres.ErrNotFound) {
			return api.UserResponse{}, err
		}
		email = &normalized
	}

	updated, err := s.users.UpdateProfile(ctx, user.ID, displayName, bio, email)
	if err != nil {
		return api.UserResponse{}, err
	}

	return s.buildUserResponse(ctx, updated)
}

func (s *AuthService) UploadAvatar(ctx context.Context, userID uint, filename string, reader io.Reader, size int64, contentType string) (api.UserResponse, error) {
	if size <= 0 || size > 5*1024*1024 {
		return api.UserResponse{}, ErrInvalidInput
	}

	contentType = normalizeImageContentType(filename, contentType)
	if !strings.HasPrefix(contentType, "image/") {
		return api.UserResponse{}, ErrInvalidInput
	}

	objectKey, publicURL, err := s.uploader.UploadAvatar(ctx, int64(userID), filename, reader, size, contentType)
	if err != nil {
		return api.UserResponse{}, fmt.Errorf("upload avatar: %w", err)
	}

	updated, err := s.users.UpdateAvatar(ctx, int64(userID), objectKey, publicURL)
	if err != nil {
		return api.UserResponse{}, err
	}

	return s.buildUserResponse(ctx, updated)
}

func (s *AuthService) issueTokens(ctx context.Context, user db.User) (api.AuthTokenResponse, error) {
	accessToken, accessExp, err := s.tokenMgr.GenerateAccessToken(uint(user.ID), user.Username)
	if err != nil {
		return api.AuthTokenResponse{}, err
	}

	refreshToken, refreshExp, err := s.tokenMgr.GenerateRefreshToken(uint(user.ID), user.Username)
	if err != nil {
		return api.AuthTokenResponse{}, err
	}

	if err := s.tokens.Store(ctx, db.RefreshToken{
		UserID:    user.ID,
		TokenHash: auth.HashToken(refreshToken),
		ExpiresAt: refreshExp,
	}); err != nil {
		return api.AuthTokenResponse{}, err
	}

	userResp, err := s.buildUserResponse(ctx, user)
	if err != nil {
		return api.AuthTokenResponse{}, err
	}

	return api.AuthTokenResponse{
		AccessToken:      accessToken,
		RefreshToken:     refreshToken,
		AccessExpiresAt:  accessExp,
		RefreshExpiresAt: refreshExp,
		TokenType:        "Bearer",
		User:             userResp,
	}, nil
}

func (s *AuthService) buildUserResponse(ctx context.Context, user db.User) (api.UserResponse, error) {
	entitlement, err := s.users.GetActivePremiumEntitlement(ctx, user.ID)
	if err != nil {
		return api.UserResponse{}, err
	}

	active := isPremiumActive(user, entitlement)
	return postgres.UserToAPI(user, entitlement, active), nil
}

func isPremiumActive(user db.User, entitlement db.PremiumEntitlement) bool {
	now := time.Now().UTC()

	if entitlement.ID != 0 {
		if entitlement.ExpiresAt == nil || entitlement.ExpiresAt.After(now) {
			return true
		}
	}

	if !user.IsPremium {
		return false
	}

	if user.PremiumUntil == nil {
		return true
	}

	return user.PremiumUntil.After(now)
}

var usernamePattern = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

func validateRegister(req api.RegisterRequest) error {
	if !usernamePattern.MatchString(req.Username) {
		return ErrInvalidInput
	}
	if !strings.Contains(req.Email, "@") {
		return ErrInvalidInput
	}
	if len(req.Password) < 8 {
		return ErrInvalidInput
	}
	return nil
}

func normalizeImageContentType(filename, contentType string) string {
	contentType = strings.TrimSpace(contentType)
	if contentType != "" && contentType != "application/octet-stream" {
		return contentType
	}
	if ext := filepath.Ext(filename); ext != "" {
		if detected := mime.TypeByExtension(ext); detected != "" {
			return detected
		}
	}
	return contentType
}
