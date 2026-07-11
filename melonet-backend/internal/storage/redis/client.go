package redis

import (
	"context"
	"fmt"

	goredis "github.com/redis/go-redis/v9"
)

type Client struct {
	client *goredis.Client
}

func Connect(ctx context.Context, redisURL string) (*Client, error) {
	opts, err := goredis.ParseURL(redisURL)
	if err != nil {
		return nil, fmt.Errorf("parse redis url: %w", err)
	}

	client := goredis.NewClient(opts)
	if err := client.Ping(ctx).Err(); err != nil {
		client.Close()
		return nil, fmt.Errorf("ping redis: %w", err)
	}

	return &Client{client: client}, nil
}

func (c *Client) Close() error {
	if c == nil || c.client == nil {
		return nil
	}
	return c.client.Close()
}

func (c *Client) Ping(ctx context.Context) error {
	return c.client.Ping(ctx).Err()
}

func (c *Client) Raw() *goredis.Client {
	return c.client
}
