package handler

import (
	"io"
	"net/http"
	"strings"

	"melonet-backend/internal/http/response"
	minioStorage "melonet-backend/internal/storage/minio"

	"github.com/gin-gonic/gin"
)

type MediaHandler struct {
	storage *minioStorage.AvatarStorage
}

func NewMediaHandler(storage *minioStorage.AvatarStorage) *MediaHandler {
	return &MediaHandler{storage: storage}
}

func (h *MediaHandler) Serve(c *gin.Context) {
	objectKey := strings.TrimPrefix(c.Param("object_path"), "/")
	if objectKey == "" || strings.Contains(objectKey, "..") {
		response.BadRequest(c, "invalid_path", "invalid media path")
		return
	}

	object, err := h.storage.Open(c.Request.Context(), objectKey)
	if err != nil {
		response.NotFound(c, "media not found")
		return
	}
	defer object.Close()

	info, err := object.Stat()
	if err != nil {
		response.NotFound(c, "media not found")
		return
	}

	if info.ContentType != "" {
		c.Header("Content-Type", info.ContentType)
	} else {
		c.Header("Content-Type", "application/octet-stream")
	}
	c.Header("Cache-Control", "public, max-age=86400")
	c.Status(http.StatusOK)
	_, _ = io.Copy(c.Writer, object)
}
