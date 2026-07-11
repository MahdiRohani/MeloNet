package middleware

import (
	"net/http"
	"strings"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/http/response"

	"github.com/gin-gonic/gin"
)

type TokenParser interface {
	ParseAccessToken(token string) (*auth.Claims, error)
}

func AuthRequired(parser TokenParser) gin.HandlerFunc {
	return func(c *gin.Context) {
		token, ok := extractBearerToken(c.GetHeader("Authorization"))
		if !ok {
			if queryToken := strings.TrimSpace(c.Query("token")); queryToken != "" {
				token = queryToken
				ok = true
			}
		}
		if !ok {
			response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
			c.Abort()
			return
		}

		claims, err := parser.ParseAccessToken(token)
		if err != nil {
			response.Error(c, http.StatusUnauthorized, "unauthorized", "invalid or expired token")
			c.Abort()
			return
		}

		c.Set(auth.GinUserIDKey, claims.UserID)
		c.Set(auth.GinUsernameKey, claims.Username)
		c.Request = c.Request.WithContext(auth.WithUsername(auth.WithUserID(c.Request.Context(), claims.UserID), claims.Username))
		c.Next()
	}
}

func extractBearerToken(header string) (string, bool) {
	header = strings.TrimSpace(header)
	if header == "" {
		return "", false
	}

	parts := strings.SplitN(header, " ", 2)
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
		return "", false
	}

	token := strings.TrimSpace(parts[1])
	if token == "" {
		return "", false
	}

	return token, true
}
