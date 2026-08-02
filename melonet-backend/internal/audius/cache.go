package audius

import (
	"sync"
	"time"
)

// responseCache is a tiny in-process TTL cache for Audius list endpoints.
// Home/search fan-out hits the same keys repeatedly within a session.
type responseCache struct {
	mu      sync.RWMutex
	entries map[string]cacheEntry
}

type cacheEntry struct {
	tracks  []Track
	expires time.Time
}

func newResponseCache() *responseCache {
	return &responseCache{entries: make(map[string]cacheEntry)}
}

func (c *responseCache) get(key string) ([]Track, bool) {
	c.mu.RLock()
	defer c.mu.RUnlock()
	entry, ok := c.entries[key]
	if !ok || time.Now().After(entry.expires) {
		return nil, false
	}
	out := make([]Track, len(entry.tracks))
	copy(out, entry.tracks)
	return out, true
}

func (c *responseCache) set(key string, tracks []Track, ttl time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()
	cloned := make([]Track, len(tracks))
	copy(cloned, tracks)
	c.entries[key] = cacheEntry{tracks: cloned, expires: time.Now().Add(ttl)}
	// Opportunistic prune when map grows.
	if len(c.entries) > 256 {
		now := time.Now()
		for k, v := range c.entries {
			if now.After(v.expires) {
				delete(c.entries, k)
			}
		}
	}
}
