# 🔴 NOTHING PLAYER (01)

<div align="center">
  <h3><strong>VLC-Grade Multi-Format Mobile Media Player with Authentic Nothing OS Dot-Matrix Aesthetic</strong></h3>
  <p><em>Engineered with AndroidX Media3 (ExoPlayer), LibVLC Native C++ Engine, Real-Time Hardware DSP Equalizer, Zero-Lag Persistent Media Cache & Pure Dot-Matrix LED Typography</em></p>
  
  <p>
    <img src="https://img.shields.io/badge/Platform-Android%2016%20%7C%20Nothing%20OS-D71921?style=flat-square" alt="Platform">
    <img src="https://img.shields.io/badge/Design-High--Density%20Micro--LED%20(NDot)-black?style=flat-square" alt="Design">
    <img src="https://img.shields.io/badge/Cache-Zero--Lag%20Instant%20Load-white?style=flat-square" alt="Cache">
    <img src="https://img.shields.io/badge/Engine-ExoPlayer%20%2B%20LibVLC-black?style=flat-square" alt="Engine">
    <img src="https://img.shields.io/badge/Audio-Dolby%20EAC3%20%2F%20DTS%205.1%20%2F%20TrueHD-D71921?style=flat-square" alt="Dolby">
    <img src="https://img.shields.io/badge/DSP-5--Band%20EQ%20%2B%20BassBoost%20%2B%203D%20Spatial-white?style=flat-square" alt="DSP">
  </p>
</div>

---

## ⚡ Key Highlights & Core Architecture

### 1. ⚡ Zero-Lag Persistent Media Cache & Asynchronous Background Sync
- **Instantaneous 0ms Startup**: Eliminates startup blank/waiting screens. Cached media items and folders are loaded from persistent disk storage (`cached_videos.json`, `cached_audios.json`) instantly when the app opens.
- **Silent Background Scanner**: MediaStore is scanned asynchronously in a background thread without blocking the UI.
- **Smooth Differential Updating**: If new files are detected or deleted by camera/other apps, cache is refreshed and UI diffs smoothly without starting from scratch or jittering.
- **Atomic Deletion Sync**: Immediate deletion from UI, disk cache, and filesystem with synchronized MediaStore purge.

---

### 2. 🎛️ Authentic Nothing OS Dot-Matrix LED Design System
- **High-Density Micro-LED `DotMatrixSeekBar`**: Upgraded timeline scrubber with tightly spaced micro-dots (`~5.2dp` pitch, `1.15dp` radius) delivering the authentic tactile Nothing OS Glyph / Dot Matrix feel instead of sparse dots.
- **`DotMatrixIconView` (9x9 LED Grid Matrix)**: Custom vector LED engine dynamically rendering icons on a 9x9 discrete circular LED dot grid (Brightness Sun with radial rays, Speaker with soundwaves, Nothing Red Mute slash, and Turbo Boost lightning).
- **`DotMatrixTextView` (5x7 Circular LED Digits & Symbols)**: Renders live percentages (`0%`–`200% BOOST`), timeline jumps (`+10s`, `-10s`), and digital clock playback durations (`MM:SS` / `HH:MM:SS`) with circular LED dot matrix typography.
- **`DotMatrixProgressBar` (14-Segment LED Matrix Dots)**: Replaced continuous smooth progress bars with authentic Glyph-style circular LED dot matrix indicators.
- **Transparent Glassmorphic Gesture HUD**: Semi-transparent `#4D000000` glassmorphic backdrop with subtle `1dp` white border (`#2BFFFFFF`) and `20dp` pill corners so video playback remains visible during gestures and double-taps.
- **App-Wide `@font/ndot` Typography**: Full dot-matrix styling applied natively across all folder names, file titles, resolutions (`1080P`, `4K`), audio codec tags (`DOLBY 5.1`), category chips, and app headers.

---

