package seed

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

// Audius is a free, legal, decentralized music network that exposes a public
// REST API (no API key required for reads) capable of streaming full tracks.
// We use it to seed the catalog with real, audible music instead of the silent
// synthetic fallback. See https://docs.audius.co/developers.
const (
	audiusRegistryURL = "https://api.audius.co"
	audiusAppName     = "MeloNet"
)

// AudiusTrack is the subset of Audius track metadata we care about for seeding.
type AudiusTrack struct {
	ID          string
	Title       string
	Artist      string
	Genre       string
	CoverURL    string // 1000x1000 artwork
	ThumbURL    string // 150x150 artwork
	DurationSec int
}

type AudiusClient struct {
	httpClient *http.Client
	appName    string
	host       string
	logger     Logger
}

func NewAudiusClient(logger Logger) *AudiusClient {
	return &AudiusClient{
		httpClient: &http.Client{Timeout: 90 * time.Second},
		appName:    audiusAppName,
		logger:     logger,
	}
}

// resolveHost picks a healthy Audius discovery node. The registry endpoint
// returns a list of nodes; the first entry is used and cached.
func (c *AudiusClient) resolveHost(ctx context.Context) (string, error) {
	if c.host != "" {
		return c.host, nil
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, audiusRegistryURL, nil)
	if err != nil {
		return "", err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("resolve audius host: %w", err)
	}
	defer resp.Body.Close()

	var payload struct {
		Data []string `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return "", fmt.Errorf("decode audius registry: %w", err)
	}
	if len(payload.Data) == 0 {
		return "", fmt.Errorf("no audius discovery nodes available")
	}
	c.host = strings.TrimRight(payload.Data[0], "/")
	return c.host, nil
}

type audiusArtwork struct {
	Small  string `json:"150x150"`
	Medium string `json:"480x480"`
	Large  string `json:"1000x1000"`
}

type audiusTrackDTO struct {
	ID           string        `json:"id"`
	Title        string        `json:"title"`
	Duration     int           `json:"duration"`
	Genre        string        `json:"genre"`
	IsStreamable *bool         `json:"is_streamable"`
	Artwork      audiusArtwork `json:"artwork"`
	User         struct {
		Name string `json:"name"`
	} `json:"user"`
}

// FetchTrending returns up to limit streamable trending tracks.
func (c *AudiusClient) FetchTrending(ctx context.Context, limit int) ([]AudiusTrack, error) {
	host, err := c.resolveHost(ctx)
	if err != nil {
		return nil, err
	}

	endpoint := fmt.Sprintf("%s/v1/tracks/trending?app_name=%s", host, url.QueryEscape(c.appName))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("fetch trending: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("fetch trending: status %d", resp.StatusCode)
	}

	var payload struct {
		Data []audiusTrackDTO `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return nil, fmt.Errorf("decode trending: %w", err)
	}

	tracks := make([]AudiusTrack, 0, limit)
	for _, dto := range payload.Data {
		if dto.IsStreamable != nil && !*dto.IsStreamable {
			continue
		}
		if dto.ID == "" || dto.Duration <= 0 {
			continue
		}
		tracks = append(tracks, AudiusTrack{
			ID:          dto.ID,
			Title:       strings.TrimSpace(dto.Title),
			Artist:      strings.TrimSpace(dto.User.Name),
			Genre:       strings.TrimSpace(dto.Genre),
			CoverURL:    firstNonEmpty(dto.Artwork.Large, dto.Artwork.Medium, dto.Artwork.Small),
			ThumbURL:    firstNonEmpty(dto.Artwork.Small, dto.Artwork.Medium, dto.Artwork.Large),
			DurationSec: dto.Duration,
		})
		if len(tracks) >= limit {
			break
		}
	}

	if len(tracks) == 0 {
		return nil, fmt.Errorf("no streamable trending tracks returned")
	}
	return tracks, nil
}

// StreamURL builds the full-track streaming URL for a track id.
func (c *AudiusClient) StreamURL(id string) string {
	return fmt.Sprintf("%s/v1/tracks/%s/stream?app_name=%s", c.host, id, url.QueryEscape(c.appName))
}

const audiusDownloadAttempts = 4

// Download fetches raw bytes from an Audius URL, following redirects. Content
// nodes are decentralized and occasionally flaky (EOF / 502), so we retry a
// few times; the stream endpoint may resolve to a different node each attempt.
func (c *AudiusClient) Download(ctx context.Context, target string) ([]byte, error) {
	if target == "" {
		return nil, fmt.Errorf("empty download url")
	}

	var lastErr error
	for attempt := 1; attempt <= audiusDownloadAttempts; attempt++ {
		data, err := c.downloadOnce(ctx, target)
		if err == nil {
			return data, nil
		}
		lastErr = err
		if c.logger != nil && attempt < audiusDownloadAttempts {
			c.logger.Warn("audius download attempt failed, retrying",
				"attempt", attempt, "error", err.Error())
		}
		select {
		case <-ctx.Done():
			return nil, ctx.Err()
		case <-time.After(time.Duration(attempt) * time.Second):
		}
	}
	return nil, lastErr
}

func (c *AudiusClient) downloadOnce(ctx context.Context, target string) ([]byte, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, target, nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("download %s: %w", target, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("download %s: status %d", target, resp.StatusCode)
	}
	data, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("read %s: %w", target, err)
	}
	if len(data) == 0 {
		return nil, fmt.Errorf("download %s: empty body", target)
	}
	return data, nil
}

func firstNonEmpty(values ...string) string {
	for _, v := range values {
		if v != "" {
			return v
		}
	}
	return ""
}
