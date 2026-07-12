package service

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

type PlaylistService struct {
	playlists *postgres.PlaylistRepository
	cache     *postgres.SongCacheRepository
	audius    *audius.Client
}

func NewPlaylistService(
	playlists *postgres.PlaylistRepository,
	cache *postgres.SongCacheRepository,
	client *audius.Client,
) *PlaylistService {
	return &PlaylistService{
		playlists: playlists,
		cache:     cache,
		audius:    client,
	}
}

func (s *PlaylistService) List(ctx context.Context, userID int64, scope string, page, limit int) ([]api.PlaylistResponse, domain.Pagination, error) {
	playlists, total, err := s.playlists.List(ctx, userID, postgres.PlaylistScope(scope), page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.PlaylistsToAPI(playlists, userID), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *PlaylistService) Get(ctx context.Context, userID, playlistID int64) (api.PlaylistDetailResponse, error) {
	playlist, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.PlaylistDetailResponse{}, ErrNotFound
		}
		return api.PlaylistDetailResponse{}, err
	}

	canView, err := s.playlists.CanView(ctx, playlist, userID)
	if err != nil {
		return api.PlaylistDetailResponse{}, err
	}
	if !canView {
		return api.PlaylistDetailResponse{}, ErrForbidden
	}

	songs, _, err := s.playlists.ListSongs(ctx, playlistID, 1, 500)
	if err != nil {
		return api.PlaylistDetailResponse{}, err
	}

	return api.PlaylistDetailResponse{
		PlaylistResponse: postgres.PlaylistToAPI(playlist, userID),
		Songs:            cachedSongsToAPI(songs),
	}, nil
}

func (s *PlaylistService) ListSongs(ctx context.Context, userID, playlistID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	playlist, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return nil, domain.Pagination{}, ErrNotFound
		}
		return nil, domain.Pagination{}, err
	}

	canView, err := s.playlists.CanView(ctx, playlist, userID)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	if !canView {
		return nil, domain.Pagination{}, ErrForbidden
	}

	songs, total, err := s.playlists.ListSongs(ctx, playlistID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return cachedSongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *PlaylistService) Create(ctx context.Context, userID int64, req api.CreatePlaylistRequest) (api.PlaylistResponse, error) {
	title := strings.TrimSpace(req.Title)
	if title == "" {
		return api.PlaylistResponse{}, fmt.Errorf("title is required")
	}

	visibility, err := parsePlaylistVisibility(req.Visibility)
	if err != nil {
		return api.PlaylistResponse{}, err
	}

	created, err := s.playlists.Create(ctx, userID, title, strings.TrimSpace(req.Description), visibility)
	if err != nil {
		return api.PlaylistResponse{}, err
	}

	playlist, err := s.playlists.GetByID(ctx, created.ID)
	if err != nil {
		return api.PlaylistResponse{}, err
	}
	return postgres.PlaylistToAPI(playlist, userID), nil
}

func (s *PlaylistService) Update(ctx context.Context, userID, playlistID int64, req api.UpdatePlaylistRequest) (api.PlaylistResponse, error) {
	playlist, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.PlaylistResponse{}, ErrNotFound
		}
		return api.PlaylistResponse{}, err
	}
	if playlist.IsSystem || playlist.OwnerID != userID {
		return api.PlaylistResponse{}, ErrForbidden
	}

	var visibility *domain.PlaylistVisibility
	if req.Visibility != nil {
		parsed, err := parsePlaylistVisibility(*req.Visibility)
		if err != nil {
			return api.PlaylistResponse{}, err
		}
		visibility = &parsed
	}

	var title *string
	if req.Title != nil {
		trimmed := strings.TrimSpace(*req.Title)
		if trimmed == "" {
			return api.PlaylistResponse{}, fmt.Errorf("title cannot be empty")
		}
		title = &trimmed
	}

	var description *string
	if req.Description != nil {
		trimmed := strings.TrimSpace(*req.Description)
		description = &trimmed
	}

	_, err = s.playlists.Update(ctx, playlistID, title, description, visibility)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.PlaylistResponse{}, ErrNotFound
		}
		return api.PlaylistResponse{}, err
	}

	updated, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		return api.PlaylistResponse{}, err
	}
	return postgres.PlaylistToAPI(updated, userID), nil
}

func (s *PlaylistService) Delete(ctx context.Context, userID, playlistID int64) error {
	playlist, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	if playlist.IsSystem || playlist.OwnerID != userID {
		return ErrForbidden
	}

	if err := s.playlists.Delete(ctx, playlistID, userID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	return nil
}

func (s *PlaylistService) AddSong(ctx context.Context, userID, playlistID int64, songID string) error {
	playlist, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	if playlist.IsSystem || playlist.OwnerID != userID {
		return ErrForbidden
	}

	if err := cacheTrackFromAudius(ctx, s.audius, s.cache, songID); err != nil {
		return ErrNotFound
	}

	if err := s.playlists.AddSong(ctx, playlistID, songID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		if errors.Is(err, postgres.ErrForbidden) {
			return ErrForbidden
		}
		return err
	}
	return nil
}

func (s *PlaylistService) RemoveSong(ctx context.Context, userID, playlistID int64, songID string) error {
	playlist, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	if playlist.IsSystem || playlist.OwnerID != userID {
		return ErrForbidden
	}

	if err := s.playlists.RemoveSong(ctx, playlistID, songID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		if errors.Is(err, postgres.ErrForbidden) {
			return ErrForbidden
		}
		return err
	}
	return nil
}

func (s *PlaylistService) ReorderSongs(ctx context.Context, userID, playlistID int64, songIDs []string) error {
	playlist, err := s.playlists.GetByID(ctx, playlistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	if playlist.IsSystem || playlist.OwnerID != userID {
		return ErrForbidden
	}

	if err := s.playlists.ReorderSongs(ctx, playlistID, songIDs); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		if errors.Is(err, postgres.ErrForbidden) {
			return ErrForbidden
		}
		return fmt.Errorf("%w: %s", ErrInvalidInput, err.Error())
	}
	return nil
}

func parsePlaylistVisibility(value string) (domain.PlaylistVisibility, error) {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "", string(domain.PlaylistPrivate):
		return domain.PlaylistPrivate, nil
	case string(domain.PlaylistPublic):
		return domain.PlaylistPublic, nil
	default:
		return "", fmt.Errorf("invalid visibility")
	}
}
