package middleware

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
)

func TestMemoryRateLimitStoreBlocksAfterLimit(t *testing.T) {
	store := NewMemoryRateLimitStore()
	ctx := context.Background()

	for i := 0; i < 3; i++ {
		allowed, err := store.Allow(ctx, "test-key", 3, time.Minute)
		if err != nil {
			t.Fatalf("Allow error: %v", err)
		}
		if !allowed {
			t.Fatalf("request %d should be allowed", i+1)
		}
	}

	allowed, err := store.Allow(ctx, "test-key", 3, time.Minute)
	if err != nil {
		t.Fatalf("Allow error: %v", err)
	}
	if allowed {
		t.Fatal("expected rate limit to block 4th request")
	}
}

func TestRateLimitMiddlewareReturns429(t *testing.T) {
	gin.SetMode(gin.TestMode)
	store := NewMemoryRateLimitStore()

	router := gin.New()
	router.GET("/limited", RateLimit(store, "test", 1, time.Minute, ClientIPKey), func(c *gin.Context) {
		c.Status(http.StatusOK)
	})

	w1 := httptest.NewRecorder()
	req1 := httptest.NewRequest(http.MethodGet, "/limited", nil)
	router.ServeHTTP(w1, req1)
	if w1.Code != http.StatusOK {
		t.Fatalf("first status = %d, want 200", w1.Code)
	}

	w2 := httptest.NewRecorder()
	req2 := httptest.NewRequest(http.MethodGet, "/limited", nil)
	router.ServeHTTP(w2, req2)
	if w2.Code != http.StatusTooManyRequests {
		t.Fatalf("second status = %d, want 429", w2.Code)
	}
}

func TestAlwaysAllowStore(t *testing.T) {
	store := alwaysAllowStore{}
	for i := 0; i < 10; i++ {
		allowed, err := store.Allow(context.Background(), "k", 1, time.Minute)
		if err != nil || !allowed {
			t.Fatal("expected always allow")
		}
	}
}
