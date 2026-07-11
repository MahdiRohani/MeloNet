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
