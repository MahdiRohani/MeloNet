//go:build integration

package integration_test

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/config"
	apphttp "melonet-backend/internal/http"
	"melonet-backend/internal/http/handler"
	"melonet-backend/internal/http/middleware"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/migrate"
	"melonet-backend/internal/repository/postgres"
	"melonet-backend/internal/service"
	redisStorage "melonet-backend/internal/storage/redis"

	"github.com/gin-gonic/gin"
)

func TestIntegrationAuthRegisterLoginMe(t *testing.T) {
	env := setupIntegration(t)
	defer env.cleanup()

	username := fmt.Sprintf("user_%d", time.Now().UnixNano())
	registerBody := map[string]string{
		"username":     username,
		"email":        username + "@melonet.test",
		"password":     "melonet123",
		"display_name": "Integration User",
	}
	regResp := env.postJSON(t, "/api/auth/register", registerBody, "")
	if regResp.StatusCode != http.StatusCreated {
		t.Fatalf("register status = %d body=%s", regResp.StatusCode, readBody(t, regResp))
	}

	loginResp := env.postJSON(t, "/api/auth/login", map[string]string{
		"login":    username,
		"password": "melonet123",
	}, "")
	if loginResp.StatusCode != http.StatusOK {
		t.Fatalf("login status = %d body=%s", loginResp.StatusCode, readBody(t, loginResp))
	}

	token := extractAccessToken(t, loginResp)
	meResp := env.get(t, "/api/auth/me", token)
	if meResp.StatusCode != http.StatusOK {
		t.Fatalf("me status = %d body=%s", meResp.StatusCode, readBody(t, meResp))
	}
}

func TestIntegrationUnauthorizedCatalog(t *testing.T) {
	env := setupIntegration(t)
	defer env.cleanup()

	resp := env.get(t, "/api/songs", "")
	if resp.StatusCode != http.StatusUnauthorized {
		t.Fatalf("status = %d, want 401", resp.StatusCode)
	}
}

func TestIntegrationPlaylistIDOR(t *testing.T) {
	env := setupIntegration(t)
	defer env.cleanup()

	userA := env.registerUser(t, "playlist_a")
	userB := env.registerUser(t, "playlist_b")

	createResp := env.postJSON(t, "/api/playlists", map[string]string{
		"title":      "Private List",
		"visibility": "private",
	}, userA)
	if createResp.StatusCode != http.StatusCreated {
		t.Fatalf("create playlist status = %d", createResp.StatusCode)
	}

	var envelope response.Envelope
	if err := json.NewDecoder(createResp.Body).Decode(&envelope); err != nil {
		t.Fatalf("decode: %v", err)
	}
	data, _ := json.Marshal(envelope.Data)
	var playlist struct {
		ID uint `json:"id"`
	}
	_ = json.Unmarshal(data, &playlist)

	deleteResp := env.request(t, http.MethodDelete, fmt.Sprintf("/api/playlists/%d", playlist.ID), "", userB)
	if deleteResp.StatusCode != http.StatusForbidden && deleteResp.StatusCode != http.StatusNotFound {
		t.Fatalf("delete by other user status = %d, want 403 or 404", deleteResp.StatusCode)
	}
}

func TestIntegrationChatConversationMembership(t *testing.T) {
	env := setupIntegration(t)
	defer env.cleanup()

	userA := env.registerUser(t, "chat_a")
	userB := env.registerUser(t, "chat_b")

	convResp := env.postJSON(t, "/api/conversations", map[string]uint{"user_id": env.userID(t, userB)}, userA)
	if convResp.StatusCode != http.StatusCreated {
		t.Fatalf("create conversation status = %d body=%s", convResp.StatusCode, readBody(t, convResp))
	}

	var envelope response.Envelope
	_ = json.NewDecoder(convResp.Body).Decode(&envelope)
	data, _ := json.Marshal(envelope.Data)
	var conversation struct {
		ID uint `json:"id"`
	}
	_ = json.Unmarshal(data, &conversation)

	msgResp := env.get(t, fmt.Sprintf("/api/conversations/%d/messages", conversation.ID), userB)
	if msgResp.StatusCode != http.StatusOK {
		t.Fatalf("member messages status = %d", msgResp.StatusCode)
	}

	userC := env.registerUser(t, "chat_c")
	foreignResp := env.get(t, fmt.Sprintf("/api/conversations/%d/messages", conversation.ID), userC)
	if foreignResp.StatusCode != http.StatusForbidden {
		t.Fatalf("foreign access status = %d, want 403", foreignResp.StatusCode)
	}
}

type integrationEnv struct {
	router http.Handler
	db     *postgres.DB
	redis  *redisStorage.Client
}

func (e *integrationEnv) cleanup() {
	if e.redis != nil {
		_ = e.redis.Close()
	}
	if e.db != nil {
		e.db.Close()
	}
}

