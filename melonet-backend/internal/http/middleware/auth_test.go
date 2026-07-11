package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"melonet-backend/internal/auth"

	"github.com/gin-gonic/gin"
)

type stubParser struct {
	claims *auth.Claims
	err    error
}

func (s stubParser) ParseAccessToken(string) (*auth.Claims, error) {
	return s.claims, s.err
}

func init() {
	gin.SetMode(gin.TestMode)
}

func TestAuthRequiredBearer(t *testing.T) {
	router := gin.New()
	router.Use(AuthRequired(stubParser{
		claims: &auth.Claims{UserID: 42, Username: "mahdi"},
	}))
	router.GET("/protected", func(c *gin.Context) {
		userID, _ := auth.UserIDFromGin(c)
		c.String(http.StatusOK, "%d", userID)
	})

	req := httptest.NewRequest(http.MethodGet, "/protected", nil)
	req.Header.Set("Authorization", "Bearer test-token")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK || w.Body.String() != "42" {
		t.Fatalf("status=%d body=%q", w.Code, w.Body.String())
	}
}

func TestAuthRequiredMissingToken(t *testing.T) {
	router := gin.New()
	router.Use(AuthRequired(stubParser{}))
	router.GET("/protected", func(c *gin.Context) {
		c.Status(http.StatusOK)
	})

	req := httptest.NewRequest(http.MethodGet, "/protected", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want 401", w.Code)
	}
}
