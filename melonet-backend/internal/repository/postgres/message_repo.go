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

	_, err = r.db.Pool.Exec(ctx, `
		UPDATE conversations SET updated_at = NOW() WHERE id = $1
	`, conversationID)
	if err != nil {
		return db.Message{}, fmt.Errorf("touch conversation: %w", err)
	}

	_, err = r.db.Pool.Exec(ctx, `
		INSERT INTO message_receipts (message_id, user_id, status, updated_at)
		VALUES ($1, $2, 'sent', NOW())
		ON CONFLICT (message_id, user_id) DO NOTHING
	`, message.ID, receiverID)
	if err != nil {
		return db.Message{}, fmt.Errorf("create receipt: %w", err)
	}

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

func (r *MessageRepository) GetByID(ctx context.Context, messageID int64) (db.Message, error) {
	var message db.Message
	err := r.db.Pool.QueryRow(ctx, `
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
			(
				SELECT cm.user_id
				FROM conversation_members AS cm
				WHERE cm.conversation_id = m.conversation_id
				  AND cm.user_id <> m.sender_id
				LIMIT 1
			) AS receiver_id
		FROM messages AS m
		WHERE m.id = $1
	`, messageID).Scan(
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
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Message{}, ErrNotFound
		}
		return db.Message{}, fmt.Errorf("get message: %w", err)
	}
	return message, nil
}

func (r *MessageRepository) ListByConversation(ctx context.Context, userID, conversationID int64, page, limit int) ([]db.Message, int, error) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 50
	}
	offset := (page - 1) * limit

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
				WHEN m.sender_id = $2 THEN (
					SELECT cm.user_id
					FROM conversation_members AS cm
					WHERE cm.conversation_id = m.conversation_id
					  AND cm.user_id <> m.sender_id
					LIMIT 1
				)
				ELSE $2
			END AS receiver_id
		FROM messages AS m
		WHERE m.conversation_id = $1
		ORDER BY m.created_at ASC, m.id ASC
		LIMIT $3 OFFSET $4
	`, conversationID, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list conversation messages: %w", err)
	}
	defer rows.Close()

	messages, err := scanMessageRows(rows)
	return messages, total, err
}

func (r *MessageRepository) ListPendingForUser(ctx context.Context, userID int64, limit int) ([]db.Message, error) {
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
			$1::bigint AS receiver_id
		FROM messages AS m
		INNER JOIN conversation_members AS cm ON cm.conversation_id = m.conversation_id
		LEFT JOIN message_receipts AS mr
			ON mr.message_id = m.id AND mr.user_id = $1
		WHERE cm.user_id = $1
		  AND m.sender_id <> $1
		  AND COALESCE(mr.status, 'sent'::message_delivery_status) = 'sent'::message_delivery_status
		ORDER BY m.created_at ASC, m.id ASC
		LIMIT $2
	`, userID, limit)
	if err != nil {
		return nil, fmt.Errorf("list pending messages: %w", err)
	}
	defer rows.Close()

	messages, err := scanMessageRows(rows)
	if err != nil {
		return nil, err
	}
	return messages, nil
}

func (r *MessageRepository) UpsertReceipt(ctx context.Context, messageID, userID int64, status domain.MessageDeliveryStatus) error {
	_, err := r.db.Pool.Exec(ctx, `
		INSERT INTO message_receipts (message_id, user_id, status, updated_at)
		VALUES ($1, $2, $3, NOW())
		ON CONFLICT (message_id, user_id)
		DO UPDATE SET status = EXCLUDED.status, updated_at = NOW()
	`, messageID, userID, status)
	if err != nil {
		return fmt.Errorf("upsert receipt: %w", err)
	}
	return nil
}

func (r *MessageRepository) MarkConversationRead(ctx context.Context, userID, conversationID int64, messageIDs []int64) (int, error) {
	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return 0, fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	var rows pgx.Rows
	if len(messageIDs) > 0 {
		rows, err = tx.Query(ctx, `
			INSERT INTO message_receipts (message_id, user_id, status, updated_at)
			SELECT m.id, $2, 'read'::message_delivery_status, NOW()
			FROM messages AS m
			WHERE m.conversation_id = $1
			  AND m.id = ANY($3)
			  AND m.sender_id <> $2
			ON CONFLICT (message_id, user_id)
			DO UPDATE SET status = 'read'::message_delivery_status, updated_at = NOW()
			RETURNING message_id
		`, conversationID, userID, messageIDs)
	} else {
		rows, err = tx.Query(ctx, `
			INSERT INTO message_receipts (message_id, user_id, status, updated_at)
			SELECT m.id, $2, 'read'::message_delivery_status, NOW()
			FROM messages AS m
			WHERE m.conversation_id = $1
			  AND m.sender_id <> $2
			ON CONFLICT (message_id, user_id)
			DO UPDATE SET status = 'read'::message_delivery_status, updated_at = NOW()
			RETURNING message_id
		`, conversationID, userID)
	}
	if err != nil {
		return 0, fmt.Errorf("mark read: %w", err)
	}
	defer rows.Close()

	updated := 0
	for rows.Next() {
		updated++
	}
	if err := rows.Err(); err != nil {
		return 0, fmt.Errorf("iterate read receipts: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return 0, fmt.Errorf("commit read receipts: %w", err)
	}
	return updated, nil
}

func (r *MessageRepository) MarkDelivered(ctx context.Context, messageID, receiverID int64) error {
	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	_, err = tx.Exec(ctx, `
		INSERT INTO message_receipts (message_id, user_id, status, updated_at)
		VALUES ($1, $2, 'delivered'::message_delivery_status, NOW())
		ON CONFLICT (message_id, user_id)
		DO UPDATE SET status = 'delivered'::message_delivery_status, updated_at = NOW()
	`, messageID, receiverID)
	if err != nil {
		return fmt.Errorf("upsert delivered receipt: %w", err)
	}

	_, err = tx.Exec(ctx, `
		UPDATE messages
		SET delivery_status = CASE
			WHEN delivery_status = 'read'::message_delivery_status THEN delivery_status
			ELSE 'delivered'::message_delivery_status
		END,
		updated_at = NOW()
		WHERE id = $1
	`, messageID)
	if err != nil {
		return fmt.Errorf("update message delivered: %w", err)
	}

	return tx.Commit(ctx)
}

func scanMessageRows(rows pgx.Rows) ([]db.Message, error) {
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
			return nil, fmt.Errorf("scan message: %w", err)
		}
		messages = append(messages, message)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate messages: %w", err)
	}
	return messages, nil
}
