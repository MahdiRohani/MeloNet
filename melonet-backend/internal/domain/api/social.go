package api

type PublicUserResponse struct {
	ID             uint   `json:"id"`
	Username       string `json:"username"`
	DisplayName    string `json:"display_name"`
	AvatarURL      string `json:"avatar_url"`
	Bio            string `json:"bio,omitempty"`
	IsPremium      bool   `json:"is_premium"`
	FollowerCount  int    `json:"follower_count"`
	FollowingCount int    `json:"following_count"`
	IsFollowing    bool   `json:"is_following"`
	IsSelf         bool   `json:"is_self"`
}

type UserSearchResult struct {
	ID          uint   `json:"id"`
	Username    string `json:"username"`
	DisplayName string `json:"display_name"`
	AvatarURL   string `json:"avatar_url"`
	IsPremium   bool   `json:"is_premium"`
}

type FollowResponse struct {
	UserID        uint `json:"user_id"`
	Following     bool `json:"following"`
	FollowerCount int  `json:"follower_count"`
}

type MarkNotificationsReadRequest struct {
	IDs []uint `json:"ids"`
}
