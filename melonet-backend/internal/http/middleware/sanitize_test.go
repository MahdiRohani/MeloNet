package middleware

import "testing"

func TestSanitizePathRedactsToken(t *testing.T) {
	got := SanitizePath("/ws/chat?token=secret-value&foo=bar")
	if got == "/ws/chat?token=secret-value&foo=bar" {
		t.Fatal("expected token to be redacted")
	}
	if got != "/ws/chat?foo=bar&token=%5Bredacted%5D" && got != "/ws/chat?token=%5Bredacted%5D&foo=bar" {
		t.Fatalf("unexpected sanitized path: %s", got)
	}
}

func TestSanitizeAuthHeader(t *testing.T) {
	got := SanitizeAuthHeader("Bearer eyJhbGciOiJIUzI1NiJ9.payload")
	if got != "Bearer [redacted]" {
		t.Fatalf("got %q", got)
	}
}
