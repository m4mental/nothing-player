package com.nothing.player;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.Activity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MusicFragment extends Fragment implements MusicAdapter.OnMusicClickListener {
    private static final String PREFS_NAME = "nothing_player_prefs";
    private static final String KEY_MUSIC_SORT = "pref_music_sort_mode";

    public static final int SORT_DATE_DESC = 0;
    public static final int SORT_DATE_ASC = 1;
    public static final int SORT_NAME_ASC = 2;
    public static final int SORT_NAME_DESC = 3;
    public static final int SORT_ARTIST_ASC = 4;
    public static final int SORT_DURATION_DESC = 5;

    private static final int TAB_TRACKS = 0;
    private static final int TAB_ARTISTS = 1;
    private static final int TAB_FAVORITES = 2;

    private int currentSortMode = SORT_DATE_DESC;
    private int currentTab = TAB_TRACKS;

    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private ImageButton btnOverflowMenu;
    private TextView chipTracks, chipArtists, chipFavorites;

    // Multi-select Views
    private View selectionActionBar;
    private TextView tvSelectionCount;
    private Button btnSelectAll, btnDeleteSelected;
    private ImageButton btnCloseSelection;

    private List<MediaItem> allTracks = new ArrayList<>();
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;

    private final MediaAutoScanner.OnMediaChangeListener mediaChangeListener = () -> {
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                Context ctx = getContext();
                if (ctx != null) {
                    allTracks = MediaRepository.getCachedAudios(ctx.getApplicationContext());
                    filterTracks();
                }
            });
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        MediaAutoScanner.getInstance().addListener(mediaChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaAutoScanner.getInstance().removeListener(mediaChangeListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> FileDeleteHelper.onActivityResult(getContext(), result.getResultCode() == Activity.RESULT_OK)
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);

        recyclerView = view.findViewById(R.id.recycler_music);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_music);
        etSearch = view.findViewById(R.id.et_search_music);
        btnOverflowMenu = view.findViewById(R.id.btn_music_overflow_menu);
        chipTracks = view.findViewById(R.id.chip_music_tracks);
        chipArtists = view.findViewById(R.id.chip_music_artists);
        chipFavorites = view.findViewById(R.id.chip_music_favorites);

        if (btnOverflowMenu != null) {
            btnOverflowMenu.setOnClickListener(v -> showMusicOptionsMenu());
        }

        if (chipTracks != null) {
            chipTracks.setOnClickListener(v -> {
                currentTab = TAB_TRACKS;
                updateMusicChipsUI();
                filterTracks();
            });
        }
        if (chipArtists != null) {
            chipArtists.setOnClickListener(v -> {
                currentTab = TAB_ARTISTS;
                updateMusicChipsUI();
                filterTracks();
            });
        }
        if (chipFavorites != null) {
            chipFavorites.setOnClickListener(v -> {
                currentTab = TAB_FAVORITES;
                updateMusicChipsUI();
                filterTracks();
            });
        }
        updateMusicChipsUI();

        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentSortMode = prefs.getInt(KEY_MUSIC_SORT, SORT_DATE_DESC);
        }

        selectionActionBar = view.findViewById(R.id.music_selection_action_bar);
        tvSelectionCount = view.findViewById(R.id.tv_music_selection_count);
        btnSelectAll = view.findViewById(R.id.btn_music_select_all);
        btnDeleteSelected = view.findViewById(R.id.btn_music_delete_selected);
        btnCloseSelection = view.findViewById(R.id.btn_close_music_selection);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MusicAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadTracks);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTracks();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Setup Selection Action Bar Listeners
        btnCloseSelection.setOnClickListener(v -> adapter.clearSelection());

        btnSelectAll.setOnClickListener(v -> adapter.selectAll());

        btnDeleteSelected.setOnClickListener(v -> confirmDeleteSelected());

        loadTracks();
        return view;
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        if (selectedCount > 0) {
            if (selectionActionBar != null) selectionActionBar.setVisibility(View.VISIBLE);
            if (tvSelectionCount != null) tvSelectionCount.setText(selectedCount + " selected");
        } else {
            if (selectionActionBar != null) selectionActionBar.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteSelected() {
        Set<MediaItem> selected = adapter.getSelectedTracks();
        if (selected.isEmpty()) return;

        int count = selected.size();
        NothingDialogHelper.showConfirmDialog(
            getContext(),
            "DELETE " + count + " TRACK" + (count > 1 ? "S" : "") + "?",
            "These audio files will be permanently deleted from device storage.",
            "DELETE",
            () -> deleteSelectedTracks(selected),
            "CANCEL",
            null
        );
    }

    private void deleteSelectedTracks(Set<MediaItem> selected) {
        if (getActivity() == null || selected == null || selected.isEmpty()) return;

        FileDeleteHelper.deleteMediaFiles(getActivity(), selected, deleteLauncher, (deletedCount, deletedPaths) -> {
            if (!isAdded() || getContext() == null) return;
            List<MediaItem> remaining = new ArrayList<>();
            for (MediaItem m : allTracks) {
                boolean matched = (m.path != null && deletedPaths.contains(m.path))
                        || (m.contentUri != null && deletedPaths.contains(m.contentUri));
                if (!matched) remaining.add(m);
            }
            allTracks = remaining;
            if (adapter != null) adapter.clearSelection();
            filterTracks();

            if (deletedCount > 0) {
                Toast.makeText(getContext(), "Deleted " + deletedCount + " track(s)", Toast.LENGTH_SHORT).show();
            }
            loadTracks(false);
        });
    }

    public void loadTracks() {
        loadTracks(true);
    }

    public void loadTracks(boolean allowSpinner) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        // 1. Instant 0ms load from cache on startup
        if (allTracks == null || allTracks.isEmpty()) {
            List<MediaItem> cached = MediaRepository.getCachedAudios(ctx.getApplicationContext());
            if (!cached.isEmpty()) {
                allTracks = cached;
                filterTracks();
            }
        }

        if (swipeRefresh != null && allowSpinner && (allTracks == null || allTracks.isEmpty())) {
            swipeRefresh.setRefreshing(true);
        }

        // 2. Perform background scan asynchronously without blocking UI
        MediaRepository.scanMedia(ctx.getApplicationContext(), (videos, audios) -> {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (audios != null) {
                        if (allTracks == null || allTracks.size() != audios.size() || !allTracks.equals(audios)) {
                            allTracks = audios;
                            filterTracks();
                        }
                    }
                });
            }
        });
    }

    private void updateMusicChipsUI() {
        setMusicChipState(chipTracks, currentTab == TAB_TRACKS);
        setMusicChipState(chipArtists, currentTab == TAB_ARTISTS);
        setMusicChipState(chipFavorites, currentTab == TAB_FAVORITES);
    }

    private void setMusicChipState(TextView chip, boolean isSelected) {
        if (chip == null || getContext() == null) return;
        chip.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(ContextCompat.getColor(getContext(), isSelected ? R.color.nothing_black : R.color.nothing_white_70));
    }

    private void filterTracks() {
        String query = etSearch != null ? etSearch.getText().toString().trim().toLowerCase(Locale.US) : "";
        Set<String> favPaths = new HashSet<>();
        if (currentTab == TAB_FAVORITES && getContext() != null) {
            favPaths.addAll(VirtualPlaylistManager.getPlaylistItems(getContext(), "FAVORITES"));
        }

        List<MediaItem> filtered = new ArrayList<>();
        for (MediaItem item : allTracks) {
            if (currentTab == TAB_FAVORITES) {
                boolean isFav = item.isFavorite
                        || (item.path != null && favPaths.contains(item.path))
                        || (item.contentUri != null && favPaths.contains(item.contentUri));
                if (!isFav) continue;
            }
            String title = item.title != null ? item.title.toLowerCase(Locale.US) : "";
            String artist = item.artist != null ? item.artist.toLowerCase(Locale.US) : "";
            if (query.isEmpty() || title.contains(query) || artist.contains(query)) {
                filtered.add(item);
            }
        }
        if (currentTab == TAB_ARTISTS) {
            Collections.sort(filtered, (a, b) -> {
                String aa = a.artist != null ? a.artist : "";
                String ab = b.artist != null ? b.artist : "";
                int cmp = aa.compareToIgnoreCase(ab);
                if (cmp != 0) return cmp;
                String ta = a.title != null ? a.title : "";
                String tb = b.title != null ? b.title : "";
                return ta.compareToIgnoreCase(tb);
            });
        } else {
            sortTracks(filtered);
        }
        if (adapter != null) adapter.setTracks(filtered);
    }

    private void sortTracks(List<MediaItem> list) {
        if (list == null || list.size() <= 1) return;
        switch (currentSortMode) {
            case SORT_DATE_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.addedAt, a.addedAt));
                break;
            case SORT_DATE_ASC:
                Collections.sort(list, (a, b) -> Long.compare(a.addedAt, b.addedAt));
                break;
            case SORT_NAME_ASC:
                Collections.sort(list, (a, b) -> {
                    String ta = a.title != null ? a.title : "";
                    String tb = b.title != null ? b.title : "";
                    return ta.compareToIgnoreCase(tb);
                });
                break;
            case SORT_NAME_DESC:
                Collections.sort(list, (a, b) -> {
                    String ta = a.title != null ? a.title : "";
                    String tb = b.title != null ? b.title : "";
                    return tb.compareToIgnoreCase(ta);
                });
                break;
            case SORT_ARTIST_ASC:
                Collections.sort(list, (a, b) -> {
                    String ta = a.artist != null ? a.artist : "";
                    String tb = b.artist != null ? b.artist : "";
                    return ta.compareToIgnoreCase(tb);
                });
                break;
            case SORT_DURATION_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.duration, a.duration));
                break;
        }
    }

    private void showMusicOptionsMenu() {
        if (getContext() == null) return;
        List<NothingDialogHelper.MenuItemOption> options = new ArrayList<>();

        options.add(new NothingDialogHelper.MenuItemOption(
                "SORT TRACKS",
                "CURRENT: " + getSortModeTitle(currentSortMode),
                this::showSortDialog
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "RESCAN STORAGE",
                "Auto-detect newly downloaded tracks",
                this::triggerManualRescan
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "AUDIO EQUALIZER",
                "Fine-tune sound & frequencies",
                () -> {
                    if (getContext() != null) {
                        startActivity(new Intent(getContext(), EqualizerActivity.class));
                    }
                }
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "SETTINGS",
                "Audio configurations & preferences",
                () -> {
                    if (getContext() != null) {
                        startActivity(new Intent(getContext(), SettingsActivity.class));
                    }
                }
        ));

        NothingDialogHelper.showMenuDialog(getContext(), "MUSIC OPTIONS", options);
    }

    private void showSortDialog() {
        if (getContext() == null) return;
        List<String> items = Arrays.asList(
                "DATE (NEWEST FIRST)",
                "DATE (OLDEST FIRST)",
                "TITLE (A - Z)",
                "TITLE (Z - A)",
                "ARTIST (A - Z)",
                "DURATION (LONGEST FIRST)"
        );

        NothingDialogHelper.showSelectionDialog(getContext(), "SORT TRACKS BY", items, currentSortMode, (index, item) -> {
            currentSortMode = index;
            if (getContext() != null) {
                getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putInt(KEY_MUSIC_SORT, currentSortMode).apply();
            }
            filterTracks();
        });
    }

    private void triggerManualRescan() {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "SCANNING STORAGE FOR NEW MEDIA...", Toast.LENGTH_SHORT).show();
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        MediaAutoScanner.getInstance().triggerImmediateScan(getContext());
    }

    private String getSortModeTitle(int sort) {
        switch (sort) {
            case SORT_DATE_ASC: return "DATE (OLDEST FIRST)";
            case SORT_NAME_ASC: return "TITLE (A - Z)";
            case SORT_NAME_DESC: return "TITLE (Z - A)";
            case SORT_ARTIST_ASC: return "ARTIST (A - Z)";
            case SORT_DURATION_DESC: return "DURATION (LONGEST)";
            case SORT_DATE_DESC:
            default: return "DATE (NEWEST FIRST)";
        }
    }

    @Override
    public void onTrackClick(MediaItem track, int position) {
        if (getActivity() instanceof MainActivity) {
            List<MediaItem> queue = adapter != null ? adapter.getTracks() : allTracks;
            int idx = queue.indexOf(track);
            if (idx < 0) idx = Math.max(0, Math.min(position, queue.size() - 1));
            ((MainActivity) getActivity()).playAudioTrack(track, queue, idx);
        }
    }

    @Override
    public void onMenuClick(MediaItem track) {
        FileInfoBottomSheet sheet = FileInfoBottomSheet.newInstance(track);
        sheet.show(getParentFragmentManager(), "FileInfoBottomSheet");
    }

    public void notifyCurrentTrackChanged(String trackId) {
        if (adapter != null) {
            adapter.setCurrentPlayingId(trackId);
        }
    }

    public boolean handleBackPress() {
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.clearSelection();
            return true;
        }
        return false;
    }
}
