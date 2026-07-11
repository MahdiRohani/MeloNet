package minio

import (
	"context"
	"fmt"
	"io"
	"path"
	"strings"

	"github.com/google/uuid"
	"github.com/minio/minio-go/v7"
)

func (c *Client) Upload(ctx context.Context, objectKey string, reader io.Reader, size int64, contentType string) error {
	_, err := c.client.PutObject(ctx, c.bucket, objectKey, reader, size, minio.PutObjectOptions{
		ContentType: contentType,
	})
	if err != nil {
		return fmt.Errorf("put object: %w", err)
	}
	return nil
}

func (c *Client) Open(ctx context.Context, objectKey string) (*minio.Object, error) {
	object, err := c.client.GetObject(ctx, c.bucket, objectKey, minio.GetObjectOptions{})
	if err != nil {
		return nil, fmt.Errorf("get object: %w", err)
	}
	return object, nil
}

type AvatarStorage struct {
	client     *Client
	publicBase string
}

func NewAvatarStorage(client *Client, publicBase string) *AvatarStorage {
	return &AvatarStorage{
		client:     client,
		publicBase: strings.TrimRight(publicBase, "/"),
	}
}

func (s *AvatarStorage) UploadAvatar(
	ctx context.Context,
	userID int64,
	filename string,
	reader io.Reader,
	size int64,
	contentType string,
) (string, string, error) {
	ext := strings.ToLower(path.Ext(filename))
	if ext == "" {
		ext = ".jpg"
	}

	objectKey := fmt.Sprintf("avatars/%d/%s%s", userID, uuid.NewString(), ext)
	if err := s.client.Upload(ctx, objectKey, reader, size, contentType); err != nil {
		return "", "", err
	}

	publicURL := fmt.Sprintf("%s/api/media/%s", s.publicBase, objectKey)
	return objectKey, publicURL, nil
}

func (s *AvatarStorage) Open(ctx context.Context, objectKey string) (*minio.Object, error) {
	return s.client.Open(ctx, objectKey)
}
