package service

import "testing"

func TestNormalizeSearchType(t *testing.T) {
	if NormalizeSearchType("SONGS") != "song" {
		t.Fatal("expected song")
	}
	if NormalizeSearchType("artists") != "artist" {
		t.Fatal("expected artist")
	}
	if NormalizeSearchType("users") != "user" {
		t.Fatal("expected user")
	}
	if NormalizeSearchType("") != "all" {
		t.Fatal("expected all")
	}
}
