package service

import (
	"testing"
	"time"

	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
)

func TestValidateRegister(t *testing.T) {
	if err := validateRegister(api.RegisterRequest{Username: "ab", Email: "bad", Password: "123"}); err == nil {
		t.Fatal("expected invalid username")
	}

	if err := validateRegister(api.RegisterRequest{Username: "valid_user", Email: "bad-email", Password: "12345678"}); err == nil {
		t.Fatal("expected invalid email")
	}

	if err := validateRegister(api.RegisterRequest{Username: "valid_user", Email: "user@melonet.local", Password: "short"}); err == nil {
		t.Fatal("expected invalid password")
	}

	if err := validateRegister(api.RegisterRequest{Username: "valid_user", Email: "user@melonet.local", Password: "melonet123"}); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestIsPremiumActive(t *testing.T) {
	later := time.Now().UTC().Add(24 * time.Hour)

	user := db.User{IsPremium: true, PremiumUntil: &later}
	if !isPremiumActive(user, db.PremiumEntitlement{}) {
		t.Fatal("expected active premium from user flag")
	}

	user = db.User{IsPremium: false}
	entitlement := db.PremiumEntitlement{ID: 1, ExpiresAt: &later, Source: "admin"}
	if !isPremiumActive(user, entitlement) {
		t.Fatal("expected active premium from entitlement")
	}
}
