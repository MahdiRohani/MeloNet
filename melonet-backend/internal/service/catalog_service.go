package service

import (
	"context"
	"strings"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
)

type CatalogService struct {
	audius *audius.Client
}

func NewCatalogService(client *audius.Client) *CatalogService {
	return &CatalogService{audius: client}
}

type SongListQuery struct {
	Category string
	GenreID  *int64
	ArtistID *int64
	AlbumID  *int64
	Sort     string
	Page     int
	Limit    int
}

func (s *CatalogService) ListSongs(ctx context.Context, query SongListQuery) ([]api.SongResponse, domain.Pagination, error) {
	genre, timeRange := categoryToTrending(query.Category, query.Sort)
	fetchLimit := query.Page * query.Limit
	if fetchLimit < 50 {
		fetchLimit = 50
	}
	if fetchLimit > 100 {
		fetchLimit = 100
	}

	tracks, err := s.audius.Trending(ctx, genre, timeRange, fetchLimit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	pageTracks, total := audius.Paginate(tracks, query.Page, query.Limit)
	return audius.TracksToAPI(s.audius.PublicBase(), pageTracks), domain.Pagination{
		Page:  query.Page,
		Limit: query.Limit,
		Total: total,
	}, nil
}

func (s *CatalogService) GetSong(ctx context.Context, songID string) (api.SongResponse, error) {
	track, err := s.audius.GetTrack(ctx, songID)
	if err != nil {
		return api.SongResponse{}, ErrNotFound
	}
	return audius.ToSongResponse(s.audius.PublicBase(), track), nil
}

func (s *CatalogService) Popular(ctx context.Context, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.trendingPage(ctx, "", "week", page, limit)
}

func (s *CatalogService) Newest(ctx context.Context, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.trendingPage(ctx, "", "month", page, limit)
}

func (s *CatalogService) Trending(ctx context.Context, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.trendingPage(ctx, "", "week", page, limit)
}

func (s *CatalogService) CategorySongs(ctx context.Context, category, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	genre, timeRange := categoryToTrending(category, sort)
	return s.trendingPage(ctx, genre, timeRange, page, limit)
}

func (s *CatalogService) trendingPage(ctx context.Context, genre, timeRange string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	fetchLimit := page * limit
	if fetchLimit < 50 {
		fetchLimit = 50
	}
	if fetchLimit > 100 {
		fetchLimit = 100
	}

	tracks, err := s.audius.Trending(ctx, genre, timeRange, fetchLimit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	pageTracks, total := audius.Paginate(tracks, page, limit)
	return audius.TracksToAPI(s.audius.PublicBase(), pageTracks), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func (s *CatalogService) ListArtists(ctx context.Context, q string, page, limit int) ([]api.ArtistResponse, domain.Pagination, error) {
	return []api.ArtistResponse{}, domain.Pagination{Page: page, Limit: limit, Total: 0}, nil
}

func (s *CatalogService) GetArtist(ctx context.Context, artistID int64) (api.ArtistResponse, error) {
	return api.ArtistResponse{}, ErrNotFound
}

func (s *CatalogService) ListArtistSongs(ctx context.Context, artistID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return []api.SongResponse{}, domain.Pagination{Page: page, Limit: limit, Total: 0}, nil
}

func (s *CatalogService) GetAlbum(ctx context.Context, albumID int64) (api.AlbumResponse, error) {
	return api.AlbumResponse{}, ErrNotFound
}

func (s *CatalogService) ListAlbumSongs(ctx context.Context, albumID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return []api.SongResponse{}, domain.Pagination{Page: page, Limit: limit, Total: 0}, nil
}

func (s *CatalogService) ListGenres(ctx context.Context, page, limit int) ([]api.GenreResponse, domain.Pagination, error) {
	return []api.GenreResponse{}, domain.Pagination{Page: page, Limit: limit, Total: 0}, nil
}

func (s *CatalogService) GetGenre(ctx context.Context, genreID int64) (api.GenreResponse, error) {
	return api.GenreResponse{}, ErrNotFound
}

func (s *CatalogService) ListGenreSongs(ctx context.Context, genreID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return []api.SongResponse{}, domain.Pagination{Page: page, Limit: limit, Total: 0}, nil
}

func categoryToTrending(category, sort string) (genre, timeRange string) {
	switch strings.ToLower(strings.TrimSpace(category)) {
	case "global":
		genre = "Electronic"
	case "iranian", "popular":
		genre = "Hip-Hop/Rap"
	case "nostalgia", "new":
		genre = "Ambient"
	default:
		genre = ""
	}

	switch strings.ToLower(strings.TrimSpace(sort)) {
	case "newest", "new":
		timeRange = "month"
	case "trending":
		timeRange = "week"
	default:
		timeRange = "week"
	}
	return genre, timeRange
}
