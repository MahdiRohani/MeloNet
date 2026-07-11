package service

import (
	"context"
	"fmt"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
	"melonet-backend/internal/repository/postgres"
)

type ChatService struct {
	repo *postgres.MessageRepository
}

func NewChatService(repo *postgres.MessageRepository) *ChatService {
	return &ChatService{repo: repo}
}

func (s *ChatService) SaveMessage(ctx context.Context, senderID, receiverID uint, input db.Message) (api.MessageResponse, error) {
	if senderID == 0 || receiverID == 0 {
		return api.MessageResponse{}, fmt.Errorf("invalid user ids")
	}

	if input.MsgType == "" {
		input.MsgType = domain.MessageTypeText
	}
	if input.CreatedAt.IsZero() {
		input.CreatedAt = time.Now().UTC()
	}

	saved, err := s.repo.Create(ctx, senderID, receiverID, input)
	if err != nil {
		return api.MessageResponse{}, err
	}

	return postgres.MessageToAPI(saved), nil
}

func (s *ChatService) History(ctx context.Context, userID, withID uint, page, limit int) ([]api.MessageResponse, domain.Pagination, error) {
	if userID == 0 || withID == 0 {
		return nil, domain.Pagination{}, fmt.Errorf("invalid user ids")
	}

	messages, total, err := s.repo.ListBetweenUsers(ctx, userID, withID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.MessagesToAPI(messages), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}
