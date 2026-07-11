package seed

import (
	_ "embed"
	"encoding/json"
	"fmt"
)

//go:embed catalog.json
var catalogJSON []byte

const (
	SourceOpenLoFi   = "open-lofi"
	SourceSoundHelix = "soundhelix"

	OpenLoFiZipURL = "https://github.com/btahir/open-lofi/releases/latest/download/openlofi.zip"
)

type Track struct {
	ID       int    `json:"id"`
	Title    string `json:"title"`
	Artist   string `json:"artist"`
	Category string `json:"category"`
	Source   string `json:"source"`
	ZipPath  string `json:"zip_path,omitempty"`
	URL      string `json:"url,omitempty"`
}

func LoadCatalog() ([]Track, error) {
	var tracks []Track
	if err := json.Unmarshal(catalogJSON, &tracks); err != nil {
		return nil, fmt.Errorf("parse catalog: %w", err)
	}
	if len(tracks) != 50 {
		return nil, fmt.Errorf("catalog must contain 50 tracks, got %d", len(tracks))
	}
	return tracks, nil
}

func (t Track) Slug() string {
	slug := slugify(t.Title)
	if slug == "" {
		return fmt.Sprintf("track-%d", t.ID)
	}
	return slug
}

func slugify(value string) string {
	var b []byte
	lastDash := false
	for i := 0; i < len(value); i++ {
		c := value[i]
		if (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') {
			b = append(b, c)
			lastDash = false
			continue
		}
		if c >= 'A' && c <= 'Z' {
			b = append(b, c+('a'-'A'))
			lastDash = false
			continue
		}
		if !lastDash && len(b) > 0 {
			b = append(b, '-')
			lastDash = true
		}
	}
	for len(b) > 0 && b[len(b)-1] == '-' {
		b = b[:len(b)-1]
	}
	return string(b)
}
