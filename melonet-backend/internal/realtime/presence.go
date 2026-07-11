package realtime

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	goredis "github.com/redis/go-redis/v9"

	redisStorage "melonet-backend/internal/storage/redis"
)

const (
	presenceKeyPrefix = "chat:presence:"
	typingKeyPrefix   = "chat:typing:"
	userChannelPrefix = "chat:user:"
	presenceTTL       = 45 * time.Second
	typingTTL         = 5 * time.Second
)

type Presence struct {
	redis *redisStorage.Client
}

func NewPresence(redis *redisStorage.Client) *Presence {
	return &Presence{redis: redis}
}

func (p *Presence) available() bool {
	return p != nil && p.redis != nil
}

func (p *Presence) SetOnline(ctx context.Context, userID uint) error {
	if !p.available() {
		return nil
	}
	key := presenceKeyPrefix + strconv.FormatUint(uint64(userID), 10)
	return p.redis.Raw().Set(ctx, key, "1", presenceTTL).Err()
}

func (p *Presence) Refresh(ctx context.Context, userID uint) error {
	return p.SetOnline(ctx, userID)
}

func (p *Presence) SetOffline(ctx context.Context, userID uint) error {
	if !p.available() {
		return nil
	}
	key := presenceKeyPrefix + strconv.FormatUint(uint64(userID), 10)
	return p.redis.Raw().Del(ctx, key).Err()
}

func (p *Presence) IsOnline(ctx context.Context, userID uint) (bool, error) {
	if !p.available() {
		return false, nil
	}
	key := presenceKeyPrefix + strconv.FormatUint(uint64(userID), 10)
	count, err := p.redis.Raw().Exists(ctx, key).Result()
	if err != nil {
		return false, err
	}
	return count > 0, nil
}

func (p *Presence) SetTyping(ctx context.Context, conversationID, userID uint, active bool) error {
	if !p.available() {
		return nil
	}
	key := fmt.Sprintf("%s%d:%d", typingKeyPrefix, conversationID, userID)
	if !active {
		return p.redis.Raw().Del(ctx, key).Err()
	}
	return p.redis.Raw().Set(ctx, key, "1", typingTTL).Err()
}

func (p *Presence) PublishToUser(ctx context.Context, userID uint, envelope WSEnvelope) error {
	if !p.available() {
		return nil
	}
	payload, err := json.Marshal(envelope)
	if err != nil {
		return err
	}
	channel := userChannelPrefix + strconv.FormatUint(uint64(userID), 10)
	return p.redis.Raw().Publish(ctx, channel, payload).Err()
}

func (p *Presence) UserChannel(userID uint) string {
	return userChannelPrefix + strconv.FormatUint(uint64(userID), 10)
}

func (p *Presence) Subscribe(ctx context.Context, userID uint) *goredis.PubSub {
	if !p.available() {
		return nil
	}
	return p.redis.Raw().Subscribe(ctx, p.UserChannel(userID))
}
