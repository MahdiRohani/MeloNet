package handler

import (
	"errors"
	"net/http"
	"strings"

	"melonet-backend/internal/auth"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type VoiceCoverHandler struct {
	covers *service.VoiceCoverService
}

func NewVoiceCoverHandler(covers *service.VoiceCoverService) *VoiceCoverHandler {
	return &VoiceCoverHandler{covers: covers}
}

func (h *VoiceCoverHandler) ListArtists(c *gin.Context) {
	artists, err := h.covers.ListArtists(c.Request.Context())
	if err != nil {
		response.InternalError(c, "failed to list voice artists")
		return
	}
	response.OK(c, artists)
}

func (h *VoiceCoverHandler) List(c *gin.Context) {
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	covers, meta, err := h.covers.ListReady(c.Request.Context(), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list voice covers")
		return
	}
	response.OKWithMeta(c, covers, meta)
}

func (h *VoiceCoverHandler) Get(c *gin.Context) {
	id, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid voice cover id")
		return
	}

	cover, err := h.covers.Get(c.Request.Context(), id)
	if err != nil {
		mapVoiceCoverError(c, err)
		return
	}
	response.OK(c, cover)
}

func (h *VoiceCoverHandler) Create(c *gin.Context) {
	userID, err := auth.UserIDFromGin(c)
	if err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	var req api.CreateVoiceCoverRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, "invalid_body", "invalid voice cover body")
		return
	}
	req.SourceSongID = strings.TrimSpace(req.SourceSongID)
	req.TargetArtistSlug = strings.TrimSpace(req.TargetArtistSlug)

	cover, err := h.covers.Create(c.Request.Context(), int64(userID), req)
	if err != nil {
		mapVoiceCoverError(c, err)
		return
	}

	if cover.Status == "ready" {
		response.OK(c, cover)
		return
	}
	response.Created(c, cover)
}

func (h *VoiceCoverHandler) Delete(c *gin.Context) {
	if _, err := auth.UserIDFromGin(c); err != nil {
		response.Error(c, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}

	id, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid voice cover id")
		return
	}

	if err := h.covers.Delete(c.Request.Context(), id); err != nil {
		mapVoiceCoverError(c, err)
		return
	}
	response.OK(c, gin.H{"deleted": true})
}

func mapVoiceCoverError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	if errors.Is(err, service.ErrBadRequest) {
		response.BadRequest(c, "invalid_request", err.Error())
		return
	}
	response.InternalError(c, "voice cover request failed")
}
