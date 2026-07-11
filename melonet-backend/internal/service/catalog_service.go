package service

import (
	"context"
	"errors"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

var ErrNotFound = errors.New("not found")

type CatalogService struct {
	songs   *postgres.SongRepository
	catalog *postgres.CatalogRepository
}

func NewCatalogService(songs *postgres.SongRepository, catalog *postgres.CatalogRepository) *CatalogService {
	return &CatalogService{songs: songs, catalog: catalog}
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
	songs, total, err := s.songs.ListFiltered(ctx, postgres.SongFilter{
		Category: query.Category,
		GenreID:  query.GenreID,
		ArtistID: query.ArtistID,
		AlbumID:  query.AlbumID,
		Sort:     query.Sort,
		Page:     query.Page,
		Limit:    query.Limit,
	})
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.SongsToAPI(songs), domain.Pagination{
		Page:  query.Page,
		Limit: query.Limit,
		Total: total,
	}, nil
}

func (s *CatalogService) GetSong(ctx context.Context, songID int64) (api.SongResponse, error) {
	song, err := s.songs.GetByID(ctx, songID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.SongResponse{}, ErrNotFound
		}
		return api.SongResponse{}, err
	}
	return postgres.SongToAPI(song), nil
}

func (s *CatalogService) Popular(ctx context.Context, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.songs.ListPopular(ctx, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.SongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *CatalogService) Newest(ctx context.Context, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.songs.ListNewest(ctx, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.SongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *CatalogService) Trending(ctx context.Context, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	songs, total, err := s.songs.ListTrending(ctx, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.SongsToAPI(songs), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *CatalogService) ListArtists(ctx context.Context, q string, page, limit int) ([]api.ArtistResponse, domain.Pagination, error) {
	artists, total, err := s.catalog.ListArtists(ctx, q, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.ArtistsToAPI(artists), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *CatalogService) GetArtist(ctx context.Context, artistID int64) (api.ArtistResponse, error) {
	artist, err := s.catalog.GetArtistByID(ctx, artistID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.ArtistResponse{}, ErrNotFound
		}
		return api.ArtistResponse{}, err
	}
	return postgres.ArtistToAPI(artist), nil
}

func (s *CatalogService) ListArtistSongs(ctx context.Context, artistID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.ListSongs(ctx, SongListQuery{
		ArtistID: &artistID,
		Sort:     "popular",
		Page:     page,
		Limit:    limit,
	})
}

func (s *CatalogService) GetAlbum(ctx context.Context, albumID int64) (api.AlbumResponse, error) {
	album, err := s.catalog.GetAlbumByID(ctx, albumID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.AlbumResponse{}, ErrNotFound
		}
		return api.AlbumResponse{}, err
	}
	return postgres.AlbumToAPI(album), nil
}

func (s *CatalogService) ListAlbumSongs(ctx context.Context, albumID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.ListSongs(ctx, SongListQuery{
		AlbumID: &albumID,
		Sort:    "title",
		Page:    page,
		Limit:   limit,
	})
}

func (s *CatalogService) ListGenres(ctx context.Context, page, limit int) ([]api.GenreResponse, domain.Pagination, error) {
	genres, total, err := s.catalog.ListGenres(ctx, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	return postgres.GenresToAPI(genres), domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

func (s *CatalogService) GetGenre(ctx context.Context, genreID int64) (api.GenreResponse, error) {
	genre, err := s.catalog.GetGenreByID(ctx, genreID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.GenreResponse{}, ErrNotFound
		}
		return api.GenreResponse{}, err
	}
	return postgres.GenreToAPI(genre), nil
}

func (s *CatalogService) ListGenreSongs(ctx context.Context, genreID int64, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.ListSongs(ctx, SongListQuery{
		GenreID: &genreID,
		Sort:    "popular",
		Page:    page,
		Limit:   limit,
	})
}

func (s *CatalogService) CategorySongs(ctx context.Context, category, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.ListSongs(ctx, SongListQuery{
		Category: category,
		Sort:     sort,
		Page:     page,
		Limit:    limit,
	})
}