### 3. 🎚️ Real-Time Hardware Equalizer DSP Engine & Dedicated Power Toggle
- **Dedicated ON/OFF Toggle Switch**: High-contrast Nothing Red styled toggle switch (`SwitchCompat`) with live status LED and controls container auto-dimming on bypass.
- **Instant Settings Access**: Equalizer can be toggled ON/OFF directly from the Settings screen or inside the Equalizer hub.
- **Active AudioSession Routing (`AudioEffectManager`)**: Directly binds to the active `audioSessionId` of `MediaPlayer` and `ExoPlayer` with system broadcast intents for hardware DSP processing.
- **5-Band Graphic Equalizer**: Precision sliders for `60Hz`, `230Hz`, `910Hz`, `3.6kHz`, and `14kHz` bands.
- **Dynamic Bass Boost & 3D Spatial Virtualizer**: Studio-grade sub-bass punch and immersive 3D surround sound.
- **Hardware Preamp Loudness Enhancer**: Native `LoudnessEnhancer` delivering millibel volume amplification without distortion.
- **Dolby Cinema & Studio Presets**: Dedicated presets including `🎬 CINEMA (3D THEATER)` (+9dB sub-bass, +2dB dialogue, +8dB detail, 100% 3D spatial surround sound), `NOTHING PUNCH`, `BASS BOOST`, `EDM`, `ROCK`, `POP`, `VOCAL`, and `FLAT`.

---

### 4. 🔊 Universal Dolby EAC-3 5.1 / AC-3 & DTS Dual-Engine
- **Dual Playback Architecture**: Combines **AndroidX Media3 (ExoPlayer)** for hardware-accelerated playback with the **LibVLC Native C++ Engine** for universal software decoding.
- **Dolby Digital Plus & Surround Sound**: Automatically downmixes multi-channel audio (`E-AC-3 5.1`, `AC-3`, `DTS`, `TrueHD`, `FLAC`, `AAC`, `Opus`) to phone stereo speakers and headphones with crystal clarity — eliminating `MediaCodecAudioRenderer` errors.
- **Intelligent Engine Auto-Selection**: Automatically routes Dolby 5.1 / multi-channel MKV files to the optimal native engine with **zero black screen** and **zero user intervention**.
- **On-the-Fly Codec Switcher (`HW+` / `VLC`)**: Instant 1-tap switching between Hardware Acceleration and Universal VLC Software Engine directly from the player top bar.

---

### 5. 🎬 Pro Gesture Cinema Controls, Zero-Lag Seeking & Chapters
- **Zero-Lag Instant Seeking Architecture**:
  - **Ultra-Low Rebuffer Wait (200ms)**: Replaced default 2000ms rebuffer delay with a 200ms instant threshold in `DefaultLoadControl`.
  - **15-Second RAM Back-Buffer (`setBackBuffer(15000, true)`)**: Preserves the last 15 seconds of decoded frames in memory for instantaneous 0ms rewind without disk re-reading.
  - **Optimized LibVLC Caching**: Reduced `:file-caching` to 150ms for instantaneous scrubbing across local media.
  - **Synchronized Touch Scrubbing**: Background progress loops pause during swipe gestures, eliminating stutter and keeping the seekbar and time HUD synchronized in real time.
- **MKV / MP4 Chapter Markers & Chapter Dialog (`[CH]`)**:
  - Embedded chapters extracted directly from media metadata via LibVLC.
  - Distinct Nothing Red LED chapter divider ticks drawn directly onto the `DotMatrixSeekBar`.
  - Dedicated chapter selection dialog displaying chapter numbers, names, and timestamp offsets.
- **Skip Intro / Recap Pill (`SKIP INTRO +85S`)**:
  - Interactive discrete Nothing OS pill auto-appears during the first 5 to 90 seconds of any episode.
  - One-tap instant 85-second jump to bypass theme songs and intros with auto-dismissal.
