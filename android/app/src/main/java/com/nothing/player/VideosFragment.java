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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private int currentCategory = CATEGORY_FOLDERS;
    private int currentSortMode = SORT_DATE_DESC;
    private final Set<String> pinnedFolders = new HashSet<>();

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private TextView chipFolders, chipSeries, chipMovies, chipAllVideos, chipNetworkStream;
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
    private String currentSelectedFolder = null;
    private String currentSelectedSeries = null;
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;

    private final MediaAutoScanner.OnMediaChangeListener mediaChangeListener = () -> {
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
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
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(getContext(), "Items deleted successfully", Toast.LENGTH_SHORT).show();
                    loadVideos(false);
                }
            }
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
            updateChipsUI();
            filterAndDisplay();
        });

        if (chipSeries != null) {
            chipSeries.setOnClickListener(v -> {
                currentCategory = CATEGORY_SERIES;
                currentSelectedFolder = null;
                currentSelectedSeries = null;
                updateChipsUI();
                filterAndDisplay();
            });
        }

        if (chipMovies != null) {
            chipMovies.setOnClickListener(v -> {
                currentCategory = CATEGORY_MOVIES;
                currentSelectedFolder = null;
                currentSelectedSeries = null;
                updateChipsUI();
                filterAndDisplay();
            });
        }

        chipAllVideos.setOnClickListener(v -> {
            currentCategory = CATEGORY_ALL_VIDEOS;
            currentSelectedFolder = null;
            currentSelectedSeries = null;
            updateChipsUI();
            filterAndDisplay();
        });

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
                int count = selected.size();
                NothingDialogHelper.showConfirmDialog(
                        getContext(),
                        "MOVE TO RECYCLE BIN",
                        "Move " + count + " item" + (count > 1 ? "s" : "") + " to Recycle Bin?\n\n(Files are kept safe for 30 days)",
                        "MOVE TO TRASH",
                        () -> {
                            int moved = 0;
                            for (Object obj : selected) {
                                if (obj instanceof MediaItem) {
                                    if (TrashManager.moveToTrash(getContext(), (MediaItem) obj)) {
                                        moved++;
                                    }
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
        if (currentSelectedFolder != null || currentSelectedSeries != null || !query.isEmpty()) {
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

        // Show recently watched on root FOLDERS page
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
            if (adapter != null) adapter.notifyDataSetChanged();
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

    private void confirmDeleteSelected() {
        Set<Object> selected = adapter.getSelectedItems();
        if (selected.isEmpty() || getContext() == null) return;

        int count = selected.size();
        List<String> actions = Arrays.asList(
                "MOVE TO RECYCLE BIN (30-DAY RECOVERY)",
                "MOVE TO PRIVATE VAULT (PIN LOCKED)",
                "PERMANENTLY DELETE FROM DEVICE"
        );

        NothingDialogHelper.showSelectionDialog(getContext(), "MANAGE " + count + " SELECTED ITEM" + (count > 1 ? "S" : ""), actions, 0, (index, item) -> {
            if (index == 0) {
                // Move to Recycle Bin
                int moved = 0;
                for (Object obj : selected) {
                    if (obj instanceof MediaItem) {
                        if (TrashManager.moveToTrash(getContext(), (MediaItem) obj)) {
                            moved++;
                        }
                    }
                }
                adapter.clearSelection();
                loadVideos(false);
                Toast.makeText(getContext(), "Moved " + moved + " item(s) to Recycle Bin", Toast.LENGTH_SHORT).show();
            } else if (index == 1) {
                // Move to Private Vault
                authenticateVaultAccess(() -> moveSelectedToVault(selected));
            } else if (index == 2) {
                // Permanent Delete
                deleteSelectedItems(selected);
            }
        });
    }

    private void moveSelectedToVault(Set<Object> selected) {
        if (getContext() == null) return;
        int moved = 0;
        for (Object obj : selected) {
            if (obj instanceof MediaItem) {
                if (VaultManager.moveToVault(getContext(), (MediaItem) obj)) {
                    moved++;
                }
            }
        }
        adapter.clearSelection();
        loadVideos(false);
        Toast.makeText(getContext(), "Moved " + moved + " item(s) to Private Vault", Toast.LENGTH_SHORT).show();
    }

    private void deleteSelectedItems(Set<Object> selected) {
        if (getActivity() == null) return;
        List<MediaItem> itemsToDelete = new ArrayList<>();

        for (Object item : selected) {
            if (item instanceof MediaItem) {
                itemsToDelete.add((MediaItem) item);
            } else if (item instanceof VideoAdapter.FolderItem) {
                VideoAdapter.FolderItem folder = (VideoAdapter.FolderItem) item;
                if ("series".equalsIgnoreCase(folder.folderType)) {
                    for (MediaItem v : allVideos) {
                        if (folder.name.equalsIgnoreCase(v.seriesName)) {
                            itemsToDelete.add(v);
                        }
                    }
                } else {
                    for (MediaItem v : allVideos) {
                        if (folder.name.equalsIgnoreCase(v.folder)) {
                            itemsToDelete.add(v);
                        }
                    }
                }
            }
        }

        FileDeleteHelper.deleteMediaFiles(getActivity(), itemsToDelete, deleteLauncher, (deletedCount, deletedPaths) -> {
            if (getContext() == null) return;
            List<MediaItem> remaining = new ArrayList<>();
            for (MediaItem v : allVideos) {
                if (!deletedPaths.contains(v.path)) remaining.add(v);
            }
            allVideos = remaining;
            adapter.clearSelection();
            filterAndDisplay();

            if (deletedCount > 0) {
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
    }

    private void setChipState(TextView chip, boolean isSelected) {
        if (chip == null || getContext() == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(getResources().getColor(R.color.nothing_black));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(getResources().getColor(R.color.nothing_white_70));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHistorySection();
        loadVideos(false);
        if (adapter != null) {
            filterAndDisplay();
        }
    }

    private void filterAndDisplay() {
        updateHistorySection();
        applyCurrentViewMode();

        String query = etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "";
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
                    if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase().contains(query))) {
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
                    if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase().contains(query))) {
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

                    if (!query.isEmpty() && !seriesName.toLowerCase().contains(query)) {
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

                    if (posterUrl == null && getContext() != null) {
                        OnlineMetadataManager.enrichSeries(getContext(), seriesName, info -> {
                            if (isAdded() && currentCategory == CATEGORY_SERIES && currentSelectedSeries == null) {
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
                    Collections.sort(seriesItems, (a, b) -> b.name.compareToIgnoreCase(a.name));
                } else {
                    Collections.sort(seriesItems, (a, b) -> a.name.compareToIgnoreCase(b.name));
                }
                displayItems.addAll(seriesItems);
                adapter.setItems(displayItems, true, false);
            } else {
                List<MediaItem> episodes = new ArrayList<>();
                for (MediaItem v : allVideos) {
                    if (currentSelectedSeries.equalsIgnoreCase(v.seriesName)) {
                        if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase().contains(query))) {
                            continue;
                        }
                        episodes.add(v);
                    }
                }
                sortEpisodes(episodes);
                displayItems.addAll(episodes);
                adapter.setItems(displayItems, false, false);
            }
        } else if (currentCategory == CATEGORY_MOVIES) {
            List<MediaItem> movieList = new ArrayList<>();
            for (MediaItem v : allVideos) {
                if ("movie".equalsIgnoreCase(v.mediaCategory)) {
                    if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase().contains(query))) {
                        continue;
                    }
                    movieList.add(v);

                    if (v.posterUrl == null || v.posterUrl.isEmpty()) {
                        OnlineMetadataManager.enrichItem(getContext(), v, info -> {
                            if (isAdded() && currentCategory == CATEGORY_MOVIES) {
                                adapter.notifyDataSetChanged();
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
                if (!query.isEmpty() && (v.title == null || !v.title.toLowerCase().contains(query))) {
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
                || (currentCategory == CATEGORY_SERIES && currentSelectedSeries == null && query.isEmpty());

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
                            Toast.makeText(getContext(), "Biometric recognition failed", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, String errString) {
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
        final EditText input = new EditText(getContext());
        input.setHint("M3U URL or local file path");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);

        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("IMPORT IPTV / M3U PLAYLIST")
                .setMessage("Paste an IPTV M3U stream URL or local playlist path:")
                .setView(input)
                .setPositiveButton("LOAD CHANNELS", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) return;
                    Toast.makeText(getContext(), "Parsing M3U Playlist...", Toast.LENGTH_SHORT).show();
                    M3uParser.loadM3u(url, new M3uParser.ParseCallback() {
                        @Override
                        public void onParsed(List<M3uParser.M3uChannel> channels) {
                            if (channels.isEmpty()) {
                                Toast.makeText(getContext(), "No stream channels found in M3U", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            List<String> names = new ArrayList<>();
                            for (M3uParser.M3uChannel ch : channels) {
                                names.add("[" + ch.group + "] " + ch.name);
                            }
                            NothingDialogHelper.showSelectionDialog(getContext(), "IPTV CHANNELS (" + channels.size() + ")", names, 0, (chIdx, chItem) -> {
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
                            Toast.makeText(getContext(), "Failed to load M3U: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void showBackgroundDownloaderDialog() {
        if (getContext() == null) return;
        final EditText input = new EditText(getContext());
        input.setHint("https://example.com/video.mp4");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setTypeface(android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL));

        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("DOWNLOAD ONLINE STREAM")
                .setMessage("Enter direct video/stream URL to download in background:")
                .setView(input)
                .setPositiveButton("DOWNLOAD", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        StreamDownloadHelper.downloadMedia(getContext(), url, "Stream_" + System.currentTimeMillis());
                    } else {
                        Toast.makeText(getContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
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
            if (h != null && h.getProgressPercent() >= 90) {
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
            deleteSelectedItems(new HashSet<>(watchedEpisodes));
            Toast.makeText(getContext(), "Reclaimed " + totalSizeStr + " of storage", Toast.LENGTH_SHORT).show();
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
            if (adapter != null) adapter.notifyDataSetChanged();
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
        boolean isPinned = pinnedFolders.contains(folder.name);
        List<String> options = Arrays.asList(
                isPinned ? "📌 UNPIN FOLDER FROM TOP" : "📌 PIN FOLDER TO TOP",
                "OPEN FOLDER"
        );
        NothingDialogHelper.showSelectionDialog(getContext(), folder.name.toUpperCase(), options, 0, (index, item) -> {
            if (index == 0) {
                if (isPinned) {
                    pinnedFolders.remove(folder.name);
                    Toast.makeText(getContext(), "Unpinned " + folder.name, Toast.LENGTH_SHORT).show();
                } else {
                    pinnedFolders.add(folder.name);
                    Toast.makeText(getContext(), "Pinned " + folder.name + " to top", Toast.LENGTH_SHORT).show();
                }
                if (getContext() != null) {
                    getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putStringSet("pinned_folders", new HashSet<>(pinnedFolders)).apply();
                }
                filterAndDisplay();
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
        if ("series".equalsIgnoreCase(folder.folderType)) {
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
        sheet.show(getParentFragmentManager(), "FileInfoBottomSheet");
    }

    public boolean handleBackPress() {
        if (adapter.isSelectionMode()) {
            adapter.clearSelection();
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
        Button btnCancelStream = dialogView.findViewById(R.id.btn_cancel_stream);
        ImageButton btnCloseDialog = dialogView.findViewById(R.id.btn_close_dialog);
        LinearLayout containerRecentStreams = dialogView.findViewById(R.id.container_recent_streams);

        if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        if (btnCancelStream != null) btnCancelStream.setOnClickListener(v -> dialog.dismiss());

        // Load recent streams
        if (containerRecentStreams != null && getContext() != null) {
            List<String> streams = StreamUrlHelper.getRecentStreams(getContext());
            for (String s : streams) {
                if (!s.trim().isEmpty()) {
                    TextView tv = new TextView(getContext());
                    tv.setText(s);
                    tv.setTextColor(getResources().getColor(R.color.nothing_white_70));
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
}
