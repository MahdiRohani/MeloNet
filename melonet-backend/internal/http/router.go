package http

import (
	"log/slog"
	"time"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/config"
	"melonet-backend/internal/http/handler"
	"melonet-backend/internal/http/middleware"

	"github.com/gin-gonic/gin"
)

type Dependencies struct {
	Config        *config.Config
	Logger        *slog.Logger
	TokenMgr      *auth.TokenManager
	RateLimit     middleware.RateLimitStore
	Health        *handler.HealthHandler
	Auth       *handler.AuthHandler
	Media      *handler.MediaHandler
	Catalog    *handler.CatalogHandler
	Search     *handler.SearchHandler
	Home       *handler.HomeHandler
	Chat       *handler.ChatHandler
	Library    *handler.LibraryHandler
	Playlist   *handler.PlaylistHandler
	Social     *handler.SocialHandler
}

func NewRouter(deps Dependencies) *gin.Engine {
	if !deps.Config.IsDevelopment() {
		gin.SetMode(gin.ReleaseMode)
	}

	router := gin.New()
	if len(deps.Config.TrustedProxies) > 0 {
		_ = router.SetTrustedProxies(deps.Config.TrustedProxies)
	}
	router.Use(
		middleware.RequestID(),
		middleware.RequestLogger(deps.Logger),
		middleware.Recovery(deps.Logger),
		middleware.SecureHeaders(deps.Config),
		middleware.Timeout(deps.Config.HTTP.ReadTimeout),
		middleware.CORS(deps.Config.CORS),
	)

	rateLimit := deps.RateLimit
	loginLimit := middleware.RateLimit(rateLimit, "login", deps.Config.RateLimit.LoginPerMinute, time.Minute, middleware.ClientIPKey)
	registerLimit := middleware.RateLimit(rateLimit, "register", deps.Config.RateLimit.RegisterPerMinute, time.Minute, middleware.ClientIPKey)
	searchLimit := middleware.RateLimit(rateLimit, "search", deps.Config.RateLimit.SearchPerMinute, time.Minute, middleware.UserIDKey)
	chatLimit := middleware.RateLimit(rateLimit, "chat", deps.Config.RateLimit.ChatPerMinute, time.Minute, middleware.UserIDKey)

	router.Static("/static", "./data")

	router.GET("/health/live", deps.Health.Live)
	router.GET("/health/ready", deps.Health.Ready)
	if deps.Media != nil {
		router.GET("/api/media/*object_path", deps.Media.Serve)
	}

	authGroup := router.Group("/api/auth")
	{
		authGroup.POST("/register", registerLimit, deps.Auth.Register)
		authGroup.POST("/login", loginLimit, deps.Auth.Login)
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
		api.POST("/songs/:id/play", deps.Library.RecordPlay)
		api.POST("/songs/:id/like", deps.Library.LikeSong)
		api.DELETE("/songs/:id/like", deps.Library.UnlikeSong)
		api.GET("/search", searchLimit, deps.Search.Search)

		api.GET("/users/search", deps.Social.SearchUsers)
		api.GET("/users/:id", deps.Social.GetProfile)
		api.GET("/users/:id/playlists", deps.Social.ListPlaylists)
		api.GET("/users/:id/followers", deps.Social.ListFollowers)
		api.GET("/users/:id/following", deps.Social.ListFollowing)
		api.POST("/users/:id/follow", deps.Social.Follow)
		api.DELETE("/users/:id/follow", deps.Social.Unfollow)

		api.GET("/notifications", deps.Social.ListNotifications)
		api.POST("/notifications/read-all", deps.Social.MarkAllNotificationsRead)
		api.PATCH("/notifications/:id/read", deps.Social.MarkNotificationRead)

		api.GET("/library/liked", deps.Library.ListLiked)
		api.GET("/library/recent", deps.Library.ListRecent)

		api.GET("/playlists", deps.Playlist.List)
		api.POST("/playlists", deps.Playlist.Create)
		api.GET("/playlists/:id", deps.Playlist.Get)
		api.PATCH("/playlists/:id", deps.Playlist.Update)
		api.DELETE("/playlists/:id", deps.Playlist.Delete)
		api.GET("/playlists/:id/songs", deps.Playlist.ListSongs)
		api.POST("/playlists/:id/songs", deps.Playlist.AddSong)
		api.PUT("/playlists/:id/songs/reorder", deps.Playlist.ReorderSongs)
		api.DELETE("/playlists/:id/songs/:songId", deps.Playlist.RemoveSong)

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

		api.GET("/conversations", deps.Chat.ListConversations)
		api.POST("/conversations", deps.Chat.CreateConversation)
		api.GET("/conversations/unread-count", deps.Chat.UnreadCount)
		api.GET("/conversations/:id", deps.Chat.GetConversation)
		api.GET("/conversations/:id/messages", deps.Chat.ConversationMessages)
		api.POST("/conversations/:id/read", deps.Chat.MarkRead)
	}

	router.GET("/ws/chat", middleware.AuthRequired(deps.TokenMgr), chatLimit, deps.Chat.WebSocket)

	return router
}
