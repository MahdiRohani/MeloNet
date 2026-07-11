package realtime

import (
	"context"

	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
)

type ChatPort interface {
	SendMessage(ctx context.Context, senderID, conversationID, receiverID int64, input db.Message) (api.MessageResponse, uint, error)
	MarkDelivered(ctx context.Context, messageID, receiverID int64) error
	MarkRead(ctx context.Context, readerID, conversationID int64, messageIDs []int64) (int64, error)
	GetOtherMember(ctx context.Context, userID, conversationID int64) (int64, error)
	ListPending(ctx context.Context, userID int64, limit int) ([]api.MessageResponse, error)
}
