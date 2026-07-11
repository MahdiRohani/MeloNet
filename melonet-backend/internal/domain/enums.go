package domain

// MessageDeliveryStatus represents server-side delivery states.
// Client-side "sending" is not persisted in the database.
type MessageDeliveryStatus string

const (
	MessageStatusSent      MessageDeliveryStatus = "sent"
	MessageStatusDelivered MessageDeliveryStatus = "delivered"
	MessageStatusRead      MessageDeliveryStatus = "read"
)

func (s MessageDeliveryStatus) Valid() bool {
	switch s {
	case MessageStatusSent, MessageStatusDelivered, MessageStatusRead:
		return true
	default:
		return false
	}
}

type MessageContentType string

const (
	MessageTypeText   MessageContentType = "text"
	MessageTypeSong   MessageContentType = "song"
	MessageTypeImage  MessageContentType = "image"
	MessageTypeSystem MessageContentType = "system"
)

func (t MessageContentType) Valid() bool {
	switch t {
	case MessageTypeText, MessageTypeSong, MessageTypeImage, MessageTypeSystem:
		return true
	default:
		return false
	}
}

type NotificationType string

const (
	NotificationFollow        NotificationType = "follow"
	NotificationLike          NotificationType = "like"
	NotificationMessage       NotificationType = "message"
	NotificationPlaylistShare NotificationType = "playlist_share"
	NotificationSystem        NotificationType = "system"
)

type PlaylistVisibility string

const (
	PlaylistPrivate PlaylistVisibility = "private"
	PlaylistPublic  PlaylistVisibility = "public"
)

type ConversationType string

const (
	ConversationDirect ConversationType = "direct"
)
