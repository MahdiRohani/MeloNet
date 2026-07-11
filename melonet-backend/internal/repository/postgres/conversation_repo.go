package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

type ConversationRepository struct {
	db *DB
}

func NewConversationRepository(db *DB) *ConversationRepository {
	return &ConversationRepository{db: db}
}

type ConversationSummary struct {
	ID          int64
	Type        domain.ConversationType
	UpdatedAt   time.Time
	OtherUser   db.UserSummary
	LastMessage *db.Message
	UnreadCount int
}

func (r *ConversationRepository) ListForUser(ctx context.Context, userID int64, page, limit int) ([]ConversationSummary, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM conversation_members AS cm
		WHERE cm.user_id = $1
	`, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count conversations: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		WITH user_conversations AS (
			SELECT cm.conversation_id
			FROM conversation_members AS cm
			WHERE cm.user_id = $1
		),
		last_messages AS (
			SELECT DISTINCT ON (m.conversation_id)
				m.conversation_id,
				m.id,
				m.sender_id,
				m.msg_type,
				m.content,
				m.song_id,
				m.delivery_status,
				m.created_at,
				m.updated_at
			FROM messages AS m
			INNER JOIN user_conversations AS uc ON uc.conversation_id = m.conversation_id
			ORDER BY m.conversation_id, m.created_at DESC, m.id DESC
		),
		unread_counts AS (
			SELECT
				m.conversation_id,
				COUNT(*)::int AS unread_count
			FROM messages AS m
			INNER JOIN user_conversations AS uc ON uc.conversation_id = m.conversation_id
			LEFT JOIN message_receipts AS mr
				ON mr.message_id = m.id AND mr.user_id = $1
			WHERE m.sender_id <> $1
			  AND COALESCE(mr.status, 'sent'::message_delivery_status) <> 'read'::message_delivery_status
			GROUP BY m.conversation_id
		)
		SELECT
			c.id,
			c.type,
			c.updated_at,
			ou.id,
			ou.username,
			ou.display_name,
			ou.avatar_url,
			ou.bio,
			ou.is_premium,
			lm.id,
			lm.sender_id,
			lm.msg_type,
			lm.content,
			lm.song_id,
			lm.delivery_status,
			lm.created_at,
			lm.updated_at,
			COALESCE(ucount.unread_count, 0)
		FROM conversations AS c
		INNER JOIN user_conversations AS uc ON uc.conversation_id = c.id
		INNER JOIN conversation_members AS cm_other
			ON cm_other.conversation_id = c.id AND cm_other.user_id <> $1
		INNER JOIN users AS ou ON ou.id = cm_other.user_id
		LEFT JOIN last_messages AS lm ON lm.conversation_id = c.id
		LEFT JOIN unread_counts AS ucount ON ucount.conversation_id = c.id
		ORDER BY COALESCE(lm.created_at, c.updated_at) DESC, c.id DESC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list conversations: %w", err)
	}
	defer rows.Close()

	summaries := make([]ConversationSummary, 0)
	for rows.Next() {
		summary, err := scanConversationSummary(rows.Scan)
		if err != nil {
			return nil, 0, err
		}
		summaries = append(summaries, summary)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate conversations: %w", err)
	}

	return summaries, total, nil
}

func (r *ConversationRepository) GetForUser(ctx context.Context, userID, conversationID int64) (ConversationSummary, error) {
	row := r.db.Pool.QueryRow(ctx, `
		WITH unread_counts AS (
			SELECT COUNT(*)::int AS unread_count
			FROM messages AS m
			LEFT JOIN message_receipts AS mr
				ON mr.message_id = m.id AND mr.user_id = $1
			WHERE m.conversation_id = $2
			  AND m.sender_id <> $1
			  AND COALESCE(mr.status, 'sent'::message_delivery_status) <> 'read'::message_delivery_status
		),
		last_message AS (
			SELECT
				m.id,
				m.conversation_id,
				m.sender_id,
				m.msg_type,
				m.content,
				m.song_id,
				m.delivery_status,
				m.created_at,
				m.updated_at
			FROM messages AS m
			WHERE m.conversation_id = $2
			ORDER BY m.created_at DESC, m.id DESC
			LIMIT 1
		)
		SELECT
			c.id,
			c.type,
			c.updated_at,
			ou.id,
			ou.username,
			ou.display_name,
			ou.avatar_url,
			ou.bio,
			ou.is_premium,
			lm.id,
			lm.sender_id,
			lm.msg_type,
			lm.content,
			lm.song_id,
			lm.delivery_status,
			lm.created_at,
			lm.updated_at,
			COALESCE((SELECT unread_count FROM unread_counts), 0)
		FROM conversations AS c
		INNER JOIN conversation_members AS cm_self
			ON cm_self.conversation_id = c.id AND cm_self.user_id = $1
		INNER JOIN conversation_members AS cm_other
			ON cm_other.conversation_id = c.id AND cm_other.user_id <> $1
		INNER JOIN users AS ou ON ou.id = cm_other.user_id
		LEFT JOIN last_message AS lm ON TRUE
		WHERE c.id = $2
	`, userID, conversationID)

	summary, err := scanConversationSummary(row.Scan)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ConversationSummary{}, ErrNotFound
		}
		return ConversationSummary{}, fmt.Errorf("get conversation: %w", err)
	}
	return summary, nil
}

func (r *ConversationRepository) CountUnread(ctx context.Context, userID int64) (int, error) {
	var count int
	err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)::int
		FROM messages AS m
		INNER JOIN conversation_members AS cm ON cm.conversation_id = m.conversation_id
		LEFT JOIN message_receipts AS mr
			ON mr.message_id = m.id AND mr.user_id = $1
		WHERE cm.user_id = $1
		  AND m.sender_id <> $1
		  AND COALESCE(mr.status, 'sent'::message_delivery_status) <> 'read'::message_delivery_status
	`, userID).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("count unread: %w", err)
	}
	return count, nil
}

