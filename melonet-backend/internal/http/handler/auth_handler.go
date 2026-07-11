package handler

import (
	"errors"
	"io"
	"net/http"
	"strings"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type AuthHandler struct {
	auth *service.AuthService
}

func NewAuthHandler(authService *service.AuthService) *AuthHandler {
	return &AuthHandler{auth: authService}
}

func (h *AuthHandler) Register(c *gin.Context) {
	var req api.RegisterRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_request", "invalid registration payload")
		return
	}

	result, err := h.auth.Register(c.Request.Context(), req)
	if err != nil {
		mapAuthError(c, err)
		return
	}

	response.Created(c, result)
}

func (h *AuthHandler) Login(c *gin.Context) {
	var req api.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_request", "invalid login payload")
		return
	}

	result, err := h.auth.Login(c.Request.Context(), req)
	if err != nil {
		mapAuthError(c, err)
		return
	}

	response.OK(c, result)
}

func (h *AuthHandler) Refresh(c *gin.Context) {
	var req api.RefreshTokenRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_request", "invalid refresh payload")
		return
	}

	result, err := h.auth.Refresh(c.Request.Context(), req.RefreshToken)
	if err != nil {
		mapAuthError(c, err)
		return
	}

	response.OK(c, result)
}

func (h *AuthHandler) Logout(c *gin.Context) {
	var req api.LogoutRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_request", "invalid logout payload")
		return
	}

	if err := h.auth.Logout(c.Request.Context(), req.RefreshToken); err != nil {
		mapAuthError(c, err)
		return
	}

	response.OK(c, gin.H{"logged_out": true})
}

func (h *AuthHandler) Me(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	user, err := h.auth.Me(c.Request.Context(), userID)
	if err != nil {
		response.InternalError(c, "failed to load profile")
		return
	}

	response.OK(c, user)
}

func (h *AuthHandler) UpdateProfile(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	var req api.UpdateProfileRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_request", "invalid profile payload")
		return
	}

	user, err := h.auth.UpdateProfile(c.Request.Context(), userID, req)
	if err != nil {
		mapAuthError(c, err)
		return
	}

	response.OK(c, user)
}

func (h *AuthHandler) UploadAvatar(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	file, err := c.FormFile("avatar")
	if err != nil {
		response.BadRequest(c, "invalid_request", "avatar file is required")
		return
	}

	opened, err := file.Open()
	if err != nil {
		response.InternalError(c, "failed to read avatar")
		return
	}
	defer opened.Close()

	user, err := h.auth.UploadAvatar(
		c.Request.Context(),
		userID,
		file.Filename,
		io.LimitReader(opened, 5*1024*1024+1),
		file.Size,
		file.Header.Get("Content-Type"),
	)
	if err != nil {
		mapAuthError(c, err)
		return
	}

	response.OK(c, user)
}

func mapAuthError(c *gin.Context, err error) {
	switch {
	case errors.Is(err, service.ErrInvalidCredentials):
		response.Error(c, http.StatusUnauthorized, "invalid_credentials", "invalid login or password")
	case errors.Is(err, service.ErrUserExists):
		response.Error(c, http.StatusConflict, "user_exists", "username or email already exists")
	case errors.Is(err, service.ErrInvalidInput):
		response.BadRequest(c, "invalid_request", err.Error())
	case errors.Is(err, service.ErrUnauthorized):
		response.Error(c, http.StatusUnauthorized, "unauthorized", "invalid or expired token")
	default:
		if strings.Contains(err.Error(), "upload avatar") {
			response.InternalError(c, "failed to upload avatar")
			return
		}
		response.InternalError(c, "authentication request failed")
	}
}