- **Precision Subtitle Delay Micro-Tuner (`SUBTITLE DELAY`)**:
  - Dedicated live offset tuner dialog supporting ±0.1s fine tuning and ±0.5s coarse adjustments.
  - Instant live audio/subtitle synchronization without stopping video playback.
- **Floating Video Preview Pop-up Box**: Live multi-threaded `MediaMetadataRetriever` frame extractor showing real-time video thumbnail preview card and timestamp directly following the seekbar thumb during scrubbing.
- **Watched Episode Storage Cleaner**:
  - Automatically prompts upon video completion to delete the watched episode and reclaim storage.
  - Displays reclaimed file size (MB/GB) and frees memory with synchronized MediaStore purge.
- **Remaining Time Countdown Display**: Right-side timer toggles between Remaining Countdown (`-MM:SS`) and Total Duration (`MM:SS`).
- **Calibrated Swipe Gestures with Axis Locking**:
  - **Left Vertical Swipe**: Screen Brightness (0% – 100%) with Dot-Matrix HUD.
  - **Right Vertical Swipe**: Volume (0% – 200%) with Nothing Red Audio Boost HUD.
  - **Directional Axis Lock**: Prevents vertical brightness/volume swipes from accidentally triggering horizontal forward/rewind seeks.
- **Pinch-to-Zoom & Multi-Scale**: Freeform scale from 0.7x to 2.5x with double-tap zoom and aspect ratio switcher (**FIT**, **FILL**, **ZOOM**).
- **Double Tap Navigation**: Instant ±10s jump on the left/right halves of the screen with dot-matrix badge feedback.
- **Screen Lock (`🔒`)**: One-tap child/pocket lock hiding all touch controls.
- **Speed Controller**: Variable playback speeds (`0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`).
- **Picture-in-Picture (PiP)**: Native floating mini-player.
- **Audio & Subtitle Selectors**: Interactive dialogs via `[AUDIO]` and `[SUB]` buttons for multi-language streams and embedded subtitles.

---

### 6. 🌐 Direct Link & YouTube Network Stream
- **Auto YouTube Stream Resolution**:
  - Automatically detects YouTube links (`youtube.com/watch?v=...`, `youtu.be/...`, `youtube.com/shorts/...`).
  - `YouTubeStreamResolver` resolves direct high-resolution HLS/MP4 streams with 2.5s quick failover.
  - Plays in Nothing Player with full Dot-Matrix HUD, swipe gesture brightness/volume, and Picture-in-Picture (PiP).
  - Dedicated hardware-accelerated `YouTubeCinemaActivity` AMOLED mode guarantees 100% playback success for all YouTube streams, shorts, and live events without ads or clutter.
- **Universal Protocol Support**: Direct support for HTTP, HTTPS, RTSP, RTMP, HLS (`.m3u8`), and DASH (`.mpd`).
- **Smart Clipboard Auto-Paste**: Automatically detects and populates copied video stream links with one-tap paste.
- **Recent Streams History**: Saves played stream links in quick-access chips.

---

### 7. 🗂️ Multi-Selection Batch Deletion (Videos, Folders & Music)
- **Contextual Action Bar**: Long-press any video card, folder, or audio track to enter batch selection mode.
- **Select All & Selection Counter**: Instant select-all button and selected item counter in Dot-Matrix font.
- **Permanent File & MediaStore Deletion**: Confirmation dialog with synchronized removal from both Android MediaStore, disk cache, and storage filesystem.

---

### 8. 🎵 Background Music Playback Service
- **Android Foreground Service (`MusicPlaybackService`)**: Continuous background audio playback utilizing `MediaStyle` notification and `MediaSessionCompat`.
- **Accurate Millisecond Seeking**: Seamless timeline scrubbing without premature track skipping.
- **Lock Screen & Headset Controls**: Full Play/Pause, Next, Previous, and Track Seek directly from lock screen, notification shade, and Bluetooth earphones.

