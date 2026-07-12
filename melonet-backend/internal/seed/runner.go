package seed

import (
	"bytes"
	"context"
	"fmt"
	"log/slog"
	"time"

	"github.com/tcolgate/mp3"

	"melonet-backend/internal/repository/postgres"
	minioStorage "melonet-backend/internal/storage/minio"
)

type Runner struct {
	logger  *slog.Logger
	repo    *postgres.SeedRepository
	storage *minioStorage.MediaStorage
	audio   *AudioSource
	audius  *AudiusClient
	mode    AudioMode
	force   bool

	audiusTracks []AudiusTrack
}

type Options struct {
	Force     bool
	AudioMode AudioMode
}

func NewRunner(
	logger *slog.Logger,
	repo *postgres.SeedRepository,
	storage *minioStorage.MediaStorage,
	cacheDir string,
	opts Options,
) *Runner {
	mode := opts.AudioMode
	if mode == "" {
		mode = AudioModeAuto
	}
	r := &Runner{
		logger:  logger,
		repo:    repo,
		storage: storage,
		audio:   NewAudioSource(cacheDir, mode, logger),
		mode:    mode,
		force:   opts.Force,
	}
	if mode == AudioModeAudius {
		r.audius = NewAudiusClient(logger)
	}
	return r
}

func (r *Runner) Run(ctx context.Context) error {
	tracks, err := LoadCatalog()
	if err != nil {
		return err
	}

	if r.mode == AudioModeAudius && r.audius != nil {
		audiusTracks, err := r.audius.FetchTrending(ctx, len(tracks))
		if err != nil {
			return fmt.Errorf("fetch audius trending: %w", err)
		}
		r.audiusTracks = audiusTracks
		r.logger.Info("fetched audius trending tracks", "count", len(audiusTracks))
	}

	r.logger.Info("starting media seed", "tracks", len(tracks), "force", r.force, "mode", string(r.mode))

	for _, track := range tracks {
		if err := r.seedTrack(ctx, track); err != nil {
			return fmt.Errorf("seed track %d (%s): %w", track.ID, track.Title, err)
		}
	}

	if err := r.repo.RefreshAlbumCovers(ctx); err != nil {
		return fmt.Errorf("refresh album covers: %w", err)
	}

	r.logger.Info("media seed completed", "tracks", len(tracks))
	return nil
}

func (r *Runner) seedTrack(ctx context.Context, track Track) error {
	existing, err := r.repo.GetSongMediaState(ctx, int64(track.ID))
	if err != nil {
		return err
	}

	if !r.force && existing.Seeded {
		r.logger.Info("skip already seeded track", "id", track.ID, "title", track.Title)
		return nil
	}

	// Effective metadata defaults to the static catalog and may be overridden
	// by a real Audius track. Category is always kept from the catalog so the
	// home feed sections stay balanced.
	title := track.Title
	artist := track.Artist

	var (
		audioData []byte
		coverData []byte
		thumbData []byte
	)

	if at, ok := r.audiusForTrack(track); ok {
		if at.Title != "" {
			title = at.Title
		}
		if at.Artist != "" {
			artist = at.Artist
		}
		data, derr := r.audius.Download(ctx, r.audius.StreamURL(at.ID))
		if derr != nil {
			// A flaky content node shouldn't abort the whole seed; fall back
			// to the bundled local/synthetic source and keep catalog metadata.
			r.logger.Warn("audius audio unavailable, using fallback source",
				"track_id", track.ID, "audius_id", at.ID, "error", derr.Error())
			title = track.Title
			artist = track.Artist
			coverData = nil
			thumbData = nil
		} else {
			audioData = data
		}
		if audioData != nil {
			if data, derr := r.audius.Download(ctx, at.CoverURL); derr == nil {
				coverData = data
			} else {
				r.logger.Warn("audius cover unavailable, generating fallback", "track_id", track.ID, "error", derr.Error())
			}
			if data, derr := r.audius.Download(ctx, at.ThumbURL); derr == nil {
				thumbData = data
			}
		}
	}

	if audioData == nil {
		audioData, err = r.audio.Fetch(ctx, track)
		if err != nil {
			return fmt.Errorf("fetch audio: %w", err)
		}
	}

	durationSec := mp3Duration(audioData)
	if durationSec <= 0 {
		durationSec = 180
	}

	slug := slugify(title)
	if slug == "" {
		slug = fmt.Sprintf("track-%d", track.ID)
	}
	audioKey := fmt.Sprintf("catalog/audio/%d/%s.mp3", track.ID, slug)
	coverKey := fmt.Sprintf("catalog/covers/%d/cover.jpg", track.ID)
	thumbKey := fmt.Sprintf("catalog/covers/%d/thumb.jpg", track.ID)

	if err := r.storage.Upload(ctx, audioKey, bytes.NewReader(audioData), int64(len(audioData)), "audio/mpeg"); err != nil {
		return fmt.Errorf("upload audio: %w", err)
	}

	if coverData == nil {
		coverData, err = GenerateCover(title, artist, track.ID)
		if err != nil {
			return fmt.Errorf("generate cover: %w", err)
		}
	}
	if err := r.storage.Upload(ctx, coverKey, bytes.NewReader(coverData), int64(len(coverData)), "image/jpeg"); err != nil {
		return fmt.Errorf("upload cover: %w", err)
	}

	if thumbData == nil {
		thumbData, err = GenerateThumbnail(title, artist, track.ID)
		if err != nil {
			return fmt.Errorf("generate thumbnail: %w", err)
		}
	}
	if err := r.storage.Upload(ctx, thumbKey, bytes.NewReader(thumbData), int64(len(thumbData)), "image/jpeg"); err != nil {
		return fmt.Errorf("upload thumbnail: %w", err)
	}

	artistID, err := r.repo.EnsureArtist(ctx, artist)
	if err != nil {
		return fmt.Errorf("ensure artist: %w", err)
	}

	genreID, err := r.repo.EnsureGenre(ctx, track.Category)
	if err != nil {
		return fmt.Errorf("ensure genre: %w", err)
	}

	if err := r.repo.UpdateSongMedia(ctx, postgres.SongMediaUpdate{
		SongID:         int64(track.ID),
		Title:          title,
		Slug:           slug,
		ArtistID:       artistID,
		GenreID:        genreID,
		Category:       track.Category,
		AudioObjectKey: audioKey,
		CoverObjectKey: coverKey,
		AudioURL:       r.storage.PublicURL(audioKey),
		CoverURL:       r.storage.PublicURL(coverKey),
		DurationSec:    durationSec,
	}); err != nil {
		return fmt.Errorf("update song media: %w", err)
	}

	r.logger.Info("seeded track",
		"id", track.ID,
		"title", title,
		"artist", artist,
		"duration_sec", durationSec,
		"audio_key", audioKey,
	)

	return nil
}

// audiusForTrack maps a catalog slot to a prefetched Audius track by position.
func (r *Runner) audiusForTrack(track Track) (AudiusTrack, bool) {
	if len(r.audiusTracks) == 0 {
		return AudiusTrack{}, false
	}
	idx := track.ID - 1
	if idx < 0 || idx >= len(r.audiusTracks) {
		return AudiusTrack{}, false
	}
	return r.audiusTracks[idx], true
}

func mp3Duration(data []byte) int {
	decoder := mp3.NewDecoder(bytes.NewReader(data))
	var frame mp3.Frame
	var skipped int
	var total time.Duration

	for {
		if err := decoder.Decode(&frame, &skipped); err != nil {
			break
		}
		total += frame.Duration()
	}

	return int(total.Seconds())
}
