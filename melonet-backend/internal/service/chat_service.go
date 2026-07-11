package service

import (
	"context"
	"errors"
	"fmt"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
	"melonet-backend/internal/repository/postgres"
)

type ChatService struct {
	messages      *postgres.MessageRepository
	conversations *postgres.ConversationRepository
}

func NewChatService(messages *postgres.MessageRepository, conversations *postgres.ConversationRepository) *ChatService {
	return &ChatService{
		messages:      messages,
		conversations: conversations,
	}
}

func (s *ChatService) ListConversations(ctx context.Context, userID int64, page, limit int) ([]api.ConversationResponse, domain.Pagination, error) {
	summaries, total, err := s.conversations.ListForUser(ctx, userID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.ConversationSummariesToAPI(summaries, userID), domain.Pagination{
		Page: page, Limit: limit, Total: total,
	}, nil
}

func (s *ChatService) GetConversation(ctx context.Context, userID, conversationID int64) (api.ConversationResponse, error) {
	summary, err := s.conversations.GetForUser(ctx, userID, conversationID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.ConversationResponse{}, ErrNotFound
		}
		return api.ConversationResponse{}, err
	}
	return postgres.ConversationSummaryToAPI(summary, userID), nil
}

func (s *ChatService) CreateConversation(ctx context.Context, userID, otherUserID int64) (api.ConversationResponse, error) {
	if userID == otherUserID {
		return api.ConversationResponse{}, fmt.Errorf("cannot create conversation with yourself")
	}
	conversationID, err := s.messages.EnsureDirectConversation(ctx, uint(userID), uint(otherUserID))
	if err != nil {
		return api.ConversationResponse{}, err
	}
	return s.GetConversation(ctx, userID, conversationID)
}

func (s *ChatService) ConversationMessages(ctx context.Context, userID, conversationID int64, page, limit int) ([]api.MessageResponse, domain.Pagination, error) {
	member, err := s.conversations.IsMember(ctx, userID, conversationID)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	if !member {
		return nil, domain.Pagination{}, ErrForbidden
	}

	messages, total, err := s.messages.ListByConversation(ctx, userID, conversationID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.MessagesToAPI(messages), domain.Pagination{
		Page: page, Limit: limit, Total: total,
	}, nil
}

func (s *ChatService) UnreadCount(ctx context.Context, userID int64) (int, error) {
	return s.conversations.CountUnread(ctx, userID)
}

func (s *ChatService) History(ctx context.Context, userID, withID uint, page, limit int) ([]api.MessageResponse, domain.Pagination, error) {
	if userID == 0 || withID == 0 {
		return nil, domain.Pagination{}, fmt.Errorf("invalid user ids")
	}

	messages, total, err := s.messages.ListBetweenUsers(ctx, userID, withID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.MessagesToAPI(messages), domain.Pagination{
		Page: page, Limit: limit, Total: total,
	}, nil
}

func (s *ChatService) SendMessage(ctx context.Context, senderID, conversationID, receiverID int64, input db.Message) (api.MessageResponse, uint, error) {
	if conversationID == 0 && receiverID == 0 {
		return api.MessageResponse{}, 0, fmt.Errorf("receiver_id or conversation_id required")
	}

	if conversationID == 0 {
		convID, err := s.messages.EnsureDirectConversation(ctx, uint(senderID), uint(receiverID))
		if err != nil {
			return api.MessageResponse{}, 0, err
		}
		conversationID = convID
	} else {
		member, err := s.conversations.IsMember(ctx, senderID, conversationID)
		if err != nil {
			return api.MessageResponse{}, 0, err
		}
		if !member {
			return api.MessageResponse{}, 0, ErrForbidden
		}
		otherID, err := s.conversations.GetOtherMemberID(ctx, senderID, conversationID)
		if err != nil {
			return api.MessageResponse{}, 0, err
		}
		receiverID = otherID
	}

	if input.CreatedAt.IsZero() {
		input.CreatedAt = time.Now().UTC()
	}

	saved, err := s.messages.Create(ctx, uint(senderID), uint(receiverID), input)
	if err != nil {
		return api.MessageResponse{}, 0, err
	}

	return postgres.MessageToAPI(saved), uint(receiverID), nil
}

func (s *ChatService) MarkDelivered(ctx context.Context, messageID, receiverID int64) error {
	return s.messages.MarkDelivered(ctx, messageID, receiverID)
}

func (s *ChatService) MarkRead(ctx context.Context, readerID, conversationID int64, messageIDs []int64) (int64, error) {
	member, err := s.conversations.IsMember(ctx, readerID, conversationID)
	if err != nil {
		return 0, err
	}
	if !member {
		return 0, ErrForbidden
	}

	if _, err := s.messages.MarkConversationRead(ctx, readerID, conversationID, messageIDs); err != nil {
		return 0, err
	}

	senderID, err := s.conversations.GetOtherMemberID(ctx, readerID, conversationID)
	if err != nil {
		return 0, err
	}
	return senderID, nil
}

func (s *ChatService) GetOtherMember(ctx context.Context, userID, conversationID int64) (int64, error) {
	return s.conversations.GetOtherMemberID(ctx, userID, conversationID)
}

func (s *ChatService) ListPending(ctx context.Context, userID int64, limit int) ([]api.MessageResponse, error) {
	messages, err := s.messages.ListPendingForUser(ctx, userID, limit)
	if err != nil {
		return nil, err
	}
	return postgres.MessagesToAPI(messages), nil
}
