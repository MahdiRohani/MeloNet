package service

import (
	"testing"
)

func TestParsePaginationDefaults(t *testing.T) {
	page, limit := ParsePagination("", "", 20)
	if page != 1 || limit != 20 {
		t.Fatalf("page=%d limit=%d, want 1 and 20", page, limit)
	}
}

func TestParsePaginationCapsLimit(t *testing.T) {
	_, limit := ParsePagination("2", "500", 20)
	if limit != 100 {
		t.Fatalf("limit=%d, want 100", limit)
	}
}

func TestParsePaginationInvalidValues(t *testing.T) {
	page, limit := ParsePagination("-1", "0", 20)
	if page != 1 || limit != 20 {
		t.Fatalf("page=%d limit=%d, want 1 and 20", page, limit)
	}
}
