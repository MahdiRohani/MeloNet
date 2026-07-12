package service

import (
	"context"
	"fmt"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/repository/postgres"
)

func cacheTrackFromAudius(ctx context.Context, client *audius.Client, cache *postgres.SongCacheRepository, songID string) error {
	track, err := client.GetTrack(ctx, songID)
	if err != nil {
		return fmt.Errorf("fetch audius track: %w", err)
	}
	song := trackToCachedSong(client.PublicBase(), track)
	return cache.Upsert(ctx, song)
}

func trackToCachedSong(publicBase string, track audius.Track) postgres.CachedSong {
	resp := audius.ToSongResponse(publicBase, track)
	return postgres.CachedSong{
		AudiusID:    track.ID,
		Title:       resp.Title,
		ArtistName:  resp.ArtistName,
		CoverURL:    resp.CoverURL,
		AudioURL:    resp.AudioURL,
		DurationSec: resp.DurationSec,
		Genre:       resp.Genre,
	}
}
