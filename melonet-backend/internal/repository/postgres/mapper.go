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
		CoverURL:      coverURL,
		CoverImageURL: coverURL,
		AudioURL:      audioURL,
		Category:      song.Category,
		Genre:         genre,
		AlbumTitle:    song.AlbumTitle,
		Lyrics:        song.Lyrics,
		DurationSec:   song.DurationSec,
		PlayCount:     song.PlayCount,
	}
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
