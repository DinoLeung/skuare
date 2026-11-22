package main

// Writes the built-in find phone tone, our own synthesized sound, so no third-party license applies.
// Run on its own: go run generateFindPhoneTone.go
//
// One loop of the tone: four short beeps on two alternating pitches, then a pause. The player loops
// it while the phone rings. Pitches around 2–2.6 kHz, where phone speakers are loudest. Every beep
// fades in and out over a few milliseconds so it doesn't click. Mono 16-bit PCM WAV, which Android's
// MediaPlayer and iOS's AVAudioPlayer both play.

import (
	"bytes"
	"encoding/binary"
	"fmt"
	"math"
	"os"
)

const (
	outputPath = "../composeApp/src/commonMain/composeResources/files/find_phone.wav"
	sampleRate = 22050
	amplitude  = 0.9
	beepLength = 0.12  // seconds
	beepGap    = 0.08  // seconds
	pauseAfter = 0.6   // seconds, after the last beep, before the loop starts again
	fadeLength = 0.005 // seconds, in and out of every beep
)

// C7 and E7, alternating
var beepPitches = []float64{2093.0, 2637.0, 2093.0, 2637.0}

func main() {
	if err := run(); err != nil {
		fmt.Fprintln(os.Stderr, "generate find phone tone:", err)
		os.Exit(1)
	}
}

func run() error {
	var samples []int16
	for i, pitch := range beepPitches {
		samples = append(samples, beep(pitch)...)
		gap := beepGap
		if i == len(beepPitches)-1 {
			gap = pauseAfter
		}
		samples = append(samples, silence(gap)...)
	}
	if err := os.WriteFile(outputPath, wav(samples), 0o644); err != nil {
		return fmt.Errorf("write %s: %w", outputPath, err)
	}
	fmt.Printf("%s: %d samples, %.2f s\n", outputPath, len(samples), float64(len(samples))/sampleRate)
	return nil
}

// sampleCount is the number of samples in the given seconds, rounded.
func sampleCount(seconds float64) int {
	return int(math.Round(seconds * sampleRate))
}

func beep(pitch float64) []int16 {
	count := sampleCount(beepLength)
	fade := sampleCount(fadeLength)
	samples := make([]int16, count)
	for i := range samples {
		envelope := 1.0
		if i < fade {
			envelope = float64(i) / float64(fade)
		} else if i >= count-fade {
			envelope = float64(count-1-i) / float64(fade)
		}
		value := amplitude * envelope * math.Sin(2*math.Pi*pitch*float64(i)/sampleRate)
		samples[i] = int16(math.Round(value * math.MaxInt16))
	}
	return samples
}

func silence(seconds float64) []int16 {
	return make([]int16, sampleCount(seconds))
}

// wav wraps mono 16-bit samples in a canonical 44-byte RIFF header.
func wav(samples []int16) []byte {
	const channels, bitsPerSample = 1, 16
	dataSize := len(samples) * bitsPerSample / 8
	var out bytes.Buffer
	write := func(value any) { _ = binary.Write(&out, binary.LittleEndian, value) }
	out.WriteString("RIFF")
	write(uint32(36 + dataSize))
	out.WriteString("WAVE")
	out.WriteString("fmt ")
	write(uint32(16)) // fmt chunk size
	write(uint16(1))  // PCM
	write(uint16(channels))
	write(uint32(sampleRate))
	write(uint32(sampleRate * channels * bitsPerSample / 8)) // byte rate
	write(uint16(channels * bitsPerSample / 8))              // block align
	write(uint16(bitsPerSample))
	out.WriteString("data")
	write(uint32(dataSize))
	write(samples)
	return out.Bytes()
}
