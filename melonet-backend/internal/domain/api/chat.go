package api

import "time"

type ConversationResponse struct {
	ID          uint                `json:"id"`
	Type        string              `json:"type"`
	OtherUser   *PublicUserResponse `json:"other_user,omitempty"`
	LastMessage *MessageResponse    `json:"last_message,omitempty"`
	UnreadCount int                 `json:"unread_count"`
	UpdatedAt   time.Time           `json:"updated_at"`
}

type CreateConversationRequest struct {
	UserID uint `json:"user_id"`
}

type UnreadCountResponse struct {
	Total int `json:"total"`
}

type MarkReadRequest struct {
	MessageIDs []uint `json:"message_ids"`
}
