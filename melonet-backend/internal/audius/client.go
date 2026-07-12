package audius

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"
)

const (
	registryURL = "https://api.audius.co"
)

type Client struct {
	httpClient *http.Client
	streamHTTP *http.Client
	appName    string
	publicBase string
	host       string
	hostMu     sync.Mutex
}

func NewClient(appName, publicBase string) *Client {
	if appName == "" {
		appName = "MeloNet"
	}
	return &Client{
		httpClient: &http.Client{Timeout: 30 * time.Second},
		streamHTTP: &http.Client{Timeout: 0}, // no timeout for streaming proxy
		appName:    appName,
		publicBase: strings.TrimRight(publicBase, "/"),
	}
}

func (c *Client) StreamHTTPClient() *http.Client {
	return c.streamHTTP
}

func (c *Client) PublicBase() string {
	return c.publicBase
}

func (c *Client) resolveHost(ctx context.Context) (string, error) {
	c.hostMu.Lock()
	defer c.hostMu.Unlock()
	if c.host != "" {
		return c.host, nil
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, registryURL, nil)
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

type trackDTO struct {
	ID           string        `json:"id"`
	Title        string        `json:"title"`
	Duration     int           `json:"duration"`
	Genre        string        `json:"genre"`
	Mood         string        `json:"mood"`
	IsStreamable *bool         `json:"is_streamable"`
	Artwork      audiusArtwork `json:"artwork"`
	User         struct {
		Name string `json:"name"`
	} `json:"user"`
}

func (c *Client) GetTrack(ctx context.Context, id string) (Track, error) {
	tracks, err := c.GetBulkTracks(ctx, []string{id})
	if err != nil {
		return Track{}, err
	}
	if len(tracks) == 0 {
		return Track{}, fmt.Errorf("track not found")
	}
	return tracks[0], nil
}

func (c *Client) GetBulkTracks(ctx context.Context, ids []string) ([]Track, error) {
	if len(ids) == 0 {
		return nil, fmt.Errorf("no track ids provided")
	}
	host, err := c.resolveHost(ctx)
	if err != nil {
		return nil, err
	}

	params := url.Values{}
	params.Set("app_name", c.appName)
	for _, id := range ids {
		params.Add("id", id)
	}

	endpoint := fmt.Sprintf("%s/v1/tracks?%s", host, params.Encode())
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("get tracks: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("get tracks: status %d", resp.StatusCode)
	}

	var payload struct {
		Data []trackDTO `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return nil, fmt.Errorf("decode tracks: %w", err)
	}

	tracks := make([]Track, 0, len(payload.Data))
	for _, dto := range payload.Data {
		if track, ok := dtoToTrack(dto); ok {
			tracks = append(tracks, track)
		}
	}
	return tracks, nil
}

func (c *Client) SearchTracks(ctx context.Context, query string, limit int) ([]Track, error) {
	host, err := c.resolveHost(ctx)
	if err != nil {
		return nil, err
	}
	if limit <= 0 {
		limit = 20
	}

	params := url.Values{}
	params.Set("query", query)
	params.Set("app_name", c.appName)
	params.Set("limit", fmt.Sprintf("%d", limit))

	endpoint := fmt.Sprintf("%s/v1/tracks/search?%s", host, params.Encode())
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("search tracks: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("search tracks: status %d", resp.StatusCode)
	}

	var payload struct {
		Data []trackDTO `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return nil, fmt.Errorf("decode search: %w", err)
	}

	return dtoListToTracks(payload.Data, limit), nil
}

func (c *Client) Trending(ctx context.Context, genre, timeRange string, limit int) ([]Track, error) {
	host, err := c.resolveHost(ctx)
	if err != nil {
		return nil, err
	}
	if limit <= 0 {
		limit = 20
	}

	params := url.Values{}
	params.Set("app_name", c.appName)
	if genre != "" {
		params.Set("genre", genre)
	}
	if timeRange != "" {
		params.Set("time", timeRange)
	}

	endpoint := fmt.Sprintf("%s/v1/tracks/trending?%s", host, params.Encode())
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("trending tracks: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("trending tracks: status %d", resp.StatusCode)
	}

	var payload struct {
		Data []trackDTO `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return nil, fmt.Errorf("decode trending: %w", err)
	}

	return dtoListToTracks(payload.Data, limit), nil
}

func (c *Client) StreamURL(ctx context.Context, trackID string) (string, error) {
	host, err := c.resolveHost(ctx)
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("%s/v1/tracks/%s/stream?app_name=%s",
		host, trackID, url.QueryEscape(c.appName)), nil
}

func (c *Client) ArtworkURL(ctx context.Context, trackID string) (string, error) {
	track, err := c.GetTrack(ctx, trackID)
	if err != nil {
		return "", err
	}
	if track.CoverURL == "" {
		return "", fmt.Errorf("no artwork for track %s", trackID)
	}
	return track.CoverURL, nil
}

func (c *Client) ProxyGet(ctx context.Context, target, rangeHeader string) (*http.Response, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, target, nil)
	if err != nil {
		return nil, err
	}
	if rangeHeader != "" {
		req.Header.Set("Range", rangeHeader)
	}
	return c.streamHTTP.Do(req)
}

func dtoListToTracks(dtos []trackDTO, limit int) []Track {
	tracks := make([]Track, 0, limit)
	for _, dto := range dtos {
		if track, ok := dtoToTrack(dto); ok {
			tracks = append(tracks, track)
			if len(tracks) >= limit {
				break
			}
		}
	}
	return tracks
}

func dtoToTrack(dto trackDTO) (Track, bool) {
	if dto.IsStreamable != nil && !*dto.IsStreamable {
		return Track{}, false
	}
	if dto.ID == "" || dto.Duration <= 0 {
		return Track{}, false
	}
	return Track{
		ID:          dto.ID,
		Title:       strings.TrimSpace(dto.Title),
		Artist:      strings.TrimSpace(dto.User.Name),
		Genre:       strings.TrimSpace(dto.Genre),
		Mood:        strings.TrimSpace(dto.Mood),
		CoverURL:    firstNonEmpty(dto.Artwork.Large, dto.Artwork.Medium, dto.Artwork.Small),
		ThumbURL:    firstNonEmpty(dto.Artwork.Small, dto.Artwork.Medium, dto.Artwork.Large),
		DurationSec: dto.Duration,
	}, true
}

func firstNonEmpty(values ...string) string {
	for _, v := range values {
		if v != "" {
			return v
		}
	}
	return ""
}

func Paginate(tracks []Track, page, limit int) ([]Track, int) {
	if page < 1 {
		page = 1
	}
	if limit < 1 {
		limit = 20
	}
	total := len(tracks)
	start := (page - 1) * limit
	if start >= total {
		return []Track{}, total
	}
	end := start + limit
	if end > total {
		end = total
	}
	return tracks[start:end], total
}

func Drain(resp *http.Response) {
	if resp != nil && resp.Body != nil {
		io.Copy(io.Discard, resp.Body)
		resp.Body.Close()
	}
}
