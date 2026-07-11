package service

import (
	"context"
	"testing"

	"melonet-backend/internal/domain/db"
)

func TestChatServiceSendMessageRequiresTarget(t *testing.T) {
	svc := NewChatService(nil, nil)
	_, _, err := svc.SendMessage(context.Background(), 1, 0, 0, db.Message{Content: "hi"})
	if err == nil {
		t.Fatal("expected error when receiver and conversation missing")
	}
}

func TestChatServiceCreateConversationRejectsSelf(t *testing.T) {
	svc := NewChatService(nil, nil)
	_, err := svc.CreateConversation(context.Background(), 1, 1)
	if err == nil {
		t.Fatal("expected error for self conversation")
	}
}

func TestChatServiceHistoryRequiresUsers(t *testing.T) {
	svc := NewChatService(nil, nil)
	_, _, err := svc.History(context.Background(), 0, 2, 1, 20)
	if err == nil {
		t.Fatal("expected invalid user ids error")
	}
}
