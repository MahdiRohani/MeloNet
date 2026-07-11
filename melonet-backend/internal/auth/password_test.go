package auth

import (
	"testing"
	"time"
)

func TestHashAndComparePassword(t *testing.T) {
	hash, err := HashPassword("melonet123")
	if err != nil {
		t.Fatalf("HashPassword() error = %v", err)
	}

	if err := ComparePassword(hash, "melonet123"); err != nil {
		t.Fatalf("ComparePassword() error = %v", err)
	}

	if err := ComparePassword(hash, "wrong"); err == nil {
		t.Fatal("expected password mismatch")
	}
}

func TestTokenManagerAccessAndRefresh(t *testing.T) {
	manager := NewTokenManager("test-secret", time.Minute, time.Hour)

	accessToken, _, err := manager.GenerateAccessToken(7, "mahdi")
	if err != nil {
		t.Fatalf("GenerateAccessToken() error = %v", err)
	}

	accessClaims, err := manager.ParseAccessToken(accessToken)
	if err != nil {
		t.Fatalf("ParseAccessToken() error = %v", err)
	}
	if accessClaims.UserID != 7 || accessClaims.Username != "mahdi" {
		t.Fatalf("claims = %+v", accessClaims)
	}

	refreshToken, _, err := manager.GenerateRefreshToken(7, "mahdi")
	if err != nil {
		t.Fatalf("GenerateRefreshToken() error = %v", err)
	}

	if _, err := manager.ParseAccessToken(refreshToken); err == nil {
		t.Fatal("refresh token must not parse as access token")
	}

	refreshClaims, err := manager.ParseRefreshToken(refreshToken)
	if err != nil {
		t.Fatalf("ParseRefreshToken() error = %v", err)
	}
	if refreshClaims.TokenUse != "refresh" {
		t.Fatalf("token use = %q", refreshClaims.TokenUse)
	}
}

func TestHashTokenDeterministic(t *testing.T) {
	a := HashToken("abc")
	b := HashToken("abc")
	if a != b {
		t.Fatalf("hash mismatch: %q vs %q", a, b)
	}
}
