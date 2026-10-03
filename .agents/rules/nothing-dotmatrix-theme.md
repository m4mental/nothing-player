# NOTHING-PLAYER: Strict Nothing OS NDOT Dot-Matrix Design System

**MANDATORY PROJECT RULE FOR ALL AGENTS & ASSISTANTS:**
Every UI change, new feature, icon, layout, or visual element in this project MUST strictly follow the Nothing OS NDOT Dot-Matrix design system. No generic material designs allowed.

### Core Visual Directives:
1. **Color Palette**:
   - Backgrounds: Pure Pitch Black (`#000000`) and Dark Surface (`#121212`).
   - Accent & Active Elements: Nothing Signature Red (`#D71921`).
   - Inactive Dots: Subtle Faint White (`#26FFFFFF`).
   - Text & Active Icons: Pure White (`#FFFFFF`) with `@font/ndot`.
   - Never use generic blue, green, yellow, or standard material colors.

2. **Dot-Matrix Micro-LED Icons**:
   - Every section icon, navigation button, and status indicator MUST be a dot-matrix icon (like `ic_dotmatrix_playlist.xml` and `ic_folder_recent.xml`).
   - Never use standard solid Material vector icons without dot-matrix styling.

3. **Typography**:
   - All labels, titles, duration tags, and buttons must use `@font/ndot` in uppercase (`ALL CAPS`).

4. **Progress Bars & Seekbars**:
   - Never use standard Android `ProgressBar`.
   - Video progress on cards must use `com.nothing.player.DotMatrixProgressBar` with red active dots and dim inactive dots.
   - Unplayed videos must have progress bar set to `GONE`.
   - Player timeline seekbar must use `com.nothing.player.DotMatrixSeekBar`.

5. **Layout & Navigation Constraints**:
   - Category buttons (`FOLDERS`, `ALL VIDEOS`, `ONLINE STREAM`) MUST remain directly under the search bar.
   - Recently Watched carousel must ONLY be visible on the home page (`currentSelectedFolder == null`). When entering any folder, it MUST be `View.GONE`.
