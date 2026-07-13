package service

import (
	"context"
	"strings"
	"sync"

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

// Curated seeds for region/theme categories. Audius has no native
// "Iranian"/"Turkish" genres, so we approximate by aggregating the catalogs of
// well-known artists and ranking the result by real popularity (plays).
var (
	iranianArtists = []string{
		"Googoosh", "Ebi", "Dariush", "Moein", "Shadmehr Aghili",
		"Sirvan Khosravi", "Mohsen Yeganeh", "Arash",
	}
	turkishArtists = []string{
		"Tarkan", "Sezen Aksu", "Sertab Erener", "Mabel Matiz",
		"Edis", "Murat Boz", "Hadise",
	}
	// Instrumental / no-vocal ("بی‌کلام") leans on largely-instrumental genres.
	instrumentalGenres = []string{"Classical", "Ambient", "Lo-Fi", "Jazz"}
)

func (s *CatalogService) ListSongs(ctx context.Context, query SongListQuery) ([]api.SongResponse, domain.Pagination, error) {
	return s.CategorySongs(ctx, query.Category, query.Sort, query.Page, query.Limit)
}

func (s *CatalogService) GetSong(ctx context.Context, songID string) (api.SongResponse, error) {
	track, err := s.audius.GetTrack(ctx, songID)
	if err != nil {
		return api.SongResponse{}, ErrNotFound
	}
	return audius.ToSongResponse(s.audius.PublicBase(), track), nil
}

// Popular = most-played of all time (genuinely popular, not just this week).
func (s *CatalogService) Popular(ctx context.Context, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.trendingPage(ctx, "", "allTime", sort, page, limit)
}

// Newest = this week's trending, which biases toward fresh/rising tracks.
func (s *CatalogService) Newest(ctx context.Context, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	if strings.TrimSpace(sort) == "" {
		sort = audius.SortNewest
	}
	return s.trendingPage(ctx, "", "week", sort, page, limit)
}

func (s *CatalogService) Trending(ctx context.Context, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.trendingPage(ctx, "", "week", sort, page, limit)
}

func (s *CatalogService) CategorySongs(ctx context.Context, category, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	switch normalizeCategory(category) {
	case "iranian":
		return s.IranianSongs(ctx, sort, page, limit)
	case "turkish":
		return s.TurkishSongs(ctx, sort, page, limit)
	case "instrumental":
		return s.InstrumentalSongs(ctx, sort, page, limit)
	}
	genre, timeRange := categoryToTrending(category, sort)
	return s.trendingPage(ctx, genre, timeRange, sort, page, limit)
}

// IranianSongs aggregates well-known Iranian artists' catalogs.
func (s *CatalogService) IranianSongs(ctx context.Context, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.curatedArtistsPage(ctx, iranianArtists, sort, page, limit)
}

// TurkishSongs aggregates well-known Turkish artists' catalogs.
func (s *CatalogService) TurkishSongs(ctx context.Context, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.curatedArtistsPage(ctx, turkishArtists, sort, page, limit)
}

// InstrumentalSongs aggregates popular tracks from largely-instrumental genres.
func (s *CatalogService) InstrumentalSongs(ctx context.Context, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	tracks := s.gatherTrendingGenres(ctx, instrumentalGenres, "allTime", 30)
	songs, pagination := s.rankedPage(tracks, sort, page, limit)
	return songs, pagination, nil
}

// curatedArtistsPage searches each artist's catalog, keeps only tracks actually
// by that artist, then ranks/sorts the merged set.
func (s *CatalogService) curatedArtistsPage(ctx context.Context, artists []string, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	tracks := s.gatherArtistTracks(ctx, artists, 25)
	songs, pagination := s.rankedPage(tracks, sort, page, limit)
	return songs, pagination, nil
}

// rankedPage dedupes, sorts by the given key, paginates and maps to API responses.
func (s *CatalogService) rankedPage(tracks []audius.Track, sort string, page, limit int) ([]api.SongResponse, domain.Pagination) {
	tracks = audius.DedupeTracks(tracks)
	audius.SortTracks(tracks, sort)
	pageTracks, total := audius.Paginate(tracks, page, limit)
	return audius.TracksToAPI(s.audius.PublicBase(), pageTracks), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}
}

