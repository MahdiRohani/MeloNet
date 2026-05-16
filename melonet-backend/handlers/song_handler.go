package handlers

import (
	"melonet-backend/database"
	"melonet-backend/models"
	"net/http"

	"github.com/gin-gonic/gin"
)

func GetSongs(c *gin.Context) {
	var songs []models.Song
	category := c.Query("category")

	if category != "" {
		database.DB.Where("category = ?", category).Find(&songs)
	} else {
		database.DB.Find(&songs)
	}

	c.JSON(http.StatusOK, songs)
}

func SearchSongs(c *gin.Context) {
	query := c.Query("q")
	var songs []models.Song

	if query == "" {
		c.JSON(http.StatusOK, gin.H{"message": "عبارت جستجو خالی است"})
		return
	}

	database.DB.Where("title LIKE ? OR artist LIKE ?", "%"+query+"%", "%"+query+"%").Find(&songs)

	c.JSON(http.StatusOK, songs)
}
