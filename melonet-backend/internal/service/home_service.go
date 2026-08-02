package service

import (
	"context"
	"sync"
	"time"

	"melonet-backend/internal/domain/api"
)

type HomeService struct {
	catalog *CatalogService

	mu       sync.RWMutex
	cached   *api.HomeFeedResponse
	cachedAt time.Time

	buildMu sync.Mutex
}

func NewHomeService(catalog *CatalogService) *HomeService {
	return &HomeService{catalog: catalog}
}

const homeCacheTTL = 5 * time.Minute

func (s *HomeService) Feed(ctx context.Context) (api.HomeFeedResponse, error) {
	s.mu.RLock()
	if s.cached != nil && time.Since(s.cachedAt) < homeCacheTTL {
		feed := *s.cached
		s.mu.RUnlock()
		return feed, nil
	}
	s.mu.RUnlock()

	// Coalesce concurrent cold builds (acts like singleflight).
	s.buildMu.Lock()
	defer s.buildMu.Unlock()

	s.mu.RLock()
	if s.cached != nil && time.Since(s.cachedAt) < homeCacheTTL {
		feed := *s.cached
		s.mu.RUnlock()
		return feed, nil
	}
	s.mu.RUnlock()

	feed, complete, err := s.buildFeed(ctx)
	if err != nil {
		s.mu.RLock()
		defer s.mu.RUnlock()
		if s.cached != nil {
			return *s.cached, nil
		}
		return api.HomeFeedResponse{}, err
	}
	// Only cache fully-built feeds so soft-deadline empties (e.g. artist rows)
	// don't stick for the full TTL.
	if complete {
		s.mu.Lock()
		s.cached = &feed
		s.cachedAt = time.Now()
		s.mu.Unlock()
	}
	return feed, nil
}

