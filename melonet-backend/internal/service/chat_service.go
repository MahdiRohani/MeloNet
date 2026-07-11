package service

import (
	"context"
	"fmt"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/repository/postgres"
)

type ChatService struct {
	repo *postgres.MessageRepository
}

func NewChatService(repo *postgres.MessageRepository) *ChatService {
	return &ChatService{repo: repo}
}

func (s *ChatService) SaveMessage(ctx context.Context, message domain.Message) (domain.Message, error) {
	if message.MsgType == "" {
		message.MsgType = "text"
	}
	if message.CreatedAt.IsZero() {
		message.CreatedAt = time.Now().UTC()
	}

	return s.repo.Create(ctx, message)
}

func (s *ChatService) History(ctx context.Context, userID, withID uint, page, limit int) ([]domain.Message, domain.Pagination, error) {
	if userID == 0 || withID == 0 {
		return nil, domain.Pagination{}, fmt.Errorf("invalid user ids")
	}

	messages, total, err := s.repo.ListBetweenUsers(ctx, userID, withID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return messages, domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}
