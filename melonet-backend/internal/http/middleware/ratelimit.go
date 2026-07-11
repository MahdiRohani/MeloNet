package middleware

import (
	"context"
	"fmt"
	"net/http"
	"strconv"
	"sync"
	"time"

	"melonet-backend/internal/config"
	"melonet-backend/internal/http/response"

	"github.com/gin-gonic/gin"
	goredis "github.com/redis/go-redis/v9"
)

type RateLimitStore interface {
	Allow(ctx context.Context, key string, limit int, window time.Duration) (bool, error)
}

type RedisRateLimitStore struct {
	client *goredis.Client
	prefix string
}

func NewRedisRateLimitStore(client *goredis.Client, prefix string) *RedisRateLimitStore {
	if prefix == "" {
		prefix = "ratelimit"
	}
	return &RedisRateLimitStore{client: client, prefix: prefix}
}

func (s *RedisRateLimitStore) Allow(ctx context.Context, key string, limit int, window time.Duration) (bool, error) {
	if s.client == nil {
		return true, nil
	}
	redisKey := fmt.Sprintf("%s:%s", s.prefix, key)
	count, err := s.client.Incr(ctx, redisKey).Result()
	if err != nil {
		return false, err
	}
	if count == 1 {
		if err := s.client.Expire(ctx, redisKey, window).Err(); err != nil {
			return false, err
		}
	}
	return count <= int64(limit), nil
}

type MemoryRateLimitStore struct {
	mu      sync.Mutex
	entries map[string]*rateEntry
}

type rateEntry struct {
	count   int
	resetAt time.Time
}

func NewMemoryRateLimitStore() *MemoryRateLimitStore {
	return &MemoryRateLimitStore{entries: make(map[string]*rateEntry)}
}

func (s *MemoryRateLimitStore) Allow(_ context.Context, key string, limit int, window time.Duration) (bool, error) {
	now := time.Now()
	s.mu.Lock()
	defer s.mu.Unlock()

	entry, ok := s.entries[key]
	if !ok || now.After(entry.resetAt) {
		s.entries[key] = &rateEntry{count: 1, resetAt: now.Add(window)}
		return true, nil
	}

	entry.count++
	return entry.count <= limit, nil
}

type KeyFunc func(*gin.Context) string

func ClientIPKey(c *gin.Context) string {
	return "ip:" + c.ClientIP()
}

func UserIDKey(c *gin.Context) string {
	userID, ok := c.Get("auth_user_id")
	if !ok {
		return ClientIPKey(c)
	}
	return fmt.Sprintf("user:%v", userID)
}

func RateLimit(store RateLimitStore, bucket string, limit int, window time.Duration, keyFunc KeyFunc) gin.HandlerFunc {
	if store == nil || limit <= 0 {
		return func(c *gin.Context) { c.Next() }
	}
	if keyFunc == nil {
		keyFunc = ClientIPKey
	}

	return func(c *gin.Context) {
		key := bucket + ":" + keyFunc(c)
		allowed, err := store.Allow(c.Request.Context(), key, limit, window)
		if err != nil {
			response.InternalError(c, "rate limiter unavailable")
			c.Abort()
			return
		}
		if !allowed {
			c.Header("Retry-After", strconv.Itoa(int(window.Seconds())))
			response.Error(c, http.StatusTooManyRequests, "rate_limited", "too many requests, please retry later")
			c.Abort()
			return
		}
		c.Next()
	}
}

func NewRateLimitStore(redisClient *goredis.Client, cfg config.RateLimitConfig) RateLimitStore {
	if !cfg.Enabled {
		return alwaysAllowStore{}
	}
	if redisClient == nil {
		return NewMemoryRateLimitStore()
	}
	return NewRedisRateLimitStore(redisClient, "melonet")
}

type alwaysAllowStore struct{}

func (alwaysAllowStore) Allow(context.Context, string, int, time.Duration) (bool, error) {
	return true, nil
}
