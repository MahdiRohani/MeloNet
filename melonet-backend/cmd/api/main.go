package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"

	"melonet-backend/internal/auth"
	apphttp "melonet-backend/internal/http"
	"melonet-backend/internal/config"
	"melonet-backend/internal/http/handler"
	"melonet-backend/internal/migrate"
	"melonet-backend/internal/realtime"
	"melonet-backend/internal/repository/postgres"
	"melonet-backend/internal/service"
	minioStorage "melonet-backend/internal/storage/minio"
	redisStorage "melonet-backend/internal/storage/redis"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))

	if err := run(logger); err != nil {
		logger.Error("application stopped with error", "error", err)
		os.Exit(1)
	}
}

func run(logger *slog.Logger) error {
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
	logger.Info("database migrations applied", "path", migrationsPath)

	db, err := postgres.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		return err
	}
	defer db.Close()

	redisClient, err := redisStorage.Connect(ctx, cfg.RedisURL)
	if err != nil {
		return err
	}
	defer redisClient.Close()

	storageClient, err := minioStorage.Connect(ctx, cfg.Storage)
	if err != nil {
		return err
	}

	tokenMgr := auth.NewTokenManager(cfg.JWTSecret, cfg.JWTAccessTTL, cfg.JWTRefreshTTL)
	avatarStorage := minioStorage.NewAvatarStorage(storageClient, cfg.PublicBaseURL)

	userRepo := postgres.NewUserRepository(db)
	tokenRepo := postgres.NewTokenRepository(db)
	songRepo := postgres.NewSongRepository(db)
	catalogRepo := postgres.NewCatalogRepository(db)
	messageRepo := postgres.NewMessageRepository(db)

	authService := service.NewAuthService(userRepo, tokenRepo, tokenMgr, avatarStorage, cfg.PublicBaseURL)
	catalogService := service.NewCatalogService(songRepo, catalogRepo)
	searchService := service.NewSearchService(songRepo, catalogRepo)
	homeService := service.NewHomeService(catalogService)
	chatService := service.NewChatService(messageRepo)
	chatHub := realtime.NewHub(logger, chatService)

	router := apphttp.NewRouter(apphttp.Dependencies{
		Config:   cfg,
		Logger:   logger,
		TokenMgr: tokenMgr,
		Health:   handler.NewHealthHandler(db, redisClient, storageClient),
		Auth:     handler.NewAuthHandler(authService),
		Media:    handler.NewMediaHandler(avatarStorage),
		Catalog:  handler.NewCatalogHandler(catalogService),
		Search:   handler.NewSearchHandler(searchService),
		Home:     handler.NewHomeHandler(homeService),
		Chat:     handler.NewChatHandler(chatService, chatHub),
	})

	server := &http.Server{
		Addr:         ":" + cfg.HTTPPort,
		Handler:      router,
		ReadTimeout:  cfg.HTTP.ReadTimeout,
		WriteTimeout: cfg.HTTP.WriteTimeout,
	}

	errCh := make(chan error, 1)
	go func() {
		logger.Info("http server starting", "port", cfg.HTTPPort, "env", cfg.AppEnv)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			errCh <- err
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	select {
	case err := <-errCh:
		return err
	case sig := <-stop:
		logger.Info("shutdown signal received", "signal", sig.String())
	}

	shutdownCtx, cancel := context.WithTimeout(context.Background(), cfg.HTTP.ShutdownTimeout)
	defer cancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		return fmt.Errorf("graceful shutdown failed: %w", err)
	}

	logger.Info("server stopped gracefully")
	return nil
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
