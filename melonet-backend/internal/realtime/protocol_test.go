package realtime

import "testing"

func TestParseEnvelope(t *testing.T) {
	envelope, err := ParseEnvelope([]byte(`{"event":"ping"}`))
	if err != nil {
		t.Fatalf("ParseEnvelope error: %v", err)
	}
	if envelope.Event != EventPing {
		t.Fatalf("event = %q, want ping", envelope.Event)
	}
}

func TestDecodeMessageSend(t *testing.T) {
	envelope, err := NewEnvelope(EventMessageSend, WSMessageSend{
		ReceiverID: 2,
		Content:    "hello",
		MsgType:    "text",
		ClientID:   "tmp-1",
	})
	if err != nil {
		t.Fatalf("NewEnvelope error: %v", err)
	}

	payload, err := DecodeData[WSMessageSend](envelope)
	if err != nil {
		t.Fatalf("DecodeData error: %v", err)
	}
	if payload.ReceiverID != 2 || payload.Content != "hello" {
		t.Fatalf("unexpected payload: %+v", payload)
	}
}

func TestEventConstants(t *testing.T) {
	if EventSongShare != "song.share" {
		t.Fatal("unexpected song.share constant")
	}
	if EventMessageAck != "message.ack" {
		t.Fatal("unexpected message.ack constant")
	}
}
