package seed

import (
	"bytes"
	"image"
	"image/color"
	"image/draw"
	"image/jpeg"
	"math"
)

const (
	coverWidth  = 512
	coverHeight = 512
	thumbWidth  = 128
	thumbHeight = 128
)

func GenerateCover(title, artist string, seed int) ([]byte, error) {
	return renderCover(title, artist, seed, coverWidth, coverHeight)
}

func GenerateThumbnail(title, artist string, seed int) ([]byte, error) {
	return renderCover(title, artist, seed, thumbWidth, thumbHeight)
}

func renderCover(title, artist string, seed, width, height int) ([]byte, error) {
	img := image.NewRGBA(image.Rect(0, 0, width, height))
	base := paletteColor(seed)
	top := paletteColor(seed + 7)

	for y := 0; y < height; y++ {
		t := float64(y) / float64(height)
		r := lerp(base.R, top.R, t)
		g := lerp(base.G, top.G, t)
		b := lerp(base.B, top.B, t)
		line := color.RGBA{R: r, G: g, B: b, A: 255}
		for x := 0; x < width; x++ {
			img.Set(x, y, line)
		}
	}

	drawAccent(img, seed, width, height)

	var buf bytes.Buffer
	if err := jpeg.Encode(&buf, img, &jpeg.Options{Quality: 85}); err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func drawAccent(img *image.RGBA, seed, width, height int) {
	overlay := image.NewRGBA(img.Bounds())
	alpha := uint8(40 + (seed % 40))
	c := color.RGBA{R: 255, G: 255, B: 255, A: alpha}
	radius := width / 4
	cx := width/2 + (seed%radius - radius/2)
	cy := height/2 + ((seed*3)%radius - radius/2)
	for y := 0; y < height; y++ {
		for x := 0; x < width; x++ {
			dx := float64(x - cx)
			dy := float64(y - cy)
			if math.Sqrt(dx*dx+dy*dy) < float64(radius) {
				overlay.Set(x, y, c)
			}
		}
	}
	draw.Draw(img, img.Bounds(), overlay, image.Point{}, draw.Over)
}

func paletteColor(seed int) color.RGBA {
	palettes := []color.RGBA{
		{R: 45, G: 62, B: 120, A: 255},
		{R: 88, G: 42, B: 96, A: 255},
		{R: 24, G: 96, B: 102, A: 255},
		{R: 120, G: 72, B: 36, A: 255},
		{R: 36, G: 88, B: 64, A: 255},
		{R: 96, G: 48, B: 72, A: 255},
		{R: 56, G: 56, B: 120, A: 255},
	}
	return palettes[seed%len(palettes)]
}

func lerp(a, b uint8, t float64) uint8 {
	return uint8(float64(a) + (float64(b)-float64(a))*t)
}
