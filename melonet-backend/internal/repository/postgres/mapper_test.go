package postgres

import (
	"testing"
	"time"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/db"
)

func TestSongToAPIUsesArtistNameAliases(t *testing.T) {
	song := db.Song{
		ID:          1,
		Title:       "Track",
		ArtistName:  "Sadegh",
		CoverURL:    "/covers/1.jpg",
		AudioURL:    "/audio/1.mp3",
		Category:    "Iranian",
		GenreName:   "Iranian",
		Lyrics:      "Lyrics",
		DurationSec: 200,
		PlayCount:   10,
	}

	resp := SongToAPI(song)

	if resp.Artist != "Sadegh" || resp.ArtistName != "Sadegh" {
		t.Fatalf("artist fields = %q / %q", resp.Artist, resp.ArtistName)
	}
	if resp.CoverURL != resp.CoverImageURL {
		t.Fatalf("cover aliases mismatch: %q vs %q", resp.CoverURL, resp.CoverImageURL)
	}
	if resp.Genre != "Iranian" {
		t.Fatalf("genre = %q", resp.Genre)
	}
}

func TestMessageToAPI(t *testing.T) {
	now := time.Now().UTC()
	message := db.Message{
		ID:             9,
		SenderID:       1,
		ReceiverID:     2,
		Content:        "hello",
		MsgType:        domain.MessageTypeText,
		DeliveryStatus: domain.MessageStatusSent,
		CreatedAt:      now,
	}

	resp := MessageToAPI(message)
	if resp.Status != "sent" || resp.MsgType != "text" {
		t.Fatalf("resp = %+v", resp)
	}
}