func setupIntegration(t *testing.T) *integrationEnv {
	t.Helper()
	if os.Getenv("INTEGRATION_TEST") != "1" {
		t.Skip("set INTEGRATION_TEST=1 to run integration tests")
	}

	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("config: %v", err)
	}

	ctx := context.Background()
	migrationsPath := os.Getenv("MIGRATIONS_PATH")
	if migrationsPath == "" {
		migrationsPath = "migrations"
	}
	if err := migrate.Up(cfg.DatabaseURL, migrationsPath); err != nil {
		t.Fatalf("migrate: %v", err)
	}

	db, err := postgres.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		t.Fatalf("db connect: %v", err)
	}

	redisClient, err := redisStorage.Connect(ctx, cfg.RedisURL)
	if err != nil {
		db.Close()
		t.Fatalf("redis connect: %v", err)
	}

	tokenMgr := auth.NewTokenManager(cfg.JWTSecret, cfg.JWTAccessTTL, cfg.JWTRefreshTTL)
	userRepo := postgres.NewUserRepository(db)
	tokenRepo := postgres.NewTokenRepository(db)
	songRepo := postgres.NewSongRepository(db)
	catalogRepo := postgres.NewCatalogRepository(db)
	messageRepo := postgres.NewMessageRepository(db)
	conversationRepo := postgres.NewConversationRepository(db)
	likeRepo := postgres.NewLikeRepository(db)
	historyRepo := postgres.NewHistoryRepository(db)
	playlistRepo := postgres.NewPlaylistRepository(db)
	followRepo := postgres.NewFollowRepository(db)
	notificationRepo := postgres.NewNotificationRepository(db)

	authService := service.NewAuthService(userRepo, tokenRepo, tokenMgr, nil, cfg.PublicBaseURL)
	catalogService := service.NewCatalogService(songRepo, catalogRepo)
	searchService := service.NewSearchService(songRepo, catalogRepo, userRepo)
	homeService := service.NewHomeService(catalogService)
	libraryService := service.NewLibraryService(likeRepo, historyRepo, songRepo)
	playlistService := service.NewPlaylistService(playlistRepo, songRepo)
	socialService := service.NewSocialService(userRepo, followRepo, playlistRepo, notificationRepo)
	chatService := service.NewChatService(messageRepo, conversationRepo)

	logger := slog.New(slog.NewTextHandler(io.Discard, nil))
	router := apphttp.NewRouter(apphttp.Dependencies{
		Config:    cfg,
		Logger:    logger,
		TokenMgr:  tokenMgr,
		RateLimit: middleware.NewMemoryRateLimitStore(),
		Health:    handler.NewHealthHandler(db, redisClient, redisClient),
		Auth:      handler.NewAuthHandler(authService),
		Catalog:   handler.NewCatalogHandler(catalogService),
		Search:    handler.NewSearchHandler(searchService),
		Home:      handler.NewHomeHandler(homeService),
		Library:   handler.NewLibraryHandler(libraryService),
		Playlist:  handler.NewPlaylistHandler(playlistService),
		Social:    handler.NewSocialHandler(socialService),
		Chat:      handler.NewChatHandler(chatService, noopChatHub{}),
	})

	return &integrationEnv{router: router, db: db, redis: redisClient}
}

type noopChatHub struct{}

func (noopChatHub) HandleConnection(_ *gin.Context, _ uint) {}

func (e *integrationEnv) postJSON(t *testing.T, path string, body any, token string) *http.Response {
	t.Helper()
	payload, _ := json.Marshal(body)
	return e.request(t, http.MethodPost, path, string(payload), token)
}

func (e *integrationEnv) get(t *testing.T, path, token string) *http.Response {
	t.Helper()
	return e.request(t, http.MethodGet, path, "", token)
}

func (e *integrationEnv) request(t *testing.T, method, path, body, token string) *http.Response {
	t.Helper()
	var reader io.Reader
	if body != "" {
		reader = bytes.NewBufferString(body)
	}
	req := httptest.NewRequest(method, path, reader)
	if body != "" {
		req.Header.Set("Content-Type", "application/json")
	}
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	w := httptest.NewRecorder()
	e.router.ServeHTTP(w, req)
	return w.Result()
}

func (e *integrationEnv) registerUser(t *testing.T, prefix string) string {
	t.Helper()
	username := fmt.Sprintf("%s_%d", prefix, time.Now().UnixNano())
	resp := e.postJSON(t, "/api/auth/register", map[string]string{
		"username":     username,
		"email":        username + "@melonet.test",
		"password":     "melonet123",
		"display_name": username,
	}, "")
	if resp.StatusCode != http.StatusCreated {
		t.Fatalf("register %s status = %d", username, resp.StatusCode)
	}
	return extractAccessToken(t, resp)
}

func (e *integrationEnv) userID(t *testing.T, token string) uint {
	t.Helper()
	resp := e.get(t, "/api/auth/me", token)
	var envelope response.Envelope
	_ = json.NewDecoder(resp.Body).Decode(&envelope)
	data, _ := json.Marshal(envelope.Data)
	var user struct {
		ID uint `json:"id"`
	}
	_ = json.Unmarshal(data, &user)
	return user.ID
}

func extractAccessToken(t *testing.T, resp *http.Response) string {
	t.Helper()
	var envelope response.Envelope
	if err := json.NewDecoder(resp.Body).Decode(&envelope); err != nil {
		t.Fatalf("decode token response: %v", err)
	}
	data, _ := json.Marshal(envelope.Data)
	var authResp struct {
		AccessToken string `json:"access_token"`
	}
	if err := json.Unmarshal(data, &authResp); err != nil {
		t.Fatalf("decode auth data: %v", err)
	}
	if authResp.AccessToken == "" {
		t.Fatal("missing access token")
	}
	return authResp.AccessToken
}

func readBody(t *testing.T, resp *http.Response) string {
	t.Helper()
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	return string(b)
}
