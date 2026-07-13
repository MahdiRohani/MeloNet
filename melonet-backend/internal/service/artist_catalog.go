package service

import (
	"context"
	"strings"
	"sync"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
)

// Region identifiers for curated artists.
const (
	RegionForeign = "foreign"
	RegionIranian = "iranian"
)

type curatedArtist struct {
	Name   string
	Region string
}

// curatedArtists is a hand-picked roster. Audius stores the real performer name
// in the track TITLE (the user.name is only the uploader), so artist lookups
// match the name against title+uploader rather than uploader alone.
var curatedArtists = []curatedArtist{
	// Foreign
	{"Skrillex", RegionForeign},
	{"Zedd", RegionForeign},
	{"Steve Aoki", RegionForeign},
	{"Marshmello", RegionForeign},
	{"ODESZA", RegionForeign},
	{"Illenium", RegionForeign},
	{"Alina Baraz", RegionForeign},
	{"Porter Robinson", RegionForeign},
	{"Flume", RegionForeign},
	{"San Holo", RegionForeign},
	// Iranian
	{"Ebi", RegionIranian},
	{"Shadmehr Aghili", RegionIranian},
	{"Googoosh", RegionIranian},
	{"Dariush", RegionIranian},
	{"Moein", RegionIranian},
	{"Sirvan Khosravi", RegionIranian},
	{"Mohsen Yeganeh", RegionIranian},
	{"Arash", RegionIranian},
	{"Siavash Ghomayshi", RegionIranian},
	{"Andy", RegionIranian},
}

func normalizeRegion(region string) string {
	switch strings.ToLower(strings.TrimSpace(region)) {
	case "iranian", "persian", "farsi":
		return RegionIranian
	case "foreign", "global", "international":
		return RegionForeign
	default:
		return ""
	}
}

func curatedArtistsByRegion(region string) []curatedArtist {
	region = normalizeRegion(region)
	if region == "" {
		return curatedArtists
	}
	out := make([]curatedArtist, 0)
	for _, a := range curatedArtists {
		if a.Region == region {
			out = append(out, a)
		}
	}
	return out
}

// artistByID reverse-maps a synthetic artist id (fnv hash of the name) back to a
// curated artist.
func artistByID(id int64) (curatedArtist, bool) {
	for _, a := range curatedArtists {
		if int64(artistID(a.Name)) == id {
			return a, true
		}
	}
	return curatedArtist{}, false
}

// ListArtistsByRegion returns curated artists (with representative artwork)
// filtered by region.
func (s *CatalogService) ListArtistsByRegion(ctx context.Context, region string, page, limit int) ([]api.ArtistResponse, domain.Pagination, error) {
	all := curatedArtistsByRegion(region)
	total := len(all)
	start := (page - 1) * limit
	if start < 0 {
		start = 0
	}
	if start >= total {
		return []api.ArtistResponse{}, domain.Pagination{Page: page, Limit: limit, Total: total}, nil
	}
	end := start + limit
	if end > total {
		end = total
	}
	pageArtists := all[start:end]

	out := make([]api.ArtistResponse, len(pageArtists))
	var wg sync.WaitGroup
	for i, a := range pageArtists {
		wg.Add(1)
		go func(i int, a curatedArtist) {
			defer wg.Done()
			out[i] = s.artistResponse(ctx, a)
		}(i, a)
	}
	wg.Wait()

	return out, domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}

// SearchArtists returns curated artists whose name matches the query.
func (s *CatalogService) SearchArtists(ctx context.Context, query string, page, limit int) ([]api.ArtistResponse, int) {
	q := strings.ToLower(strings.TrimSpace(query))
	if q == "" {
		return []api.ArtistResponse{}, 0
	}
	matches := make([]curatedArtist, 0)
	for _, a := range curatedArtists {
		if strings.Contains(strings.ToLower(a.Name), q) {
			matches = append(matches, a)
		}
	}
	total := len(matches)
	start := (page - 1) * limit
	if start < 0 {
		start = 0
	}
	if start >= total {
		return []api.ArtistResponse{}, total
	}
	end := start + limit
	if end > total {
		end = total
	}
	pageArtists := matches[start:end]

	out := make([]api.ArtistResponse, len(pageArtists))
	var wg sync.WaitGroup
	for i, a := range pageArtists {
		wg.Add(1)
		go func(i int, a curatedArtist) {
			defer wg.Done()
			out[i] = s.artistResponse(ctx, a)
		}(i, a)
	}
	wg.Wait()
	return out, total
}

// GetArtistByID returns a single curated artist by synthetic id.
func (s *CatalogService) GetArtistByID(ctx context.Context, id int64) (api.ArtistResponse, error) {
	a, ok := artistByID(id)
	if !ok {
		return api.ArtistResponse{}, ErrNotFound
	}
	return s.artistResponse(ctx, a), nil
}

// ArtistSongsByID returns the songs of a curated artist, ranked/sorted.
func (s *CatalogService) ArtistSongsByID(ctx context.Context, id int64, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	a, ok := artistByID(id)
	if !ok {
		return nil, domain.Pagination{}, ErrNotFound
	}
	tracks := s.artistTracks(ctx, a.Name, 60)
	audius.SortTracks(tracks, sort)
	pageTracks, total := audius.Paginate(tracks, page, limit)
	return audius.TracksToAPI(s.audius.PublicBase(), pageTracks), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

// artistResponse builds an ArtistResponse, using the artwork of the artist's
// most popular track as the (circular) image.
func (s *CatalogService) artistResponse(ctx context.Context, a curatedArtist) api.ArtistResponse {
	tracks := s.artistTracks(ctx, a.Name, 20)
	audius.SortByPopularity(tracks)
	image := ""
	if len(tracks) > 0 {
		image = audius.ArtURL(s.audius.PublicBase(), tracks[0].ID)
	}
	return api.ArtistResponse{
		ID:        artistID(a.Name),
		Name:      a.Name,
		ImageURL:  image,
		SongCount: len(tracks),
		Region:    a.Region,
	}
}

// artistTracks searches Audius for an artist and keeps only tracks that are
// actually theirs (matching the name inside title or uploader).
func (s *CatalogService) artistTracks(ctx context.Context, name string, limit int) []audius.Track {
	tracks, err := s.audius.SearchTracks(ctx, name, limit)
	if err != nil {
		return nil
	}
	filtered := make([]audius.Track, 0, len(tracks))
	for _, t := range tracks {
		if trackMatchesArtist(t, name) {
			filtered = append(filtered, t)
		}
	}
	return audius.DedupeTracks(filtered)
}
