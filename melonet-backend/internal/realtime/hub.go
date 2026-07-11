package realtime

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"sync"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/db"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	goredis "github.com/redis/go-redis/v9"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 45 * time.Second
	pingPeriod     = 20 * time.Second
	maxMessageSize = 64 * 1024
)

type Hub struct {
	logger   *slog.Logger
	chat     *service.ChatService
	presence *Presence
	clients  map[uint]*clientConn
	mu       sync.RWMutex
}

type clientConn struct {
	conn *websocket.Conn
	send chan WSEnvelope
}

func NewHub(logger *slog.Logger, chat *service.ChatService, presence *Presence) *Hub {
	return &Hub{
		logger:   logger,
		chat:     chat,
		presence: presence,
		clients:  make(map[uint]*clientConn),
	}
}

func (h *Hub) HandleConnection(c *gin.Context, userID uint) {
	upgrader := websocket.Upgrader{
		ReadBufferSize:  1024,
		WriteBufferSize: 1024,
		CheckOrigin: func(r *http.Request) bool {
			return true
		},
	}

	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		h.logger.Error("websocket upgrade failed", "user_id", userID, "error", err)
		return
	}

	client := &clientConn{
		conn: conn,
		send: make(chan WSEnvelope, 32),
	}

	h.register(userID, client)
	ctx := c.Request.Context()
	_ = h.presence.SetOnline(ctx, userID)
	h.flushPending(ctx, userID)

	pubsub := h.presence.Subscribe(ctx, userID)
	defer pubsub.Close()

	h.logger.Info("chat client connected", "user_id", userID)

	done := make(chan struct{})
	go h.writePump(userID, client, done)
	go h.redisPump(userID, client, pubsub, done)

	h.readPump(ctx, userID, client, conn)

	close(done)
	h.unregister(userID, client)
	_ = h.presence.SetOffline(context.Background(), userID)
	conn.Close()
	h.logger.Info("chat client disconnected", "user_id", userID)
}

func (h *Hub) register(userID uint, client *clientConn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if existing, ok := h.clients[userID]; ok {
		close(existing.send)
		_ = existing.conn.Close()
	}
	h.clients[userID] = client
}

func (h *Hub) unregister(userID uint, client *clientConn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	current, ok := h.clients[userID]
	if ok && current == client {
		delete(h.clients, userID)
		close(client.send)
	}
}

func (h *Hub) readPump(ctx context.Context, userID uint, client *clientConn, conn *websocket.Conn) {
	conn.SetReadLimit(maxMessageSize)
	_ = conn.SetReadDeadline(time.Now().Add(pongWait))
	conn.SetPongHandler(func(string) error {
		_ = conn.SetReadDeadline(time.Now().Add(pongWait))
		_ = h.presence.Refresh(ctx, userID)
		return nil
	})

	for {
		_, payload, err := conn.ReadMessage()
		if err != nil {
			h.logger.Debug("websocket read ended", "user_id", userID, "error", err)
			return
		}

		envelope, err := ParseEnvelope(payload)
		if err != nil {
			h.sendError(client, "invalid_event", "invalid websocket payload")
			continue
		}

		if err := h.handleEvent(ctx, userID, client, envelope); err != nil {
			h.logger.Error("websocket event failed", "user_id", userID, "event", envelope.Event, "error", err)
			h.sendError(client, "event_failed", err.Error())
		}
	}
}

func (h *Hub) writePump(userID uint, client *clientConn, done <-chan struct{}) {
	ticker := time.NewTicker(pingPeriod)
	defer ticker.Stop()

	for {
		select {
		case <-done:
			return
		case envelope, ok := <-client.send:
			_ = client.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				_ = client.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := client.conn.WriteJSON(envelope); err != nil {
				h.logger.Debug("websocket write failed", "user_id", userID, "error", err)
				return
			}
		case <-ticker.C:
			_ = client.conn.SetWriteDeadline(time.Now().Add(writeWait))
			pong, _ := NewEnvelope(EventPing, nil)
			if err := client.conn.WriteJSON(pong); err != nil {
				return
			}
			_ = h.presence.Refresh(context.Background(), userID)
		}
	}
}

func (h *Hub) redisPump(userID uint, client *clientConn, pubsub *goredis.PubSub, done <-chan struct{}) {
	ch := pubsub.Channel()
	for {
		select {
		case <-done:
			return
		case msg, ok := <-ch:
			if !ok {
				return
			}
			var envelope WSEnvelope
			if err := json.Unmarshal([]byte(msg.Payload), &envelope); err != nil {
				continue
			}
			h.enqueue(client, envelope)
		}
	}
}

func (h *Hub) handleEvent(ctx context.Context, userID uint, client *clientConn, envelope WSEnvelope) error {
	switch envelope.Event {
	case EventPing:
		h.enqueue(client, mustEnvelope(EventPong, nil))
		return nil
	case EventPong:
		_ = h.presence.Refresh(ctx, userID)
		return nil
	case EventMessageSend, EventSongShare:
		return h.handleMessageSend(ctx, userID, client, envelope)
	case EventMessageRead:
		return h.handleMessageRead(ctx, userID, envelope)
	case EventTypingStart, EventTypingStop:
		return h.handleTyping(ctx, userID, envelope)
	default:
		return fmt.Errorf("unsupported event %q", envelope.Event)
	}
}

