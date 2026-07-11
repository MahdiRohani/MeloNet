package realtime

import (
	"encoding/json"
	"fmt"
)

const (
	EventPing            = "ping"
	EventPong            = "pong"
	EventMessageSend     = "message.send"
	EventMessageNew      = "message.new"
	EventMessageAck      = "message.ack"
	EventMessageDelivered = "message.delivered"
	EventMessageRead     = "message.read"
	EventTypingStart     = "typing.start"
	EventTypingStop      = "typing.stop"
	EventSongShare       = "song.share"
	EventError           = "error"
)

type WSEnvelope struct {
	Event string          `json:"event"`
	Data  json.RawMessage `json:"data,omitempty"`
}

type WSMessageSend struct {
	ConversationID uint   `json:"conversation_id,omitempty"`
	ReceiverID     uint   `json:"receiver_id,omitempty"`
	Content        string `json:"content"`
	MsgType        string `json:"msg_type"`
	SongID         *uint  `json:"song_id,omitempty"`
	ClientID       string `json:"client_id,omitempty"`
}

type WSMessageAck struct {
	ClientID       string `json:"client_id,omitempty"`
	MessageID      uint   `json:"message_id"`
	ConversationID uint   `json:"conversation_id"`
	Status         string `json:"status"`
}

type WSMessageDelivered struct {
	MessageID      uint `json:"message_id"`
	ConversationID uint `json:"conversation_id"`
}

type WSMessageRead struct {
	ConversationID uint   `json:"conversation_id"`
	MessageIDs     []uint `json:"message_ids,omitempty"`
}

type WSMessageReadNotice struct {
	ConversationID uint   `json:"conversation_id"`
	ReaderID       uint   `json:"reader_id"`
	MessageIDs     []uint `json:"message_ids,omitempty"`
}

type WSTyping struct {
	ConversationID uint `json:"conversation_id"`
	UserID         uint `json:"user_id,omitempty"`
}

type WSError struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func NewEnvelope(event string, data any) (WSEnvelope, error) {
	if data == nil {
		return WSEnvelope{Event: event}, nil
	}
	raw, err := json.Marshal(data)
	if err != nil {
		return WSEnvelope{}, err
	}
	return WSEnvelope{Event: event, Data: raw}, nil
}

func ParseEnvelope(raw []byte) (WSEnvelope, error) {
	var envelope WSEnvelope
	if err := json.Unmarshal(raw, &envelope); err != nil {
		return WSEnvelope{}, err
	}
	if envelope.Event == "" {
		return WSEnvelope{}, fmt.Errorf("missing event")
	}
	return envelope, nil
}

func DecodeData[T any](envelope WSEnvelope) (T, error) {
	var data T
	if len(envelope.Data) == 0 {
		return data, nil
	}
	err := json.Unmarshal(envelope.Data, &data)
	return data, err
}
