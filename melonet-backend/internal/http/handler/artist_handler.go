package handler

import (
	"net/http"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type ArtistHandler struct {
	catalog *service.CatalogService
	follows *service.ArtistFollowService
}

func NewArtistHandler(catalog *service.CatalogService, follows *service.ArtistFollowService) *ArtistHandler {
	return &ArtistHandler{catalog: catalog, follows: follows}
}

func (h *ArtistHandler) Get(c *gin.Context) {
	artistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid artist id")
		return
	}

	artist, err := h.catalog.GetArtist(c.Request.Context(), artistID)
	if err != nil {
		mapCatalogError(c, err)
		return
	}

	if viewerID, err := auth.UserIDFromGin(c); err == nil {
		if following, err := h.follows.IsFollowing(c.Request.Context(), int64(viewerID), artistID); err == nil {
			artist.IsFollowing = following
		}
	}

	response.OK(c, artist)
}

func (h *ArtistHandler) Follow(c *gin.Context) {
	viewerID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}
	artistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid artist id")
		return
	}

	artist, err := h.follows.Follow(c.Request.Context(), int64(viewerID), artistID)
	if err != nil {
		mapCatalogError(c, err)
		return
	}
	response.OK(c, artist)
}

func (h *ArtistHandler) Unfollow(c *gin.Context) {
	viewerID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}
	artistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid artist id")
		return
	}

	artist, err := h.follows.Unfollow(c.Request.Context(), int64(viewerID), artistID)
	if err != nil {
		mapCatalogError(c, err)
		return
	}
	response.OK(c, artist)
}

func (h *ArtistHandler) ListFollowing(c *gin.Context) {
	viewerID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	artists, meta, err := h.follows.ListFollowing(c.Request.Context(), int64(viewerID), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list followed artists")
		return
	}
	response.OKWithMeta(c, artists, meta)
}
