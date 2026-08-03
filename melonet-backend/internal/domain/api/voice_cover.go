package api

import "time"

type VoiceArtistResponse struct {
	ID          int64  `json:"id"`
	Slug        string `json:"slug"`
	DisplayName string `json:"display_name"`
	AvatarURL   string `json:"avatar_url"`
	Enabled     bool   `json:"enabled"`
}

type VoiceCoverResponse struct {
	ID               int64      `json:"id"`
	SourceSongID     string     `json:"source_song_id"`
	TargetArtistID   int64      `json:"target_artist_id"`
	TargetArtistSlug string     `json:"target_artist_slug"`
	TargetArtistName string     `json:"target_artist_name"`
	Status           string     `json:"status"`
	AudioURL         string     `json:"audio_url,omitempty"`
	CoverURL         string     `json:"cover_url"`
	SourceTitle      string     `json:"source_title"`
	SourceArtist     string     `json:"source_artist"`
	Error            string     `json:"error,omitempty"`
	ProgressPct      int        `json:"progress_pct"`
	ProgressStage    string     `json:"progress_stage"`
	EtaSeconds       int        `json:"eta_seconds"`
	CreatedAt        time.Time  `json:"created_at"`
	UpdatedAt        time.Time  `json:"updated_at"`
	ReadyAt          *time.Time `json:"ready_at,omitempty"`
}

type CreateVoiceCoverRequest struct {
	SourceSongID     string `json:"source_song_id" binding:"required"`
	TargetArtistSlug string `json:"target_artist_slug" binding:"required"`
}
