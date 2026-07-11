package postgres

import (
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
)

func SongToAPI(song db.Song) api.SongResponse {
	artistName := song.ArtistName
	if artistName == "" {
		artistName = "Unknown Artist"
	}

	coverURL := song.CoverURL
	audioURL := song.AudioURL
	genre := song.GenreName
	if genre == "" {
		genre = song.Category
	}

	return api.SongResponse{
		ID:            uint(song.ID),
		Title:         song.Title,
		Artist:        artistName,
		ArtistName:    artistName,
		ArtistID:      uint(song.ArtistID),
		CoverURL:      coverURL,
		CoverImageURL: coverURL,
		AudioURL:      audioURL,
		Category:      song.Category,
		Genre:         genre,
		AlbumTitle:    song.AlbumTitle,
		Lyrics:        song.Lyrics,
		DurationSec:   song.DurationSec,
		PlayCount:     song.PlayCount,
		AlbumID:       uintPtrToUint(song.AlbumID),
		GenreID:       uintPtrToUint(song.GenreID),
	}
}

func uintPtrToUint(value *int64) uint {
	if value == nil {
		return 0
	}
	return uint(*value)
}

func ArtistToAPI(artist db.Artist) api.ArtistResponse {
	return api.ArtistResponse{
		ID:        uint(artist.ID),
		Name:      artist.Name,
		Slug:      artist.Slug,
		Bio:       artist.Bio,
		ImageURL:  artist.ImageURL,
		SongCount: artist.SongCount,
	}
}

func ArtistsToAPI(artists []db.Artist) []api.ArtistResponse {
	result := make([]api.ArtistResponse, 0, len(artists))
	for _, artist := range artists {
		result = append(result, ArtistToAPI(artist))
	}
	return result
}

func AlbumToAPI(album db.Album) api.AlbumResponse {
	return api.AlbumResponse{
		ID:          uint(album.ID),
		Title:       album.Title,
		Slug:        album.Slug,
		CoverURL:    album.CoverURL,
		ArtistID:    uint(album.ArtistID),
		ArtistName:  album.ArtistName,
		ReleaseDate: album.ReleaseDate,
		SongCount:   album.SongCount,
	}
}

func AlbumsToAPI(albums []db.Album) []api.AlbumResponse {
	result := make([]api.AlbumResponse, 0, len(albums))
	for _, album := range albums {
		result = append(result, AlbumToAPI(album))
	}
	return result
}

func GenreToAPI(genre db.Genre) api.GenreResponse {
	return api.GenreResponse{
		ID:        uint(genre.ID),
		Name:      genre.Name,
		Slug:      genre.Slug,
		SongCount: genre.SongCount,
	}
}

func GenresToAPI(genres []db.Genre) []api.GenreResponse {
	result := make([]api.GenreResponse, 0, len(genres))
	for _, genre := range genres {
		result = append(result, GenreToAPI(genre))
	}
	return result
}

func SongsToAPI(songs []db.Song) []api.SongResponse {
	result := make([]api.SongResponse, 0, len(songs))
	for _, song := range songs {
		result = append(result, SongToAPI(song))
	}
	return result
}

func MessageToAPI(message db.Message) api.MessageResponse {
	return api.MessageResponse{
		ID:         uint(message.ID),
		SenderID:   uint(message.SenderID),
		ReceiverID: uint(message.ReceiverID),
		Content:    message.Content,
		MsgType:    string(message.MsgType),
		Status:     string(message.DeliveryStatus),
		CreatedAt:  message.CreatedAt,
	}
}

func MessagesToAPI(messages []db.Message) []api.MessageResponse {
	result := make([]api.MessageResponse, 0, len(messages))
	for _, message := range messages {
		result = append(result, MessageToAPI(message))
	}
	return result
}

func UserToAPI(user db.User, premium db.PremiumEntitlement, active bool) api.UserResponse {
	email := ""
	if user.Email != nil {
		email = *user.Email
	}

	resp := api.UserResponse{
		ID:          uint(user.ID),
		Username:    user.Username,
		Email:       email,
		DisplayName: user.DisplayName,
		AvatarURL:   user.AvatarURL,
		Bio:         user.Bio,
		IsPremium:   active,
		Premium: api.PremiumDTO{
			Active:    active,
			ExpiresAt: user.PremiumUntil,
		},
	}

	if premium.Source != "" {
		resp.Premium.Source = premium.Source
		if premium.ExpiresAt != nil {
			resp.Premium.ExpiresAt = premium.ExpiresAt
		}
	}

	return resp
}
