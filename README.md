# 🔴 NOTHING PLAYER (01)

<div align="center">
  <h3><strong>VLC-Grade Multi-Format Mobile Media Player with Authentic Nothing OS Aesthetic</strong></h3>
  <p><em>Engineered with AndroidX Media3 (ExoPlayer), LibVLC Native C++ Engine, Real-Time Hardware DSP Equalizer, and Nothing OS Minimalist Design Language</em></p>
  
  <p>
    <img src="https://img.shields.io/badge/Platform-Android%2016%20%7C%20Nothing%20OS-D71921?style=flat-square" alt="Platform">
    <img src="https://img.shields.io/badge/Engine-ExoPlayer%20%2B%20LibVLC-black?style=flat-square" alt="Engine">
    <img src="https://img.shields.io/badge/Audio-Dolby%20EAC3%20%2F%20DTS%205.1%20%2F%20TrueHD-D71921?style=flat-square" alt="Dolby">
    <img src="https://img.shields.io/badge/DSP-5--Band%20EQ%20%2B%20BassBoost%20%2B%203D%20Spatial-white?style=flat-square" alt="DSP">
  </p>
</div>

---

## ⚡ Key Highlights & Core Architecture

### 1. 🔊 Universal Dolby EAC-3 5.1 / AC-3 & DTS Dual-Engine
- **Dual Playback Architecture**: Combines **AndroidX Media3 (ExoPlayer)** for ultra-fast hardware-accelerated playback with the **LibVLC Native C++ Engine** for universal software decoding.
- **Dolby Digital Plus & Surround Sound**: Automatically downmixes multi-channel audio (`E-AC-3 5.1`, `AC-3`, `DTS`, `TrueHD`, `FLAC`, `AAC`, `Opus`) to phone stereo speakers and headphones with crystal clarity — eliminating `MediaCodecAudioRenderer` errors.
- **Intelligent Engine Auto-Selection**: Automatically routes Dolby 5.1 / multi-channel MKV files to the optimal native engine with **zero black screen** and **zero user intervention**.
- **On-the-Fly Codec Switcher (`HW+` / `VLC`)**: Instant 1-tap switching between Hardware Acceleration and Universal VLC Software Engine directly from the player top bar.

---

### 2. 🎛️ Real-Time Hardware Equalizer DSP Engine
- **Active AudioSession Routing (`AudioEffectManager`)**: Directly binds to the active `audioSessionId` of `MediaPlayer` and `ExoPlayer` with system broadcast intents for hardware DSP processing.
- **5-Band Graphic Equalizer**: Precision sliders for `60Hz`, `230Hz`, `910Hz`, `3.6kHz`, and `14kHz` bands.
- **Dynamic Bass Boost & 3D Spatial Virtualizer**: Studio-grade sub-bass punch and immersive 3D surround sound.
- **Hardware Preamp Loudness Enhancer**: Native `LoudnessEnhancer` delivering millibel volume amplification without distortion.
- **Dolby Cinema & Studio Presets**: Includes dedicated presets:
  - `🎬 CINEMA (3D THEATER)`: Subwoofer sub-bass rumble (+9dB), dialogue clarity (+2dB), high-frequency detail (+8dB), and 100% 3D spatial surround sound.
  - `NOTHING PUNCH`, `BASS BOOST`, `EDM`, `ROCK`, `POP`, `VOCAL`, and `FLAT`.
- **Master Power Bypass Switch**: True hardware bypass with red LED status indicator.

---

### 3. 🎬 Pro Gesture Cinema Controls & Floating Thumbnail Scrubber
- **Floating Video Preview Pop-up Box**: Live multi-threaded `MediaMetadataRetriever` frame extractor showing real-time video thumbnail preview card and timestamp directly following the seekbar thumb during scrubbing.
- **Remaining Time Countdown Display**: Right-side timer toggles between Remaining Countdown (`-MM:SS`) and Total Duration (`MM:SS`).
- **Calibrated Swipe Gestures with Axis Locking**:
  - **Left Vertical Swipe**: Screen Brightness (0% – 100%) with HUD.
  - **Right Vertical Swipe**: Volume (0% – 200%) with Audio Boost.
  - **Directional Axis Lock**: Prevents vertical brightness/volume swipes from accidentally triggering horizontal forward/rewind seeks.
- **Pinch-to-Zoom & Multi-Scale**: Freeform scale from 0.7x to 2.5x with double-tap zoom and aspect ratio switcher (**FIT**, **FILL**, **ZOOM**).
- **Double Tap Navigation**: Instant ±10s jump on the left/right halves of the screen.
- **Screen Lock (`🔒`)**: One-tap child/pocket lock hiding all touch controls.
- **Speed Controller**: Variable playback speeds (`0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`).
- **Picture-in-Picture (PiP)**: Native floating mini-player.
- **Audio & Subtitle Selectors**: Interactive dialogs via `[AUDIO]` and `[SUB]` buttons for multi-language streams and embedded subtitles.

---

### 4. 🌐 Direct Link Network Video Stream
- **Online Stream Dialog**: Access via the **"ONLINE STREAM"** category chip in the video explorer.
- **Universal Protocol Support**: Play direct links from HTTP, HTTPS, RTSP, RTMP, HLS (`.m3u8`), and DASH (`.mpd`).
- **Smart Clipboard Auto-Paste**: Automatically detects and populates copied video stream links with one-tap paste.
- **Prominent High-Contrast Play Buttons**: Signature Nothing Red (`#D71921`) action buttons and keyboard `Enter`/`Go` integration.
- **Recent Streams History**: Saves played stream links in quick-access chips.

---

### 5. 🗂️ Multi-Selection Batch Deletion (Videos, Folders & Music)
- **Contextual Action Bar**: Long-press any video card, folder, or audio track to enter batch selection mode.
- **Select All & Selection Counter**: Instant select-all button and selected item counter.
- **Permanent File & MediaStore Deletion**: Confirmation dialog with synchronized removal from both Android MediaStore and storage filesystem.

---

### 6. 🎵 Background Music Playback Service
- **Android Foreground Service (`MusicPlaybackService`)**: Continuous background audio playback utilizing `MediaStyle` notification and `MediaSessionCompat`.
- **Accurate Millisecond Seeking**: Seamless timeline scrubbing without premature track skipping.
- **Lock Screen & Headset Controls**: Full Play/Pause, Next, Previous, and Track Seek directly from lock screen, notification shade, and Bluetooth earphones.

---

## 🛠️ Tech Stack

- **Android Native**: Java / Kotlin (Android SDK 34+ / Android 16 API 36)
- **Frontend / Hybrid**: React 19, TypeScript, Vite, Vanilla CSS Design System, Capacitor 8
- **Video & Audio Engines**:
  - `androidx.media3:media3-exoplayer:1.3.1`
  - `androidx.media3:media3-ui:1.3.1`
  - `org.videolan.android:libvlc-all:3.5.1`
- **Background Media & DSP**:
  - `androidx.media:media:1.7.0` (`MediaSessionCompat`, `NotificationCompat.MediaStyle`)
  - Native `android.media.audiofx` (`Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`)
- **Image & Thumbnail Caching**: `com.github.bumptech.glide:glide:4.16.0`

---

## 📲 Build & Deployment

```bash
# 1. Clone repository
git clone https://github.com/m4mental/nothing-player.git
cd nothing-player

# 2. Build Native Android APK
cd android
./gradlew assembleDebug

# 3. Install to Connected Nothing Phone (2a) via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