func (r *ConversationRepository) IsMember(ctx context.Context, userID, conversationID int64) (bool, error) {
	var exists bool
	err := r.db.Pool.QueryRow(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM conversation_members
			WHERE conversation_id = $1 AND user_id = $2
		)
	`, conversationID, userID).Scan(&exists)
	if err != nil {
		return false, fmt.Errorf("check membership: %w", err)
	}
	return exists, nil
}

func (r *ConversationRepository) GetOtherMemberID(ctx context.Context, userID, conversationID int64) (int64, error) {
	var otherID int64
	err := r.db.Pool.QueryRow(ctx, `
		SELECT user_id
		FROM conversation_members
		WHERE conversation_id = $1 AND user_id <> $2
		LIMIT 1
	`, conversationID, userID).Scan(&otherID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return 0, ErrNotFound
		}
		return 0, fmt.Errorf("get other member: %w", err)
	}
	return otherID, nil
}

func (r *ConversationRepository) Touch(ctx context.Context, conversationID int64) error {
	_, err := r.db.Pool.Exec(ctx, `
		UPDATE conversations SET updated_at = NOW() WHERE id = $1
	`, conversationID)
	if err != nil {
		return fmt.Errorf("touch conversation: %w", err)
	}
	return nil
}

func scanConversationSummary(scan func(dest ...any) error) (ConversationSummary, error) {
	var summary ConversationSummary
	var (
		lastMessageID     *int64
		lastSenderID      *int64
		lastMsgType       *domain.MessageContentType
		lastContent       *string
		lastSongID        *int64
		lastDelivery      *domain.MessageDeliveryStatus
		lastCreatedAt     *time.Time
		lastUpdatedAt     *time.Time
	)

	if err := scan(
		&summary.ID,
		&summary.Type,
		&summary.UpdatedAt,
		&summary.OtherUser.ID,
		&summary.OtherUser.Username,
		&summary.OtherUser.DisplayName,
		&summary.OtherUser.AvatarURL,
		&summary.OtherUser.Bio,
		&summary.OtherUser.IsPremium,
		&lastMessageID,
		&lastSenderID,
		&lastMsgType,
		&lastContent,
		&lastSongID,
		&lastDelivery,
		&lastCreatedAt,
		&lastUpdatedAt,
		&summary.UnreadCount,
	); err != nil {
		return ConversationSummary{}, err
	}

	if lastMessageID != nil {
		summary.LastMessage = &db.Message{
			ID:               *lastMessageID,
			ConversationID:   summary.ID,
			SenderID:         *lastSenderID,
			MsgType:          *lastMsgType,
			Content:          *lastContent,
			SongID:           lastSongID,
			DeliveryStatus:   *lastDelivery,
			CreatedAt:        *lastCreatedAt,
			UpdatedAt:        *lastUpdatedAt,
		}
	}

	return summary, nil
}

func ConversationSummaryToAPI(summary ConversationSummary, viewerID int64) api.ConversationResponse {
	resp := api.ConversationResponse{
		ID:          uint(summary.ID),
		Type:        string(summary.Type),
		UnreadCount: summary.UnreadCount,
		UpdatedAt:   summary.UpdatedAt,
		OtherUser: &api.PublicUserResponse{
			ID:          uint(summary.OtherUser.ID),
			Username:    summary.OtherUser.Username,
			DisplayName: summary.OtherUser.DisplayName,
			AvatarURL:   summary.OtherUser.AvatarURL,
			Bio:         summary.OtherUser.Bio,
			IsPremium:   summary.OtherUser.IsPremium,
		},
	}
	if summary.LastMessage != nil {
		msg := MessageToAPI(*summary.LastMessage)
		resp.LastMessage = &msg
	}
	return resp
}

func ConversationSummariesToAPI(summaries []ConversationSummary, viewerID int64) []api.ConversationResponse {
	result := make([]api.ConversationResponse, 0, len(summaries))
	for _, summary := range summaries {
		result = append(result, ConversationSummaryToAPI(summary, viewerID))
	}
	return result
}
