package middleware

import (
	"context"
	"log/slog"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

const RequestIDHeader = "X-Request-ID"
const RequestIDKey = "request_id"

func RequestID() gin.HandlerFunc {
	return func(c *gin.Context) {
		requestID := c.GetHeader(RequestIDHeader)
		if requestID == "" {
			requestID = uuid.NewString()
		}

		c.Set(RequestIDKey, requestID)
		c.Writer.Header().Set(RequestIDHeader, requestID)
		c.Next()
	}
}

func RequestLogger(logger *slog.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path
		rawQuery := c.Request.URL.RawQuery

		c.Next()

		latency := time.Since(start)
		status := c.Writer.Status()
		requestID, _ := c.Get(RequestIDKey)

		if rawQuery != "" {
			path = path + "?" + rawQuery
		}

		logger.Info("request completed",
			"request_id", requestID,
			"method", c.Request.Method,
			"path", SanitizePath(path),
			"status", status,
			"latency_ms", latency.Milliseconds(),
			"client_ip", c.ClientIP(),
			"has_auth", SanitizeAuthHeader(c.GetHeader("Authorization")) != "",
		)
	}
}

func Recovery(logger *slog.Logger) gin.HandlerFunc {
	return gin.CustomRecovery(func(c *gin.Context, recovered any) {
		requestID, _ := c.Get(RequestIDKey)
		logger.Error("panic recovered",
			"request_id", requestID,
			"error", recovered,
			"path", c.Request.URL.Path,
		)
		c.AbortWithStatusJSON(http.StatusInternalServerError, gin.H{
			"data": nil,
			"error": gin.H{
				"code":    "internal_error",
				"message": "unexpected server error",
			},
		})
	})
}

func Timeout(timeout time.Duration) gin.HandlerFunc {
	return func(c *gin.Context) {
		if timeout <= 0 {
			c.Next()
			return
		}

		ctx, cancel := context.WithTimeout(c.Request.Context(), timeout)
		defer cancel()

		c.Request = c.Request.WithContext(ctx)
		c.Next()

		if ctx.Err() != nil && !c.IsAborted() && c.Writer.Status() == http.StatusOK {
			c.AbortWithStatusJSON(http.StatusGatewayTimeout, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "request_timeout",
					"message": "request timed out",
				},
			})
		}
	}
}
