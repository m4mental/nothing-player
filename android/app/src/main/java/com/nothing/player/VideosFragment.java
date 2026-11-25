package com.nothing.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.Window;
import android.view.Gravity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class VideosFragment extends Fragment implements VideoAdapter.OnItemClickListener {
    private static final String PREFS_NAME = "nothing_player_prefs";
    private static final String KEY_VIEW_MODE = "pref_videos_view_mode";
    private static final String KEY_SORT_MODE = "pref_videos_sort_mode";

    public static final int SORT_DATE_DESC = 0;
    public static final int SORT_DATE_ASC = 1;
    public static final int SORT_NAME_ASC = 2;
    public static final int SORT_NAME_DESC = 3;
    public static final int SORT_SIZE_DESC = 4;
    public static final int SORT_DURATION_DESC = 5;

    private int currentViewMode = VideoAdapter.VIEW_MODE_GRID_2;
    public static final int CATEGORY_FOLDERS = 0;
    public static final int CATEGORY_SERIES = 1;
    public static final int CATEGORY_MOVIES = 2;
    public static final int CATEGORY_ALL_VIDEOS = 3;
    public static final int CATEGORY_PLAYLISTS = 4;

    private int currentCategory = CATEGORY_FOLDERS;
    private String currentSelectedPlaylist = null;
    private int currentSortMode = SORT_DATE_DESC;
    private final Set<String> pinnedFolders = new HashSet<>();

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private TextView chipFolders, chipSeries, chipMovies, chipAllVideos, chipPlaylists, chipNetworkStream;
    private ImageButton btnOverflowMenu;

    // Multi-select Views
    private View selectionActionBar, searchFilterBar;
    private TextView tvSelectionCount;
    private Button btnSelectAll, btnVaultSelected, btnTrashSelected, btnDeleteSelected;
    private ImageButton btnCloseSelection;

    // Recently Watched Carousel & History Views
    private View sectionHistory;
    private RecyclerView recyclerHistory;
    private Button btnOpenHistorySheet;
    private HistoryAdapter historyAdapter;

    // Smart Continue Series Views
    private View sectionContinueSeries;
    private TextView tvContinueSeriesName, tvContinueSeriesEpisode, tvContinueSeriesTimeLeft;
    private ImageView ivContinueSeriesThumb;
    private DotMatrixProgressBar pbContinueSeriesProgress;
    private Button btnContinueSeriesResume;
    private MediaItem continueSeriesItem = null;

    private List<MediaItem> allVideos = new ArrayList<>();
    private final Set<String> attemptedSeriesMeta = new HashSet<>();
    private final Set<String> attemptedMovieMeta = new HashSet<>();
    private String currentSelectedFolder = null;
    private String currentSelectedSeries = null;
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;

    private final MediaAutoScanner.OnMediaChangeListener mediaChangeListener = () -> {
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                Context ctx = getContext();
                if (ctx != null) {
                    allVideos = MediaRepository.getCachedVideos(ctx.getApplicationContext());
                    filterAndDisplay();
                    updateHistorySection();
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
        View view = inflater.inflate(R.layout.fragment_videos, container, false);

        recyclerView = view.findViewById(R.id.recycler_videos);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_videos);
        etSearch = view.findViewById(R.id.et_search_videos);
        chipFolders = view.findViewById(R.id.chip_folders);
        chipSeries = view.findViewById(R.id.chip_series);
        chipMovies = view.findViewById(R.id.chip_movies);
        chipAllVideos = view.findViewById(R.id.chip_all_videos);
        chipPlaylists = view.findViewById(R.id.chip_playlists);
        chipNetworkStream = view.findViewById(R.id.chip_network_stream);
        btnOverflowMenu = view.findViewById(R.id.btn_videos_overflow_menu);

        if (btnOverflowMenu != null) {
            btnOverflowMenu.setOnClickListener(v -> showVideosOptionsMenu());
        }

        selectionActionBar = view.findViewById(R.id.selection_action_bar);
        searchFilterBar = view.findViewById(R.id.search_filter_bar);
        tvSelectionCount = view.findViewById(R.id.tv_selection_count);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnVaultSelected = view.findViewById(R.id.btn_vault_selected);
        btnTrashSelected = view.findViewById(R.id.btn_trash_selected);
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected);
        btnCloseSelection = view.findViewById(R.id.btn_close_selection);

        // History Carousel Views
        sectionHistory = view.findViewById(R.id.section_history_carousel);
        recyclerHistory = view.findViewById(R.id.recycler_history_horizontal);
        btnOpenHistorySheet = view.findViewById(R.id.btn_open_history_sheet);

        if (recyclerHistory != null) {
            recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            historyAdapter = new HistoryAdapter(getContext(), this::onHistoryItemClick);
            recyclerHistory.setAdapter(historyAdapter);
        }

        if (btnOpenHistorySheet != null) {
            btnOpenHistorySheet.setOnClickListener(v -> showHistoryBottomSheet());
        }

        // Smart Continue Series Views
        sectionContinueSeries = view.findViewById(R.id.section_continue_series);
        tvContinueSeriesName = view.findViewById(R.id.tv_continue_series_name);
        tvContinueSeriesEpisode = view.findViewById(R.id.tv_continue_series_episode);
        tvContinueSeriesTimeLeft = view.findViewById(R.id.tv_continue_series_time_left);
        ivContinueSeriesThumb = view.findViewById(R.id.iv_continue_series_thumb);
        pbContinueSeriesProgress = view.findViewById(R.id.pb_continue_series_progress);
        btnContinueSeriesResume = view.findViewById(R.id.btn_continue_series_resume);

        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentViewMode = prefs.getInt(KEY_VIEW_MODE, VideoAdapter.VIEW_MODE_GRID_2);
            currentSortMode = prefs.getInt(KEY_SORT_MODE, SORT_DATE_DESC);
        Set<String> savedPinned = prefs.getStringSet("pinned_folders", null);
        if (savedPinned != null) {
            pinnedFolders.addAll(savedPinned);
        }
        }

        adapter = new VideoAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);
        applyCurrentViewMode();

        chipFolders.setOnClickListener(v -> {
            currentCategory = CATEGORY_FOLDERS;
            currentSelectedFolder = null;
            currentSelectedSeries = null;
            currentSelectedPlaylist = null;
            updateChipsUI();
            filterAndDisplay();
        });

        if (chipSeries != null) {
            chipSeries.setOnClickListener(v -> {
                currentCategory = CATEGORY_SERIES;
                currentSelectedFolder = null;
                currentSelectedSeries = null;
                currentSelectedPlaylist = null;
                updateChipsUI();
                filterAndDisplay();
            });
        }

        if (chipMovies != null) {
            chipMovies.setOnClickListener(v -> {
                currentCategory = CATEGORY_MOVIES;
                currentSelectedFolder = null;
                currentSelectedSeries = null;
                currentSelectedPlaylist = null;
                updateChipsUI();
                filterAndDisplay();
            });
        }

        chipAllVideos.setOnClickListener(v -> {
            currentCategory = CATEGORY_ALL_VIDEOS;
            currentSelectedFolder = null;
            currentSelectedSeries = null;
            currentSelectedPlaylist = null;
            updateChipsUI();
            filterAndDisplay();
        });

        if (chipPlaylists != null) {
            chipPlaylists.setOnClickListener(v -> {
                currentCategory = CATEGORY_PLAYLISTS;
                currentSelectedFolder = null;
                currentSelectedSeries = null;
                currentSelectedPlaylist = null;
                updateChipsUI();
                filterAndDisplay();
            });
        }

        if (chipNetworkStream != null) {
            chipNetworkStream.setOnClickListener(v -> showNetworkStreamDialog());
        }

        swipeRefresh.setOnRefreshListener(this::loadVideos);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplay();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Setup Selection Action Bar Listeners
        btnCloseSelection.setOnClickListener(v -> adapter.clearSelection());
        btnSelectAll.setOnClickListener(v -> adapter.selectAll());
        if (btnVaultSelected != null) {
            btnVaultSelected.setOnClickListener(v -> {
                Set<Object> selected = adapter.getSelectedItems();
                if (selected.isEmpty() || getContext() == null) return;
                authenticateVaultAccess(() -> moveSelectedToVault(selected));
            });
        }
        if (btnTrashSelected != null) {
            btnTrashSelected.setOnClickListener(v -> {
                Set<Object> selected = adapter.getSelectedItems();
                if (selected.isEmpty() || getContext() == null) return;
                List<MediaItem> targetItems = expandSelectedMediaItems(selected);
                if (targetItems.isEmpty()) {
                    Toast.makeText(getContext(), "No movable videos in selection", Toast.LENGTH_SHORT).show();
                    return;
                }
                int count = targetItems.size();
                NothingDialogHelper.showConfirmDialog(
                        getContext(),
                        "MOVE TO RECYCLE BIN",
                        "Move " + count + " item" + (count > 1 ? "s" : "") + " to Recycle Bin?\n\n(Files are kept safe for 30 days)",
                        "MOVE TO TRASH",
                        () -> {
                            int moved = 0;
                            for (MediaItem mi : targetItems) {
                                if (TrashManager.moveToTrash(getContext(), mi)) {
                                    moved++;
                                }
                            }
                            adapter.clearSelection();
                            loadVideos(false);
                            Toast.makeText(getContext(), "Moved " + moved + " item(s) to Recycle Bin", Toast.LENGTH_SHORT).show();
                        },
                        "CANCEL",
                        null
                );
            });
        }
        btnDeleteSelected.setOnClickListener(v -> confirmDeleteSelected());

        loadVideos();
        updateHistorySection();
        return view;
    }

    private void updateHistorySection() {
        if (getContext() == null) return;

        String query = etSearch != null ? etSearch.getText().toString().trim() : "";
        if (currentSelectedFolder != null || currentSelectedSeries != null || currentSelectedPlaylist != null || !query.isEmpty()) {
            if (sectionHistory != null) sectionHistory.setVisibility(View.GONE);
            if (sectionContinueSeries != null) sectionContinueSeries.setVisibility(View.GONE);
            return;
        }

        // If in SERIES category, show smart Continue Series card
        if (currentCategory == CATEGORY_SERIES) {
            if (sectionHistory != null) sectionHistory.setVisibility(View.GONE);
            updateContinueSeriesCard();
            return;
        } else {
            if (sectionContinueSeries != null) sectionContinueSeries.setVisibility(View.GONE);
        }

        // Strictly show Recently Watched ONLY on the root FOLDERS Home Page (AGENTS.md §5)
        if (currentCategory != CATEGORY_FOLDERS) {
            if (sectionHistory != null) sectionHistory.setVisibility(View.GONE);
            return;
        }

        if (sectionHistory == null || historyAdapter == null) return;
        List<PlaybackHistoryManager.HistoryItem> history = PlaybackHistoryManager.getHistoryList(getContext());
        if (history != null && !history.isEmpty()) {
            sectionHistory.setVisibility(View.VISIBLE);
            historyAdapter.setItems(history);
        } else {
            sectionHistory.setVisibility(View.GONE);
        }
    }

    private void updateContinueSeriesCard() {
        if (sectionContinueSeries == null || getContext() == null) return;

        List<PlaybackHistoryManager.HistoryItem> history = PlaybackHistoryManager.getHistoryList(getContext());
        if (history == null || history.isEmpty()) {
            sectionContinueSeries.setVisibility(View.GONE);
            return;
        }

        PlaybackHistoryManager.HistoryItem matchedHistory = null;
        MediaItem matchedVideo = null;

        for (PlaybackHistoryManager.HistoryItem h : history) {
            if (h.isCompleted || (h.durationMs > 0 && h.getProgressPercent() >= 95)) {
                continue;
            }
            for (MediaItem v : allVideos) {
                String path = v.path != null ? v.path : v.contentUri;
                if (h.pathOrUri != null && h.pathOrUri.equalsIgnoreCase(path) && "series".equalsIgnoreCase(v.mediaCategory)) {
                    matchedHistory = h;
                    matchedVideo = v;
                    break;
                }
            }
            if (matchedVideo != null) break;
        }

        if (matchedVideo == null || matchedHistory == null) {
            sectionContinueSeries.setVisibility(View.GONE);
            return;
        }

        continueSeriesItem = matchedVideo;
        sectionContinueSeries.setVisibility(View.VISIBLE);

        if (tvContinueSeriesName != null) {
            tvContinueSeriesName.setText(matchedVideo.seriesName != null ? matchedVideo.seriesName : "Series");
        }

        if (tvContinueSeriesEpisode != null) {
            String epText = matchedVideo.episodeNumber > 0 ? String.format(java.util.Locale.US, "EP %02d", matchedVideo.episodeNumber) : "";
            if (matchedVideo.seasonNumber > 0) {
                epText += " • S" + matchedVideo.seasonNumber;
            }
            if (matchedVideo.cleanTitle != null && !matchedVideo.cleanTitle.isEmpty()) {
                epText += " • " + matchedVideo.cleanTitle;
            }
            tvContinueSeriesEpisode.setText(epText);
        }

        long remainingMs = Math.max(0, matchedHistory.durationMs - matchedHistory.positionMs);
        if (tvContinueSeriesTimeLeft != null) {
            int mins = (int) (remainingMs / (1000 * 60));
            tvContinueSeriesTimeLeft.setText(mins > 0 ? (mins + " MIN LEFT") : "IN PROGRESS");
        }

        if (pbContinueSeriesProgress != null) {
            pbContinueSeriesProgress.setProgress(matchedHistory.getProgressPercent());
        }

        if (ivContinueSeriesThumb != null) {
            Object thumb = matchedVideo.posterUrl != null && !matchedVideo.posterUrl.isEmpty() ? matchedVideo.posterUrl : matchedVideo.path;
            com.bumptech.glide.Glide.with(getContext())
                    .load(thumb)
                    .centerCrop()
                    .into(ivContinueSeriesThumb);
        }

        View.OnClickListener resumeListener = v -> {
            if (continueSeriesItem != null) onVideoClick(continueSeriesItem);
        };
        sectionContinueSeries.setOnClickListener(resumeListener);
        if (btnContinueSeriesResume != null) {
            btnContinueSeriesResume.setOnClickListener(resumeListener);
        }
    }

    private void onHistoryItemClick(PlaybackHistoryManager.HistoryItem item) {
        if (getContext() == null || item == null) return;
        if (BackgroundPlaybackManager.getInstance().hasActiveSession()) {
            String activePath = BackgroundPlaybackManager.getInstance().getCurrentPath();
            if (activePath != null && activePath.equals(item.pathOrUri)) {
                Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
                intent.putExtra("resume_active_playback", true);
                startActivity(intent);
                return;
            } else {
                BackgroundPlaybackManager.getInstance().stopAndRelease();
            }
        }
        Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(item.pathOrUri));
        intent.putExtra("path", item.pathOrUri);
        intent.putExtra("video_path", item.pathOrUri);
        intent.putExtra("contentUri", item.pathOrUri);
        intent.putExtra("video_uri", item.pathOrUri);
        intent.putExtra("title", item.title);
        intent.putExtra("video_title", item.title);
        intent.putExtra("position", item.positionMs);
        intent.putExtra("start_over", false);
        startActivity(intent);
    }

    private void showHistoryBottomSheet() {
        HistoryBottomSheet sheet = HistoryBottomSheet.newInstance();
        sheet.setOnHistoryChangedListener(() -> {
            updateHistorySection();
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            }
        });
        sheet.show(getChildFragmentManager(), "HistoryBottomSheet");
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

    private List<MediaItem> expandSelectedMediaItems(Set<Object> selected) {
        List<MediaItem> items = new ArrayList<>();
        if (selected == null || allVideos == null) return items;
        Set<String> seenKeys = new HashSet<>();

        for (Object obj : selected) {
            if (obj instanceof MediaItem) {
                MediaItem mi = (MediaItem) obj;
                String k = mi.path != null ? mi.path : mi.contentUri;
                if (seenKeys.add(k != null ? k : String.valueOf(mi.hashCode()))) {
                    items.add(mi);
                }
            } else if (obj instanceof VideoAdapter.FolderItem) {
                VideoAdapter.FolderItem folder = (VideoAdapter.FolderItem) obj;
                if (folder.isRecentFolder || "vault".equalsIgnoreCase(folder.folderType) || "trash".equalsIgnoreCase(folder.folderType)) {
                    continue;
                }
                if ("series".equalsIgnoreCase(folder.folderType)) {
                    for (MediaItem v : allVideos) {
                        if (folder.name != null && folder.name.equalsIgnoreCase(v.seriesName)) {
                            String k = v.path != null ? v.path : v.contentUri;
                            if (seenKeys.add(k != null ? k : String.valueOf(v.hashCode()))) items.add(v);
                        }
                    }
                } else if ("playlist".equalsIgnoreCase(folder.folderType) && getContext() != null) {
                    String plName = folder.name != null ? folder.name.replace("★ ", "") : "";
                    Set<String> plPaths = new HashSet<>(VirtualPlaylistManager.getPlaylistItems(getContext(), plName));
                    for (MediaItem v : allVideos) {
                        String k = v.path != null ? v.path : v.contentUri;
                        if (plPaths.contains(k) || (v.path != null && plPaths.contains(v.path)) || (v.contentUri != null && plPaths.contains(v.contentUri))) {
                            if (seenKeys.add(k != null ? k : String.valueOf(v.hashCode()))) items.add(v);
                        }
                    }
                } else {
                    for (MediaItem v : allVideos) {
                        if (folder.name != null && folder.name.equalsIgnoreCase(v.folder)) {
                            String k = v.path != null ? v.path : v.contentUri;
                            if (seenKeys.add(k != null ? k : String.valueOf(v.hashCode()))) items.add(v);
                        }
                    }
                }
            }
        }
        return items;
    }

    private void confirmDeleteSelected() {
        Set<Object> selected = adapter.getSelectedItems();
        if (selected.isEmpty() || getContext() == null) return;
        List<MediaItem> targetItems = expandSelectedMediaItems(selected);
        if (targetItems.isEmpty()) {
            Toast.makeText(getContext(), "No videos in selection", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = targetItems.size();
        List<String> actions = Arrays.asList(
                "MOVE TO RECYCLE BIN (30-DAY RECOVERY)",
                "MOVE TO PRIVATE VAULT (PIN LOCKED)",
                "PERMANENTLY DELETE FROM DEVICE"
        );

        NothingDialogHelper.showSelectionDialog(getContext(), "MANAGE " + count + " SELECTED ITEM" + (count > 1 ? "S" : ""), actions, 0, (index, item) -> {
            if (index == 0) {
                int moved = 0;
                for (MediaItem mi : targetItems) {
                    if (TrashManager.moveToTrash(getContext(), mi)) {
                        moved++;
                    }
                }
                adapter.clearSelection();
                loadVideos(false);
                Toast.makeText(getContext(), "Moved " + moved + " item(s) to Recycle Bin", Toast.LENGTH_SHORT).show();
            } else if (index == 1) {
                authenticateVaultAccess(() -> moveSelectedToVault(selected));
            } else if (index == 2) {
                deleteSelectedItems(selected, null);
            }
        });
    }

    private void moveSelectedToVault(Set<Object> selected) {
        if (!isAdded() || getContext() == null) return;
        List<MediaItem> targetItems = expandSelectedMediaItems(selected);
        int moved = 0;
        for (MediaItem mi : targetItems) {
            if (VaultManager.moveToVault(getContext(), mi)) {
                moved++;
            }
        }
        if (adapter != null) adapter.clearSelection();
        loadVideos(false);
        Toast.makeText(getContext(), "Moved " + moved + " item(s) to Private Vault", Toast.LENGTH_SHORT).show();
    }

    private void deleteSelectedItems(Set<Object> selected, @Nullable FileDeleteHelper.DeleteCallback postDeleteHook) {
        if (getActivity() == null) return;
        List<MediaItem> itemsToDelete = expandSelectedMediaItems(selected);
        if (itemsToDelete.isEmpty()) {
            if (adapter != null) adapter.clearSelection();
            return;
        }

        FileDeleteHelper.deleteMediaFiles(getActivity(), itemsToDelete, deleteLauncher, (deletedCount, deletedPaths) -> {
            if (!isAdded() || getContext() == null) return;
            List<MediaItem> remaining = new ArrayList<>();
            for (MediaItem v : allVideos) {
                boolean matched = (v.path != null && deletedPaths.contains(v.path))
                        || (v.contentUri != null && deletedPaths.contains(v.contentUri));
                if (!matched) remaining.add(v);
            }
            allVideos = remaining;
            if (adapter != null) adapter.clearSelection();
            filterAndDisplay();

            if (postDeleteHook != null) {
                postDeleteHook.onCompleted(deletedCount, deletedPaths);
            } else if (deletedCount > 0) {
                Toast.makeText(getContext(), "Deleted " + deletedCount + " item(s)", Toast.LENGTH_SHORT).show();
            }
            loadVideos(false);
        });
    }

    public void loadVideos() {
        loadVideos(true);
    }

    public void loadVideos(boolean allowSpinner) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        // 1. Instant 0ms load from cache on startup
        if (allVideos == null || allVideos.isEmpty()) {
            List<MediaItem> cached = MediaRepository.getCachedVideos(ctx.getApplicationContext());
            if (!cached.isEmpty()) {
                allVideos = cached;
                filterAndDisplay();
            }
        }

        // Only show spinner if explicitly pulled by user and list was already populated
        if (swipeRefresh != null && allowSpinner && (allVideos == null || allVideos.isEmpty())) {
            swipeRefresh.setRefreshing(true);
        }

        // 2. Perform background scan asynchronously without blocking UI
        MediaRepository.scanMedia(ctx.getApplicationContext(), (videos, audios) -> {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (videos != null) {
                        // Check if list changed
                        if (allVideos == null || allVideos.size() != videos.size() || !allVideos.equals(videos)) {
                            allVideos = videos;
                            filterAndDisplay();
                        }
                    }
                });
            }
        });
    }

    private void updateChipsUI() {
        setChipState(chipFolders, currentCategory == CATEGORY_FOLDERS);
        setChipState(chipSeries, currentCategory == CATEGORY_SERIES);
        setChipState(chipMovies, currentCategory == CATEGORY_MOVIES);
        setChipState(chipAllVideos, currentCategory == CATEGORY_ALL_VIDEOS);
        setChipState(chipPlaylists, currentCategory == CATEGORY_PLAYLISTS);
    }

    private void setChipState(TextView chip, boolean isSelected) {
        if (chip == null || getContext() == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(getContext(), R.color.nothing_black));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(getContext(), R.color.nothing_white_70));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentViewMode = prefs.getInt(KEY_VIEW_MODE, currentViewMode);
        }
        updateHistorySection();
        loadVideos(false);
        if (adapter != null) {
            filterAndDisplay();
        }
    }

    private void filterAndDisplay() {
        updateHistorySection();
        applyCurrentViewMode();

        String query = etSearch != null ? etSearch.getText().toString().trim().toLowerCase(Locale.US) : "";
        List<Object> displayItems = new ArrayList<>();

        if (currentCategory == CATEGORY_FOLDERS) {
            if (currentSelectedFolder == null && query.isEmpty()) {
                // Dedicated "Recently" folder at the top of the folder list
                if (allVideos != null && !allVideos.isEmpty()) {
                    boolean hasAnyNew = MediaStateManager.hasAnyNewVideos(getContext(), allVideos);
                    displayItems.add(new VideoAdapter.FolderItem("Recently", allVideos.size(), hasAnyNew, true));
                }


                Map<String, Integer> folderMap = new HashMap<>();
                for (MediaItem v : allVideos) {
                    String folder = v.folder != null ? v.folder : "Storage";
                    folderMap.put(folder, folderMap.getOrDefault(folder, 0) + 1);
                }

                List<VideoAdapter.FolderItem> folderItems = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : folderMap.entrySet()) {
                    String folderName = entry.getKey();
                    int count = entry.getValue();
                    boolean hasNew = MediaStateManager.folderHasNewVideos(getContext(), folderName, allVideos);
                    VideoAdapter.FolderItem fItem = new VideoAdapter.FolderItem(folderName, count, hasNew, false, "storage", null, null);
                    fItem.isPinned = pinnedFolders.contains(folderName);
                    folderItems.add(fItem);
                }

                // Sort folders: Pinned folders always on top
                Collections.sort(folderItems, (a, b) -> {
                    if (a.isPinned != b.isPinned) {
                        return a.isPinned ? -1 : 1;
                    }
                    if (currentSortMode == SORT_NAME_DESC) {
                        return b.name.compareToIgnoreCase(a.name);
                    }
                    return a.name.compareToIgnoreCase(b.name);
                });
                displayItems.addAll(folderItems);

                adapter.setItems(displayItems, true, false);
            } else if (currentSelectedFolder != null && currentSelectedFolder.equalsIgnoreCase("Recently")) {
                List<MediaItem> sortedVideos = new ArrayList<>();
                for (MediaItem v : allVideos) {
                    if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase(Locale.US).contains(query))) {
                        continue;
                    }
                    sortedVideos.add(v);
                }
                sortVideoList(sortedVideos);
                displayItems.addAll(sortedVideos);
                adapter.setItems(displayItems, false, true);
            } else {
                List<MediaItem> videoList = new ArrayList<>();
                for (MediaItem v : allVideos) {
                    if (currentSelectedFolder != null && !currentSelectedFolder.equalsIgnoreCase(v.folder)) {
                        continue;
                    }
                    if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase(Locale.US).contains(query))) {
                        continue;
                    }
                    videoList.add(v);
                }
                sortVideoList(videoList);
                displayItems.addAll(videoList);
                adapter.setItems(displayItems, false, false);
            }
        } else if (currentCategory == CATEGORY_SERIES) {
            if (currentSelectedSeries == null) {
                // Group videos into virtual series folders
                Map<String, List<MediaItem>> seriesMap = new HashMap<>();
                for (MediaItem v : allVideos) {
                    if ("series".equalsIgnoreCase(v.mediaCategory) && v.seriesName != null && !v.seriesName.isEmpty()) {
                        List<MediaItem> list = seriesMap.get(v.seriesName);
                        if (list == null) {
                            list = new ArrayList<>();
                            seriesMap.put(v.seriesName, list);
                        }
                        list.add(v);
                    }
                }

                List<VideoAdapter.FolderItem> seriesItems = new ArrayList<>();
                for (Map.Entry<String, List<MediaItem>> entry : seriesMap.entrySet()) {
                    String seriesName = entry.getKey();
                    List<MediaItem> epList = entry.getValue();

                    if (!query.isEmpty() && !seriesName.toLowerCase(Locale.US).contains(query)) {
                        continue;
                    }

                    boolean hasNew = false;
                    for (MediaItem ep : epList) {
                        String path = ep.path != null ? ep.path : ep.contentUri;
                        if (MediaStateManager.isVideoNew(getContext(), path)) {
                            hasNew = true;
                            break;
                        }
                    }

                    OnlineMetadataManager.MetadataInfo meta = OnlineMetadataManager.getCached(getContext(), seriesName);
                    String posterUrl = meta != null ? meta.posterUrl : null;
                    String subtitle = epList.size() + (epList.size() == 1 ? " Episode" : " Episodes");
                    if (meta != null && meta.releaseYear != null && !meta.releaseYear.isEmpty()) {
                        subtitle = meta.releaseYear + " • " + subtitle;
                    }

                    String seriesMetaKey = seriesName.trim().toLowerCase(Locale.US);
                    if ((posterUrl == null || posterUrl.isEmpty()) && getContext() != null && !attemptedSeriesMeta.contains(seriesMetaKey)) {
                        attemptedSeriesMeta.add(seriesMetaKey);
                        OnlineMetadataManager.enrichSeries(getContext(), seriesName, info -> {
                            if (isAdded() && info != null && info.posterUrl != null && !info.posterUrl.isEmpty()
                                    && currentCategory == CATEGORY_SERIES && currentSelectedSeries == null) {
                                filterAndDisplay();
                            }
                        });
                    }

                    seriesItems.add(new VideoAdapter.FolderItem(
                            seriesName,
                            epList.size(),
                            hasNew,
                            false,
                            "series",
                            posterUrl,
                            subtitle
                    ));
                }

                if (currentSortMode == SORT_NAME_DESC) {
                    Collections.sort(seriesItems, (a, b) -> {
                        String na = a != null && a.name != null ? a.name : "";
                        String nb = b != null && b.name != null ? b.name : "";
                        return nb.compareToIgnoreCase(na);
                    });
                } else {
                    Collections.sort(seriesItems, (a, b) -> {
                        String na = a != null && a.name != null ? a.name : "";
                        String nb = b != null && b.name != null ? b.name : "";
                        return na.compareToIgnoreCase(nb);
                    });
                }
                displayItems.addAll(seriesItems);
                adapter.setItems(displayItems, true, false);
            } else {
                List<MediaItem> episodes = new ArrayList<>();
                for (MediaItem v : allVideos) {
                    if (currentSelectedSeries.equalsIgnoreCase(v.seriesName)) {
                        if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase(Locale.US).contains(query))) {
                            continue;
                        }
                        episodes.add(v);
                    }
                }
                sortEpisodes(episodes);
                displayItems.addAll(episodes);
                adapter.setItems(displayItems, false, false);
            }
        } else if (currentCategory == CATEGORY_PLAYLISTS) {
            if (currentSelectedPlaylist == null) {
                List<String> names = VirtualPlaylistManager.getPlaylistNames(getContext());
                for (String pName : names) {
                    if (!query.isEmpty() && !pName.toLowerCase(Locale.US).contains(query)) {
                        continue;
                    }
                    List<String> items = VirtualPlaylistManager.getPlaylistItems(getContext(), pName);
                    displayItems.add(new VideoAdapter.FolderItem("★ " + pName, items.size(), false, false, "playlist", null, null));
                }
                adapter.setItems(displayItems, true, false);
            } else {
                List<String> pItems = VirtualPlaylistManager.getPlaylistItems(getContext(), currentSelectedPlaylist);
                Set<String> itemSet = new HashSet<>(pItems);
                List<MediaItem> pVideos = new ArrayList<>();
                for (MediaItem v : allVideos) {
                    String pKey = v.path != null ? v.path : v.contentUri;
                    if (itemSet.contains(pKey) || (v.path != null && itemSet.contains(v.path)) || (v.contentUri != null && itemSet.contains(v.contentUri))) {
                        if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase(Locale.US).contains(query))) {
                            continue;
                        }
                        pVideos.add(v);
                    }
                }
                sortVideoList(pVideos);
                displayItems.addAll(pVideos);
                adapter.setItems(displayItems, false, false);
            }
        } else if (currentCategory == CATEGORY_MOVIES) {
            List<MediaItem> movieList = new ArrayList<>();
            for (MediaItem v : allVideos) {
                if ("movie".equalsIgnoreCase(v.mediaCategory)) {
                    if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase(Locale.US).contains(query))) {
                        continue;
                    }
                    movieList.add(v);

                    String movieMetaKey = (v.path != null ? v.path : (v.title != null ? v.title : "")).toLowerCase(Locale.US);
                    if ((v.posterUrl == null || v.posterUrl.isEmpty()) && !attemptedMovieMeta.contains(movieMetaKey)) {
                        attemptedMovieMeta.add(movieMetaKey);
                        OnlineMetadataManager.enrichItem(getContext(), v, info -> {
                            if (isAdded() && info != null && info.posterUrl != null && !info.posterUrl.isEmpty()
                                    && currentCategory == CATEGORY_MOVIES && adapter != null && adapter.getItemCount() > 0) {
                                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                            }
                        });
                    }
                }
            }
            sortVideoList(movieList);
            displayItems.addAll(movieList);
            adapter.setItems(displayItems, false, false);
        } else { // CATEGORY_ALL_VIDEOS
            List<MediaItem> videoList = new ArrayList<>();
            for (MediaItem v : allVideos) {
                if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase(Locale.US).contains(query))) {
                    continue;
                }
                videoList.add(v);
            }
            sortVideoList(videoList);
            displayItems.addAll(videoList);
            adapter.setItems(displayItems, false, false);
        }
    }

    private void sortEpisodes(List<MediaItem> list) {
        if (list == null || list.size() <= 1) return;
        Collections.sort(list, (a, b) -> {
            int sCompare = Integer.compare(a.seasonNumber, b.seasonNumber);
            if (sCompare != 0) return sCompare;
            int eCompare = Integer.compare(a.episodeNumber, b.episodeNumber);
            if (eCompare != 0) return eCompare;
            String ta = a.title != null ? a.title : "";
            String tb = b.title != null ? b.title : "";
            return ta.compareToIgnoreCase(tb);
        });
    }

    private void applyCurrentViewMode() {
        if (getContext() == null || recyclerView == null) return;
        String query = etSearch != null ? etSearch.getText().toString().trim() : "";
        boolean isFolderGrid = (currentCategory == CATEGORY_FOLDERS && currentSelectedFolder == null && query.isEmpty())
                || (currentCategory == CATEGORY_SERIES && currentSelectedSeries == null && query.isEmpty())
                || (currentCategory == CATEGORY_PLAYLISTS && currentSelectedPlaylist == null);

        if (isFolderGrid) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
            if (adapter != null) adapter.setViewMode(VideoAdapter.VIEW_MODE_GRID_2);
        } else {
            int span = (currentViewMode == VideoAdapter.VIEW_MODE_GRID_3) ? 3 :
                       (currentViewMode == VideoAdapter.VIEW_MODE_LIST) ? 1 : 2;
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), span));
            if (adapter != null) adapter.setViewMode(currentViewMode);
        }
    }

    private void sortVideoList(List<MediaItem> list) {
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
            case SORT_SIZE_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.size, a.size));
                break;
            case SORT_DURATION_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.duration, a.duration));
                break;
        }
    }

    private void openPrivateVault() {
        if (getContext() == null) return;
        startActivity(new Intent(getContext(), VaultActivity.class));
    }

    private void authenticateVaultAccess(Runnable onSuccess) {
        if (getContext() == null) return;
        if (!VaultManager.isSecurityConfigured(getContext())) {
            Toast.makeText(getContext(), "Please configure Vault Security first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getContext(), VaultActivity.class));
            return;
        }

        if (VaultManager.isBiometricEnabled(getContext()) && BiometricAuthHelper.isBiometricAvailable(getContext()) && getActivity() != null) {
            String fallback = VaultManager.LOCK_TYPE_PATTERN.equals(VaultManager.getLockType(getContext())) ? "USE PATTERN" : "USE PIN";
            BiometricAuthHelper.authenticate(
                    getActivity(),
                    "AUTHENTICATE VAULT ACCESS",
                    "Touch fingerprint sensor or look at camera",
                    fallback,
                    new BiometricAuthHelper.BiometricCallback() {
                        @Override
                        public void onAuthenticationSucceeded() {
                            if (onSuccess != null) onSuccess.run();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            if (!isAdded() || getContext() == null) return;
                            Toast.makeText(getContext(), "Biometric recognition failed", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, String errString) {
                            if (!isAdded() || getContext() == null) return;
                            showFallbackAuthDialog(onSuccess);
                        }
                    }
            );
        } else {
            showFallbackAuthDialog(onSuccess);
        }
    }

    private void showFallbackAuthDialog(Runnable onSuccess) {
        if (VaultManager.LOCK_TYPE_PATTERN.equals(VaultManager.getLockType(getContext()))) {
            showVerifyPatternDialog(onSuccess);
        } else {
            showVerifyVaultPinDialog(onSuccess);
        }
    }

    private void showVerifyPatternDialog(Runnable onSuccess) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_vault_pattern, null);
        DotMatrixPatternView patternView = dialogView.findViewById(R.id.pv_dialog_pattern);
        TextView tvError = dialogView.findViewById(R.id.tv_dialog_pattern_error);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_pattern_cancel);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        patternView.setOnPatternListener(new DotMatrixPatternView.OnPatternListener() {
            @Override
            public void onPatternStart() {
                tvError.setVisibility(View.GONE);
            }

            @Override
            public void onPatternComplete(String pattern) {
                if (VaultManager.verifyPattern(getContext(), pattern)) {
                    dialog.dismiss();
                    if (onSuccess != null) onSuccess.run();
                } else {
                    tvError.setVisibility(View.VISIBLE);
                    patternView.showError();
                }
            }
        });

        dialog.show();
    }

    private void showSetVaultPinDialog(Runnable onSuccess) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_vault_pin, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_pin_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_pin_subtitle);
        EditText etPin = dialogView.findViewById(R.id.et_dialog_pin);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_pin_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_pin_confirm);

        tvTitle.setText("SET PRIVATE VAULT PIN");
        tvSubtitle.setText("Create a 4-digit security PIN for your private video vault:");
        btnConfirm.setText("SET PIN");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            if (pin.length() >= 4) {
                VaultManager.setPin(getContext(), pin);
                Toast.makeText(getContext(), "Vault PIN Set Successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (onSuccess != null) onSuccess.run();
            } else {
                Toast.makeText(getContext(), "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showVerifyVaultPinDialog(Runnable onSuccess) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_vault_pin, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_pin_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_pin_subtitle);
        EditText etPin = dialogView.findViewById(R.id.et_dialog_pin);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_pin_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_pin_confirm);

        tvTitle.setText("UNLOCK PRIVATE VAULT");
        tvSubtitle.setText("Enter your 4-digit PIN to move videos to vault:");
        btnConfirm.setText("UNLOCK");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            if (VaultManager.verifyPin(getContext(), pin)) {
                dialog.dismiss();
                if (onSuccess != null) onSuccess.run();
            } else {
                Toast.makeText(getContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showVaultContentDialog() {
        if (getContext() == null) return;
        List<VaultManager.VaultItem> items = VaultManager.getVaultItems(getContext());
        if (items.isEmpty()) {
            Toast.makeText(getContext(), "Private Vault is empty. Select videos and tap 'Move to Vault'", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> titles = new ArrayList<>();
        for (VaultManager.VaultItem v : items) {
            titles.add(v.title);
        }

        NothingDialogHelper.showSelectionDialog(getContext(), "PRIVATE VAULT (" + items.size() + ")", titles, 0, (index, item) -> {
            VaultManager.VaultItem selected = items.get(index);
            List<String> actions = Arrays.asList("PLAY IN PLAYER", "RESTORE TO STORAGE", "PERMANENT DELETE");
            NothingDialogHelper.showSelectionDialog(getContext(), selected.title, actions, 0, (aIdx, aItem) -> {
                if (aIdx == 0) {
                    Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
                    intent.putExtra("path", selected.vaultPath);
                    intent.putExtra("video_path", selected.vaultPath);
                    intent.putExtra("title", selected.title);
                    intent.putExtra("video_title", selected.title);
                    startActivity(intent);
                } else if (aIdx == 1) {
                    if (VaultManager.restoreItem(getContext(), selected)) {
                        Toast.makeText(getContext(), "Restored to storage", Toast.LENGTH_SHORT).show();
                        loadVideos(false);
                    }
                } else if (aIdx == 2) {
                    VaultManager.deletePermanently(getContext(), selected);
                    Toast.makeText(getContext(), "Deleted from vault", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void openRecycleBin() {
        if (getContext() == null) return;
        startActivity(new Intent(getContext(), RecycleBinActivity.class));
    }

    private void showM3uPlaylistDialog() {
        if (getContext() == null) return;
        NothingDialogHelper.showInputDialog(
                getContext(),
                "IMPORT IPTV / M3U PLAYLIST",
                "Paste an IPTV M3U stream URL or local playlist path:",
                "M3U URL or local file path",
                "LOAD CHANNELS",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI,
                url -> {
                    if (!isAdded() || getContext() == null || url.isEmpty()) return;
                    Toast.makeText(getContext(), "Parsing M3U Playlist...", Toast.LENGTH_SHORT).show();
                    M3uParser.loadM3u(url, new M3uParser.ParseCallback() {
                        @Override
                        public void onParsed(List<M3uParser.M3uChannel> channels) {
                            if (!isAdded() || getContext() == null) return;
                            if (channels.isEmpty()) {
                                Toast.makeText(getContext(), "No stream channels found in M3U", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            List<String> names = new ArrayList<>();
                            for (M3uParser.M3uChannel ch : channels) {
                                names.add("[" + ch.group + "] " + ch.name);
                            }
                            NothingDialogHelper.showSelectionDialog(getContext(), "IPTV CHANNELS (" + channels.size() + ")", names, 0, (chIdx, chItem) -> {
                                if (!isAdded() || getContext() == null) return;
                                M3uParser.M3uChannel sel = channels.get(chIdx);
                                Intent playerIntent = new Intent(getContext(), ExoVideoPlayerActivity.class);
                                playerIntent.setAction(Intent.ACTION_VIEW);
                                playerIntent.setData(Uri.parse(sel.streamUrl));
                                playerIntent.putExtra("video_uri", sel.streamUrl);
                                playerIntent.putExtra("title", sel.name);
                                playerIntent.putExtra("video_title", sel.name);
                                startActivity(playerIntent);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded() || getContext() == null) return;
                            Toast.makeText(getContext(), "Failed to load M3U: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
        );
    }

    private void showBackgroundDownloaderDialog() {
        if (getContext() == null) return;
        NothingDialogHelper.showInputDialog(
                getContext(),
                "DOWNLOAD ONLINE STREAM",
                "Enter direct video/stream URL to download in background:",
                "https://example.com/video.mp4",
                "DOWNLOAD",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI,
                url -> {
                    if (!isAdded() || getContext() == null) return;
                    if (!url.isEmpty()) {
                        StreamDownloadHelper.downloadMedia(getContext(), url, "Stream_" + System.currentTimeMillis());
                    } else {
                        Toast.makeText(getContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showVideosOptionsMenu() {
        if (getContext() == null) return;
        List<NothingDialogHelper.MenuItemOption> options = new ArrayList<>();

        options.add(new NothingDialogHelper.MenuItemOption(
                "🔒 PRIVATE VAULT",
                "PIN-protected hidden video vault",
                this::openPrivateVault
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "🗑️ RECYCLE BIN",
                "30-day trash recovery (" + TrashManager.getTrashItems(getContext()).size() + " items)",
                this::openRecycleBin
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "📡 IPTV / M3U STREAM",
                "Load live channel playlists",
                this::showM3uPlaylistDialog
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "⬇️ BACKGROUND DOWNLOADER",
                "Download video/stream to storage",
                this::showBackgroundDownloaderDialog
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "VIEW MODE",
                "CURRENT: " + getViewModeTitle(currentViewMode),
                this::showViewModeDialog
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "SORT VIDEOS",
                "CURRENT: " + getSortModeTitle(currentSortMode),
                this::showSortDialog
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "RESCAN STORAGE",
                "Auto-detect newly downloaded media",
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
                "CLEAN WATCHED EPISODES",
                "Reclaim storage by removing finished episodes",
                this::showCleanWatchedEpisodesDialog
        ));

        options.add(new NothingDialogHelper.MenuItemOption(
                "SETTINGS",
                "Hardware acceleration & gestures",
                () -> {
                    if (getContext() != null) {
                        startActivity(new Intent(getContext(), SettingsActivity.class));
                    }
                }
        ));

        NothingDialogHelper.showMenuDialog(getContext(), "VIDEO OPTIONS", options);
    }

    private void showCleanWatchedEpisodesDialog() {
        if (getContext() == null || getActivity() == null) return;

        List<MediaItem> watchedEpisodes = new ArrayList<>();
        long totalBytes = 0;

        for (MediaItem item : allVideos) {
            String path = item.path != null ? item.path : item.contentUri;
            if (path == null) continue;
            if (currentSelectedSeries != null && !currentSelectedSeries.equalsIgnoreCase(item.seriesName)) {
                continue;
            }
            if (!"series".equalsIgnoreCase(item.mediaCategory) && currentSelectedSeries == null) {
                if (item.episodeNumber <= 0 && item.seasonNumber <= 0) continue;
            }

            PlaybackHistoryManager.HistoryItem h = PlaybackHistoryManager.getProgress(getContext(), path);
            if (h != null && (h.isCompleted || h.getProgressPercent() >= 90)) {
                watchedEpisodes.add(item);
                totalBytes += item.size;
            }
        }

        if (watchedEpisodes.isEmpty()) {
            Toast.makeText(getContext(), "No completed episodes found (>= 90% watched)", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_clean_watched);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialog.findViewById(R.id.tv_clean_watched_subtitle);
        LinearLayout layoutList = dialog.findViewById(R.id.layout_clean_episodes_list);
        TextView tvReclaimable = dialog.findViewById(R.id.tv_clean_total_reclaimable);
        Button btnKeep = dialog.findViewById(R.id.btn_clean_cancel);
        Button btnDelete = dialog.findViewById(R.id.btn_clean_confirm);

        double totalMb = totalBytes / (1024.0 * 1024.0);
        String totalSizeStr = totalMb >= 1024.0
                ? String.format(java.util.Locale.US, "%.2f GB", totalMb / 1024.0)
                : String.format(java.util.Locale.US, "%.1f MB", totalMb);

        if (tvSubtitle != null) {
            tvSubtitle.setText(String.format(java.util.Locale.US, "%d WATCHED EPISODE(S) FOUND. DELETE TO FREE STORAGE?", watchedEpisodes.size()));
        }
        if (tvReclaimable != null) {
            tvReclaimable.setText("TOTAL RECLAIMABLE: " + totalSizeStr);
        }

        if (layoutList != null) {
            layoutList.removeAllViews();
            float density = getResources().getDisplayMetrics().density;
            for (MediaItem ep : watchedEpisodes) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, (int) (6 * density), 0, (int) (6 * density));

                TextView dot = new TextView(getContext());
                dot.setText("● ");
                dot.setTextColor(Color.parseColor("#D71921"));
                dot.setTextSize(9f);
                row.addView(dot);

                TextView name = new TextView(getContext());
                String title = ep.cleanTitle != null && !ep.cleanTitle.isEmpty() ? ep.cleanTitle : (ep.title != null ? ep.title : "Episode");
                if (ep.episodeNumber > 0) {
                    title = String.format(java.util.Locale.US, "EP %02d • %s", ep.episodeNumber, title);
                }
                name.setText(title.toUpperCase());
                name.setTextColor(Color.WHITE);
                name.setTextSize(8f);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                name.setLayoutParams(lp);
                row.addView(name);

                double epMb = ep.size / (1024.0 * 1024.0);
                String epSizeStr = epMb >= 1024.0
                        ? String.format(java.util.Locale.US, "%.2f GB", epMb / 1024.0)
                        : String.format(java.util.Locale.US, "%.1f MB", epMb);

                TextView sz = new TextView(getContext());
                sz.setText(epSizeStr);
                sz.setTextColor(Color.parseColor("#88FFFFFF"));
                sz.setTextSize(7.5f);
                row.addView(sz);

                layoutList.addView(row);
            }
        }

        if (btnKeep != null) btnKeep.setOnClickListener(v -> dialog.dismiss());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            deleteSelectedItems(new HashSet<>(watchedEpisodes), (deletedCount, deletedPaths) -> {
                if (getContext() != null && deletedCount > 0) {
                    Toast.makeText(getContext(), "Reclaimed " + totalSizeStr + " of storage", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showViewModeDialog() {
        if (getContext() == null) return;
        List<String> items = Arrays.asList(
                "GRID (2 COLUMNS)",
                "GRID (3 COLUMNS COMPACT)",
                "LIST (DETAILED ROWS)"
        );
        int selectedIndex = 0;
        if (currentViewMode == VideoAdapter.VIEW_MODE_GRID_3) selectedIndex = 1;
        else if (currentViewMode == VideoAdapter.VIEW_MODE_LIST) selectedIndex = 2;

        NothingDialogHelper.showSelectionDialog(getContext(), "SELECT VIEW MODE", items, selectedIndex, (index, item) -> {
            int newMode = VideoAdapter.VIEW_MODE_GRID_2;
            if (index == 1) newMode = VideoAdapter.VIEW_MODE_GRID_3;
            else if (index == 2) newMode = VideoAdapter.VIEW_MODE_LIST;

            currentViewMode = newMode;
            if (getContext() != null) {
                getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putInt(KEY_VIEW_MODE, currentViewMode).apply();
            }
            applyCurrentViewMode();
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            }
        });
    }

    private void showSortDialog() {
        if (getContext() == null) return;
        List<String> items = Arrays.asList(
                "DATE (NEWEST FIRST)",
                "DATE (OLDEST FIRST)",
                "NAME (A - Z)",
                "NAME (Z - A)",
                "SIZE (LARGEST FIRST)",
                "DURATION (LONGEST FIRST)"
        );

        NothingDialogHelper.showSelectionDialog(getContext(), "SORT VIDEOS BY", items, currentSortMode, (index, item) -> {
            currentSortMode = index;
            if (getContext() != null) {
                getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putInt(KEY_SORT_MODE, currentSortMode).apply();
            }
            filterAndDisplay();
        });
    }

    private void triggerManualRescan() {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "SCANNING STORAGE FOR NEW MEDIA...", Toast.LENGTH_SHORT).show();
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        MediaAutoScanner.getInstance().triggerImmediateScan(getContext());
    }

    private String getViewModeTitle(int mode) {
        if (mode == VideoAdapter.VIEW_MODE_GRID_3) return "GRID (3 COLUMNS)";
        if (mode == VideoAdapter.VIEW_MODE_LIST) return "LIST (ROWS)";
        return "GRID (2 COLUMNS)";
    }

    private String getSortModeTitle(int sort) {
        switch (sort) {
            case SORT_DATE_ASC: return "DATE (OLDEST FIRST)";
            case SORT_NAME_ASC: return "NAME (A - Z)";
            case SORT_NAME_DESC: return "NAME (Z - A)";
            case SORT_SIZE_DESC: return "SIZE (LARGEST)";
            case SORT_DURATION_DESC: return "DURATION (LONGEST)";
            case SORT_DATE_DESC:
            default: return "DATE (NEWEST FIRST)";
        }
    }

    @Override
    public void onVideoClick(MediaItem video) {
        if (getContext() != null) {
            String path = video.path != null ? video.path : video.contentUri;
            MediaStateManager.markVideoOpened(getContext(), path);
        }

        Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
        if (BackgroundPlaybackManager.getInstance().hasActiveSession()) {
            String activePath = BackgroundPlaybackManager.getInstance().getCurrentPath();
            String clickedPath = video.path != null ? video.path : video.contentUri;
            if (activePath != null && (activePath.equals(clickedPath) || (video.contentUri != null && activePath.equals(video.contentUri)))) {
                intent.putExtra("resume_active_playback", true);
            } else {
                BackgroundPlaybackManager.getInstance().stopAndRelease();
            }
        }
        intent.putExtra("path", video.path);
        intent.putExtra("video_path", video.path);
        intent.putExtra("contentUri", video.contentUri);
        intent.putExtra("video_uri", video.contentUri);
        intent.putExtra("title", video.title);
        intent.putExtra("video_title", video.title);

        // Build folder / active queue
        ArrayList<String> queuePaths = new ArrayList<>();
        ArrayList<String> queueUris = new ArrayList<>();
        ArrayList<String> queueTitles = new ArrayList<>();
        ArrayList<Long> queueDurations = new ArrayList<>();
        int clickedIndex = 0;

        List<MediaItem> activeList = new ArrayList<>();
        if (currentSelectedFolder != null && currentSelectedFolder.equalsIgnoreCase("Recently")) {
            activeList = new ArrayList<>(allVideos);
            Collections.sort(activeList, (a, b) -> Long.compare(b.addedAt, a.addedAt));
        } else if (currentSelectedSeries != null) {
            for (MediaItem v : allVideos) {
                if (currentSelectedSeries.equalsIgnoreCase(v.seriesName)) {
                    activeList.add(v);
                }
            }
            sortEpisodes(activeList);
        } else if (currentSelectedPlaylist != null) {
            List<String> pItems = VirtualPlaylistManager.getPlaylistItems(getContext(), currentSelectedPlaylist);
            Set<String> itemSet = new HashSet<>(pItems);
            for (MediaItem v : allVideos) {
                String pKey = v.path != null ? v.path : v.contentUri;
                if (itemSet.contains(pKey) || (v.path != null && itemSet.contains(v.path)) || (v.contentUri != null && itemSet.contains(v.contentUri))) {
                    activeList.add(v);
                }
            }
            sortVideoList(activeList);
        } else if (currentCategory == CATEGORY_MOVIES) {
            for (MediaItem v : allVideos) {
                if ("movie".equalsIgnoreCase(v.mediaCategory)) {
                    activeList.add(v);
                }
            }
            sortVideoList(activeList);
        } else if (allVideos != null) {
            for (MediaItem v : allVideos) {
                if (currentSelectedFolder != null && !currentSelectedFolder.equalsIgnoreCase(v.folder)) {
                    continue;
                }
                activeList.add(v);
            }
            sortVideoList(activeList);
        }

        for (int i = 0; i < activeList.size(); i++) {
            MediaItem item = activeList.get(i);
            queuePaths.add(item.path != null ? item.path : "");
            queueUris.add(item.contentUri != null ? item.contentUri : "");
            queueTitles.add(item.title != null ? item.title : "");
            queueDurations.add(item.duration);
            if (video.path != null && video.path.equals(item.path)) {
                clickedIndex = i;
            } else if (video.contentUri != null && video.contentUri.equals(item.contentUri)) {
                clickedIndex = i;
            }
        }

        intent.putStringArrayListExtra("playlist_paths", queuePaths);
        intent.putStringArrayListExtra("playlist_uris", queueUris);
        intent.putStringArrayListExtra("playlist_titles", queueTitles);
        intent.putExtra("playlist_index", clickedIndex);

        startActivity(intent);
    }

    @Override
    public void onFolderLongClick(VideoAdapter.FolderItem folder) {
        if (folder == null || getContext() == null) return;
        if (folder.isRecentFolder || "playlist".equalsIgnoreCase(folder.folderType)) {
            if (adapter != null) adapter.startSelection(folder);
            return;
        }
        boolean isPinned = pinnedFolders.contains(folder.name);
        List<String> options = Arrays.asList(
                isPinned ? "UNPIN FOLDER FROM TOP" : "PIN FOLDER TO TOP",
                "SELECT FOLDER ITEMS",
                "OPEN FOLDER"
        );
        String title = folder.name != null ? folder.name.toUpperCase(Locale.US) : "FOLDER";
        NothingDialogHelper.showSelectionDialog(getContext(), title, options, 0, (index, item) -> {
            if (!isAdded() || getContext() == null) return;
            if (index == 0) {
                if (isPinned) {
                    pinnedFolders.remove(folder.name);
                    Toast.makeText(getContext(), "Unpinned " + folder.name, Toast.LENGTH_SHORT).show();
                } else {
                    pinnedFolders.add(folder.name);
                    Toast.makeText(getContext(), "Pinned " + folder.name + " to top", Toast.LENGTH_SHORT).show();
                }
                getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putStringSet("pinned_folders", new HashSet<>(pinnedFolders)).apply();
                filterAndDisplay();
            } else if (index == 1) {
                if (adapter != null) adapter.startSelection(folder);
            } else {
                onFolderClick(folder);
            }
        });
    }

    @Override
    public void onFolderClick(VideoAdapter.FolderItem folder) {
        if (folder == null) return;
        if ("🔒 PRIVATE VAULT".equalsIgnoreCase(folder.name) || "Private Vault".equalsIgnoreCase(folder.name)) {
            openPrivateVault();
            return;
        }
        if ("🗑️ RECYCLE BIN".equalsIgnoreCase(folder.name) || "Recycle Bin".equalsIgnoreCase(folder.name)) {
            openRecycleBin();
            return;
        }
        if ("playlist".equalsIgnoreCase(folder.folderType)) {
            currentSelectedPlaylist = folder.name != null ? folder.name.replace("★ ", "") : "";
        } else if ("series".equalsIgnoreCase(folder.folderType)) {
            currentSelectedSeries = folder.name;
        } else {
            currentSelectedFolder = folder.name;
        }
        updateChipsUI();
        filterAndDisplay();
    }

    @Override
    public void onMenuClick(MediaItem video) {
        FileInfoBottomSheet sheet = FileInfoBottomSheet.newInstance(video);
        sheet.setOnMetadataUpdatedListener(() -> {
            if (isAdded()) loadVideos(false);
        });
        sheet.show(getParentFragmentManager(), "FileInfoBottomSheet");
    }

    public boolean handleBackPress() {
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.clearSelection();
            return true;
        }
        if (currentSelectedPlaylist != null) {
            currentSelectedPlaylist = null;
            updateChipsUI();
            filterAndDisplay();
            return true;
        }
        if (currentSelectedSeries != null) {
            currentSelectedSeries = null;
            updateChipsUI();
            filterAndDisplay();
            return true;
        }
        if (currentSelectedFolder != null) {
            currentSelectedFolder = null;
            updateChipsUI();
            filterAndDisplay();
            return true;
        }
        return false;
    }

    private void showNetworkStreamDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_network_stream, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etStreamUrl = dialogView.findViewById(R.id.et_stream_url);
        Button btnPlayStream = dialogView.findViewById(R.id.btn_play_stream);
        Button btnPlayDirect = dialogView.findViewById(R.id.btn_play_direct);
        Button btnPasteUrl = dialogView.findViewById(R.id.btn_paste_url);
        Button btnPcNasBrowser = dialogView.findViewById(R.id.btn_open_pc_nas_browser);
        Button btnCancelStream = dialogView.findViewById(R.id.btn_cancel_stream);
        ImageButton btnCloseDialog = dialogView.findViewById(R.id.btn_close_dialog);
        LinearLayout containerRecentStreams = dialogView.findViewById(R.id.container_recent_streams);

        if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        if (btnCancelStream != null) btnCancelStream.setOnClickListener(v -> dialog.dismiss());
        if (btnPcNasBrowser != null) {
            btnPcNasBrowser.setOnClickListener(v -> {
                dialog.dismiss();
                showPcNasBrowserDialog();
            });
        }

        // Load recent streams
        if (containerRecentStreams != null && getContext() != null) {
            List<String> streams = StreamUrlHelper.getRecentStreams(getContext());
            for (String s : streams) {
                if (!s.trim().isEmpty()) {
                    TextView tv = new TextView(getContext());
                    tv.setText(s);
                    tv.setTextColor(ContextCompat.getColor(getContext(), R.color.nothing_white_70));
                    tv.setTextSize(11);
                    tv.setPadding(0, 10, 0, 10);
                    tv.setOnClickListener(v -> {
                        etStreamUrl.setText(s);
                        etStreamUrl.setSelection(s.length());
                    });
                    containerRecentStreams.addView(tv);
                }
            }
        }

        // Auto paste if clipboard has URL
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            if (item != null && item.getText() != null) {
                String extracted = StreamUrlHelper.extractUrl(item.getText().toString());
                if (extracted != null) {
                    etStreamUrl.setText(extracted);
                    etStreamUrl.setSelection(extracted.length());
                }
            }
        }

        if (btnPasteUrl != null) {
            btnPasteUrl.setOnClickListener(v -> {
                if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    if (item != null && item.getText() != null) {
                        String extracted = StreamUrlHelper.extractUrl(item.getText().toString());
                        if (extracted != null) {
                            etStreamUrl.setText(extracted);
                            etStreamUrl.setSelection(etStreamUrl.getText().length());
                        } else {
                            etStreamUrl.setText(item.getText().toString().trim());
                            etStreamUrl.setSelection(etStreamUrl.getText().length());
                        }
                    }
                }
            });
        }

        Runnable startStreamRunnable = () -> {
            String rawInput = etStreamUrl.getText().toString().trim();
            String url = StreamUrlHelper.extractUrl(rawInput);
            if (url == null && StreamUrlHelper.isOnlineStream(rawInput)) {
                url = rawInput;
            }

            if (url == null || url.isEmpty() || !StreamUrlHelper.isOnlineStream(url)) {
                Toast.makeText(getContext(), "Please enter or paste a valid stream URL", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to recent streams
            StreamUrlHelper.saveRecentStream(requireContext(), url);

            dialog.dismiss();

            Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.putExtra("video_uri", url);
            intent.putExtra("contentUri", url);
            intent.putExtra("path", url);
            intent.putExtra("video_path", url);
            intent.putExtra("title", "Online Stream");
            intent.putExtra("video_title", "Online Stream");
            startActivity(intent);
        };

        if (btnPlayStream != null) btnPlayStream.setOnClickListener(v -> startStreamRunnable.run());
        if (btnPlayDirect != null) btnPlayDirect.setOnClickListener(v -> startStreamRunnable.run());
        dialog.show();
    }

    private void showPcNasBrowserDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_pc_network_browser, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_network_dialog);
        Button btnScanWifi = dialogView.findViewById(R.id.btn_scan_wifi_devices);
        LinearLayout containerDevices = dialogView.findViewById(R.id.container_discovered_devices);
        Button btnProtocol = dialogView.findViewById(R.id.btn_select_protocol);
        EditText etHost = dialogView.findViewById(R.id.et_network_host);
        EditText etPath = dialogView.findViewById(R.id.et_network_path);
        EditText etUser = dialogView.findViewById(R.id.et_network_user);
        EditText etPass = dialogView.findViewById(R.id.et_network_pass);
        Button btnConnect = dialogView.findViewById(R.id.btn_connect_network_stream);

        final String[] protocols = {"SMB", "WEBDAV", "FTP", "HTTP"};
        final int[] protoIdx = {0};

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnProtocol != null) {
            btnProtocol.setOnClickListener(v -> {
                protoIdx[0] = (protoIdx[0] + 1) % protocols.length;
                btnProtocol.setText(protocols[protoIdx[0]]);
            });
        }

        List<NetworkBrowserHelper.NetworkShareItem> saved = NetworkBrowserHelper.getSavedShares(getContext());
        if (containerDevices != null && !saved.isEmpty()) {
            containerDevices.setVisibility(View.VISIBLE);
            for (NetworkBrowserHelper.NetworkShareItem item : saved) {
                TextView row = new TextView(getContext());
                row.setText("● " + item.protocol.toUpperCase(Locale.US) + "  " + item.host + (item.sharePath != null ? item.sharePath : ""));
                row.setTextColor(Color.WHITE);
                row.setTextSize(9.5f);
                row.setPadding(8, 10, 8, 10);
                row.setOnClickListener(v -> {
                    for (int p = 0; p < protocols.length; p++) {
                        if (protocols[p].equalsIgnoreCase(item.protocol)) {
                            protoIdx[0] = p;
                            if (btnProtocol != null) btnProtocol.setText(protocols[p]);
                            break;
                        }
                    }
                    if (etHost != null) etHost.setText(item.host);
                    if (etPath != null) etPath.setText(item.sharePath);
                    if (etUser != null) etUser.setText(item.username);
                    if (etPass != null) etPass.setText(item.password);
                });
                containerDevices.addView(row);
            }
        }

        if (btnScanWifi != null) {
            btnScanWifi.setOnClickListener(v -> {
                String localIp = getLocalIpv4Address();
                if (localIp == null) {
                    Toast.makeText(getContext(), "Connect to Wi-Fi / LAN to scan devices", Toast.LENGTH_SHORT).show();
                    return;
                }
                btnScanWifi.setEnabled(false);
                btnScanWifi.setText("SCANNING SUBNET (" + localIp + ")...");
                if (containerDevices != null) {
                    containerDevices.setVisibility(View.VISIBLE);
                }
                NetworkBrowserHelper.scanLocalSubnet(localIp, new NetworkBrowserHelper.ScanCallback() {
                    @Override
                    public void onDeviceFound(String ip, String detectedService) {
                        if (!isAdded() || getContext() == null || !dialog.isShowing() || containerDevices == null) return;
                        TextView tv = new TextView(getContext());
                        tv.setText("● " + ip + "  (" + detectedService + ")");
                        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.nothing_red));
                        tv.setTextSize(9.5f);
                        tv.setPadding(8, 10, 8, 10);
                        tv.setOnClickListener(c -> {
                            if (etHost != null) etHost.setText(ip);
                            if (btnProtocol != null) {
                                if (detectedService.contains("FTP")) protoIdx[0] = 2;
                                else if (detectedService.contains("WEBDAV")) protoIdx[0] = 1;
                                else protoIdx[0] = 0;
                                btnProtocol.setText(protocols[protoIdx[0]]);
                            }
                        });
                        containerDevices.addView(tv);
                    }

                    @Override
                    public void onScanComplete(List<String> activeHosts) {
                        if (!isAdded() || !dialog.isShowing()) return;
                        btnScanWifi.setEnabled(true);
                        btnScanWifi.setText("● SCAN COMPLETE (" + activeHosts.size() + " FOUND)");
                    }
                });
            });
        }

        if (btnConnect != null) {
            btnConnect.setOnClickListener(v -> {
                if (getContext() == null) return;
                String host = etHost != null ? etHost.getText().toString().trim() : "";
                if (host.isEmpty()) {
                    Toast.makeText(getContext(), "Enter host or IP address", Toast.LENGTH_SHORT).show();
                    return;
                }
                String proto = btnProtocol != null ? btnProtocol.getText().toString().trim() : protocols[protoIdx[0]];
                String path = etPath != null ? etPath.getText().toString().trim() : "";
                String user = etUser != null ? etUser.getText().toString().trim() : "";
                String pass = etPass != null ? etPass.getText().toString().trim() : "";

                NetworkBrowserHelper.NetworkShareItem share = new NetworkBrowserHelper.NetworkShareItem(
                        proto + " " + host, proto, host, 0, user, pass, path);
                String streamUrl = share.toStreamUrl();
                NetworkBrowserHelper.saveShare(getContext(), share);
                StreamUrlHelper.saveRecentStream(getContext(), streamUrl);
                dialog.dismiss();

                Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(streamUrl));
                intent.putExtra("video_uri", streamUrl);
                intent.putExtra("contentUri", streamUrl);
                intent.putExtra("path", streamUrl);
                intent.putExtra("video_path", streamUrl);
                intent.putExtra("title", share.title);
                intent.putExtra("video_title", share.title);
                startActivity(intent);
            });
        }

        dialog.show();
    }

    private static String getLocalIpv4Address() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
