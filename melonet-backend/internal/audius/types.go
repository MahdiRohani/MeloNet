package audius

// Track is the subset of Audius track metadata used at runtime.
type Track struct {
	ID          string
	Title       string
	Artist      string
	Genre       string
	Mood        string
	CoverURL    string
	ThumbURL    string
	DurationSec int
}
