package service

import (
	"context"
	"fmt"
	"strings"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

type SearchService struct {
	audius *audius.Client
	users  *postgres.UserRepository
}

func NewSearchService(client *audius.Client, users *postgres.UserRepository) *SearchService {
	return &SearchService{audius: client, users: users}
}

func NormalizeSearchType(searchType string) string {
	switch strings.ToLower(strings.TrimSpace(searchType)) {
	case "song", "songs":
		return "song"
	case "artist", "artists":
		return "artist"
	case "album", "albums":
		return "album"
	case "genre", "genres":
		return "genre"
	case "user", "users":
		return "user"
	default:
		return "all"
	}
}

func (s *SearchService) Search(ctx context.Context, query, searchType string, page, limit int) (api.SearchResponse, domain.Pagination, error) {
	query = strings.TrimSpace(query)
	if query == "" {
		return api.SearchResponse{}, domain.Pagination{}, fmt.Errorf("empty query")
	}

	searchType = NormalizeSearchType(searchType)
	result := api.SearchResponse{Query: query, Type: searchType}
	meta := domain.Pagination{Page: page, Limit: limit}

	switch searchType {
	case "song":
		fetchLimit := page * limit
		if fetchLimit < 50 {
			fetchLimit = 50
		}
		if fetchLimit > 100 {
			fetchLimit = 100
		}
		tracks, err := s.audius.SearchTracks(ctx, query, fetchLimit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		pageTracks, total := audius.Paginate(tracks, page, limit)
		result.Songs = audius.TracksToAPI(s.audius.PublicBase(), pageTracks)
		meta.Total = total
	case "user":
		users, total, err := s.users.Search(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		result.Users = postgres.UserSummariesToSearchResults(users)
		meta.Total = total
	default:
		fetchLimit := page * limit
		if fetchLimit < 50 {
			fetchLimit = 50
		}
		if fetchLimit > 100 {
			fetchLimit = 100
		}
		tracks, err := s.audius.SearchTracks(ctx, query, fetchLimit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		pageTracks, total := audius.Paginate(tracks, page, limit)
		result.Songs = audius.TracksToAPI(s.audius.PublicBase(), pageTracks)
		meta.Total = total

		previewLimit := 5
		if limit < previewLimit {
			previewLimit = limit
		}
		users, _, _ := s.users.Search(ctx, query, 1, previewLimit)
		result.Users = postgres.UserSummariesToSearchResults(users)
	}

	return result, meta, nil
}
