package http

import (
	"log/slog"

	"melonet-backend/internal/config"
	"melonet-backend/internal/http/handler"
	"melonet-backend/internal/http/middleware"

	"github.com/gin-gonic/gin"
)

type Dependencies struct {
	Config  *config.Config
	Logger  *slog.Logger
	Health  *handler.HealthHandler
	Songs   *handler.SongHandler
	Chat    *handler.ChatHandler
}

func NewRouter(deps Dependencies) *gin.Engine {
	if !deps.Config.IsDevelopment() {
		gin.SetMode(gin.ReleaseMode)
	}

	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.RequestLogger(deps.Logger),
		middleware.Recovery(deps.Logger),
		middleware.Timeout(deps.Config.HTTP.ReadTimeout),
		middleware.CORS(deps.Config.CORS),
	)

	router.Static("/static", "./data")

	router.GET("/health/live", deps.Health.Live)
	router.GET("/health/ready", deps.Health.Ready)

	api := router.Group("/api")
	{
		api.GET("/songs", deps.Songs.List)
		api.GET("/search", deps.Songs.Search)
		api.GET("/chat/history", deps.Chat.History)
	}

	router.GET("/ws/chat", deps.Chat.WebSocket)

	return router
}
