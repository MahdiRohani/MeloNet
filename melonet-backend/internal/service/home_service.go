package service

import (
	"context"

	"melonet-backend/internal/domain/api"
)

type HomeService struct {
	catalog *CatalogService
}

func NewHomeService(catalog *CatalogService) *HomeService {
	return &HomeService{catalog: catalog}
}

func (s *HomeService) Feed(ctx context.Context) (api.HomeFeedResponse, error) {
	carouselSongs, _, err := s.catalog.Trending(ctx, 1, 5)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	popular, _, err := s.catalog.Popular(ctx, 1, 10)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	newest, _, err := s.catalog.Newest(ctx, 1, 10)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	electronic, _, err := s.catalog.CategorySongs(ctx, "Global", "popular", 1, 10)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	hiphop, _, err := s.catalog.CategorySongs(ctx, "Popular", "newest", 1, 10)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	ambient, _, err := s.catalog.CategorySongs(ctx, "Nostalgia", "popular", 1, 10)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	return api.HomeFeedResponse{
		Carousel: carouselSongs,
		QuickActions: []api.QuickActionResponse{
			{ID: "search", Title: "Search", Target: "search", Icon: "search"},
			{ID: "popular", Title: "Popular", Target: "catalog/popular", Icon: "trending_up"},
			{ID: "new", Title: "New Releases", Target: "catalog/new", Icon: "new_releases"},
			{ID: "global", Title: "Global", Target: "songs?category=Global", Icon: "public"},
		},
		Rows: []api.HomeRowResponse{
			{ID: "popular", Title: "Popular", RowType: "songs", SeeAllPath: "/api/catalog/popular", Items: popular},
			{ID: "new", Title: "New Releases", RowType: "songs", SeeAllPath: "/api/catalog/new", Items: newest},
			{ID: "global", Title: "Electronic", RowType: "songs", SeeAllPath: "/api/songs?category=Global", Items: electronic},
			{ID: "local", Title: "Hip-Hop", RowType: "songs", SeeAllPath: "/api/songs?category=Popular", Items: hiphop},
			{ID: "nostalgia", Title: "Ambient", RowType: "songs", SeeAllPath: "/api/songs?category=Nostalgia", Items: ambient},
		},
	}, nil
}