func (s *HomeService) buildFeed(ctx context.Context) (api.HomeFeedResponse, bool, error) {
	type songsResult struct {
		items []api.SongResponse
		err   error
		done  bool
	}
	type artistsResult struct {
		items []api.ArtistResponse
		err   error
		done  bool
	}

	var (
		carousel     songsResult
		popular      songsResult
		newest       songsResult
		hiphop       songsResult
		iranian      songsResult
		turkish      songsResult
		instrumental songsResult
		foreignArt   artistsResult
		iranianArt   artistsResult
		mu           sync.Mutex
	)

	var wg sync.WaitGroup
	runSongs := func(dst *songsResult, fn func() ([]api.SongResponse, error)) {
		wg.Add(1)
		go func() {
			defer wg.Done()
			items, err := fn()
			mu.Lock()
			*dst = songsResult{items: items, err: err, done: true}
			mu.Unlock()
		}()
	}
	runArtists := func(dst *artistsResult, fn func() ([]api.ArtistResponse, error)) {
		wg.Add(1)
		go func() {
			defer wg.Done()
			items, err := fn()
			mu.Lock()
			*dst = artistsResult{items: items, err: err, done: true}
			mu.Unlock()
		}()
	}

	runSongs(&carousel, func() ([]api.SongResponse, error) {
		items, _, err := s.catalog.Trending(ctx, "", 1, 5)
		return items, err
	})
	runSongs(&popular, func() ([]api.SongResponse, error) {
		items, _, err := s.catalog.Popular(ctx, "", 1, 12)
		return items, err
	})
	runSongs(&newest, func() ([]api.SongResponse, error) {
		items, _, err := s.catalog.Newest(ctx, "", 1, 12)
		return items, err
	})
	runSongs(&hiphop, func() ([]api.SongResponse, error) {
		items, _, err := s.catalog.CategorySongs(ctx, "Popular", "trending", 1, 10)
		return items, err
	})
	runSongs(&iranian, func() ([]api.SongResponse, error) {
		items, _, err := s.catalog.IranianSongs(ctx, "", 1, 12)
		return items, err
	})
	runSongs(&turkish, func() ([]api.SongResponse, error) {
		items, _, err := s.catalog.TurkishSongs(ctx, "", 1, 12)
		return items, err
	})
	runSongs(&instrumental, func() ([]api.SongResponse, error) {
		items, _, err := s.catalog.InstrumentalSongs(ctx, "", 1, 12)
		return items, err
	})
	runArtists(&foreignArt, func() ([]api.ArtistResponse, error) {
		items, _, err := s.catalog.ListArtistsByRegion(ctx, RegionForeign, 1, 10)
		return items, err
	})
	runArtists(&iranianArt, func() ([]api.ArtistResponse, error) {
		items, _, err := s.catalog.ListArtistsByRegion(ctx, RegionIranian, 1, 10)
		return items, err
	})

	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()

	// Prefer carousel, popular, and artist rows (signature home sections), then
	// give remaining curated song rows a short grace period.
	preferredReady := make(chan struct{})
	go func() {
		ticker := time.NewTicker(25 * time.Millisecond)
		defer ticker.Stop()
		for {
			mu.Lock()
			ready := carousel.done && popular.done && foreignArt.done && iranianArt.done
			mu.Unlock()
			if ready {
				close(preferredReady)
				return
			}
			select {
			case <-done:
				close(preferredReady)
				return
			case <-ticker.C:
			}
		}
	}()

	select {
	case <-done:
	case <-preferredReady:
		select {
		case <-done:
		case <-time.After(2 * time.Second):
		case <-ctx.Done():
		}
	case <-ctx.Done():
	}

	mu.Lock()
	defer mu.Unlock()

	if !carousel.done || carousel.err != nil {
		if carousel.err != nil {
			return api.HomeFeedResponse{}, false, carousel.err
		}
		return api.HomeFeedResponse{}, false, context.DeadlineExceeded
	}
	if !popular.done || popular.err != nil {
		if popular.err != nil {
			return api.HomeFeedResponse{}, false, popular.err
		}
		return api.HomeFeedResponse{}, false, context.DeadlineExceeded
	}
	if newest.err != nil {
		newest.items = []api.SongResponse{}
	}
	if !newest.done {
		newest.items = []api.SongResponse{}
	}
	if hiphop.err != nil || !hiphop.done {
		hiphop.items = []api.SongResponse{}
	}
	if iranian.err != nil || !iranian.done {
		iranian.items = []api.SongResponse{}
	}
	if turkish.err != nil || !turkish.done {
		turkish.items = []api.SongResponse{}
	}
	if instrumental.err != nil || !instrumental.done {
		instrumental.items = []api.SongResponse{}
	}
	if foreignArt.err != nil || !foreignArt.done {
		foreignArt.items = []api.ArtistResponse{}
	}
	if iranianArt.err != nil || !iranianArt.done {
		iranianArt.items = []api.ArtistResponse{}
	}

	complete := newest.done && hiphop.done && iranian.done && turkish.done &&
		instrumental.done && foreignArt.done && iranianArt.done

	return api.HomeFeedResponse{
		Carousel: carousel.items,
		QuickActions: []api.QuickActionResponse{
			{ID: "liked", Title: "Liked", Target: "liked", Icon: "favorite"},
			{ID: "playlists", Title: "Playlists", Target: "playlists", Icon: "playlist"},
			{ID: "recent", Title: "Recent", Target: "recent", Icon: "history"},
			{ID: "following", Title: "Following", Target: "following", Icon: "people"},
		},
		Rows: []api.HomeRowResponse{
			{ID: "popular", Title: "Popular", RowType: "songs", SeeAllPath: "/api/catalog/popular", Items: popular.items},
			{ID: "new", Title: "New Releases", RowType: "songs", SeeAllPath: "/api/catalog/new", Items: newest.items},
			{ID: "iranian", Title: "Iranian", RowType: "songs", SeeAllPath: "/api/songs?category=Iranian", Items: iranian.items},
			{ID: "turkish", Title: "Turkish", RowType: "songs", SeeAllPath: "/api/songs?category=Turkish", Items: turkish.items},
			{ID: "instrumental", Title: "Instrumental", RowType: "songs", SeeAllPath: "/api/songs?category=Instrumental", Items: instrumental.items},
			{ID: "hiphop", Title: "Hip-Hop", RowType: "songs", SeeAllPath: "/api/songs?category=Popular", Items: hiphop.items},
		},
		ArtistRows: []api.HomeArtistRowResponse{
			{ID: "artists_foreign", Title: "Popular Artists", SeeAllPath: "/api/artists?region=foreign", Items: foreignArt.items},
			{ID: "artists_iranian", Title: "Iranian Artists", SeeAllPath: "/api/artists?region=iranian", Items: iranianArt.items},
		},
	}, complete, nil
}
