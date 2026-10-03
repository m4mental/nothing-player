# NOTHING-PLAYER Project Guidelines & Design Contract

## 🛑 Strict Design Rule: Nothing OS NDOT Dot-Matrix Theme Only
Every UI screen, icon, section, badge, indicator, progress bar, and layout in this application MUST follow the **Nothing OS NDOT Dot-Matrix (Black & Red)** design language.

### 1. Color Palette (Black & Red Signature):
- **Background**: AMOLED Pure Black (`#000000`) & Dark Surface (`#121212`).
- **Accent / Active Elements**: Nothing Signature Red (`#D71921`).
- **Dim / Inactive Matrix Elements**: Subtle Faint LED White (`#26FFFFFF`).
- **Text & High Contrast**: Pure White (`#FFFFFF`) with `@font/ndot`.
- Generic primary colors (blue, green, yellow, purple) are strictly forbidden.

### 2. Dot-Matrix Micro-LED Icons:
- All icons and section graphics must be styled as discrete dot-matrix LED grids (like `ic_dotmatrix_playlist.xml` and `ic_folder_recent.xml`). Never use plain generic flat vector icons.

### 3. Typography:
- Always use `android:fontFamily="@font/ndot"` with `android:textStyle="bold"`. All labels and button text must be uppercase (`ALL CAPS`).

### 4. Progress Bars:
- Video card progress must use `com.nothing.player.DotMatrixProgressBar` (red active dots, dim inactive dots, dark translucent backing).
- Unplayed videos must hide progress (`View.GONE`).
- Seekbar must use `com.nothing.player.DotMatrixSeekBar`.

### 5. Layout & Navigation Directives:
- Top search bar must be directly followed by the category tabs: `FOLDERS`, `ALL VIDEOS`, `ONLINE STREAM`.
- **Recently Watched** carousel is strictly visible on the root Home Page only (`currentSelectedFolder == null`). It must be `View.GONE` inside any folder.
- Playing any video from inside a folder enqueues the folder as an active playlist with the dot-matrix playlist button.
