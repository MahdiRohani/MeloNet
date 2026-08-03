package redis

import (
	"context"
	"encoding/json"
	"fmt"
)

const VoiceCoverJobsKey = "voice_cover:jobs"

type VoiceCoverJob struct {
	CoverID int64 `json:"cover_id"`
}

// EnqueueVoiceCover pushes a cover job onto the Redis list consumed by the voice worker.
func (c *Client) EnqueueVoiceCover(ctx context.Context, coverID int64) error {
	if c == nil || c.client == nil {
		return fmt.Errorf("redis client unavailable")
	}
	payload, err := json.Marshal(VoiceCoverJob{CoverID: coverID})
	if err != nil {
		return fmt.Errorf("marshal voice cover job: %w", err)
	}
	if err := c.client.LPush(ctx, VoiceCoverJobsKey, payload).Err(); err != nil {
		return fmt.Errorf("enqueue voice cover job: %w", err)
	}
	return nil
}
