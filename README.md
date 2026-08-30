# 🔴 NOTHING PLAYER (01)

<div align="center">
  <p><strong>VLC-Grade Multi-Format Mobile & Desktop Media Player Suite with Authentic Nothing OS Aesthetic</strong></p>
  <p><em>Built with React, TypeScript, Capacitor Android, Web Audio API DSP, and TailwindCSS</em></p>
</div>

---

## ⚡ Key Highlights

- **Authentic Nothing OS Aesthetic**: Monochromatic carbon UI, NDot Matrix typography, translucent smoke glass, and signature Nothing Red (`#D71921`) accents.
- **Dynamic Reactive Glyph Interface**: LED visualizer strips and ring that pulse to real-time audio frequencies using Web Audio API FFT analysis.
- **VLC Mobile Touch Gestures**:
  - **Left Vertical Swipe**: Brightness (0% to 150%) with Nothing on-screen HUD.
  - **Right Vertical Swipe**: Volume & **200% Audio Boost** with dynamics limiter.
  - **Horizontal Swipe**: High precision timeline scrub.
  - **Double Tap Left/Right**: Fast ±10s jump.
- **Dual Dedicated Hubs**:
  - **🎵 [01 / MUSIC] Hub**: Audiophile turntable deck, synchronized `.lrc` lyrics auto-scroller, waveform scrubber, 10-band Equalizer with preamp & bass punch, play queue.
  - **🎬 [02 / VIDEO] Cinema Hub**: Subtitles engine (`.srt`/`.vtt` with delay sync), aspect ratio switcher (16:9, 4:3, 21:9, Fit, Stretch), retro CRT scanline & Nothing Red duotone shaders, screenshot frame grabber, frame-by-frame navigation, Picture-in-Picture.
  - **📁 [03 / LIBRARY] Media Vault**: Multi-format local file & folder import (MP3, WAV, FLAC, AAC, MP4, MKV, WebM, MOV, HLS), persistent IndexedDB offline storage.
  - **📻 [04 / RADIO] World Tuner**: 24/7 live internet streaming stations (Synthwave, Lo-Fi, Techno, Space Ambient).
- **Android MediaSession**: Background audio playback, lock screen metadata & notification tray controls.
- **Automated GitHub Actions CI/CD**: Auto-compiles **Android APK (`NothingPlayer-v1.0.apk`)** and publishes GitHub Releases with push changelog.

---

## 📲 Download & Installation

### Android APK
On every push to `main`, GitHub Actions automatically builds the Android APK. Download the latest release from the [GitHub Releases](https://github.com/m4mental/nothing-player/releases) tab.

---

## 🛠️ Development & Local Run

```bash
# 1. Install dependencies
npm install

# 2. Start local development server
npm run dev

# 3. Build Web Bundle
npm run build

# 4. Sync Android Project
npx cap sync android
```

---

## 🎹 VLC Keyboard Shortcuts

| Key | Action |
| --- | --- |
| `Space` | Play / Pause toggle |
| `←` / `→` | Seek 5s backward / forward |
| `↑` / `↓` | Increase / Decrease volume (up to 200%) |
| `M` | Mute / Unmute toggle |
| `N` / `B` | Next / Previous track in queue |
| `S` | Shortcuts & Gesture Guide Modal |
