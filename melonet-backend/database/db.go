package database

import (
	"fmt"
	"melonet-backend/models"

	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

var DB *gorm.DB

func InitDB() {
	var err error
	DB, err = gorm.Open(sqlite.Open("melonet.db"), &gorm.Config{})
	if err != nil {
		panic("خطا در اتصال به دیتابیس!")
	}

	DB.AutoMigrate(&models.Song{}, &models.User{}, &models.Message{})

	var count int64
	DB.Model(&models.Song{}).Count(&count)
	if count == 0 {
		seedData()
	}
}

func seedData() {

	users := []models.User{
		{Username: "mahdi", AvatarURL: "/static/covers/cover1.jpg", IsPremium: true},
		{Username: "student_test", AvatarURL: "/static/covers/cover2.jpg", IsPremium: false},
	}
	DB.Create(&users)

	categories := []string{"Iranian", "Global", "Nostalgia", "New", "Popular"}

	var songs []models.Song
	for i := 1; i <= 50; i++ {

		category := categories[i%len(categories)]

		song := models.Song{
			Title:       fmt.Sprintf("MeloNet Track %d", i),
			Artist:      fmt.Sprintf("Artist Name %d", (i%7)+1),
			CoverURL:    fmt.Sprintf("/static/covers/cover%d.jpg", i),
			AudioURL:    fmt.Sprintf("/static/audio/song%d.mp3", i),
			Category:    category,
			Lyrics:      fmt.Sprintf("[00:00] Intro Track %d\n[00:05] This is synchronization lyrics test\n[00:12] MeloNet is working perfectly!", i),
			DurationSec: 150 + (i * 2),
		}
		songs = append(songs, song)
	}

	DB.Create(&songs)
	fmt.Println("🎉 دیتابیس MeloNet با ۵۰ آهنگ واقعی و فایل‌های مربوطه با موفقیت ست شد!")
}
