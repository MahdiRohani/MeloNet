package security_test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"melonet-backend/internal/config"
	"melonet-backend/internal/http/middleware"

	"github.com/gin-gonic/gin"
)

func TestProductionConfigRejectsWeakSecrets(t *testing.T) {
	cfg := &config.Config{
		AppEnv:      "production",
		DatabaseURL: "postgres://melonet:melonet@localhost:5432/melonet?sslmode=disable",
		JWTSecret:   "dev-jwt-secret-change-in-production",
		CORS: config.CORSConfig{
			AllowedOrigins: []string{"https://app.example.com"},
		},
	}
	if err := cfg.Validate(); err == nil {
		t.Fatal("expected production validation to reject default JWT secret")
	}
}

func TestProductionConfigRejectsWildcardCORS(t *testing.T) {
	cfg := &config.Config{
		AppEnv:      "production",
		DatabaseURL: "postgres://melonet:melonet@localhost:5432/melonet?sslmode=disable",
		JWTSecret:   "production-secret-with-enough-entropy",
		CORS: config.CORSConfig{
			AllowedOrigins: []string{"*"},
		},
	}
	if err := cfg.Validate(); err == nil {
		t.Fatal("expected production validation to reject wildcard CORS")
	}
}

func TestSecureHeadersApplied(t *testing.T) {
	gin.SetMode(gin.TestMode)
	cfg := &config.Config{AppEnv: "development"}

	router := gin.New()
	router.Use(middleware.SecureHeaders(cfg))
	router.GET("/check", func(c *gin.Context) { c.Status(http.StatusOK) })

	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/check", nil)
	router.ServeHTTP(w, req)

	for _, header := range []string{
		"X-Content-Type-Options",
		"X-Frame-Options",
		"Referrer-Policy",
		"Permissions-Policy",
	} {
		if w.Header().Get(header) == "" {
			t.Fatalf("expected %s response header", header)
		}
	}
}

func TestLogsDoNotExposeBearerTokens(t *testing.T) {
	got := middleware.SanitizeAuthHeader("Bearer super-secret-token")
	if got != "Bearer [redacted]" {
		t.Fatalf("got %q, want redacted bearer header", got)
	}
}

func TestWebSocketTokenQueryRedactedInLogs(t *testing.T) {
	got := middleware.SanitizePath("/ws/chat?token=abc123")
	if got == "/ws/chat?token=abc123" {
		t.Fatal("expected websocket token query to be redacted")
	}
}
