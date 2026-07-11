package main

import (
	"context"
	"flag"
	"fmt"
	"log/slog"
	"os"
	"path/filepath"

	"melonet-backend/internal/config"
	"melonet-backend/internal/migrate"
	"melonet-backend/internal/repository/postgres"
	"melonet-backend/internal/seed"
	minioStorage "melonet-backend/internal/storage/minio"
)

func main() {
	force := flag.Bool("force", false, "re-seed tracks even if already uploaded")
	flag.Parse()

	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))

	if err := run(logger, *force); err != nil {
		logger.Error("seed failed", "error", err)
		os.Exit(1)
	}
}

func run(logger *slog.Logger, force bool) error {
	cfg, err := config.Load()
	if err != nil {
		return err
	}

	ctx := context.Background()

	migrationsPath, err := resolveMigrationsPath()
	if err != nil {
		return err
	}

	if err := migrate.Up(cfg.DatabaseURL, migrationsPath); err != nil {
		return fmt.Errorf("database migration failed: %w", err)
	}

	db, err := postgres.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		return err
	}
	defer db.Close()

	storageClient, err := minioStorage.Connect(ctx, cfg.Storage)
	if err != nil {
		return err
	}

	mediaStorage := minioStorage.NewMediaStorage(storageClient, cfg.PublicBaseURL)
	seedRepo := postgres.NewSeedRepository(db)
	cacheDir := resolveCacheDir()

	runner := seed.NewRunner(logger, seedRepo, mediaStorage, cacheDir, seed.Options{Force: force})
	return runner.Run(ctx)
}

func resolveMigrationsPath() (string, error) {
	if envPath := os.Getenv("MIGRATIONS_PATH"); envPath != "" {
		return envPath, nil
	}

	wd, err := os.Getwd()
	if err != nil {
		return "", fmt.Errorf("resolve working directory: %w", err)
	}

	candidates := []string{
		filepath.Join(wd, "migrations"),
		filepath.Join(wd, "melonet-backend", "migrations"),
	}

	for _, candidate := range candidates {
		if info, err := os.Stat(candidate); err == nil && info.IsDir() {
			return candidate, nil
		}
	}

	return "", fmt.Errorf("migrations directory not found")
}

func resolveCacheDir() string {
	if envPath := os.Getenv("SEED_CACHE_DIR"); envPath != "" {
		return envPath
	}

	wd, _ := os.Getwd()
	candidates := []string{
		filepath.Join(wd, "data", ".cache"),
		filepath.Join(wd, "melonet-backend", "data", ".cache"),
	}
	for _, candidate := range candidates {
		if err := os.MkdirAll(candidate, 0o755); err == nil {
			return candidate
		}
	}
	return filepath.Join(os.TempDir(), "melonet-seed-cache")
}
