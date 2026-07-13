package service

import (
	"context"
	"fmt"
	"hash/fnv"
	"strings"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

type SearchService struct {
	audius  *audius.Client
	users   *postgres.UserRepository
	catalog *CatalogService
}

func NewSearchService(client *audius.Client, users *postgres.UserRepository, catalog *CatalogService) *SearchService {
	return &SearchService{audius: client, users: users, catalog: catalog}
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
		tracks, err := s.searchTracksRanked(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		pageTracks, total := audius.Paginate(tracks, page, limit)
		result.Songs = audius.TracksToAPI(s.audius.PublicBase(), pageTracks)
		meta.Total = total
	case "artist":
		artists, total := s.catalog.SearchArtists(ctx, query, page, limit)
		result.Artists = artists
		meta.Total = total
	case "user":
		users, total, err := s.users.Search(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		result.Users = postgres.UserSummariesToSearchResults(users)
		meta.Total = total
	default:
		tracks, err := s.searchTracksRanked(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		pageTracks, total := audius.Paginate(tracks, page, limit)
		result.Songs = audius.TracksToAPI(s.audius.PublicBase(), pageTracks)
		meta.Total = total

		// Artist + user previews so the "All" tab surfaces every result kind.
		artistPreview, _ := s.catalog.SearchArtists(ctx, query, 1, 5)
		result.Artists = artistPreview

		previewLimit := 5
		if limit < previewLimit {
			previewLimit = limit
		}
		users, _, _ := s.users.Search(ctx, query, 1, previewLimit)
		result.Users = postgres.UserSummariesToSearchResults(users)
	}

	return result, meta, nil
}

// searchTracksRanked fetches a page-worth of Audius track results and ranks them
// by real popularity (play count) so the most-listened matches surface first.
func (s *SearchService) searchTracksRanked(ctx context.Context, query string, page, limit int) ([]audius.Track, error) {
	fetchLimit := page * limit
	if fetchLimit < 50 {
		fetchLimit = 50
	}
	if fetchLimit > 100 {
		fetchLimit = 100
	}
	tracks, err := s.audius.SearchTracks(ctx, query, fetchLimit)
	if err != nil {
		return nil, err
	}
	audius.SortByPopularity(tracks)
	return tracks, nil
}

// artistID derives a stable positive 32-bit-safe id from an artist name so the
// client can key/route on it without a real artist record existing.
func artistID(name string) uint {
	h := fnv.New32a()
	_, _ = h.Write([]byte(strings.ToLower(strings.TrimSpace(name))))
	return uint(h.Sum32()%2_000_000_000) + 1
}
