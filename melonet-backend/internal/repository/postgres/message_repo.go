package postgres

import (
	"context"
	"fmt"

	"melonet-backend/internal/domain"
)

type MessageRepository struct {
	db *DB
}

func NewMessageRepository(db *DB) *MessageRepository {
	return &MessageRepository{db: db}
}

func (r *MessageRepository) Create(ctx context.Context, message domain.Message) (domain.Message, error) {
	err := r.db.Pool.QueryRow(ctx, `
		INSERT INTO messages (sender_id, receiver_id, content, msg_type, created_at)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id, sender_id, receiver_id, content, msg_type, created_at
	`, message.SenderID, message.ReceiverID, message.Content, message.MsgType, message.CreatedAt).Scan(
		&message.ID,
		&message.SenderID,
		&message.ReceiverID,
		&message.Content,
		&message.MsgType,
		&message.CreatedAt,
	)
	if err != nil {
		return domain.Message{}, fmt.Errorf("create message: %w", err)
	}
	return message, nil
}

func (r *MessageRepository) ListBetweenUsers(ctx context.Context, userID, withID uint, page, limit int) ([]domain.Message, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 50
	}
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM messages
		WHERE (sender_id = $1 AND receiver_id = $2) OR (sender_id = $2 AND receiver_id = $1)
	`, userID, withID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count messages: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT id, sender_id, receiver_id, content, msg_type, created_at
		FROM messages
		WHERE (sender_id = $1 AND receiver_id = $2) OR (sender_id = $2 AND receiver_id = $1)
		ORDER BY created_at ASC
		LIMIT $3 OFFSET $4
	`, userID, withID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list messages: %w", err)
	}
	defer rows.Close()

	messages := make([]domain.Message, 0)
	for rows.Next() {
		var message domain.Message
		if err := rows.Scan(
			&message.ID,
			&message.SenderID,
			&message.ReceiverID,
			&message.Content,
			&message.MsgType,
			&message.CreatedAt,
		); err != nil {
			return nil, 0, fmt.Errorf("scan message: %w", err)
		}
		messages = append(messages, message)
	}

	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate messages: %w", err)
	}

	return messages, total, nil
}
