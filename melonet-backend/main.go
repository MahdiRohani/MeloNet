package main

import (
	"melonet-backend/database"
	"melonet-backend/handlers"

	"github.com/gin-gonic/gin"
)

func main() {

	database.InitDB()

	r := gin.Default()

	r.Static("/static", "./data")

	api := r.Group("/api")
	{
		api.GET("/songs", handlers.GetSongs)
		api.GET("/search", handlers.SearchSongs)
		api.GET("/chat/history", handlers.GetChatHistory)
	}

	r.GET("/ws/chat", handlers.HandleChatConnections)

	r.Run(":8080")
}
