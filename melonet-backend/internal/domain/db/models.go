package db

import (
	"time"

	"melonet-backend/internal/domain"
)

type User struct {
	ID               int64
	Username         string
	Email            *string
	PasswordHash     string
	DisplayName      string
	Bio              string
	AvatarURL        string
	AvatarObjectKey  string
	IsPremium        bool
	PremiumUntil     *time.Time
	CreatedAt        time.Time
	UpdatedAt        time.Time
}

type RefreshToken struct {
	ID        int64
	UserID    int64
	TokenHash string
	ExpiresAt time.Time
	RevokedAt *time.Time
	CreatedAt time.Time
}

type Artist struct {
	ID              int64
	Name            string
	Slug            string
	Bio             string
	ImageObjectKey  string
	ImageURL        string
	CreatedAt       time.Time
	UpdatedAt       time.Time
}

type Genre struct {
	ID        int64
	Name      string
	Slug      string
	CreatedAt time.Time
}

type Album struct {
	ID              int64
	ArtistID        int64
	Title           string
	Slug            string
	CoverObjectKey  string
	CoverURL        string
	ReleaseDate     *time.Time
	CreatedAt       time.Time
	UpdatedAt       time.Time
}

type Song struct {
	ID              int64
	ArtistID        int64
	AlbumID         *int64
	GenreID         *int64
	Title           string
	Slug            string
	CoverObjectKey  string
	AudioObjectKey  string
	CoverURL        string
	AudioURL        string
	Category        string
	Lyrics          string
	DurationSec     int
	PlayCount       int
	PublishedAt     *time.Time
	CreatedAt       time.Time
	UpdatedAt       time.Time
	ArtistName      string
	GenreName       string
	AlbumTitle      string
}

type Playlist struct {
	ID              int64
	OwnerID         int64
	Title           string
	Description     string
	Visibility      domain.PlaylistVisibility
	CoverObjectKey  string
	CoverURL        string
	IsSystem        bool
	CreatedAt       time.Time
	UpdatedAt       time.Time
}

type PlaylistSong struct {
	PlaylistID int64
	SongID     int64
	Position   int
	AddedAt    time.Time
}

type Like struct {
	UserID    int64
	SongID    int64
	CreatedAt time.Time
}

type Follow struct {
	FollowerID  int64
	FollowingID int64
	CreatedAt   time.Time
}

type PlayHistory struct {
	ID                int64
	UserID            int64
	SongID            int64
	PlayedAt          time.Time
	DurationPlayedSec int
	Source            string
}

type DownloadEntitlement struct {
	ID        int64
	UserID    int64
	SongID    int64
	GrantedAt time.Time
	ExpiresAt *time.Time
}

type Conversation struct {
	ID        int64
	Type      domain.ConversationType
	CreatedAt time.Time
	UpdatedAt time.Time
}

type ConversationMember struct {
	ConversationID int64
	UserID         int64
	JoinedAt       time.Time
}

type Message struct {
	ID               int64
	ConversationID   int64
	SenderID         int64
	MsgType          domain.MessageContentType
	Content          string
	SongID           *int64
	DeliveryStatus   domain.MessageDeliveryStatus
	CreatedAt        time.Time
	UpdatedAt        time.Time
	ReceiverID       int64
}

type MessageReceipt struct {
	ID        int64
	MessageID int64
	UserID    int64
	Status    domain.MessageDeliveryStatus
	UpdatedAt time.Time
}

type Notification struct {
	ID        int64
	UserID    int64
	Type      domain.NotificationType
	Title     string
	Body      string
	Payload   []byte
	ReadAt    *time.Time
	CreatedAt time.Time
}