---

### 9. 🔲 Dynamic Multi-View Modes & List Layouts
- **Multiple Layout Densities**: Instant switching between **Grid (2 Columns)**, **Grid (3 Columns Compact)**, and **List (Detailed Rows)**.
- **Detailed Row List Card (`item_video_list.xml`)**: High-contrast row representation featuring video thumbnail, title in NDOT typography, resolution badge, duration chip, file size, and dot-matrix progress indicator.
- **Preference Persistence**: User-selected view modes are stored across sessions in `SharedPreferences`.

---

### 10. 🔀 Multi-Attribute Media Sorting Engine
- **Versatile Sort Modes**: Sort media items in both Video and Music tabs by:
  - **Date Added**: Newest First / Oldest First
  - **Alphabetical**: Name (A – Z) / Name (Z – A)
  - **File Size**: Largest First
  - **Duration**: Longest First
- **Instant Non-Blocking Reordering**: Re-sorts and updates in-memory cached lists without re-reading disks or triggering UI latency.

---

### 11. 🛰️ Autonomous Storage Observer (`MediaAutoScanner`) & Custom NDOT Dialogs
- **`MediaAutoScanner` Background Observer**: Automatically monitors storage changes and triggers differential scanning when new files are downloaded or recorded.
- **Manual Rescan Action**: 1-tap "RESCAN STORAGE" trigger in the options menu for forced synchronization.
- **Nothing OS NDOT Dialog Framework (`NothingDialogHelper`)**: System-wide custom dialogs featuring AMOLED pure black backgrounds, Nothing Red LED accents, and NDOT typography for selections, options menus, and destructive confirmation prompts.

---

### 12. 🍿 Smart Media Organizer & Series Season Hub
- **Autonomous Regex Classifier (`MediaItem.detectCategory`)**: Intelligently classifies downloaded videos into **Movies**, **Web Series**, and general videos using high-precision regex matching (`S\d+E\d+`, `\d+x\d+`, `Season \d+`, `EP\d+`).
- **Online Metadata Enrichment (`OnlineMetadataManager`)**: Seamlessly enriches local series and movies with official posters, clean display titles, and release years via open REST endpoints (TVMaze & iTunes).
- **Smart "Continue Watching" Series Hub**: Dedicated interactive banner atop the Series catalog featuring the last-watched episode, remaining time (`X MIN LEFT`), `DotMatrixProgressBar`, and 1-tap instant `RESUME`.
- **Manual Re-Classifier ("MATCH SHOW / MOVIE INFO")**: Interactive Nothing OS dialog in File Info to reclassify and search metadata for obscure or misnamed files with instant cache synchronization.

---

### 13. ⏱️ Next Episode Countdown Overlay (Netflix Style)
- **Automatic 15-Second HUD Trigger**: When playing a folder or series playlist, a discrete Nothing OS HUD card appears in the bottom-right corner during the last 15 seconds of the episode.
- **Live Dot-Matrix Countdown**: Shows active red LED countdown (`● NEXT EPISODE IN 10S`) and the title of the next episode.
- **Interactive Controls**:
  - `CANCEL`: Suppresses the overlay and allows watching the current episode's credits.
  - `PLAY NOW`: Instantly transitions to the next episode without waiting.
  - **Auto-Play**: Automatically plays the next episode seamlessly when the timer reaches 0s.

---

### 14. 💬 Keyless Online Subtitles Downloader (`OnlineSubtitleManager`)
- **Built-in Subtitle Repository Client**: Keyless, open search directly integrated into the player's subtitle selector dialog for both ExoPlayer (HW+) and LibVLC (SW+).
- **Multi-Language Filters**: Search by video title with language filters (**ALL**, **ENGLISH**, **HINDI**), displaying ratings and download counts.
- **In-Memory GZIP Extraction**: Automatically downloads and uncompresses `.gz` subtitle streams into `.srt` format in memory.
- **Instant Engine Attachment**: Auto-loads downloaded subtitles into the active player with zero playback interruption.

