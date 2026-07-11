package minio

import (
	"context"
	"fmt"

	"melonet-backend/internal/config"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
)

type Client struct {
	client *minio.Client
	bucket string
}

func Connect(ctx context.Context, cfg config.StorageConfig) (*Client, error) {
	client, err := minio.New(cfg.Endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.AccessKey, cfg.SecretKey, ""),
		Secure: cfg.UseSSL,
		Region: cfg.Region,
	})
	if err != nil {
		return nil, fmt.Errorf("create minio client: %w", err)
	}

	storage := &Client{
		client: client,
		bucket: cfg.Bucket,
	}

	if err := storage.ensureBucket(ctx); err != nil {
		return nil, err
	}

	return storage, nil
}

func (c *Client) ensureBucket(ctx context.Context) error {
	exists, err := c.client.BucketExists(ctx, c.bucket)
	if err != nil {
		return fmt.Errorf("check bucket: %w", err)
	}

	if !exists {
		if err := c.client.MakeBucket(ctx, c.bucket, minio.MakeBucketOptions{}); err != nil {
			return fmt.Errorf("create bucket: %w", err)
		}
	}

	return nil
}

func (c *Client) Ping(ctx context.Context) error {
	exists, err := c.client.BucketExists(ctx, c.bucket)
	if err != nil {
		return fmt.Errorf("ping storage: %w", err)
	}
	if !exists {
		return fmt.Errorf("bucket %q does not exist", c.bucket)
	}
	return nil
}

func (c *Client) Raw() *minio.Client {
	return c.client
}

func (c *Client) Bucket() string {
	return c.bucket
}
