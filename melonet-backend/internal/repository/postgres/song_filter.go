package postgres

import (
	"fmt"
	"strings"
)

type SongFilter struct {
	Category string
	GenreID  *int64
	ArtistID *int64
	AlbumID  *int64
	Sort     string
	Page     int
	Limit    int
}

func (f SongFilter) normalized() SongFilter {
	if f.Page < 1 {
		f.Page = 1
	}
	if f.Limit < 1 {
		f.Limit = 20
	}
	if f.Limit > 100 {
		f.Limit = 100
	}
	f.Sort = NormalizeSongSort(f.Sort)
	return f
}

func NormalizeSongSort(sort string) string {
	switch strings.ToLower(strings.TrimSpace(sort)) {
	case "popular":
		return "popular"
	case "newest", "new":
		return "newest"
	case "title":
		return "title"
	case "trending":
		return "trending"
	default:
		return "newest"
	}
}

func (f SongFilter) orderClause() string {
	switch f.Sort {
	case "popular":
		return "ORDER BY s.play_count DESC, s.id DESC"
	case "newest":
		return "ORDER BY s.published_at DESC NULLS LAST, s.id DESC"
	case "title":
		return "ORDER BY s.title ASC, s.id ASC"
	case "trending":
		return "ORDER BY s.play_count DESC, s.published_at DESC NULLS LAST, s.id DESC"
	default:
		return "ORDER BY s.id ASC"
	}
}

func (f SongFilter) whereClause(startIndex int) (string, []any) {
	clauses := make([]string, 0, 4)
	args := make([]any, 0, 4)
	index := startIndex

	if f.Category != "" {
		clauses = append(clauses, fmt.Sprintf("s.category = $%d", index))
		args = append(args, f.Category)
		index++
	}
	if f.GenreID != nil {
		clauses = append(clauses, fmt.Sprintf("s.genre_id = $%d", index))
		args = append(args, *f.GenreID)
		index++
	}
	if f.ArtistID != nil {
		clauses = append(clauses, fmt.Sprintf("s.artist_id = $%d", index))
		args = append(args, *f.ArtistID)
		index++
	}
	if f.AlbumID != nil {
		clauses = append(clauses, fmt.Sprintf("s.album_id = $%d", index))
		args = append(args, *f.AlbumID)
		index++
	}

	if len(clauses) == 0 {
		return "", args
	}

	return " WHERE " + strings.Join(clauses, " AND "), args
}
