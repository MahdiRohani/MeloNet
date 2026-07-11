package realtime

import (
	"context"
	"encoding/json"
	"testing"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
)

type mockChatPort struct {
	sent         []api.MessageResponse
	delivered    []int64
	readCalls    int
	otherMember  int64
	pending      []api.MessageResponse
}

func (m *mockChatPort) SendMessage(_ context.Context, senderID, conversationID, receiverID int64, input db.Message) (api.MessageResponse, uint, error) {
	msg := api.MessageResponse{
		ID:             42,
		ConversationID: 7,
		SenderID:       uint(senderID),
		ReceiverID:     uint(receiverID),
		Content:        input.Content,
		MsgType:        string(input.MsgType),
		Status:         string(domain.MessageStatusSent),
		CreatedAt:      time.Now().UTC(),
	}
	m.sent = append(m.sent, msg)
	return msg, uint(receiverID), nil
}

func (m *mockChatPort) MarkDelivered(_ context.Context, messageID, receiverID int64) error {
	m.delivered = append(m.delivered, messageID)
	return nil
}

func (m *mockChatPort) MarkRead(_ context.Context, _, _ int64, _ []int64) (int64, error) {
	m.readCalls++
	return 99, nil
}

func (m *mockChatPort) GetOtherMember(_ context.Context, _, _ int64) (int64, error) {
	return m.otherMember, nil
}

func (m *mockChatPort) ListPending(_ context.Context, _ int64, _ int) ([]api.MessageResponse, error) {
	return m.pending, nil
}

func TestHubHandleMessageSendAckAndDeliver(t *testing.T) {
	chat := &mockChatPort{otherMember: 2}
	hub := &Hub{chat: chat, presence: NewPresence(nil)}

	client := &clientConn{send: make(chan WSEnvelope, 4)}
	envelope, err := NewEnvelope(EventMessageSend, WSMessageSend{
		ReceiverID: 2,
		Content:    "hello",
		MsgType:    "text",
		ClientID:   "tmp-1",
	})
	if err != nil {
		t.Fatalf("NewEnvelope: %v", err)
	}

	if err := hub.handleEvent(context.Background(), 1, client, envelope); err != nil {
		t.Fatalf("handleEvent: %v", err)
	}

	if len(chat.sent) != 1 {
		t.Fatalf("sent messages = %d, want 1", len(chat.sent))
	}
	if len(chat.delivered) != 1 {
		t.Fatalf("delivered messages = %d, want 1", len(chat.delivered))
	}

	if len(client.send) < 1 {
		t.Fatal("expected ack on client channel")
	}
	first := <-client.send
	if first.Event != EventMessageAck {
		t.Fatalf("first event = %q, want message.ack", first.Event)
	}
}

func TestHubHandleMessageReadNotifiesSender(t *testing.T) {
	chat := &mockChatPort{otherMember: 2}
	hub := &Hub{chat: chat, presence: NewPresence(nil)}

	envelope, _ := NewEnvelope(EventMessageRead, WSMessageRead{
		ConversationID: 7,
		MessageIDs:     []uint{42},
	})

	if err := hub.handleEvent(context.Background(), 2, &clientConn{send: make(chan WSEnvelope, 1)}, envelope); err != nil {
		t.Fatalf("handleEvent: %v", err)
	}
	if chat.readCalls != 1 {
		t.Fatal("expected mark read call")
	}
}

func TestHubPingReturnsPong(t *testing.T) {
	hub := &Hub{presence: NewPresence(nil)}
	client := &clientConn{send: make(chan WSEnvelope, 1)}

	if err := hub.handleEvent(context.Background(), 1, client, WSEnvelope{Event: EventPing}); err != nil {
		t.Fatalf("handleEvent: %v", err)
	}

	select {
	case envelope := <-client.send:
		if envelope.Event != EventPong {
			t.Fatalf("event = %q, want pong", envelope.Event)
		}
	default:
		t.Fatal("expected pong response")
	}
}

func TestHubSongShareUsesSongType(t *testing.T) {
	chat := &mockChatPort{}
	hub := &Hub{chat: chat, presence: NewPresence(nil)}
	songID := uint(5)
	envelope, _ := NewEnvelope(EventSongShare, WSMessageSend{
		ReceiverID: 2,
		Content:    "check this",
		SongID:     &songID,
	})

	_ = hub.handleEvent(context.Background(), 1, &clientConn{send: make(chan WSEnvelope, 2)}, envelope)
	if len(chat.sent) != 1 || chat.sent[0].MsgType != string(domain.MessageTypeSong) {
		t.Fatalf("expected song message, got %+v", chat.sent)
	}
}

func TestHubDecodeReadEnvelope(t *testing.T) {
	raw, _ := json.Marshal(WSMessageRead{ConversationID: 3, MessageIDs: []uint{1, 2}})
	envelope := WSEnvelope{Event: EventMessageRead, Data: raw}
	payload, err := DecodeData[WSMessageRead](envelope)
	if err != nil || payload.ConversationID != 3 || len(payload.MessageIDs) != 2 {
		t.Fatalf("unexpected payload: %+v err=%v", payload, err)
	}
}
