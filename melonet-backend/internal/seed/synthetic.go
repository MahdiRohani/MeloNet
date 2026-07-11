package seed

// Synthetic audio generation used when real audio sources are unreachable
// (e.g. GitHub / SoundHelix blocked or throttled). It produces a structurally
// valid MPEG-1 Layer III file made of silent frames so the whole media
// pipeline (upload, HTTP Range streaming, ExoPlayer playback and seek) can be
// exercised without any network access.

const (
	// MPEG-1 Layer III, 64 kbps, 44.1 kHz, mono, no CRC.
	// Header bytes: 0xFF 0xFB 0x50 0xC0
	syntheticFrameHeader0 = 0xFF
	syntheticFrameHeader1 = 0xFB
	syntheticFrameHeader2 = 0x50
	syntheticFrameHeader3 = 0xC0

	syntheticBitrate    = 64000
	syntheticSampleRate = 44100
	syntheticSamples    = 1152 // samples per MPEG-1 Layer III frame
)

// syntheticFrameSize is the byte length of one frame at the chosen bitrate.
// size = floor(144 * bitrate / sampleRate) (+padding, which we keep at 0).
const syntheticFrameSize = 144 * syntheticBitrate / syntheticSampleRate

// SyntheticMP3 builds a silent but valid MP3 of approximately durationSec seconds.
func SyntheticMP3(durationSec int) []byte {
	if durationSec <= 0 {
		durationSec = 180
	}

	framesPerSecond := float64(syntheticSampleRate) / float64(syntheticSamples)
	frameCount := int(framesPerSecond*float64(durationSec)) + 1

	frame := make([]byte, syntheticFrameSize)
	frame[0] = syntheticFrameHeader0
	frame[1] = syntheticFrameHeader1
	frame[2] = syntheticFrameHeader2
	frame[3] = syntheticFrameHeader3
	// Remaining bytes stay zero: zeroed side-info + main data decode to silence.

	out := make([]byte, 0, frameCount*syntheticFrameSize)
	for i := 0; i < frameCount; i++ {
		out = append(out, frame...)
	}
	return out
}

// syntheticDurationForTrack returns a deterministic, varied duration per track
// so the seeded catalog does not have identical lengths.
func syntheticDurationForTrack(track Track) int {
	return 135 + (track.ID%8)*15 // 135s .. 240s
}
