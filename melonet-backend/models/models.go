package models

import "time"

type Song struct {
	ID          uint   `gorm:"primaryKey" json:"id"`
	Title       string `json:"title"`
	Artist      string `json:"artist"`
	CoverURL    string `json:"cover_url"`
	AudioURL    string `json:"audio_url"`
	Category    string `json:"category"`
	Lyrics      string `json:"lyrics"`
	DurationSec int    `json:"duration_sec"`
}

type User struct {
	ID        uint   `gorm:"primaryKey" json:"id"`
	Username  string `gorm:"unique" json:"username"`
	AvatarURL string `json:"avatar_url"`
	IsPremium bool   `json:"is_premium"`
}

type Message struct {
	ID         uint      `gorm:"primaryKey" json:"id"`
	SenderID   uint      `json:"sender_id"`
	ReceiverID uint      `json:"receiver_id"`
	Content    string    `json:"content"`
	MsgType    string    `json:"msg_type"`
	CreatedAt  time.Time `json:"created_at"`
}
