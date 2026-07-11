package handler

import (
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"

	"melonet-backend/internal/http/response"
	minioStorage "melonet-backend/internal/storage/minio"

	"github.com/gin-gonic/gin"
	"github.com/minio/minio-go/v7"
)

type MediaHandler struct {
	storage *minioStorage.MediaStorage
}

func NewMediaHandler(storage *minioStorage.MediaStorage) *MediaHandler {
	return &MediaHandler{storage: storage}
}

func (h *MediaHandler) Serve(c *gin.Context) {
	objectKey := strings.TrimPrefix(c.Param("object_path"), "/")
	if objectKey == "" || strings.Contains(objectKey, "..") {
		response.BadRequest(c, "invalid_path", "invalid media path")
		return
	}

	ctx := c.Request.Context()
	info, err := h.storage.Stat(ctx, objectKey)
	if err != nil {
		response.NotFound(c, "media not found")
		return
	}

	contentType := info.ContentType
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	c.Header("Content-Type", contentType)
	c.Header("Cache-Control", "public, max-age=86400")
	c.Header("Accept-Ranges", "bytes")

	rangeHeader := c.GetHeader("Range")
	if rangeHeader != "" {
		h.serveRange(c, objectKey, info, rangeHeader)
		return
	}

	c.Header("Content-Length", strconv.FormatInt(info.Size, 10))
	c.Status(http.StatusOK)

	object, err := h.storage.Open(ctx, objectKey, minio.GetObjectOptions{})
	if err != nil {
		response.NotFound(c, "media not found")
		return
	}
	defer object.Close()

	_, _ = io.Copy(c.Writer, object)
}

func (h *MediaHandler) serveRange(c *gin.Context, objectKey string, info minio.ObjectInfo, rangeHeader string) {
	start, end, err := parseRange(rangeHeader, info.Size)
	if err != nil {
		c.Header("Content-Range", fmt.Sprintf("bytes */%d", info.Size))
		c.Status(http.StatusRequestedRangeNotSatisfiable)
		return
	}

	length := end - start + 1
	opts := minio.GetObjectOptions{}
	opts.SetRange(start, end)

	object, err := h.storage.Open(c.Request.Context(), objectKey, opts)
	if err != nil {
		response.NotFound(c, "media not found")
		return
	}
	defer object.Close()

	c.Header("Content-Range", fmt.Sprintf("bytes %d-%d/%d", start, end, info.Size))
	c.Header("Content-Length", strconv.FormatInt(length, 10))
	c.Status(http.StatusPartialContent)

	_, _ = io.Copy(c.Writer, object)
}

func parseRange(rangeHeader string, size int64) (int64, int64, error) {
	if !strings.HasPrefix(rangeHeader, "bytes=") {
		return 0, 0, fmt.Errorf("unsupported range unit")
	}

	spec := strings.TrimPrefix(rangeHeader, "bytes=")
	if strings.Contains(spec, ",") {
		return 0, 0, fmt.Errorf("multiple ranges not supported")
	}

	parts := strings.Split(spec, "-")
	if len(parts) != 2 {
		return 0, 0, fmt.Errorf("invalid range")
	}

	var start, end int64
	if parts[0] == "" {
		suffix, err := strconv.ParseInt(parts[1], 10, 64)
		if err != nil || suffix <= 0 {
			return 0, 0, fmt.Errorf("invalid suffix range")
		}
		start = size - suffix
		if start < 0 {
			start = 0
		}
		end = size - 1
	} else {
		var err error
		start, err = strconv.ParseInt(parts[0], 10, 64)
		if err != nil || start < 0 || start >= size {
			return 0, 0, fmt.Errorf("invalid start")
		}
		if parts[1] == "" {
			end = size - 1
		} else {
			end, err = strconv.ParseInt(parts[1], 10, 64)
			if err != nil || end < start {
				return 0, 0, fmt.Errorf("invalid end")
			}
			if end >= size {
				end = size - 1
			}
		}
	}

	return start, end, nil
}
