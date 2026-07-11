package http

import (
	"log/slog"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/config"
	"melonet-backend/internal/http/handler"
	"melonet-backend/internal/http/middleware"

	"github.com/gin-gonic/gin"
)

type Dependencies struct {
	Config     *config.Config
	Logger     *slog.Logger
	TokenMgr   *auth.TokenManager
	Health     *handler.HealthHandler
	Auth       *handler.AuthHandler
	Media      *handler.MediaHandler
	Songs      *handler.SongHandler
	Chat       *handler.ChatHandler
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
	router.GET("/api/media/*object_path", deps.Media.Serve)

	authGroup := router.Group("/api/auth")
	{
		authGroup.POST("/register", deps.Auth.Register)
		authGroup.POST("/login", deps.Auth.Login)
		authGroup.POST("/refresh", deps.Auth.Refresh)
		authGroup.POST("/logout", deps.Auth.Logout)

		protectedAuth := authGroup.Group("")
		protectedAuth.Use(middleware.AuthRequired(deps.TokenMgr))
		{
			protectedAuth.GET("/me", deps.Auth.Me)
			protectedAuth.PATCH("/me", deps.Auth.UpdateProfile)
			protectedAuth.POST("/me/avatar", deps.Auth.UploadAvatar)
		}
	}

	api := router.Group("/api")
	api.Use(middleware.AuthRequired(deps.TokenMgr))
	{
		api.GET("/songs", deps.Songs.List)
		api.GET("/search", deps.Songs.Search)
		api.GET("/chat/history", deps.Chat.History)
	}

	router.GET("/ws/chat", middleware.AuthRequired(deps.TokenMgr), deps.Chat.WebSocket)

	return router
}
