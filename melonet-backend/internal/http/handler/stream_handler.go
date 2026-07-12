package handler

import (
	"io"
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

	upstream, err := h.audius.ProxyGet(ctx, streamURL, c.GetHeader("Range"))
	if err != nil {
		response.InternalError(c, "failed to stream track")
		return
	}
	defer audius.Drain(upstream)

	copyProxyHeaders(c, upstream.Header)
	c.Status(upstream.StatusCode)
	_, _ = io.Copy(c.Writer, upstream.Body)
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

	upstream, err := h.audius.ProxyGet(ctx, artURL, "")
	if err != nil {
		response.InternalError(c, "failed to fetch artwork")
		return
	}
	defer audius.Drain(upstream)

	copyProxyHeaders(c, upstream.Header)
	c.Header("Cache-Control", "public, max-age=86400")
	c.Status(upstream.StatusCode)
	_, _ = io.Copy(c.Writer, upstream.Body)
}

func copyProxyHeaders(c *gin.Context, headers http.Header) {
	for _, key := range []string{"Content-Type", "Content-Length", "Content-Range", "Accept-Ranges"} {
		if value := headers.Get(key); value != "" {
			c.Header(key, value)
		}
	}
}
