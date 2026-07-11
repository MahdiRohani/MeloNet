package service

import "testing"

func TestParsePlaylistVisibility(t *testing.T) {
	tests := []struct {
		input   string
		want    string
		wantErr bool
	}{
		{"", "private", false},
		{"private", "private", false},
		{"public", "public", false},
		{"PUBLIC", "public", false},
		{"hidden", "", true},
	}

	for _, tc := range tests {
		got, err := parsePlaylistVisibility(tc.input)
		if tc.wantErr {
			if err == nil {
				t.Fatalf("input %q: expected error", tc.input)
			}
			continue
		}
		if err != nil {
			t.Fatalf("input %q: unexpected error: %v", tc.input, err)
		}
		if string(got) != tc.want {
			t.Fatalf("input %q: got %q, want %q", tc.input, got, tc.want)
		}
	}
}
