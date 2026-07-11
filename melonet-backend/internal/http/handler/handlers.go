package handler

import (
	"context"
	"errors"
	"net/http"
	"strconv"

	"melonet-backend/internal/http/response"

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
