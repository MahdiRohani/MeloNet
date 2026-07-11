package handler

import (
	"errors"
	"net/http"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type LibraryHandler struct {
	library *service.LibraryService
}

func NewLibraryHandler(library *service.LibraryService) *LibraryHandler {
	return &LibraryHandler{library: library}
}

func (h *LibraryHandler) ListLiked(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.library.ListLikedSongs(c.Request.Context(), int64(userID), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list liked songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *LibraryHandler) ListRecent(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.library.ListRecentSongs(c.Request.Context(), int64(userID), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list recent songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *LibraryHandler) LikeSong(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	songID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid song id")
		return
	}

	result, err := h.library.LikeSong(c.Request.Context(), int64(userID), songID)
	if err != nil {
		mapLibraryError(c, err)
		return
	}
	response.OK(c, result)
}

func (h *LibraryHandler) UnlikeSong(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	songID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid song id")
		return
	}

	result, err := h.library.UnlikeSong(c.Request.Context(), int64(userID), songID)
	if err != nil {
		mapLibraryError(c, err)
		return
	}
	response.OK(c, result)
}

func (h *LibraryHandler) RecordPlay(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	songID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid song id")
		return
	}

	var req api.PlayEventRequest
	if err := c.ShouldBindJSON(&req); err != nil && c.Request.ContentLength > 0 {
		response.BadRequest(c, "invalid_body", "invalid play event body")
		return
	}

	result, err := h.library.RecordPlay(c.Request.Context(), int64(userID), songID, req)
	if err != nil {
		mapLibraryError(c, err)
		return
	}
	response.OK(c, result)
}

func mapLibraryError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	response.InternalError(c, "library request failed")
}

type PlaylistHandler struct {
	playlists *service.PlaylistService
}

func NewPlaylistHandler(playlists *service.PlaylistService) *PlaylistHandler {
	return &PlaylistHandler{playlists: playlists}
}

func (h *PlaylistHandler) List(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	playlists, meta, err := h.playlists.List(c.Request.Context(), int64(userID), c.Query("scope"), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list playlists")
		return
	}
	response.OKWithMeta(c, playlists, meta)
}

func (h *PlaylistHandler) Get(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	playlistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid playlist id")
		return
	}

	playlist, err := h.playlists.Get(c.Request.Context(), int64(userID), playlistID)
	if err != nil {
		mapPlaylistError(c, err)
		return
	}
	response.OK(c, playlist)
}

func (h *PlaylistHandler) Create(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	var req api.CreatePlaylistRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_body", "invalid playlist body")
		return
	}

	playlist, err := h.playlists.Create(c.Request.Context(), int64(userID), req)
	if err != nil {
		mapPlaylistMutationError(c, err)
		return
	}
	response.Created(c, playlist)
}

func (h *PlaylistHandler) Update(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	playlistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid playlist id")
		return
	}

	var req api.UpdatePlaylistRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_body", "invalid playlist body")
		return
	}

	playlist, err := h.playlists.Update(c.Request.Context(), int64(userID), playlistID, req)
	if err != nil {
		mapPlaylistError(c, err)
		return
	}
	response.OK(c, playlist)
}

func (h *PlaylistHandler) Delete(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	playlistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid playlist id")
		return
	}

	if err := h.playlists.Delete(c.Request.Context(), int64(userID), playlistID); err != nil {
		mapPlaylistError(c, err)
		return
	}
	response.OK(c, gin.H{"deleted": true})
}

func (h *PlaylistHandler) ListSongs(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	playlistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid playlist id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 50)
	songs, meta, err := h.playlists.ListSongs(c.Request.Context(), int64(userID), playlistID, page, limit)
	if err != nil {
		mapPlaylistError(c, err)
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *PlaylistHandler) AddSong(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	playlistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid playlist id")
		return
	}

	var req api.AddPlaylistSongRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.SongID == 0 {
		response.BadRequest(c, "invalid_body", "song_id is required")
		return
	}

	if err := h.playlists.AddSong(c.Request.Context(), int64(userID), playlistID, int64(req.SongID)); err != nil {
		mapPlaylistError(c, err)
		return
	}
	response.Created(c, gin.H{"playlist_id": playlistID, "song_id": req.SongID})
}

func (h *PlaylistHandler) RemoveSong(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	playlistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid playlist id")
		return
	}

	songID, err := parsePathID(c, "songId")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid song id")
		return
	}

	if err := h.playlists.RemoveSong(c.Request.Context(), int64(userID), playlistID, songID); err != nil {
		mapPlaylistError(c, err)
		return
	}
	response.OK(c, gin.H{"playlist_id": playlistID, "song_id": songID, "removed": true})
}

func (h *PlaylistHandler) ReorderSongs(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	playlistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid playlist id")
		return
	}

	var req api.ReorderPlaylistSongsRequest
	if err := c.ShouldBindJSON(&req); err != nil || len(req.SongIDs) == 0 {
		response.BadRequest(c, "invalid_body", "song_ids is required")
		return
	}

	if err := h.playlists.ReorderSongs(c.Request.Context(), int64(userID), playlistID, req.SongIDs); err != nil {
		mapPlaylistMutationError(c, err)
		return
	}
	response.OK(c, gin.H{"playlist_id": playlistID, "reordered": true})
}

func mapPlaylistError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	if errors.Is(err, service.ErrForbidden) {
		response.Error(c, http.StatusForbidden, "forbidden", "you do not have access to this playlist")
		return
	}
	response.InternalError(c, "playlist request failed")
}

func mapPlaylistMutationError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	if errors.Is(err, service.ErrForbidden) {
		response.Error(c, http.StatusForbidden, "forbidden", "you cannot modify this playlist")
		return
	}
	if errors.Is(err, service.ErrInvalidInput) {
		response.BadRequest(c, "invalid_request", err.Error())
		return
	}
	response.BadRequest(c, "invalid_request", err.Error())
}