---

### 15. 🔒 Private Vault & Multi-Modal Biometric Security Hub (`VaultActivity`)
- **Discrete Nothing OS 3x3 Dot-Matrix Pattern Lock (`DotMatrixPatternView`)**: Custom gesture engine rendering a 3x3 circular LED dot grid with Nothing Red connecting lines, active node illumination, and tactile haptic ticks.
- **Hardware Biometric Integration (`BiometricAuthHelper`)**: Native AndroidX BiometricPrompt supporting in-display/capacitive fingerprint sensors and system face unlock with hardware keystore security.
- **Dedicated Vault Security Hub (`dialog_vault_security_config.xml`)**: Seamless configuration to switch between 4-digit PIN and 3x3 Pattern, toggle biometric unlock with live sensor telemetry, and set auto-prompt behavior.
- **Isolated Encrypted Storage (`.vault/`)**: Media items moved to the vault are decoupled from standard directories and shielded with `.nomedia` markers, fully hidden from gallery scanners and third-party applications.
- **Full In-Vault Management**: Complete preview, in-player playback, instant restoration to original folders, and irreversible permanent deletion.

---

### 16. 🗑️ 30-Day Auto-Retention Recycle Bin (`RecycleBinActivity` & `TrashManager`)
- **Accidental Deletion Shield**: Deleted videos are safely moved to a local trash repository rather than immediately destroyed.
- **Dynamic 30-Day Expiry Countdown**: Displays live dot-matrix countdown badges (`● X DAYS LEFT`) per item with automatic cleanup upon expiration.
- **One-Tap Safe Restore**: Restores quarantined videos directly to their original filesystem directories and triggers MediaStore indexing.
- **Batch Permanent Disposal**: One-tap "EMPTY RECYCLE BIN" confirmation modal for instant storage recovery.

---

### 17. 🎛️ Floating Draggable Hardware Equalizer Window HUD
- **Draggable AMOLED Black Card**: Compact floating equalizer HUD with discrete Nothing Red accents that floats over the active video playback window.
- **Multi-Band DSP Control**: 5-band graphic faders (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz), Bass Boost, 3D Surround Virtualizer, and Preamp gain sliders.
- **Dual-Engine Synchronization (`AudioEffectManager`)**: Direct hardware AudioSession binding synchronized across both AndroidX ExoPlayer and LibVLC software decoders.
- **Quick Preset Chips**: Instant selection between Flat, Rock, Pop, Jazz, Electronic, Vocal Boost, Classical, and Bass Max profiles.

---

### 18. 📻 IPTV / M3U Stream Playlist Parser & Background Downloader
- **M3U / M3U8 Playlist Parsing (`M3uParser`)**: Parses local and remote live IPTV playlists with channel grouping, logo detection, and category filtering.
- **Background Media Downloader (`StreamDownloadHelper`)**: Download live network streams directly to local storage asynchronously with progress notifications.

---

## 🛠️ Tech Stack

- **Android Native**: Java / Kotlin (Android SDK 34+ / Android 16 API 36)
- **Typography & Aesthetics**: Nothing OS NDot / Silkscreen Dot-Matrix Font, Custom Canvas LED Renderers
- **Persistent Cache**: Disk JSON serialization (`cached_videos.json`, `cached_audios.json`)
- **Frontend / Hybrid**: React 19, TypeScript, Vite, Vanilla CSS Design System, Capacitor 8
- **Video & Audio Engines**:
  - `androidx.media3:media3-exoplayer:1.3.1`
  - `androidx.media3:media3-ui:1.3.1`
  - `org.videolan.android:libvlc-all:3.5.1`
  - `androidx.biometric:biometric:1.1.0` (Fingerprint & Face Recognition)
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
