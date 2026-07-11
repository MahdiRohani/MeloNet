package domain

import "testing"

func TestMessageDeliveryStatusValid(t *testing.T) {
	if !MessageStatusSent.Valid() || !MessageStatusDelivered.Valid() || !MessageStatusRead.Valid() {
		t.Fatal("expected valid delivery statuses")
	}
	if MessageDeliveryStatus("sending").Valid() {
		t.Fatal("client-side sending must not be a persisted status")
	}
}

func TestMessageContentTypeValid(t *testing.T) {
	if !MessageTypeText.Valid() || !MessageTypeSong.Valid() {
		t.Fatal("expected valid message content types")
	}
}
