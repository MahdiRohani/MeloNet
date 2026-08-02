package minio

import (
	"context"
	"fmt"
	"io"
	"path"
	"strings"

	"github.com/google/uuid"
)

// UploadChatAudio stores a chat-shared audio object under chat/{userID}/...
func (s *MediaStorage) UploadChatAudio(
	ctx context.Context,
	userID int64,
	filename string,
	reader io.Reader,
	size int64,
	contentType string,
) (string, string, error) {
	ext := strings.ToLower(path.Ext(filename))
	if ext == "" {
		ext = ".mp3"
	}

	objectKey := fmt.Sprintf("chat/%d/%s%s", userID, uuid.NewString(), ext)
	if err := s.Upload(ctx, objectKey, reader, size, contentType); err != nil {
		return "", "", err
	}

	return objectKey, s.PublicURL(objectKey), nil
}