func (s *CatalogService) gatherArtistTracks(ctx context.Context, artists []string, perArtist int) []audius.Track {
	var (
		mu  sync.Mutex
		wg  sync.WaitGroup
		all []audius.Track
	)
	for _, artist := range artists {
		wg.Add(1)
		go func(artist string) {
			defer wg.Done()
			tracks, err := s.audius.SearchTracks(ctx, artist, perArtist)
			if err != nil {
				return
			}
			filtered := filterTracksByArtist(tracks, artist)
			mu.Lock()
			all = append(all, filtered...)
			mu.Unlock()
		}(artist)
	}
	wg.Wait()
	return all
}

func (s *CatalogService) gatherTrendingGenres(ctx context.Context, genres []string, timeRange string, perGenre int) []audius.Track {
	var (
		mu  sync.Mutex
		wg  sync.WaitGroup
		all []audius.Track
	)
	for _, genre := range genres {
		wg.Add(1)
		go func(genre string) {
			defer wg.Done()
			tracks, err := s.audius.Trending(ctx, genre, timeRange, perGenre)
			if err != nil {
				return
			}
			mu.Lock()
			all = append(all, tracks...)
			mu.Unlock()
		}(genre)
	}
	wg.Wait()
	return all
}

// filterTracksByArtist keeps only tracks that actually belong to the artist.
func filterTracksByArtist(tracks []audius.Track, artist string) []audius.Track {
	out := make([]audius.Track, 0, len(tracks))
	for _, t := range tracks {
		if trackMatchesArtist(t, artist) {
			out = append(out, t)
		}
	}
	return out
}

// trackMatchesArtist reports whether a track belongs to the given artist. On
// Audius the real performer is usually in the TITLE (e.g. "Ebi - Goriz"), while
// user.name is only the uploader ("Greatest Hits"), so we match against both.
func trackMatchesArtist(t audius.Track, artist string) bool {
	name := strings.ToLower(strings.TrimSpace(artist))
	if name == "" {
		return false
	}
	hay := strings.ToLower(t.Title + " " + t.Artist)
	if strings.Contains(hay, name) {
		return true
	}
	// Fall back to matching the most distinctive (last) token, e.g. "aghili".
	tokens := strings.Fields(name)
	if len(tokens) > 0 {
		last := tokens[len(tokens)-1]
		if len(last) >= 4 && strings.Contains(hay, last) {
			return true
		}
	}
	return false
}

func normalizeCategory(category string) string {
	switch strings.ToLower(strings.TrimSpace(category)) {
	case "iranian", "persian", "farsi":
		return "iranian"
	case "turkish", "turkey", "türkçe":
		return "turkish"
	case "instrumental", "no-vocal", "bikalam", "bi-kalam":
		return "instrumental"
	default:
		return strings.ToLower(strings.TrimSpace(category))
	}
}

// trendingPage fetches Audius trending and applies a user-facing sort key.
func (s *CatalogService) trendingPage(ctx context.Context, genre, timeRange, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
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

	audius.SortTracks(tracks, sort)

	pageTracks, total := audius.Paginate(tracks, page, limit)
	return audius.TracksToAPI(s.audius.PublicBase(), pageTracks), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

// ListArtists returns curated artists, optionally filtered by ?region=.
func (s *CatalogService) ListArtists(ctx context.Context, region string, page, limit int) ([]api.ArtistResponse, domain.Pagination, error) {
	return s.ListArtistsByRegion(ctx, region, page, limit)
}

func (s *CatalogService) GetArtist(ctx context.Context, artistID int64) (api.ArtistResponse, error) {
	return s.GetArtistByID(ctx, artistID)
}

func (s *CatalogService) ListArtistSongs(ctx context.Context, artistID int64, sort string, page, limit int) ([]api.SongResponse, domain.Pagination, error) {
	return s.ArtistSongsByID(ctx, artistID, sort, page, limit)
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
	case "popular":
		genre = "Hip-Hop/Rap"
	case "nostalgia":
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
		timeRange = "allTime"
	}
	return genre, timeRange
}
