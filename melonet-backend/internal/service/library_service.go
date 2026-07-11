package service

import (
	"context"
	"errors"
	"strings"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

var (
	ErrForbidden = errors.New("forbidden")
)

type LibraryService struct {
	likes    *postgres.LikeRepository
	history  *postgres.HistoryRepository
	songs    *postgres.SongRepository
}

func NewLibraryService(
	likes *postgres.LikeRepository,
	history *postgres.HistoryRepository,
	songs *postgres.SongRepository,
) *LibraryService {
	return &LibraryService{
		likes:   likes,
		history: history,
		songs:   songs,
	}
}

func (s *LibraryService) LikeSong(ctx context.Context, userID, songID int64) (api.LikeResponse, error) {
	if err := s.ensureSongExists(ctx, songID); err != nil {
		return api.LikeResponse{}, err
	}
	if err := s.likes.Like(ctx, userID, songID); err != nil {
		return api.LikeResponse{}, err
	}
	return api.LikeResponse{SongID: uint(songID), Liked: true}, nil
}

func (s *LibraryService) UnlikeSong(ctx context.Context, userID, songID int64) (api.LikeResponse, error) {
	if err := s.likes.Unlike(ctx, userID, songID); err != nil {
		return api.LikeResponse{}, err
	}
	return api.LikeResponse{SongID: uint(songID), Liked: false}, nil
}

func (s *LibraryService) ListLikedSongs(ctx context.Context, userID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.likes.ListLikedSongs(ctx, userID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.SongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *LibraryService) ListRecentSongs(ctx context.Context, userID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.history.ListRecentSongs(ctx, userID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.SongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *LibraryService) RecordPlay(ctx context.Context, userID, songID int64, req api.PlayEventRequest) (api.PlayEventResponse, error) {
	if err := s.ensureSongExists(ctx, songID); err != nil {
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
		SongID:    uint(songID),
		PlayCount: playCount,
		PlayedAt:  playedAt,
	}, nil
}

func (s *LibraryService) ensureSongExists(ctx context.Context, songID int64) error {
	_, err := s.songs.GetByID(ctx, songID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	return nil
}
