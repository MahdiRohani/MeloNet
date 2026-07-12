package postgres

import (
	"strconv"

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
		ID:            strconv.FormatInt(song.ID, 10),
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

func PlaylistToAPI(playlist db.Playlist, viewerID int64) api.PlaylistResponse {
	return api.PlaylistResponse{
		ID:          uint(playlist.ID),
		OwnerID:     uint(playlist.OwnerID),
		OwnerName:   playlist.OwnerName,
		Title:       playlist.Title,
		Description: playlist.Description,
		Visibility:  string(playlist.Visibility),
		CoverURL:    playlist.CoverURL,
		IsSystem:    playlist.IsSystem,
		IsOwner:     viewerID > 0 && playlist.OwnerID == viewerID,
		SongCount:   playlist.SongCount,
	}
}

func PlaylistsToAPI(playlists []db.Playlist, viewerID int64) []api.PlaylistResponse {
	result := make([]api.PlaylistResponse, 0, len(playlists))
	for _, playlist := range playlists {
		result = append(result, PlaylistToAPI(playlist, viewerID))
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
	resp := api.MessageResponse{
		ID:             uint(message.ID),
		ConversationID: uint(message.ConversationID),
		SenderID:       uint(message.SenderID),
		ReceiverID:     uint(message.ReceiverID),
		Content:        message.Content,
		MsgType:        string(message.MsgType),
		Status:         string(message.DeliveryStatus),
		CreatedAt:      message.CreatedAt,
	}
	if message.SongID != nil {
		resp.SongID = uint(*message.SongID)
	}
	return resp
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

func UserSummaryToSearchResult(user db.UserSummary) api.UserSearchResult {
	return api.UserSearchResult{
		ID:          uint(user.ID),
		Username:    user.Username,
		DisplayName: user.DisplayName,
		AvatarURL:   user.AvatarURL,
		IsPremium:   user.IsPremium,
	}
}

func UserSummariesToSearchResults(users []db.UserSummary) []api.UserSearchResult {
	result := make([]api.UserSearchResult, 0, len(users))
	for _, user := range users {
		result = append(result, UserSummaryToSearchResult(user))
	}
	return result
}

func PublicUserToAPI(profile db.PublicUserProfile, viewerID int64) api.PublicUserResponse {
	return api.PublicUserResponse{
		ID:             uint(profile.ID),
		Username:       profile.Username,
		DisplayName:    profile.DisplayName,
		AvatarURL:      profile.AvatarURL,
		Bio:            profile.Bio,
		IsPremium:      profile.IsPremium,
		FollowerCount:  profile.FollowerCount,
		FollowingCount: profile.FollowingCount,
		IsFollowing:    profile.IsFollowing,
		IsSelf:         viewerID > 0 && profile.ID == viewerID,
	}
}

func UserSummariesToPublicAPI(users []db.UserSummary) []api.PublicUserResponse {
	result := make([]api.PublicUserResponse, 0, len(users))
	for _, user := range users {
		result = append(result, api.PublicUserResponse{
			ID:          uint(user.ID),
			Username:    user.Username,
			DisplayName: user.DisplayName,
			AvatarURL:   user.AvatarURL,
			Bio:         user.Bio,
			IsPremium:   user.IsPremium,
		})
	}
	return result
}

func NotificationToAPI(notification db.Notification) api.NotificationResponse {
	return api.NotificationResponse{
		ID:        uint(notification.ID),
		Type:      string(notification.Type),
		Title:     notification.Title,
		Body:      notification.Body,
		Read:      notification.ReadAt != nil,
		CreatedAt: notification.CreatedAt,
	}
}

func NotificationsToAPI(notifications []db.Notification) []api.NotificationResponse {
	result := make([]api.NotificationResponse, 0, len(notifications))
	for _, notification := range notifications {
		result = append(result, NotificationToAPI(notification))
	}
	return result
}
