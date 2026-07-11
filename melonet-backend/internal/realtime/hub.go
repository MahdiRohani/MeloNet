package realtime

import (
	"context"
	"log/slog"
	"net/http"
	"sync"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

type Hub struct {
	logger  *slog.Logger
	chat    *service.ChatService
	clients map[uint]*websocket.Conn
	mu      sync.RWMutex
}

type WSMessage struct {
	SenderID   uint   `json:"sender_id"`
	ReceiverID uint   `json:"receiver_id"`
	Content    string `json:"content"`
	MsgType    string `json:"msg_type"`
}

func NewHub(logger *slog.Logger, chat *service.ChatService) *Hub {
	return &Hub{
		logger:  logger,
		chat:    chat,
		clients: make(map[uint]*websocket.Conn),
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

	h.register(userID, conn)
	h.logger.Info("chat client connected", "user_id", userID)

	defer func() {
		h.unregister(userID, conn)
		conn.Close()
		h.logger.Info("chat client disconnected", "user_id", userID)
	}()

	for {
		var wsMsg WSMessage
		if err := conn.ReadJSON(&wsMsg); err != nil {
			h.logger.Debug("websocket read ended", "user_id", userID, "error", err)
			break
		}

		msgType := domain.MessageContentType(wsMsg.MsgType)
		if !msgType.Valid() {
			msgType = domain.MessageTypeText
		}

		saved, err := h.chat.SaveMessage(c.Request.Context(), userID, wsMsg.ReceiverID, db.Message{
			Content:   wsMsg.Content,
			MsgType:   msgType,
			CreatedAt: time.Now().UTC(),
		})
		if err != nil {
			h.logger.Error("failed to persist chat message", "user_id", userID, "error", err)
			continue
		}

		h.deliver(saved)
	}
}

func (h *Hub) register(userID uint, conn *websocket.Conn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.clients[userID] = conn
}

func (h *Hub) unregister(userID uint, conn *websocket.Conn) {
	h.mu.Lock()
	defer h.mu.Unlock()

	current, ok := h.clients[userID]
	if ok && current == conn {
		delete(h.clients, userID)
	}
}

func (h *Hub) deliver(message api.MessageResponse) {
	h.mu.RLock()
	receiverConn, ok := h.clients[message.ReceiverID]
	h.mu.RUnlock()

	if !ok {
		return
	}

	if err := receiverConn.WriteJSON(message); err != nil {
		h.logger.Error("failed to deliver chat message",
			"receiver_id", message.ReceiverID,
			"error", err,
		)
		receiverConn.Close()
		h.unregister(message.ReceiverID, receiverConn)
	}
}

// Ping exists for future health/presence integration.
func (h *Hub) Ping(_ context.Context) error {
	return nil
}
