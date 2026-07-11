package postgres

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

type NotificationRepository struct {
	db *DB
}

func NewNotificationRepository(db *DB) *NotificationRepository {
	return &NotificationRepository{db: db}
}

func (r *NotificationRepository) Create(ctx context.Context, userID int64, notifType domain.NotificationType, title, body string, payload map[string]any) (db.Notification, error) {
	payloadJSON, err := json.Marshal(payload)
	if err != nil {
		return db.Notification{}, fmt.Errorf("marshal payload: %w", err)
	}

	var notification db.Notification
	err = r.db.Pool.QueryRow(ctx, `
		INSERT INTO notifications (user_id, type, title, body, payload)
		VALUES ($1, $2::notification_type, $3, $4, $5::jsonb)
		RETURNING id, user_id, type, title, body, payload, read_at, created_at
	`, userID, string(notifType), title, body, payloadJSON).Scan(
		&notification.ID,
		&notification.UserID,
		&notification.Type,
		&notification.Title,
		&notification.Body,
		&notification.Payload,
		&notification.ReadAt,
		&notification.CreatedAt,
	)
	if err != nil {
		return db.Notification{}, fmt.Errorf("create notification: %w", err)
	}
	return notification, nil
}

func (r *NotificationRepository) List(ctx context.Context, userID int64, page, limit int, unreadOnly bool) ([]db.Notification, int, error) {
	offset := (page - 1) * limit
	filter := ""
	if unreadOnly {
		filter = " AND read_at IS NULL"
	}

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM notifications
		WHERE user_id = $1`+filter, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count notifications: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT id, user_id, type, title, body, payload, read_at, created_at
		FROM notifications
		WHERE user_id = $1`+filter+`
		ORDER BY created_at DESC, id DESC
		LIMIT $2 OFFSET $3
	`, userID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list notifications: %w", err)
	}
	defer rows.Close()

	notifications := make([]db.Notification, 0)
	for rows.Next() {
		var notification db.Notification
		if err := rows.Scan(
			&notification.ID,
			&notification.UserID,
			&notification.Type,
			&notification.Title,
			&notification.Body,
			&notification.Payload,
			&notification.ReadAt,
			&notification.CreatedAt,
		); err != nil {
			return nil, 0, fmt.Errorf("scan notification: %w", err)
		}
		notifications = append(notifications, notification)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("iterate notifications: %w", err)
	}

	return notifications, total, nil
}

func (r *NotificationRepository) MarkRead(ctx context.Context, userID, notificationID int64) error {
	tag, err := r.db.Pool.Exec(ctx, `
		UPDATE notifications
		SET read_at = NOW()
		WHERE id = $1 AND user_id = $2 AND read_at IS NULL
	`, notificationID, userID)
	if err != nil {
		return fmt.Errorf("mark notification read: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}

func (r *NotificationRepository) MarkAllRead(ctx context.Context, userID int64) (int, error) {
	tag, err := r.db.Pool.Exec(ctx, `
		UPDATE notifications
		SET read_at = NOW()
		WHERE user_id = $1 AND read_at IS NULL
	`, userID)
	if err != nil {
		return 0, fmt.Errorf("mark all notifications read: %w", err)
	}
	return int(tag.RowsAffected()), nil
}

func (r *NotificationRepository) GetByID(ctx context.Context, userID, notificationID int64) (db.Notification, error) {
	var notification db.Notification
	err := r.db.Pool.QueryRow(ctx, `
		SELECT id, user_id, type, title, body, payload, read_at, created_at
		FROM notifications
		WHERE id = $1 AND user_id = $2
	`, notificationID, userID).Scan(
		&notification.ID,
		&notification.UserID,
		&notification.Type,
		&notification.Title,
		&notification.Body,
		&notification.Payload,
		&notification.ReadAt,
		&notification.CreatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Notification{}, ErrNotFound
		}
		return db.Notification{}, fmt.Errorf("get notification: %w", err)
	}
	return notification, nil
}
