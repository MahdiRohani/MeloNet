package service

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/domain/db"
	"melonet-backend/internal/repository/postgres"
)

type SocialService struct {
	users         *postgres.UserRepository
	follows       *postgres.FollowRepository
	playlists     *postgres.PlaylistRepository
	notifications *postgres.NotificationRepository
}

func NewSocialService(
	users *postgres.UserRepository,
	follows *postgres.FollowRepository,
	playlists *postgres.PlaylistRepository,
	notifications *postgres.NotificationRepository,
) *SocialService {
	return &SocialService{
		users:         users,
		follows:       follows,
		playlists:     playlists,
		notifications: notifications,
	}
}

func (s *SocialService) SearchUsers(ctx context.Context, query string, page, limit int) ([]api.UserSearchResult, domain.Pagination, error) {
	query = strings.TrimSpace(query)
	if query == "" {
		return nil, domain.Pagination{}, fmt.Errorf("empty query")
	}

	users, total, err := s.users.Search(ctx, query, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.UserSummariesToSearchResults(users), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func (s *SocialService) GetPublicProfile(ctx context.Context, viewerID, targetID int64) (api.PublicUserResponse, error) {
	summary, err := s.users.GetPublicSummary(ctx, targetID)
	if err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.PublicUserResponse{}, ErrNotFound
		}
		return api.PublicUserResponse{}, err
	}

	followers, err := s.follows.CountFollowers(ctx, targetID)
	if err != nil {
		return api.PublicUserResponse{}, err
	}
	following, err := s.follows.CountFollowing(ctx, targetID)
	if err != nil {
		return api.PublicUserResponse{}, err
	}

	isFollowing := false
	if viewerID > 0 && viewerID != targetID {
		isFollowing, err = s.follows.IsFollowing(ctx, viewerID, targetID)
		if err != nil {
			return api.PublicUserResponse{}, err
		}
	}

	profile := db.PublicUserProfile{
		UserSummary:    summary,
		FollowerCount:  followers,
		FollowingCount: following,
		IsFollowing:    isFollowing,
	}
	return postgres.PublicUserToAPI(profile, viewerID), nil
}

func (s *SocialService) ListPublicPlaylists(ctx context.Context, viewerID, targetID int64, page, limit int) ([]api.PlaylistResponse, domain.Pagination, error) {
	if _, err := s.users.GetPublicSummary(ctx, targetID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return nil, domain.Pagination{}, ErrNotFound
		}
		return nil, domain.Pagination{}, err
	}

	playlists, total, err := s.playlists.ListPublicByOwner(ctx, targetID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.PlaylistsToAPI(playlists, viewerID), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func (s *SocialService) Follow(ctx context.Context, followerID, targetID int64) (api.FollowResponse, error) {
	if followerID == targetID {
		return api.FollowResponse{}, fmt.Errorf("cannot follow yourself")
	}

	if _, err := s.users.GetPublicSummary(ctx, targetID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return api.FollowResponse{}, ErrNotFound
		}
		return api.FollowResponse{}, err
	}

	created, err := s.follows.Follow(ctx, followerID, targetID)
	if err != nil {
		return api.FollowResponse{}, err
	}

	if created {
		follower, err := s.users.GetPublicSummary(ctx, followerID)
		if err == nil {
			_, _ = s.notifications.Create(ctx, targetID, domain.NotificationFollow, "New follower", fmt.Sprintf("%s started following you.", follower.DisplayName), map[string]any{
				"follower_id":       follower.ID,
				"follower_username": follower.Username,
			})
		}
	}

	count, err := s.follows.CountFollowers(ctx, targetID)
	if err != nil {
		return api.FollowResponse{}, err
	}

	return api.FollowResponse{
		UserID:        uint(targetID),
		Following:     true,
		FollowerCount: count,
	}, nil
}

func (s *SocialService) Unfollow(ctx context.Context, followerID, targetID int64) (api.FollowResponse, error) {
	if err := s.follows.Unfollow(ctx, followerID, targetID); err != nil {
		return api.FollowResponse{}, err
	}

	count, err := s.follows.CountFollowers(ctx, targetID)
	if err != nil {
		return api.FollowResponse{}, err
	}

	return api.FollowResponse{
		UserID:        uint(targetID),
		Following:     false,
		FollowerCount: count,
	}, nil
}

func (s *SocialService) ListFollowers(ctx context.Context, targetID int64, page, limit int) ([]api.PublicUserResponse, domain.Pagination, error) {
	if _, err := s.users.GetPublicSummary(ctx, targetID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return nil, domain.Pagination{}, ErrNotFound
		}
		return nil, domain.Pagination{}, err
	}

	users, total, err := s.follows.ListFollowers(ctx, targetID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.UserSummariesToPublicAPI(users), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func (s *SocialService) ListFollowing(ctx context.Context, targetID int64, page, limit int) ([]api.PublicUserResponse, domain.Pagination, error) {
	if _, err := s.users.GetPublicSummary(ctx, targetID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return nil, domain.Pagination{}, ErrNotFound
		}
		return nil, domain.Pagination{}, err
	}

	users, total, err := s.follows.ListFollowing(ctx, targetID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.UserSummariesToPublicAPI(users), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func (s *SocialService) ListNotifications(ctx context.Context, userID int64, page, limit int, unreadOnly bool) ([]api.NotificationResponse, domain.Pagination, error) {
	notifications, total, err := s.notifications.List(ctx, userID, page, limit, unreadOnly)
	if err != nil {
		return nil, domain.Pagination{}, err
	}

	return postgres.NotificationsToAPI(notifications), domain.Pagination{
		Page:  page,
		Limit: limit,
		Total: total,
	}, nil
}

func (s *SocialService) MarkNotificationRead(ctx context.Context, userID, notificationID int64) error {
	if err := s.notifications.MarkRead(ctx, userID, notificationID); err != nil {
		if errors.Is(err, postgres.ErrNotFound) {
			return ErrNotFound
		}
		return err
	}
	return nil
}

func (s *SocialService) MarkAllNotificationsRead(ctx context.Context, userID int64) (int, error) {
	return s.notifications.MarkAllRead(ctx, userID)
}
