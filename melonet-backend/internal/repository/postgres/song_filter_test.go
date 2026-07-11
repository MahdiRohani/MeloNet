package postgres

import "testing"

func TestNormalizeSongSort(t *testing.T) {
	cases := map[string]string{
		"popular":  "popular",
		"NEWEST":   "newest",
		"new":      "newest",
		"title":    "title",
		"trending": "trending",
		"":         "newest",
	}

	for input, expected := range cases {
		if got := NormalizeSongSort(input); got != expected {
			t.Fatalf("NormalizeSongSort(%q) = %q, want %q", input, got, expected)
		}
	}
}

func TestSongFilterWhereClause(t *testing.T) {
	genreID := int64(3)
	filter := SongFilter{Category: "Global", GenreID: &genreID}.normalized()
	where, args := filter.whereClause(1)

	if where == "" || len(args) != 2 {
		t.Fatalf("where=%q args=%v", where, args)
	}
}
