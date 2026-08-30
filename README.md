# 🔴 NOTHING PLAYER (02)

<div align="center">
  <h3><strong>VLC-Grade Multi-Format Mobile Media Player with Authentic Nothing OS Aesthetic</strong></h3>
  <p><em>Built with React, TypeScript, Capacitor Android, AndroidX Media3, and LibVLC Native C++ Engine</em></p>
  
  <p>
    <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Web-D71921?style=flat-square" alt="Platform">
    <img src="https://img.shields.io/badge/Engine-Media3%20%2B%20LibVLC-black?style=flat-square" alt="Engine">
    <img src="https://img.shields.io/badge/Audio-Dolby%20EAC3%20%2F%20DTS%205.1-D71921?style=flat-square" alt="Dolby">
    <img src="https://img.shields.io/badge/Style-Nothing%20OS%20Minimal-white?style=flat-square" alt="Style">
  </p>
</div>

---

## ⚡ Key Highlights & Architecture

### 1. 🔊 Universal Dolby EAC-3 5.1 / AC-3 & DTS Dual-Engine
- **Dual Playback Architecture**: Combines **AndroidX Media3 (ExoPlayer)** for ultra-fast hardware-accelerated playback with the **LibVLC Native C++ Engine** for universal software decoding.
- **Dolby Digital Plus & Surround Sound**: Automatically downmixes multi-channel audio (`E-AC-3 5.1`, `AC-3`, `DTS`, `TrueHD`, `FLAC`, `AAC`, `Opus`) to phone stereo speakers and headphones with crystal clarity — eliminating `MediaCodecAudioRenderer` errors.
- **Intelligent Engine Auto-Selection**: Automatically routes Dolby 5.1 / multi-channel MKV files to the optimal native engine with **zero black screen** and **zero user intervention**.
- **On-the-Fly Codec Switcher (`HW+` / `SW+`)**: Instant 1-tap switching between Hardware Acceleration and Universal VLC Software Engine directly from the player top bar.

---

### 2. 🎬 Pro Gesture Cinema Controls (XPlayer & VLC Style)
- **Vertical Swipe Controls**:
  - **Left Side**: Screen Brightness (0% – 100%) with real-time Nothing OS HUD.
  - **Right Side**: Volume control with **200% Audio Boost** limiter.
- **Double Tap Navigation**: Instant ±10s jump on the left/right halves of the screen.
- **Screen Lock (`🔒`)**: One-tap child/pocket lock hiding all touch controls.
- **Aspect Ratio Switcher**: Seamless cycling between **FIT**, **FILL (Stretch)**, and **ZOOM (Crop)**.
- **Speed Controller**: Variable playback speeds (`0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`).
- **Picture-in-Picture (PiP)**: Native floating mini-player while using other apps.
- **Audio & Subtitle Selectors**: Interactive dialogs via `[AUDIO]` and `[SUB]` buttons for multi-language streams (e.g. Hindi DD5.1, English) and embedded subtitles (`ESub`, `SRT`, `ASS`).

---

### 3. 🎵 Background Music Playback Service
- **Android Foreground Service (`MusicPlaybackService`)**: Continuous background audio playback utilizing `MediaStyle` notification and `MediaSessionCompat`.
- **Lock Screen & Headset Controls**: Full Play/Pause, Next, Previous, and Track Seek directly from lock screen, notification shade, and Bluetooth earphones.
- **True Background Lifecycle**: Audio continues playing uninterrupted when switching apps or locking the device.

---

### 4. ℹ️ Detailed File Specs & Storage Path Modal
- **Glassmorphic Bottom Sheet**: Accessible via the three-dot (`⋮`) action button on any video or audio item.
- **Exact File Storage Path**: Displays the full absolute storage path (e.g., `/storage/emulated/0/UCDownloads/video/...`).
- **1-Tap Path Copy**: Instant clipboard copy button with tactile haptic feedback.
- **Full Media Metadata**: Live display of video resolution, audio codecs, bitrate, container format, file size in MB/GB, duration, parent folder, and date added.

---

### 5. 🌐 Network Stream Player
- **Direct Video URL Playback**: Dedicated `[STREAM]` button in Video Explorer.
- **Universal Protocol Support**: Play direct links from HTTP, HTTPS, RTSP, HLS (`.m3u8`), and DASH (`.mpd`).
- **Quick Paste & Demo Streams**: 1-tap clipboard paste button, built-in demo streams (1080p FHD), and recent stream history.

---

### 6. 📁 Clean Folder Explorer & Nothing OS Design
- **Parent Folder Grouping**: Video Explorer strictly groups files by parent folder without displaying redundant flat video lists.
- **Monochromatic Minimal Aesthetics**: Authentic Nothing OS NDot matrix typography, glassmorphic translucent panels, and signature Nothing Red accents.
- **Hardware Back Navigation**: Intuitive step-by-step back navigation that respects folder hierarchies without accidentally exiting the app.

---

## 🛠️ Tech Stack

- **Frontend**: React 19, TypeScript, Vite, Vanilla CSS Design System
- **Mobile Runtime**: Capacitor 8 (Android)
- **Video & Audio Engines**:
  - `androidx.media3:media3-exoplayer:1.3.1`
  - `androidx.media3:media3-ui:1.3.1`
  - `org.videolan.android:libvlc-all:3.5.1`
- **Background Media**: `androidx.media:media:1.7.0` (`MediaSessionCompat`, `NotificationCompat.MediaStyle`)

---

## 📲 Build & Development

### Prerequisites
- Node.js 18+
- Android SDK (API Level 34+)
- JDK 17 or JDK 21

### Step-by-Step Setup

```bash
# 1. Clone repository
git clone https://github.com/m4mental/nothing-player.git
cd nothing-player

# 2. Install dependencies
npm install

# 3. Start local web development server
npm run dev

# 4. Build web production assets
npm run build

# 5. Sync with Android native project
npx cap sync android

# 6. Build debug APK
cd android
./gradlew assembleDebug --no-daemon
```

The compiled APK will be located at:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
