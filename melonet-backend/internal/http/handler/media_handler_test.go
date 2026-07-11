package handler

import "testing"

func TestParseRangeFull(t *testing.T) {
	start, end, err := parseRange("bytes=0-", 1000)
	if err != nil {
		t.Fatalf("parseRange error: %v", err)
	}
	if start != 0 || end != 999 {
		t.Fatalf("range = %d-%d, want 0-999", start, end)
	}
}

func TestParseRangeSuffix(t *testing.T) {
	start, end, err := parseRange("bytes=-500", 1000)
	if err != nil {
		t.Fatalf("parseRange error: %v", err)
	}
	if start != 500 || end != 999 {
		t.Fatalf("range = %d-%d, want 500-999", start, end)
	}
}

func TestParseRangeExplicit(t *testing.T) {
	start, end, err := parseRange("bytes=100-199", 1000)
	if err != nil {
		t.Fatalf("parseRange error: %v", err)
	}
	if start != 100 || end != 199 {
		t.Fatalf("range = %d-%d, want 100-199", start, end)
	}
}

func TestParseRangeInvalid(t *testing.T) {
	_, _, err := parseRange("bytes=1000-2000", 500)
	if err == nil {
		t.Fatal("expected error for out-of-range start")
	}
}
