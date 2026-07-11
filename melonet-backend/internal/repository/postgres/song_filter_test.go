package postgres

import (
	"strings"
	"testing"
)

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

func TestBuildSongListQuery(t *testing.T) {
	genreID := int64(3)

	// With a WHERE filter: category placeholder is $1, limit/offset $2/$3.
	filtered := SongFilter{Category: "Global", GenreID: &genreID}.normalized()
	where, args := filtered.whereClause(1)
	limitPos, offsetPos := paginationPlaceholders(len(args))
	q := buildSongListQuery(where, filtered.orderClause(), limitPos, offsetPos)

	if strings.Contains(q, "$1ORDER") || strings.Contains(q, "$2ORDER") {
		t.Fatalf("WHERE and ORDER BY fused without a space:\n%s", q)
	}
	if !strings.Contains(q, "LIMIT $3 OFFSET $4") {
		t.Fatalf("expected LIMIT $3 OFFSET $4, got:\n%s", q)
	}

	// Without any filter: limit/offset must be $1/$2 and remain valid SQL.
	empty := SongFilter{}.normalized()
	whereEmpty, argsEmpty := empty.whereClause(1)
	lp, op := paginationPlaceholders(len(argsEmpty))
	qEmpty := buildSongListQuery(whereEmpty, empty.orderClause(), lp, op)
	if !strings.Contains(qEmpty, "LIMIT $1 OFFSET $2") {
		t.Fatalf("expected LIMIT $1 OFFSET $2 for unfiltered query, got:\n%s", qEmpty)
	}
	if strings.Contains(qEmpty, "$-1") || strings.Contains(qEmpty, "$0") {
		t.Fatalf("invalid placeholder in unfiltered query:\n%s", qEmpty)
	}
}

func TestPaginationPlaceholders(t *testing.T) {
	cases := []struct {
		argCount           int
		wantLimit, wantOff int
	}{
		{0, 1, 2}, // no WHERE filters — the case that previously produced $-1/$0
		{1, 2, 3},
		{3, 4, 5},
	}
	for _, tc := range cases {
		limitPos, offsetPos := paginationPlaceholders(tc.argCount)
		if limitPos != tc.wantLimit || offsetPos != tc.wantOff {
			t.Fatalf("paginationPlaceholders(%d) = ($%d, $%d), want ($%d, $%d)",
				tc.argCount, limitPos, offsetPos, tc.wantLimit, tc.wantOff)
		}
		if limitPos < 1 || offsetPos < 1 {
			t.Fatalf("placeholders must be >= 1, got ($%d, $%d)", limitPos, offsetPos)
		}
	}
}
