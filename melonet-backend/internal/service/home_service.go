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
	carouselSongs, _, err := s.catalog.Trending(ctx, "", 1, 5)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	popular, _, err := s.catalog.Popular(ctx, "", 1, 12)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	newest, _, err := s.catalog.Newest(ctx, "", 1, 12)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	hiphop, _, err := s.catalog.CategorySongs(ctx, "Popular", "trending", 1, 10)
	if err != nil {
		return api.HomeFeedResponse{}, err
	}

	// Curated region/theme rows are aggregated from artist searches / genre
	// trending and can be flaky, so they must never fail the whole feed.
	iranian, _, err := s.catalog.IranianSongs(ctx, "", 1, 12)
	if err != nil {
		iranian = nil
	}
	turkish, _, err := s.catalog.TurkishSongs(ctx, "", 1, 12)
	if err != nil {
		turkish = nil
	}
	instrumental, _, err := s.catalog.InstrumentalSongs(ctx, "", 1, 12)
	if err != nil {
		instrumental = nil
	}

	// Artist rows (rendered as circular chips) — also non-fatal.
	foreignArtists, _, err := s.catalog.ListArtistsByRegion(ctx, RegionForeign, 1, 10)
	if err != nil {
		foreignArtists = nil
	}
	iranianArtists, _, err := s.catalog.ListArtistsByRegion(ctx, RegionIranian, 1, 10)
	if err != nil {
		iranianArtists = nil
	}

	return api.HomeFeedResponse{
		Carousel: carouselSongs,
		QuickActions: []api.QuickActionResponse{
			{ID: "liked", Title: "Liked", Target: "liked", Icon: "favorite"},
			{ID: "playlists", Title: "Playlists", Target: "playlists", Icon: "playlist"},
			{ID: "recent", Title: "Recent", Target: "recent", Icon: "history"},
			{ID: "following", Title: "Following", Target: "following", Icon: "people"},
		},
		Rows: []api.HomeRowResponse{
			{ID: "popular", Title: "Popular", RowType: "songs", SeeAllPath: "/api/catalog/popular", Items: popular},
			{ID: "new", Title: "New Releases", RowType: "songs", SeeAllPath: "/api/catalog/new", Items: newest},
			{ID: "iranian", Title: "Iranian", RowType: "songs", SeeAllPath: "/api/songs?category=Iranian", Items: iranian},
			{ID: "turkish", Title: "Turkish", RowType: "songs", SeeAllPath: "/api/songs?category=Turkish", Items: turkish},
			{ID: "instrumental", Title: "Instrumental", RowType: "songs", SeeAllPath: "/api/songs?category=Instrumental", Items: instrumental},
			{ID: "hiphop", Title: "Hip-Hop", RowType: "songs", SeeAllPath: "/api/songs?category=Popular", Items: hiphop},
		},
		ArtistRows: []api.HomeArtistRowResponse{
			{ID: "artists_foreign", Title: "Popular Artists", SeeAllPath: "/api/artists?region=foreign", Items: foreignArtists},
			{ID: "artists_iranian", Title: "Iranian Artists", SeeAllPath: "/api/artists?region=iranian", Items: iranianArtists},
		},
	}, nil
}
