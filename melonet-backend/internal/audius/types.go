package audius

// Track is the subset of Audius track metadata used at runtime.
type Track struct {
	ID            string
	Title         string
	Artist        string
	Genre         string
	Mood          string
	CoverURL      string
	ThumbURL      string
	DurationSec   int
	PlayCount     int
	RepostCount   int
	FavoriteCount int
	ReleaseDate   string
}

// Popularity is a single relevance score used to rank tracks. Plays dominate,
// with reposts/favorites as secondary signals of how well-liked a track is.
func (t Track) Popularity() int {
	return t.PlayCount + (t.RepostCount+t.FavoriteCount)*5
}
