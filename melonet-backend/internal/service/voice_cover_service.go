package service

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
	redisStorage "melonet-backend/internal/storage/redis"
)

type VoiceCoverService struct {
	repo     *postgres.VoiceCoverRepository
	catalog  *CatalogService
	redis    *redisStorage.Client
	mediaURL func(objectKey string) string
	deleteObject func(ctx context.Context, objectKey string) error
	publicBase string
}

func NewVoiceCoverService(
	repo *postgres.VoiceCoverRepository,
	catalog *CatalogService,
	redis *redisStorage.Client,
	mediaURL func(objectKey string) string,
	deleteObject func(ctx context.Context, objectKey string) error,
	publicBase string,
) *VoiceCoverService {
	return &VoiceCoverService{
		repo:         repo,
		catalog:      catalog,
		redis:        redis,
		mediaURL:     mediaURL,
		deleteObject: deleteObject,
		publicBase:   strings.TrimRight(publicBase, "/"),
	}
}

func (s *VoiceCoverService) ListArtists(ctx context.Context) ([]api.VoiceArtistResponse, error) {
	artists, err := s.repo.ListArtists(ctx, true)
	if err != nil {
		return nil, err
	}
	out := make([]api.VoiceArtistResponse, 0, len(artists))
	for _, a := range artists {
		out = append(out, api.VoiceArtistResponse{
			ID:          a.ID,
			Slug:        a.Slug,
			DisplayName: a.DisplayName,
			AvatarURL:   s.resolvePublicURL(a.AvatarURL),
			Enabled:     a.Enabled,
		})
	}
	return out, nil
}

func (s *VoiceCoverService) ListReady(ctx context.Context, page, limit int) ([]api.VoiceCoverResponse, domain.Pagination, error) {
	covers, total, err := s.repo.ListReady(ctx, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	out := make([]api.VoiceCoverResponse, 0, len(covers))
	for _, c := range covers {
		out = append(out, s.toResponse(c))
	}
	return out, domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *VoiceCoverService) Get(ctx context.Context, id int64) (api.VoiceCoverResponse, error) {
	cover, err := s.repo.GetByID(ctx, id)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.VoiceCoverResponse{}, ErrNotFound
		}
		return api.VoiceCoverResponse{}, err
	}
	return s.toResponse(cover), nil
}

func (s *VoiceCoverService) Create(ctx context.Context, userID int64, req api.CreateVoiceCoverRequest) (api.VoiceCoverResponse, error) {
	songID := strings.TrimSpace(req.SourceSongID)
	slug := strings.TrimSpace(strings.ToLower(req.TargetArtistSlug))
	if songID == "" || slug == "" {
		return api.VoiceCoverResponse{}, fmt.Errorf("%w: source_song_id and target_artist_slug are required", ErrBadRequest)
	}

	artist, err := s.repo.GetArtistBySlug(ctx, slug)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.VoiceCoverResponse{}, ErrNotFound
		}
		return api.VoiceCoverResponse{}, err
	}
	if !artist.Enabled {
		return api.VoiceCoverResponse{}, fmt.Errorf("%w: artist is disabled", ErrBadRequest)
	}

	song, err := s.catalog.GetSong(ctx, songID)
	if err != nil {
		return api.VoiceCoverResponse{}, err
	}

	requestedBy := userID
	cover, requeue, err := s.repo.CreatePending(
		ctx,
		songID,
		artist.ID,
		song.Title,
		song.ArtistName,
		song.CoverURL,
		&requestedBy,
	)
	if err != nil {
		return api.VoiceCoverResponse{}, err
	}

	if requeue {
		if err := s.redis.EnqueueVoiceCover(ctx, cover.ID); err != nil {
			return api.VoiceCoverResponse{}, fmt.Errorf("enqueue voice cover job: %w", err)
		}
	}

	return s.toResponse(cover), nil
}

func (s *VoiceCoverService) Delete(ctx context.Context, id int64) error {
	objectKey, err := s.repo.Delete(ctx, id)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	if objectKey != "" && s.deleteObject != nil {
		if delErr := s.deleteObject(ctx, objectKey); delErr != nil {
			// Row is already gone; log-worthy but don't fail the client.
			return nil
		}
	}
	return nil
}

func (s *VoiceCoverService) resolvePublicURL(raw string) string {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return ""
	}
	if strings.HasPrefix(raw, "http://") || strings.HasPrefix(raw, "https://") {
		return raw
	}
	if s.publicBase == "" {
		return raw
	}
	if strings.HasPrefix(raw, "/") {
		return s.publicBase + raw
	}
	return s.publicBase + "/" + raw
}

func (s *VoiceCoverService) toResponse(c postgres.VoiceCover) api.VoiceCoverResponse {
	resp := api.VoiceCoverResponse{
		ID:               c.ID,
		SourceSongID:     c.SourceSongID,
		TargetArtistID:   c.TargetArtistID,
		TargetArtistSlug: c.TargetArtistSlug,
		TargetArtistName: c.TargetArtistName,
		Status:           c.Status,
		CoverURL:         c.CoverURL,
		SourceTitle:      c.SourceTitle,
		SourceArtist:     c.SourceArtist,
		Error:            c.Error,
		ProgressPct:      c.ProgressPct,
		ProgressStage:    c.ProgressStage,
		EtaSeconds:       c.EtaSeconds,
		CreatedAt:        c.CreatedAt,
		UpdatedAt:        c.UpdatedAt,
		ReadyAt:          c.ReadyAt,
	}
	if c.Status == "ready" && c.AudioObjectKey != "" && s.mediaURL != nil {
		resp.AudioURL = s.mediaURL(c.AudioObjectKey)
	}
	return resp
}
