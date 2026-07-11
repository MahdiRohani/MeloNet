package domain

import "time"

type Song struct {
	ID          uint   `json:"id"`
	Title       string `json:"title"`
	Artist      string `json:"artist"`
	CoverURL    string `json:"cover_url"`
	AudioURL    string `json:"audio_url"`
	Category    string `json:"category"`
	Lyrics      string `json:"lyrics"`
	DurationSec int    `json:"duration_sec"`
}

type User struct {
	ID        uint   `json:"id"`
	Username  string `json:"username"`
	AvatarURL string `json:"avatar_url"`
	IsPremium bool   `json:"is_premium"`
}

type Message struct {
	ID         uint      `json:"id"`
	SenderID   uint      `json:"sender_id"`
	ReceiverID uint      `json:"receiver_id"`
	Content    string    `json:"content"`
	MsgType    string    `json:"msg_type"`
	CreatedAt  time.Time `json:"created_at"`
}

type Pagination struct {
	Page  int `json:"page"`
	Limit int `json:"limit"`
	Total int `json:"total"`
}
