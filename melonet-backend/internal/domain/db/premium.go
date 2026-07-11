package db

import "time"

type PremiumEntitlement struct {
	ID        int64
	UserID    int64
	Source    string
	GrantedAt time.Time
	ExpiresAt *time.Time
	RevokedAt *time.Time
	Metadata  []byte
}
