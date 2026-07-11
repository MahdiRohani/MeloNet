package handler

import (
	"context"
	"errors"
	"net/http"
	"strconv"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type HealthChecker interface {
	Ping(ctx context.Context) error
}

type HealthHandler struct {
	db      HealthChecker
	redis   HealthChecker
	storage HealthChecker
}

func NewHealthHandler(db, redis, storage HealthChecker) *HealthHandler {
	return &HealthHandler{
		db:      db,
		redis:   redis,
		storage: storage,
	}
}

type healthStatus struct {
	Status   string            `json:"status"`
	Services map[string]string `json:"services"`
}

func (h *HealthHandler) Live(c *gin.Context) {
	response.OK(c, gin.H{"status": "ok"})
}

func (h *HealthHandler) Ready(c *gin.Context) {
	ctx := c.Request.Context()
	services := map[string]string{
		"database": "ok",
		"redis":    "ok",
		"storage":  "ok",
	}
	overall := "ok"

	if err := h.db.Ping(ctx); err != nil {
		services["database"] = err.Error()
		overall = "degraded"
	}
	if err := h.redis.Ping(ctx); err != nil {
		services["redis"] = err.Error()
		overall = "degraded"
	}
	if err := h.storage.Ping(ctx); err != nil {
		services["storage"] = err.Error()
		overall = "degraded"
	}

	statusCode := http.StatusOK
	if overall != "ok" {
		statusCode = http.StatusServiceUnavailable
	}

	c.JSON(statusCode, response.Envelope{
		Data: healthStatus{
			Status:   overall,
			Services: services,
		},
		Error: nil,
	})
}

type ChatHandler struct {
	chat *service.ChatService
	hub  ChatHub
}

type ChatHub interface {
	HandleConnection(c *gin.Context, userID uint)
}

func NewChatHandler(chat *service.ChatService, hub ChatHub) *ChatHandler {
	return &ChatHandler{chat: chat, hub: hub}
}

func (h *ChatHandler) History(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	withID, err := parseUintQuery(c, "with_id")
	if err != nil {
		response.BadRequest(c, "invalid_with_id", "with_id is required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 50)
	messages, meta, err := h.chat.History(c.Request.Context(), userID, withID, page, limit)
	if err != nil {
		response.BadRequest(c, "invalid_request", err.Error())
		return
	}

	response.OKWithMeta(c, messages, meta)
}

func (h *ChatHandler) WebSocket(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	h.hub.HandleConnection(c, userID)
}

func parseUintQuery(c *gin.Context, key string) (uint, error) {
	value := c.Query(key)
	if value == "" {
		return 0, errors.New("missing query parameter")
	}

	parsed, err := strconv.ParseUint(value, 10, 32)
	if err != nil {
		return 0, err
	}

	return uint(parsed), nil
}
