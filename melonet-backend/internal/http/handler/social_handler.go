package handler

import (
	"errors"
	"net/http"
	"strconv"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type SocialHandler struct {
	social *service.SocialService
}

func NewSocialHandler(social *service.SocialService) *SocialHandler {
	return &SocialHandler{social: social}
}

func (h *SocialHandler) SearchUsers(c *gin.Context) {
	query := c.Query("q")
	if query == "" {
		response.BadRequest(c, "invalid_query", "search query is required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	users, meta, err := h.social.SearchUsers(c.Request.Context(), query, page, limit)
	if err != nil {
		response.BadRequest(c, "invalid_query", err.Error())
		return
	}
	response.OKWithMeta(c, users, meta)
}

func (h *SocialHandler) GetProfile(c *gin.Context) {
	viewerID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	targetID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid user id")
		return
	}

	profile, err := h.social.GetPublicProfile(c.Request.Context(), int64(viewerID), targetID)
	if err != nil {
		mapSocialError(c, err)
		return
	}
	response.OK(c, profile)
}

func (h *SocialHandler) ListPlaylists(c *gin.Context) {
	viewerID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	targetID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid user id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	playlists, meta, err := h.social.ListPublicPlaylists(c.Request.Context(), int64(viewerID), targetID, page, limit)
	if err != nil {
		mapSocialError(c, err)
		return
	}
	response.OKWithMeta(c, playlists, meta)
}

func (h *SocialHandler) Follow(c *gin.Context) {
	viewerID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	targetID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid user id")
		return
	}

	result, err := h.social.Follow(c.Request.Context(), int64(viewerID), targetID)
	if err != nil {
		mapSocialMutationError(c, err)
		return
	}
	response.OK(c, result)
}

func (h *SocialHandler) Unfollow(c *gin.Context) {
	viewerID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	targetID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid user id")
		return
	}

	result, err := h.social.Unfollow(c.Request.Context(), int64(viewerID), targetID)
	if err != nil {
		mapSocialError(c, err)
		return
	}
	response.OK(c, result)
}

func (h *SocialHandler) ListFollowers(c *gin.Context) {
	targetID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid user id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	users, meta, err := h.social.ListFollowers(c.Request.Context(), targetID, page, limit)
	if err != nil {
		mapSocialError(c, err)
		return
	}
	response.OKWithMeta(c, users, meta)
}

func (h *SocialHandler) ListFollowing(c *gin.Context) {
	targetID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid user id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	users, meta, err := h.social.ListFollowing(c.Request.Context(), targetID, page, limit)
	if err != nil {
		mapSocialError(c, err)
		return
	}
	response.OKWithMeta(c, users, meta)
}

func (h *SocialHandler) ListNotifications(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	unreadOnly := false
	if value := c.Query("unread"); value != "" {
		parsed, err := strconv.ParseBool(value)
		if err == nil {
			unreadOnly = parsed
		}
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	notifications, meta, err := h.social.ListNotifications(c.Request.Context(), int64(userID), page, limit, unreadOnly)
	if err != nil {
		response.InternalError(c, "failed to list notifications")
		return
	}
	response.OKWithMeta(c, notifications, meta)
}

func (h *SocialHandler) MarkNotificationRead(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	notificationID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid notification id")
		return
	}

	if err := h.social.MarkNotificationRead(c.Request.Context(), int64(userID), notificationID); err != nil {
		mapSocialError(c, err)
		return
	}
	response.OK(c, gin.H{"read": true})
}

func (h *SocialHandler) MarkAllNotificationsRead(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	count, err := h.social.MarkAllNotificationsRead(c.Request.Context(), int64(userID))
	if err != nil {
		response.InternalError(c, "failed to mark notifications read")
		return
	}
	response.OK(c, gin.H{"marked_read": count})
}

func mapSocialError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	response.InternalError(c, "social request failed")
}

func mapSocialMutationError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	if err != nil && err.Error() == "cannot follow yourself" {
		response.BadRequest(c, "invalid_request", err.Error())
		return
	}
	response.InternalError(c, "social request failed")
}
