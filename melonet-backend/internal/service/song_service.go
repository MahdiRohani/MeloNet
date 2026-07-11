package service

import (
	"context"
	"fmt"
	"strconv"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

type SongService struct {
	repo *postgres.SongRepository
}

func NewSongService(repo *postgres.SongRepository) *SongService {
	return &SongService{repo: repo}
}

func (s *SongService) List(ctx context.Context, category string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.repo.List(ctx, category, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.SongsToAPI(songs), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func (s *SongService) Search(ctx context.Context, query string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	if query == "" {
		return nil, domain.Pagination{}, fmt.Errorf("empty query")
	}

	songs, total, err := s.repo.Search(ctx, query, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.SongsToAPI(songs), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func ParsePagination(pageStr, limitStr string, defaultLimit int) (int, int) {
	page := 1
	limit := defaultLimit

	if pageStr != "" {
		if parsed, err := strconv.Atoi(pageStr); err == nil && parsed > 0 {
			page = parsed
		}
	}

	if limitStr != "" {
		if parsed, err := strconv.Atoi(limitStr); err == nil && parsed > 0 {
			limit = parsed
		}
	}

	if limit > 100 {
		limit = 100
	}

	return page, limit
}
