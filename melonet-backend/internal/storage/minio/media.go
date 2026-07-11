package minio

import (
	"context"
	"fmt"
	"io"
	"strings"

	"github.com/minio/minio-go/v7"
)

type MediaStorage struct {
	client     *Client
	publicBase string
}

func NewMediaStorage(client *Client, publicBase string) *MediaStorage {
	return &MediaStorage{
		client:     client,
		publicBase: strings.TrimRight(publicBase, "/"),
	}
}

func (s *MediaStorage) PublicURL(objectKey string) string {
	return fmt.Sprintf("%s/api/media/%s", s.publicBase, objectKey)
}

func (s *MediaStorage) Upload(ctx context.Context, objectKey string, reader io.Reader, size int64, contentType string) error {
	return s.client.Upload(ctx, objectKey, reader, size, contentType)
}

func (s *MediaStorage) Open(ctx context.Context, objectKey string, opts minio.GetObjectOptions) (*minio.Object, error) {
	object, err := s.client.Raw().GetObject(ctx, s.client.Bucket(), objectKey, opts)
	if err != nil {
		return nil, fmt.Errorf("get object: %w", err)
	}
	return object, nil
}

func (s *MediaStorage) Stat(ctx context.Context, objectKey string) (minio.ObjectInfo, error) {
	info, err := s.client.Raw().StatObject(ctx, s.client.Bucket(), objectKey, minio.StatObjectOptions{})
	if err != nil {
		return minio.ObjectInfo{}, fmt.Errorf("stat object: %w", err)
	}
	return info, nil
}

func (s *MediaStorage) Exists(ctx context.Context, objectKey string) (bool, error) {
	_, err := s.Stat(ctx, objectKey)
	if err == nil {
		return true, nil
	}
	errResp := minio.ToErrorResponse(err)
	if errResp.Code == "NoSuchKey" {
		return false, nil
	}
	return false, err
}
