package seed

import (
	"bytes"
	"testing"

	"github.com/tcolgate/mp3"
)

func TestSyntheticMP3IsDecodable(t *testing.T) {
	data := SyntheticMP3(30)
	if len(data) == 0 {
		t.Fatal("synthetic mp3 is empty")
	}

	decoder := mp3.NewDecoder(bytes.NewReader(data))
	var frame mp3.Frame
	var skipped int
	frames := 0
	for {
		if err := decoder.Decode(&frame, &skipped); err != nil {
			break
		}
		frames++
	}

	if frames == 0 {
		t.Fatal("expected the mp3 decoder to read at least one valid frame")
	}
}

func TestSyntheticDurationVariesByTrack(t *testing.T) {
	a := syntheticDurationForTrack(Track{ID: 1})
	b := syntheticDurationForTrack(Track{ID: 2})
	if a == b {
		t.Fatalf("expected different durations, got %d and %d", a, b)
	}
	if a < 60 {
		t.Fatalf("duration too short: %d", a)
	}
}
