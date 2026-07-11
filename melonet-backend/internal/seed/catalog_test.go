package seed

import "testing"

func TestLoadCatalog(t *testing.T) {
	tracks, err := LoadCatalog()
	if err != nil {
		t.Fatalf("LoadCatalog error: %v", err)
	}
	if len(tracks) != 50 {
		t.Fatalf("track count = %d, want 50", len(tracks))
	}

	seen := make(map[int]struct{}, len(tracks))
	for _, track := range tracks {
		if track.ID <= 0 {
			t.Fatalf("invalid track id: %d", track.ID)
		}
		if _, ok := seen[track.ID]; ok {
			t.Fatalf("duplicate track id: %d", track.ID)
		}
		seen[track.ID] = struct{}{}

		if track.Title == "" || track.Artist == "" || track.Category == "" {
			t.Fatalf("track %d missing metadata", track.ID)
		}

		switch track.Source {
		case SourceOpenLoFi:
			if track.ZipPath == "" {
				t.Fatalf("track %d missing zip_path", track.ID)
			}
		case SourceSoundHelix:
			if track.URL == "" {
				t.Fatalf("track %d missing url", track.ID)
			}
		default:
			t.Fatalf("track %d has unknown source %q", track.ID, track.Source)
		}
	}
}

func TestTrackSlug(t *testing.T) {
	track := Track{ID: 7, Title: "Sunset Offbeat!"}
	if got := track.Slug(); got != "sunset-offbeat" {
		t.Fatalf("slug = %q, want sunset-offbeat", got)
	}
}

func TestGenerateCover(t *testing.T) {
	data, err := GenerateCover("Test Song", "Test Artist", 3)
	if err != nil {
		t.Fatalf("GenerateCover error: %v", err)
	}
	if len(data) < 100 {
		t.Fatalf("cover too small: %d bytes", len(data))
	}
}
