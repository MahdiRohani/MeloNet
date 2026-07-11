package config

import (
	"os"
	"testing"
	"time"
)

func TestLoadDefaults(t *testing.T) {
	t.Setenv("DATABASE_URL", "postgres://test:test@localhost:5432/test?sslmode=disable")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}

	if cfg.HTTPPort != "8080" {
		t.Errorf("HTTPPort = %q, want 8080", cfg.HTTPPort)
	}
	if cfg.AppEnv != "development" {
		t.Errorf("AppEnv = %q, want development", cfg.AppEnv)
	}
	if cfg.Storage.Bucket != "melonet-media" {
		t.Errorf("Storage.Bucket = %q, want melonet-media", cfg.Storage.Bucket)
	}
	if cfg.HTTP.ReadTimeout != 15*time.Second {
		t.Errorf("HTTP.ReadTimeout = %v, want 15s", cfg.HTTP.ReadTimeout)
	}
}

func TestLoadMissingDatabaseURL(t *testing.T) {
	t.Setenv("DATABASE_URL", "")

	_, err := Load()
	if err == nil {
		t.Fatal("expected error when DATABASE_URL is empty")
	}
}

func TestLoadFromEnv(t *testing.T) {
	t.Setenv("DATABASE_URL", "postgres://custom:custom@db:5432/melonet?sslmode=disable")
	t.Setenv("APP_ENV", "staging")
	t.Setenv("HTTP_PORT", "9090")
	t.Setenv("STORAGE_USE_SSL", "true")
	t.Setenv("CORS_ALLOWED_ORIGINS", "https://app.example.com, https://admin.example.com")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}

	if cfg.AppEnv != "staging" {
		t.Errorf("AppEnv = %q, want staging", cfg.AppEnv)
	}
	if cfg.HTTPPort != "9090" {
		t.Errorf("HTTPPort = %q, want 9090", cfg.HTTPPort)
	}
	if !cfg.Storage.UseSSL {
		t.Error("Storage.UseSSL = false, want true")
	}
	if len(cfg.CORS.AllowedOrigins) != 2 {
		t.Fatalf("len(CORS.AllowedOrigins) = %d, want 2", len(cfg.CORS.AllowedOrigins))
	}
}

func TestIsDevelopment(t *testing.T) {
	cfg := &Config{AppEnv: "development"}
	if !cfg.IsDevelopment() {
		t.Error("expected development mode")
	}

	cfg.AppEnv = "production"
	if cfg.IsDevelopment() {
		t.Error("expected non-development mode")
	}
}

func TestMain(m *testing.M) {
	os.Unsetenv("DATABASE_URL")
	os.Exit(m.Run())
}
