package api

import "time"

type SongResponse struct {
	ID            uint   `json:"id"`
	Title         string `json:"title"`
	Artist        string `json:"artist"`
	ArtistName    string `json:"artist_name"`
	ArtistID      uint   `json:"artist_id,omitempty"`
	AlbumID       uint   `json:"album_id,omitempty"`
	GenreID       uint   `json:"genre_id,omitempty"`
	CoverURL      string `json:"cover_url"`
	CoverImageURL string `json:"cover_image_url"`
	AudioURL      string `json:"audio_url"`
	Category      string `json:"category"`
	Genre         string `json:"genre,omitempty"`
	AlbumTitle    string `json:"album_title,omitempty"`
	Lyrics        string `json:"lyrics"`
	DurationSec   int    `json:"duration_sec"`
	PlayCount     int    `json:"play_count,omitempty"`
}

type ArtistResponse struct {
	ID        uint   `json:"id"`
	Name      string `json:"name"`
	Slug      string `json:"slug"`
	Bio       string `json:"bio,omitempty"`
	ImageURL  string `json:"image_url"`
	SongCount int    `json:"song_count,omitempty"`
}

type AlbumResponse struct {
	ID          uint       `json:"id"`
	Title       string     `json:"title"`
	Slug        string     `json:"slug"`
	CoverURL    string     `json:"cover_url"`
	ArtistID    uint       `json:"artist_id"`
	ArtistName  string     `json:"artist_name"`
	ReleaseDate *time.Time `json:"release_date,omitempty"`
	SongCount   int        `json:"song_count,omitempty"`
}

type GenreResponse struct {
	ID        uint   `json:"id"`
	Name      string `json:"name"`
	Slug      string `json:"slug"`
	SongCount int    `json:"song_count,omitempty"`
}

type SearchResponse struct {
	Query   string             `json:"query"`
	Type    string             `json:"type"`
	Songs   []SongResponse     `json:"songs,omitempty"`
	Artists []ArtistResponse   `json:"artists,omitempty"`
	Albums  []AlbumResponse    `json:"albums,omitempty"`
	Genres  []GenreResponse    `json:"genres,omitempty"`
	Users   []UserSearchResult `json:"users,omitempty"`
}

type QuickActionResponse struct {
	ID     string `json:"id"`
	Title  string `json:"title"`
	Target string `json:"target"`
	Icon   string `json:"icon,omitempty"`
}

type HomeRowResponse struct {
	ID         string         `json:"id"`
	Title      string         `json:"title"`
	RowType    string         `json:"row_type"`
	SeeAllPath string         `json:"see_all_path,omitempty"`
	Items      []SongResponse `json:"items"`
}

type HomeFeedResponse struct {
	Carousel     []SongResponse          `json:"carousel"`
	QuickActions []QuickActionResponse   `json:"quick_actions"`
	Rows         []HomeRowResponse       `json:"rows"`
}
