package minio

import (
	"context"
	"fmt"
	"io"
)

// UploadVoiceCover stores a finished voice-cover audio object under voice-covers/.
func (s *MediaStorage) UploadVoiceCover(
	ctx context.Context,
	coverID int64,
	reader io.Reader,
	size int64,
	contentType string,
) (string, string, error) {
	if contentType == "" {
		contentType = "audio/mpeg"
	}
	objectKey := fmt.Sprintf("voice-covers/%d.mp3", coverID)
	if err := s.Upload(ctx, objectKey, reader, size, contentType); err != nil {
		return "", "", err
	}
	return objectKey, s.PublicURL(objectKey), nil
}
