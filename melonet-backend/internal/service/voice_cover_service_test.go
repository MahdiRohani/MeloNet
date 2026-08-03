package service

import (
	"errors"
	"testing"
	"time"

	"melonet-backend/internal/repository/postgres"
)

func TestVoiceCoverServiceToResponseReadyHasAudioURL(t *testing.T) {
	svc := &VoiceCoverService{
		mediaURL: func(key string) string {
			return "http://localhost:8080/api/media/" + key
		},
	}
	readyAt := time.Now().UTC()
	resp := svc.toResponse(postgres.VoiceCover{
		ID:               7,
		SourceSongID:     "track-1",
		TargetArtistID:   2,
		TargetArtistSlug: "ebi",
		TargetArtistName: "ابی",
		Status:           "ready",
		AudioObjectKey:   "voice-covers/7.mp3",
		CoverURL:         "https://cdn.example/cover.jpg",
		SourceTitle:      "Test Song",
		SourceArtist:     "Artist",
		CreatedAt:        readyAt,
		UpdatedAt:        readyAt,
		ReadyAt:          &readyAt,
	})
	if resp.AudioURL != "http://localhost:8080/api/media/voice-covers/7.mp3" {
		t.Fatalf("audio url = %q", resp.AudioURL)
	}
	if resp.Status != "ready" || resp.TargetArtistSlug != "ebi" {
		t.Fatalf("unexpected response: %+v", resp)
	}
}

func TestVoiceCoverServiceToResponsePendingOmitsAudioURL(t *testing.T) {
	svc := &VoiceCoverService{
		mediaURL: func(key string) string {
			return "http://localhost:8080/api/media/" + key
		},
	}
	resp := svc.toResponse(postgres.VoiceCover{
		ID:             1,
		Status:         "pending",
		AudioObjectKey: "",
	})
	if resp.AudioURL != "" {
		t.Fatalf("expected empty audio url, got %q", resp.AudioURL)
	}
}

func TestErrBadRequestWrapping(t *testing.T) {
	err := errors.New("bad request: artist is disabled")
	wrapped := errors.Join(ErrBadRequest, err)
	if !errors.Is(wrapped, ErrBadRequest) {
		t.Fatal("expected ErrBadRequest")
	}
}
