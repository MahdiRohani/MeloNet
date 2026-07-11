package response

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"melonet-backend/internal/domain"

	"github.com/gin-gonic/gin"
)

func init() {
	gin.SetMode(gin.TestMode)
}

func TestOK(t *testing.T) {
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)

	OK(c, gin.H{"items": []string{"a"}})

	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", w.Code)
	}

	var body Envelope
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if body.Error != nil {
		t.Fatalf("error = %+v, want nil", body.Error)
	}
	if body.Data == nil {
		t.Fatal("data is nil")
	}
}

func TestError(t *testing.T) {
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)

	Error(c, http.StatusBadRequest, "invalid_query", "query is empty")

	var body Envelope
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if body.Data != nil {
		t.Fatalf("data = %+v, want nil", body.Data)
	}
	if body.Error == nil || body.Error.Code != "invalid_query" {
		t.Fatalf("error = %+v", body.Error)
	}
}

func TestOKWithMeta(t *testing.T) {
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)

	OKWithMeta(c, []int{1, 2}, domain.Pagination{Page: 1, Limit: 20, Total: 2})

	var body Envelope
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if body.Meta == nil || body.Meta.Total != 2 {
		t.Fatalf("meta = %+v", body.Meta)
	}
}
