package service

import (
	"context"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/api"
	"melonet-backend/internal/repository/postgres"
)

type ArtistFollowService struct {
	repo    *postgres.ArtistFollowRepository
	catalog *CatalogService
}

func NewArtistFollowService(repo *postgres.ArtistFollowRepository, catalog *CatalogService) *ArtistFollowService {
	return &ArtistFollowService{repo: repo, catalog: catalog}
}

// Follow resolves the artist from the curated catalog (to snapshot name/image)
// and records the follow.
func (s *ArtistFollowService) Follow(ctx context.Context, userID, artistID int64) (api.ArtistResponse, error) {
	artist, err := s.catalog.GetArtistByID(ctx, artistID)
	if err != nil {
		return api.ArtistResponse{}, err
	}
	if _, err := s.repo.Follow(ctx, userID, artistID, artist.Name, artist.ImageURL, artist.Region); err != nil {
		return api.ArtistResponse{}, err
	}
	artist.IsFollowing = true
	return artist, nil
}

func (s *ArtistFollowService) Unfollow(ctx context.Context, userID, artistID int64) (api.ArtistResponse, error) {
	if err := s.repo.Unfollow(ctx, userID, artistID); err != nil {
		return api.ArtistResponse{}, err
	}
	artist, err := s.catalog.GetArtistByID(ctx, artistID)
	if err != nil {
		return api.ArtistResponse{ID: uint(artistID), IsFollowing: false}, nil
	}
	artist.IsFollowing = false
	return artist, nil
}

func (s *ArtistFollowService) IsFollowing(ctx context.Context, userID, artistID int64) (bool, error) {
	return s.repo.IsFollowing(ctx, userID, artistID)
}

func (s *ArtistFollowService) ListFollowing(ctx context.Context, userID int64, page, limit int) ([]api.ArtistResponse, domain.Pagination, error) {
	follows, total, err := s.repo.ListFollowing(ctx, userID, page, limit)
	if err != nil {
		return nil, domain.Pagination{}, err
	}
	out := make([]api.ArtistResponse, 0, len(follows))
	for _, f := range follows {
		out = append(out, api.ArtistResponse{
			ID:          uint(f.ArtistID),
			Name:        f.ArtistName,
			ImageURL:    f.ImageURL,
			Region:      f.Region,
			IsFollowing: true,
		})
	}
	return out, domain.Pagination{Page: page, Limit: limit, Total: total}, nil
}
