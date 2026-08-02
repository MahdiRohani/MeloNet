package handler

import (
	"net/http"
	"strings"

	"melonet-backend/internal/audius"
	"melonet-backend/internal/http/response"

	"github.com/gin-gonic/gin"
)

type StreamHandler struct {
	audius *audius.Client
}

func NewStreamHandler(client *audius.Client) *StreamHandler {
	return &StreamHandler{audius: client}
}

func (h *StreamHandler) Stream(c *gin.Context) {
	trackID := strings.TrimSpace(c.Param("id"))
	if trackID == "" {
		response.BadRequest(c, "invalid_id", "invalid track id")
		return
	}

	ctx := c.Request.Context()
	streamURL, err := h.audius.StreamURL(ctx, trackID)
	if err != nil {
		response.InternalError(c, "failed to resolve stream")
		return
	}

	// Redirect client straight to Audius CDN — avoids double-hop proxy latency.
	c.Redirect(http.StatusFound, streamURL)
}

func (h *StreamHandler) Artwork(c *gin.Context) {
	trackID := strings.TrimSpace(c.Param("id"))
	if trackID == "" {
		response.BadRequest(c, "invalid_id", "invalid track id")
		return
	}

	ctx := c.Request.Context()
	artURL, err := h.audius.ArtworkURL(ctx, trackID)
	if err != nil {
		response.NotFound(c, "artwork not found")
		return
	}

	c.Header("Cache-Control", "public, max-age=86400")
	c.Redirect(http.StatusFound, artURL)
}
