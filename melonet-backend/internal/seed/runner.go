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
	force   bool
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
	return &Runner{
		logger:  logger,
		repo:    repo,
		storage: storage,
		audio:   NewAudioSource(cacheDir, mode, logger),
		force:   opts.Force,
	}
}

func (r *Runner) Run(ctx context.Context) error {
	tracks, err := LoadCatalog()
	if err != nil {
		return err
	}

	r.logger.Info("starting media seed", "tracks", len(tracks), "force", r.force)

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

	audioData, err := r.audio.Fetch(ctx, track)
	if err != nil {
		return fmt.Errorf("fetch audio: %w", err)
	}

	durationSec := mp3Duration(audioData)
	if durationSec <= 0 {
		durationSec = 180
	}

	slug := track.Slug()
	audioKey := fmt.Sprintf("catalog/audio/%d/%s.mp3", track.ID, slug)
	coverKey := fmt.Sprintf("catalog/covers/%d/cover.jpg", track.ID)
	thumbKey := fmt.Sprintf("catalog/covers/%d/thumb.jpg", track.ID)

	if err := r.storage.Upload(ctx, audioKey, bytes.NewReader(audioData), int64(len(audioData)), "audio/mpeg"); err != nil {
		return fmt.Errorf("upload audio: %w", err)
	}

	coverData, err := GenerateCover(track.Title, track.Artist, track.ID)
	if err != nil {
		return fmt.Errorf("generate cover: %w", err)
	}
	if err := r.storage.Upload(ctx, coverKey, bytes.NewReader(coverData), int64(len(coverData)), "image/jpeg"); err != nil {
		return fmt.Errorf("upload cover: %w", err)
	}

	thumbData, err := GenerateThumbnail(track.Title, track.Artist, track.ID)
	if err != nil {
		return fmt.Errorf("generate thumbnail: %w", err)
	}
	if err := r.storage.Upload(ctx, thumbKey, bytes.NewReader(thumbData), int64(len(thumbData)), "image/jpeg"); err != nil {
		return fmt.Errorf("upload thumbnail: %w", err)
	}

	artistID, err := r.repo.EnsureArtist(ctx, track.Artist)
	if err != nil {
		return fmt.Errorf("ensure artist: %w", err)
	}

	genreID, err := r.repo.EnsureGenre(ctx, track.Category)
	if err != nil {
		return fmt.Errorf("ensure genre: %w", err)
	}

	if err := r.repo.UpdateSongMedia(ctx, postgres.SongMediaUpdate{
		SongID:         int64(track.ID),
		Title:          track.Title,
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
		"title", track.Title,
		"duration_sec", durationSec,
		"audio_key", audioKey,
	)

	return nil
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
