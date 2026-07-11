package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

type MessageRepository struct {
	db *DB
}

func NewMessageRepository(db *DB) *MessageRepository {
	return &MessageRepository{db: db}
}

func (r *MessageRepository) FindDirectConversationID(ctx context.Context, userID, withID uint) (int64, error) {
	var conversationID int64
	err := r.db.Pool.QueryRow(ctx, `
		SELECT cm1.conversation_id
		FROM conversation_members AS cm1
		INNER JOIN conversation_members AS cm2
			ON cm2.conversation_id = cm1.conversation_id
		   AND cm2.user_id = $2
		WHERE cm1.user_id = $1
		LIMIT 1
	`, userID, withID).Scan(&conversationID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return 0, nil
		}
		return 0, fmt.Errorf("find conversation: %w", err)
	}
	return conversationID, nil
}

func (r *MessageRepository) EnsureDirectConversation(ctx context.Context, userID, withID uint) (int64, error) {
	if userID == 0 || withID == 0 || userID == withID {
		return 0, fmt.Errorf("invalid conversation participants")
	}

	conversationID, err := r.FindDirectConversationID(ctx, userID, withID)
	if err != nil {
		return 0, err
	}
	if conversationID != 0 {
		return conversationID, nil
	}

	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return 0, fmt.Errorf("begin conversation tx: %w", err)
	}
	defer tx.Rollback(ctx)

	if err := tx.QueryRow(ctx, `
		INSERT INTO conversations (type)
		VALUES ('direct')
		RETURNING id
	`).Scan(&conversationID); err != nil {
		return 0, fmt.Errorf("create conversation: %w", err)
	}

	for _, memberID := range []uint{userID, withID} {
		if _, err := tx.Exec(ctx, `
			INSERT INTO conversation_members (conversation_id, user_id)
			VALUES ($1, $2)
			ON CONFLICT DO NOTHING
		`, conversationID, memberID); err != nil {
			return 0, fmt.Errorf("add conversation member: %w", err)
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return 0, fmt.Errorf("commit conversation tx: %w", err)
	}

	return conversationID, nil
}

func (r *MessageRepository) Create(ctx context.Context, senderID, receiverID uint, message db.Message) (db.Message, error) {
	conversationID, err := r.EnsureDirectConversation(ctx, senderID, receiverID)
	if err != nil {
		return db.Message{}, err
	}

	msgType := message.MsgType
	if msgType == "" {
		msgType = domain.MessageTypeText
	}
	if !msgType.Valid() {
		msgType = domain.MessageTypeText
	}

	createdAt := message.CreatedAt
	if createdAt.IsZero() {
		createdAt = time.Now().UTC()
	}

	err = r.db.Pool.QueryRow(ctx, `
		INSERT INTO messages (conversation_id, sender_id, msg_type, content, song_id, delivery_status, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, 'sent', $6, $6)
		RETURNING id, conversation_id, sender_id, msg_type, content, song_id, delivery_status, created_at, updated_at
	`, conversationID, senderID, msgType, message.Content, message.SongID, createdAt).Scan(
		&message.ID,
		&message.ConversationID,
		&message.SenderID,
		&message.MsgType,
		&message.Content,
		&message.SongID,
		&message.DeliveryStatus,
		&message.CreatedAt,
		&message.UpdatedAt,
	)
	if err != nil {
		return db.Message{}, fmt.Errorf("create message: %w", err)
	}

	message.ReceiverID = int64(receiverID)
	return message, nil
}

func (r *MessageRepository) ListBetweenUsers(ctx context.Context, userID, withID uint, page, limit int) ([]db.Message, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 50
	}
	offset := (page - 1) * limit

	conversationID, err := r.FindDirectConversationID(ctx, userID, withID)
	if err != nil {
		return nil, 0, err
	}
	if conversationID == 0 {
		return []db.Message{}, 0, nil
	}

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM messages WHERE conversation_id = $1
	`, conversationID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count messages: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT
			m.id,
			m.conversation_id,
			m.sender_id,
			m.msg_type,
			m.content,
			m.song_id,
			m.delivery_status,
			m.created_at,
			m.updated_at,
			CASE
				WHEN m.sender_id = $2 THEN $3
				ELSE $2
			END AS receiver_id
		FROM messages AS m
		WHERE m.conversation_id = $1
		ORDER BY m.created_at ASC
		LIMIT $4 OFFSET $5
	`, conversationID, userID, withID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list messages: %w", err)
	}
	defer rows.Close()

	messages := make([]db.Message, 0)
	for rows.Next() {
		var message db.Message
		if err := rows.Scan(
			&message.ID,
			&message.ConversationID,
			&message.SenderID,
			&message.MsgType,
			&message.Content,
			&message.SongID,
			&message.DeliveryStatus,
			&message.CreatedAt,
			&message.UpdatedAt,
			&message.ReceiverID,
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
