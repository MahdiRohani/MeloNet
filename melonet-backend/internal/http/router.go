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
	Catalog    *handler.CatalogHandler
	Search     *handler.SearchHandler
	Home       *handler.HomeHandler
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
		api.GET("/home", deps.Home.Feed)

		api.GET("/songs", deps.Catalog.ListSongs)
		api.GET("/songs/:id", deps.Catalog.GetSong)
		api.GET("/search", deps.Search.Search)

		api.GET("/catalog/popular", deps.Catalog.Popular)
		api.GET("/catalog/new", deps.Catalog.Newest)
		api.GET("/catalog/trending", deps.Catalog.Trending)

		api.GET("/artists", deps.Catalog.ListArtists)
		api.GET("/artists/:id", deps.Catalog.GetArtist)
		api.GET("/artists/:id/songs", deps.Catalog.ListArtistSongs)

		api.GET("/albums/:id", deps.Catalog.GetAlbum)
		api.GET("/albums/:id/songs", deps.Catalog.ListAlbumSongs)

		api.GET("/genres", deps.Catalog.ListGenres)
		api.GET("/genres/:id", deps.Catalog.GetGenre)
		api.GET("/genres/:id/songs", deps.Catalog.ListGenreSongs)

		api.GET("/chat/history", deps.Chat.History)
	}

	router.GET("/ws/chat", middleware.AuthRequired(deps.TokenMgr), deps.Chat.WebSocket)

	return router
}
