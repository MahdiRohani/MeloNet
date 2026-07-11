package service

import (
	"context"
	"fmt"
	"strings"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

type SearchService struct {
	songs   *postgres.SongRepository
	catalog *postgres.CatalogRepository
}

func NewSearchService(songs *postgres.SongRepository, catalog *postgres.CatalogRepository) *SearchService {
	return &SearchService{songs: songs, catalog: catalog}
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
		songs, total, err := s.songs.SearchSongs(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		result.Songs = postgres.SongsToAPI(songs)
		meta.Total = total
	case "artist":
		artists, total, err := s.catalog.SearchArtists(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		result.Artists = postgres.ArtistsToAPI(artists)
		meta.Total = total
	case "album":
		albums, total, err := s.catalog.SearchAlbums(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		result.Albums = postgres.AlbumsToAPI(albums)
		meta.Total = total
	case "genre":
		genres, total, err := s.catalog.SearchGenres(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		result.Genres = postgres.GenresToAPI(genres)
		meta.Total = total
	default:
		songs, songTotal, err := s.songs.SearchSongs(ctx, query, page, limit)
		if err != nil {
			return api.SearchResponse{}, domain.Pagination{}, err
		}
		result.Songs = postgres.SongsToAPI(songs)
		meta.Total = songTotal

		previewLimit := 5
		if limit < previewLimit {
			previewLimit = limit
		}
		artists, _, _ := s.catalog.SearchArtists(ctx, query, 1, previewLimit)
		albums, _, _ := s.catalog.SearchAlbums(ctx, query, 1, previewLimit)
		genres, _, _ := s.catalog.SearchGenres(ctx, query, 1, previewLimit)
		result.Artists = postgres.ArtistsToAPI(artists)
		result.Albums = postgres.AlbumsToAPI(albums)
		result.Genres = postgres.GenresToAPI(genres)
	}

	return result, meta, nil
}
