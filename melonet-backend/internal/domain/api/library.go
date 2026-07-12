package api

import "time"

type PlayEventRequest struct {
	DurationPlayedSec int    `json:"duration_played_sec"`
	Source            string `json:"source"`
}

type PlayEventResponse struct {
	SongID    string    `json:"song_id"`
	PlayCount int       `json:"play_count"`
	PlayedAt  time.Time `json:"played_at"`
}

type LikeResponse struct {
	SongID string `json:"song_id"`
	Liked  bool   `json:"liked"`
}

type PlaylistDetailResponse struct {
	PlaylistResponse
	Songs []SongResponse `json:"songs"`
}

type CreatePlaylistRequest struct {
	Title       string `json:"title"`
	Description string `json:"description"`
	Visibility  string `json:"visibility"`
}

type UpdatePlaylistRequest struct {
	Title       *string `json:"title"`
	Description *string `json:"description"`
	Visibility  *string `json:"visibility"`
}

type AddPlaylistSongRequest struct {
	SongID string `json:"song_id"`
}

type ReorderPlaylistSongsRequest struct {
	SongIDs []string `json:"song_ids"`
}
