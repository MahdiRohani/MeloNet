package audius

import (
	"strings"

	"melonet-backend/internal/domain/api"
)

func ToSongResponse(publicBase string, track Track) api.SongResponse {
	base := strings.TrimRight(publicBase, "/")
	cover := proxyArtURL(base, track.ID)
	genre := strings.TrimSpace(track.Genre)
	return api.SongResponse{
		ID:            track.ID,
		Title:         track.Title,
		Artist:        track.Artist,
		ArtistName:    track.Artist,
		CoverURL:      cover,
		CoverImageURL: cover,
		AudioURL:      proxyStreamURL(base, track.ID),
		Category:      mapGenreToCategory(genre),
		Genre:         genre,
		DurationSec:   track.DurationSec,
	}
}

func proxyStreamURL(publicBase, trackID string) string {
	return publicBase + "/api/stream/" + trackID
}

func proxyArtURL(publicBase, trackID string) string {
	return publicBase + "/api/art/" + trackID
}

// ArtURL exposes the proxied artwork URL for a track (used when building
// non-song responses such as grouped artists).
func ArtURL(publicBase, trackID string) string {
	return proxyArtURL(strings.TrimRight(publicBase, "/"), trackID)
}

func TracksToAPI(publicBase string, tracks []Track) []api.SongResponse {
	out := make([]api.SongResponse, 0, len(tracks))
	for _, track := range tracks {
		out = append(out, ToSongResponse(publicBase, track))
	}
	return out
}

func mapGenreToCategory(genre string) string {
	switch strings.ToLower(genre) {
	case "electronic", "dance", "house", "techno", "dubstep", "trance":
		return "Global"
	case "hip-hop/rap", "hip hop", "rap", "r&b", "rnb":
		return "Popular"
	case "ambient", "classical", "jazz", "folk", "acoustic":
		return "Nostalgia"
	case "rock", "metal", "punk":
		return "New"
	default:
		if genre == "" {
			return "Popular"
		}
		return "Global"
	}
}
