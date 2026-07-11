package handler

import (
	"errors"
	"net/http"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type ChatHandler struct {
	chat *service.ChatService
	hub  ChatHub
}

type ChatHub interface {
	HandleConnection(c *gin.Context, userID uint)
}

func NewChatHandler(chat *service.ChatService, hub ChatHub) *ChatHandler {
	return &ChatHandler{chat: chat, hub: hub}
}

func (h *ChatHandler) ListConversations(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	conversations, meta, err := h.chat.ListConversations(c.Request.Context(), int64(userID), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list conversations")
		return
	}
	response.OKWithMeta(c, conversations, meta)
}

func (h *ChatHandler) GetConversation(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	conversationID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid conversation id")
		return
	}

	conversation, err := h.chat.GetConversation(c.Request.Context(), int64(userID), conversationID)
	if err != nil {
		mapChatError(c, err)
		return
	}
	response.OK(c, conversation)
}

func (h *ChatHandler) CreateConversation(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	var req api.CreateConversationRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.UserID == 0 {
		response.BadRequest(c, "invalid_body", "user_id is required")
		return
	}

	conversation, err := h.chat.CreateConversation(c.Request.Context(), int64(userID), int64(req.UserID))
	if err != nil {
		mapChatMutationError(c, err)
		return
	}
	response.Created(c, conversation)
}

func (h *ChatHandler) ConversationMessages(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	conversationID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid conversation id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 50)
	messages, meta, err := h.chat.ConversationMessages(c.Request.Context(), int64(userID), conversationID, page, limit)
	if err != nil {
		mapChatError(c, err)
		return
	}
	response.OKWithMeta(c, messages, meta)
}

func (h *ChatHandler) UnreadCount(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	count, err := h.chat.UnreadCount(c.Request.Context(), int64(userID))
	if err != nil {
		response.InternalError(c, "failed to count unread messages")
		return
	}
	response.OK(c, api.UnreadCountResponse{Total: count})
}

func (h *ChatHandler) MarkRead(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	conversationID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid conversation id")
		return
	}

	var req api.MarkReadRequest
	_ = c.ShouldBindJSON(&req)

	messageIDs := make([]int64, 0, len(req.MessageIDs))
	for _, id := range req.MessageIDs {
		messageIDs = append(messageIDs, int64(id))
	}

	if _, err := h.chat.MarkRead(c.Request.Context(), int64(userID), conversationID, messageIDs); err != nil {
		mapChatError(c, err)
		return
	}
	response.OK(c, gin.H{"read": true})
}

func (h *ChatHandler) History(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	withID, err := parseUintQuery(c, "with_id")
	if err != nil {
		response.BadRequest(c, "invalid_with_id", "with_id is required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 50)
	messages, meta, err := h.chat.History(c.Request.Context(), userID, withID, page, limit)
	if err != nil {
		response.BadRequest(c, "invalid_request", err.Error())
		return
	}

	response.OKWithMeta(c, messages, meta)
}

func (h *ChatHandler) WebSocket(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	h.hub.HandleConnection(c, userID)
}

func mapChatError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	if errors.Is(err, service.ErrForbidden) {
		response.Error(c, http.StatusForbidden, "forbidden", "access denied")
		return
	}
	response.InternalError(c, "chat request failed")
}

func mapChatMutationError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	if err != nil && err.Error() == "cannot create conversation with yourself" {
		response.BadRequest(c, "invalid_request", err.Error())
		return
	}
	response.InternalError(c, "chat request failed")
}
