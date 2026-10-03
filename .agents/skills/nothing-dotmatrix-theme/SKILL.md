---
name: nothing-dotmatrix-theme
description: Enforces Nothing OS NDOT Dot-Matrix design system (Black & Red signature palette, NDOT typography, micro-LED dot matrix icons, and hardware minimalism) across all UI components, layouts, icons, and screens in NOTHING-PLAYER. Activate whenever creating, modifying, or reviewing any UI, icon, screen, or component.
---

# Nothing OS NDOT Dot-Matrix Design System

This skill enforces the official **Nothing OS & CMF** design language for the **NOTHING-PLAYER** project. Every UI screen, widget, icon, progress indicator, and layout must adhere strictly to these principles.

---

## 🎨 1. Signature Color Palette

All UI components must exclusively use this curated palette. Never introduce generic primary colors (plain blue, green, yellow, purple).

| Token Name | Hex Code | Purpose & Usage |
| :--- | :--- | :--- |
| **Nothing Black (Base)** | `#000000` | Deep pitch background for AMOLED contrast |
| **Nothing Surface** | `#121212` / `#161616` | Card background, bottom sheets, dialogue boxes |
| **Nothing Signature Red** | `#D71921` / `#FF1A24` | Primary brand accent, active playback dots, live badges, seekbars |
| **Subtle Matrix LED (Inactive)**| `#26FFFFFF` / `#33FFFFFF` | Faint unplayed dot matrix pixels, inactive track dots |
| **Nothing White (Primary)** | `#FFFFFF` | High-contrast NDOT titles, icons, and primary labels |
| **Nothing White Dim** | `#80FFFFFF` (50%) | Subtext, timestamps, video duration, secondary metadata |
| **Nothing White Subtle** | `#40FFFFFF` (25%) | Borders, dividers, subtle pill outlines, inactive chips |
| **Translucent LED Backing** | `#B3000000` (70%) | Dark strip under dot-matrix progress bars for thumbnail contrast |

---

## 🔲 2. NDOT Dot-Matrix Iconography (Mandatory)

**Strict Rule: Never use standard generic Material/Android vector icons directly without dot-matrix styling.**

1. **Micro-LED Grid Structure**:
   - Icons must be composed of discrete dots or micro-squares (matrix elements), similar to the Nothing Phone Glyph Interface and CMF design.
   - Reference icon: `res/drawable/ic_dotmatrix_playlist.xml` and `res/drawable/ic_folder_recent.xml`.
2. **Icon Sizing & Visual Weight**:
   - Standard action bar / button icons: `24dp` to `32dp`.
   - Grid dot diameter: `1.5dp` to `2.5dp` depending on icon size.
   - Dot color: `#FFFFFF` (standard), `#D71921` (active/selected), or `#80FFFFFF` (dim).

### Example Dot Matrix Icon Vector XML:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="28dp"
    android:height="28dp"
    android:viewportWidth="28"
    android:viewportHeight="28">
    <path
        android:fillColor="#D71921"
        android:pathData="
            M4,6 A1.5,1.5 0 1,1 4,9 A1.5,1.5 0 1,1 4,6 Z
            M9,6 A1.5,1.5 0 1,1 9,9 A1.5,1.5 0 1,1 9,6 Z
            M14,6 A1.5,1.5 0 1,1 14,9 A1.5,1.5 0 1,1 14,6 Z
            M19,6 A1.5,1.5 0 1,1 19,9 A1.5,1.5 0 1,1 19,6 Z
            M24,6 A1.5,1.5 0 1,1 24,9 A1.5,1.5 0 1,1 24,6 Z" />
</vector>
```

---

## 🔤 3. Typography & Badges

1. **NDOT Font Everywhere**:
   - Every `TextView`, `Button`, `Badge`, and label must declare:
     ```xml
     android:fontFamily="@font/ndot"
     android:textStyle="bold"
     ```
   - Labels should be uppercase (`ALL CAPS`), e.g., `FOLDERS`, `ALL VIDEOS`, `ONLINE STREAM`, `RECENTLY WATCHED`, `HISTORY ⊞`, `RESUME`, `START OVER`.
2. **Status Badges & Pills**:
   - Badges (like `1080p`, `5.1`, `NEW`, `4K`, `FIT`) must use pill background with NDOT font (`textSize="8sp"` to `10sp`).
   - Active badge background: Nothing Red `#D71921` or Translucent Black `#CC000000`.

---

## 📊 4. Progress Bars & Seekbars (Dot-Matrix LED Style)

**Strict Rule: Never use standard solid progress bars (`ProgressBar`). Always use custom dot-matrix components.**

1. **Video Card & History Progress**:
   - Use `com.nothing.player.DotMatrixProgressBar`.
   - Layout:
     ```xml
     <com.nothing.player.DotMatrixProgressBar
         android:id="@+id/video_progress_bar"
         android:layout_width="match_parent"
         android:layout_height="5dp"
         android:layout_alignParentBottom="true"
         android:visibility="gone" />
     ```
   - Active watched portion: Nothing Red (`#D71921`) micro-dots.
   - Unwatched portion: Dim LED dots (`#26FFFFFF`).
   - Unplayed videos: Visibility MUST be `View.GONE`.
2. **Player Seekbar**:
   - Use `com.nothing.player.DotMatrixSeekBar`.
   - Digital LED time counters use `com.nothing.player.DotMatrixTextView`.

---

## 📱 5. Screen Hierarchy & Navigation Rules

1. **Top Header & Search**:
   - Search bar at the top (`et_search_videos`).
   - Category filter buttons (`FOLDERS`, `ALL VIDEOS`, `ONLINE STREAM`) MUST be placed directly beneath the search bar.
2. **Recently Watched Section**:
   - Located below the category buttons.
   - **Crucial Rule**: Recently Watched carousel is **ONLY visible on the Root Home Page** (`currentSelectedFolder == null`).
   - When entering **any folder** (e.g., "Recently", "Movies", "Downloads"), the carousel must be strictly set to `View.GONE`.
3. **Folders as Playlists**:
   - Tapping a video inside any folder must automatically enqueue all folder videos into the playlist with the dot-matrix playlist icon.

---

## 🛠️ 6. Pre-Implementation Checklist for Any UI Work
Before submitting any UI code, verify:
- [ ] Is background pure pitch black (`#000000`)?
- [ ] Are accent highlights Nothing Signature Red (`#D71921`)?
- [ ] Are all fonts using `@font/ndot` in uppercase?
- [ ] Are all icons micro-dot matrix or Nothing industrial style?
- [ ] Are progress bars using `DotMatrixProgressBar` instead of solid standard bars?
- [ ] Is Recently Watched strictly hidden inside folders?
