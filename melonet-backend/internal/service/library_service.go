package service

import (
	"context"
	"errors"
	"strings"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

type LibraryService struct {
	likes   *postgres.LikeRepository
	history *postgres.HistoryRepository
	cache   *postgres.SongCacheRepository
	audius  *audius.Client
}

func NewLibraryService(
	likes *postgres.LikeRepository,
	history *postgres.HistoryRepository,
	cache *postgres.SongCacheRepository,
	client *audius.Client,
) *LibraryService {
	return &LibraryService{
		likes:   likes,
		history: history,
		cache:   cache,
		audius:  client,
	}
}

func (s *LibraryService) LikeSong(ctx context.Context, userID int64, songID string) (api.LikeResponse, error) {
	if err := s.ensureSongCached(ctx, songID); err != nil {
		return api.LikeResponse{}, err
	}
	if err := s.likes.Like(ctx, userID, songID); err != nil {
		return api.LikeResponse{}, err
	}
	return api.LikeResponse{SongID: songID, Liked: true}, nil
}

func (s *LibraryService) UnlikeSong(ctx context.Context, userID int64, songID string) (api.LikeResponse, error) {
	if err := s.likes.Unlike(ctx, userID, songID); err != nil {
		return api.LikeResponse{}, err
	}
	return api.LikeResponse{SongID: songID, Liked: false}, nil
}

func (s *LibraryService) ListLikedSongs(ctx context.Context, userID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.likes.ListLikedSongs(ctx, userID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return cachedSongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *LibraryService) ListRecentSongs(ctx context.Context, userID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.history.ListRecentSongs(ctx, userID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return cachedSongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *LibraryService) RecordPlay(ctx context.Context, userID int64, songID string, req api.PlayEventRequest) (api.PlayEventResponse, error) {
	if err := s.ensureSongCached(ctx, songID); err != nil {
		return api.PlayEventResponse{}, err
	}

	duration := req.DurationPlayedSec
	if duration < 0 {
		duration = 0
	}

	source := strings.TrimSpace(req.Source)
	playedAt, err := s.history.RecordPlay(ctx, userID, songID, duration, source)
	if err != nil {
		return api.PlayEventResponse{}, err
	}

	playCount, err := s.history.GetPlayCount(ctx, songID)
	if err != nil {
		return api.PlayEventResponse{}, err
	}

	return api.PlayEventResponse{
		SongID:    songID,
		PlayCount: playCount,
		PlayedAt:  playedAt,
	}, nil
}

func (s *LibraryService) ensureSongCached(ctx context.Context, songID string) error {
	if songID == "" {
		return ErrNotFound
	}
	if _, err := s.cache.GetByID(ctx, songID); err == nil {
		return nil
	} else if !errors.Is(err, postgres.ErrNotFound) {
		return err
	}
	if err := cacheTrackFromAudius(ctx, s.audius, s.cache, songID); err != nil {
		return ErrNotFound
	}
	return nil
}

func cachedSongsToAPI(songs []postgres.CachedSong) []api.SongResponse {
	out := make([]api.SongResponse, 0, len(songs))
	for _, song := range songs {
		out = append(out, postgres.CachedSongToAPI(song))
	}
	return out
}
