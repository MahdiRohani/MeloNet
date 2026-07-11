package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"melonet-backend/internal/config"

	"github.com/gin-gonic/gin"
)

func TestSecureHeadersSetsHSTSInProductionHTTPS(t *testing.T) {
	gin.SetMode(gin.TestMode)
	cfg := &config.Config{AppEnv: "production"}

	router := gin.New()
	router.Use(SecureHeaders(cfg))
	router.GET("/check", func(c *gin.Context) { c.Status(http.StatusOK) })

	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/check", nil)
	req.Header.Set("X-Forwarded-Proto", "https")
	router.ServeHTTP(w, req)

	if got := w.Header().Get("Strict-Transport-Security"); got == "" {
		t.Fatal("expected HSTS header behind HTTPS in production")
	}
}