func (h *Hub) handleMessageSend(ctx context.Context, userID uint, client *clientConn, envelope WSEnvelope) error {
	payload, err := DecodeData[WSMessageSend](envelope)
	if err != nil {
		return err
	}

	msgType := domain.MessageContentType(payload.MsgType)
	if envelope.Event == EventSongShare {
		msgType = domain.MessageTypeSong
	}
	if !msgType.Valid() {
		msgType = domain.MessageTypeText
	}

	input := db.Message{
		Content: payload.Content,
		MsgType: msgType,
	}
	if payload.SongID != nil {
		songID := int64(*payload.SongID)
		input.SongID = &songID
	}

	saved, receiverID, err := h.chat.SendMessage(ctx, int64(userID), int64(payload.ConversationID), int64(payload.ReceiverID), input)
	if err != nil {
		return err
	}

	ack, _ := NewEnvelope(EventMessageAck, WSMessageAck{
		ClientID:       payload.ClientID,
		MessageID:      saved.ID,
		ConversationID: saved.ConversationID,
		Status:         string(domain.MessageStatusSent),
	})
	h.enqueue(client, ack)

	newMsg, _ := NewEnvelope(EventMessageNew, saved)
	h.deliverToUser(ctx, receiverID, newMsg)

	if err := h.chat.MarkDelivered(ctx, int64(saved.ID), int64(receiverID)); err == nil {
		delivered, _ := NewEnvelope(EventMessageDelivered, WSMessageDelivered{
			MessageID:      saved.ID,
			ConversationID: saved.ConversationID,
		})
		h.deliverToUser(ctx, userID, delivered)
	}

	return nil
}

func (h *Hub) handleMessageRead(ctx context.Context, userID uint, envelope WSEnvelope) error {
	payload, err := DecodeData[WSMessageRead](envelope)
	if err != nil {
		return err
	}

	messageIDs := make([]int64, 0, len(payload.MessageIDs))
	for _, id := range payload.MessageIDs {
		messageIDs = append(messageIDs, int64(id))
	}

	senderID, err := h.chat.MarkRead(ctx, int64(userID), int64(payload.ConversationID), messageIDs)
	if err != nil {
		return err
	}
	if senderID == 0 {
		return nil
	}

	notice, _ := NewEnvelope(EventMessageRead, WSMessageReadNotice{
		ConversationID: payload.ConversationID,
		ReaderID:       userID,
		MessageIDs:     payload.MessageIDs,
	})
	h.deliverToUser(ctx, uint(senderID), notice)
	return nil
}

func (h *Hub) handleTyping(ctx context.Context, userID uint, envelope WSEnvelope) error {
	payload, err := DecodeData[WSTyping](envelope)
	if err != nil {
		return err
	}

	active := envelope.Event == EventTypingStart
	if err := h.presence.SetTyping(ctx, payload.ConversationID, userID, active); err != nil {
		return err
	}

	otherID, err := h.chat.GetOtherMember(ctx, int64(userID), int64(payload.ConversationID))
	if err != nil {
		return err
	}

	event := EventTypingStop
	if active {
		event = EventTypingStart
	}
	typing, _ := NewEnvelope(event, WSTyping{
		ConversationID: payload.ConversationID,
		UserID:         userID,
	})
	h.deliverToUser(ctx, uint(otherID), typing)
	return nil
}

func (h *Hub) flushPending(ctx context.Context, userID uint) {
	messages, err := h.chat.ListPending(ctx, int64(userID), 100)
	if err != nil {
		return
	}
	for _, message := range messages {
		envelope, _ := NewEnvelope(EventMessageNew, message)
		h.deliverToUser(ctx, userID, envelope)
		_ = h.chat.MarkDelivered(ctx, int64(message.ID), int64(userID))
	}
}

func (h *Hub) deliverToUser(ctx context.Context, userID uint, envelope WSEnvelope) {
	if h.deliverLocal(userID, envelope) {
		return
	}
	_ = h.presence.PublishToUser(ctx, userID, envelope)
}

func (h *Hub) deliverLocal(userID uint, envelope WSEnvelope) bool {
	h.mu.RLock()
	client, ok := h.clients[userID]
	h.mu.RUnlock()
	if !ok {
		return false
	}
	h.enqueue(client, envelope)
	return true
}

func (h *Hub) enqueue(client *clientConn, envelope WSEnvelope) {
	select {
	case client.send <- envelope:
	default:
	}
}

func (h *Hub) sendError(client *clientConn, code, message string) {
	envelope, _ := NewEnvelope(EventError, WSError{Code: code, Message: message})
	h.enqueue(client, envelope)
}

func mustEnvelope(event string, data any) WSEnvelope {
	envelope, err := NewEnvelope(event, data)
	if err != nil {
		return WSEnvelope{Event: EventError, Data: json.RawMessage(`{"code":"internal","message":"encode failed"}`)}
	}
	return envelope
}

func (h *Hub) Ping(_ context.Context) error {
	return nil
}
