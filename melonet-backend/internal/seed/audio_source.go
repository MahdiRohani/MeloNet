package seed

import (
	"archive/zip"
	"bytes"
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"time"
)

// AudioMode controls how track audio is obtained.
type AudioMode string

const (
	// AudioModeDownload only uses the real remote sources and fails on error.
	AudioModeDownload AudioMode = "download"
	// AudioModeSynthetic never touches the network and always generates audio.
	AudioModeSynthetic AudioMode = "synthetic"
	// AudioModeAuto tries the real source first and falls back to synthetic audio.
	AudioModeAuto AudioMode = "auto"
	// AudioModeAudius seeds real, streamable tracks from the Audius network and
	// re-hosts audio + artwork in local object storage.
	AudioModeAudius AudioMode = "audius"
)

func ParseAudioMode(value string) AudioMode {
	switch AudioMode(value) {
	case AudioModeDownload:
		return AudioModeDownload
	case AudioModeSynthetic:
		return AudioModeSynthetic
	case AudioModeAudius:
		return AudioModeAudius
	default:
		return AudioModeAuto
	}
}

type AudioSource struct {
	httpClient *http.Client
	cacheDir   string
	localDir   string
	zipPath    string
	mode       AudioMode
	logger     Logger

	zipUnavailable bool
}

// Logger is the minimal logging surface the audio source needs.
type Logger interface {
	Warn(msg string, args ...any)
}

func NewAudioSource(cacheDir string, mode AudioMode, logger Logger) *AudioSource {
	return &AudioSource{
		httpClient: &http.Client{Timeout: 8 * time.Minute},
		cacheDir:   cacheDir,
		localDir:   filepath.Join(filepath.Dir(cacheDir), "audio"),
		zipPath:    filepath.Join(cacheDir, "openlofi.zip"),
		mode:       mode,
		logger:     logger,
	}
}

// fetchLocal returns bundled real audio for a track when a matching file
// exists under the local audio directory (data/audio/song{ID}.mp3). These
// files contain actual sound, unlike the silent synthetic fallback.
func (s *AudioSource) fetchLocal(track Track) ([]byte, bool) {
	if s.localDir == "" {
		return nil, false
	}
	path := filepath.Join(s.localDir, fmt.Sprintf("song%d.mp3", track.ID))
	data, err := os.ReadFile(path)
	if err != nil || len(data) == 0 {
		return nil, false
	}
	return data, true
}

func (s *AudioSource) Fetch(ctx context.Context, track Track) ([]byte, error) {
	// Prefer real bundled audio when available so playback has actual sound,
	// regardless of network reachability or mode.
	if data, ok := s.fetchLocal(track); ok {
		return data, nil
	}

	if s.mode == AudioModeSynthetic {
		return SyntheticMP3(syntheticDurationForTrack(track)), nil
	}

	data, err := s.fetchRemote(ctx, track)
	if err == nil {
		return data, nil
	}

	if s.mode == AudioModeAuto {
		if s.logger != nil {
			s.logger.Warn("real audio unavailable, using synthetic fallback",
				"track_id", track.ID, "title", track.Title, "error", err.Error())
		}
		return SyntheticMP3(syntheticDurationForTrack(track)), nil
	}

	return nil, err
}

func (s *AudioSource) fetchRemote(ctx context.Context, track Track) ([]byte, error) {
	switch track.Source {
	case SourceSoundHelix:
		if track.URL == "" {
			return nil, fmt.Errorf("track %d missing url", track.ID)
		}
		return s.downloadURL(ctx, track.URL)
	case SourceOpenLoFi:
		if track.ZipPath == "" {
			return nil, fmt.Errorf("track %d missing zip_path", track.ID)
		}
		if s.zipUnavailable {
			return nil, fmt.Errorf("open-lofi archive previously unavailable")
		}
		return s.fetchFromZip(ctx, track.ZipPath)
	default:
		return nil, fmt.Errorf("unsupported source %q", track.Source)
	}
}

const (
	trackDownloadTimeout = 45 * time.Second
	zipDownloadTimeout   = 2 * time.Minute
)

func (s *AudioSource) downloadURL(ctx context.Context, url string) ([]byte, error) {
	return s.downloadURLWithTimeout(ctx, url, trackDownloadTimeout)
}

func (s *AudioSource) downloadURLWithTimeout(ctx context.Context, url string, timeout time.Duration) ([]byte, error) {
	ctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("download %s: %w", url, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("download %s: status %d", url, resp.StatusCode)
	}

	data, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("read body: %w", err)
	}
	if len(data) == 0 {
		return nil, fmt.Errorf("download %s: empty body", url)
	}
	return data, nil
}

func (s *AudioSource) fetchFromZip(ctx context.Context, zipEntry string) ([]byte, error) {
	if err := s.ensureZip(ctx); err != nil {
		return nil, err
	}

	reader, err := zip.OpenReader(s.zipPath)
	if err != nil {
		return nil, fmt.Errorf("open zip: %w", err)
	}
	defer reader.Close()

	for _, file := range reader.File {
		if file.Name != zipEntry {
			continue
		}
		rc, err := file.Open()
		if err != nil {
			return nil, fmt.Errorf("open zip entry: %w", err)
		}
		defer rc.Close()

		data, err := io.ReadAll(rc)
		if err != nil {
			return nil, fmt.Errorf("read zip entry: %w", err)
		}
		if len(data) == 0 {
			return nil, fmt.Errorf("zip entry %q is empty", zipEntry)
		}
		return data, nil
	}

	return nil, fmt.Errorf("zip entry %q not found", zipEntry)
}

func (s *AudioSource) ensureZip(ctx context.Context) error {
	if info, err := os.Stat(s.zipPath); err == nil && info.Size() > 0 {
		return nil
	}

	if err := os.MkdirAll(s.cacheDir, 0o755); err != nil {
		return fmt.Errorf("create cache dir: %w", err)
	}

	data, err := s.downloadURLWithTimeout(ctx, OpenLoFiZipURL, zipDownloadTimeout)
	if err != nil {
		// Remember the failure so subsequent open-lofi tracks fall back
		// immediately instead of re-attempting the slow download each time.
		s.zipUnavailable = true
		return fmt.Errorf("download open-lofi archive: %w", err)
	}

	tmpPath := s.zipPath + ".tmp"
	if err := os.WriteFile(tmpPath, data, 0o644); err != nil {
		return fmt.Errorf("write zip temp: %w", err)
	}
	if err := os.Rename(tmpPath, s.zipPath); err != nil {
		return fmt.Errorf("rename zip: %w", err)
	}

	if _, err := zip.NewReader(bytes.NewReader(data), int64(len(data))); err != nil {
		return fmt.Errorf("validate zip: %w", err)
	}

	return nil
}
