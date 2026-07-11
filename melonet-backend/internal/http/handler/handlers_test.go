package handler

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"melonet-backend/internal/http/response"

	"github.com/gin-gonic/gin"
)

type mockChecker struct {
	err error
}

func (m mockChecker) Ping(_ context.Context) error {
	return m.err
}

func init() {
	gin.SetMode(gin.TestMode)
}

func TestHealthLive(t *testing.T) {
	h := NewHealthHandler(mockChecker{}, mockChecker{}, mockChecker{})
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest(http.MethodGet, "/health/live", nil)

	h.Live(c)

	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", w.Code)
	}
}

func TestHealthReadyAllOK(t *testing.T) {
	h := NewHealthHandler(mockChecker{}, mockChecker{}, mockChecker{})
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest(http.MethodGet, "/health/ready", nil)

	h.Ready(c)

	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", w.Code)
	}

	var body response.Envelope
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
}

func TestHealthReadyDegraded(t *testing.T) {
	h := NewHealthHandler(mockChecker{err: errors.New("db down")}, mockChecker{}, mockChecker{})
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest(http.MethodGet, "/health/ready", nil)

	h.Ready(c)

	if w.Code != http.StatusServiceUnavailable {
		t.Fatalf("status = %d, want 503", w.Code)
	}
}
