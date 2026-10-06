package com.nothing.player;

import android.app.Activity;
import android.app.AlertDialog;
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
    private int currentSortMode = SORT_DATE_DESC;

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private TextView chipFolders, chipAllVideos, chipNetworkStream;
    private ImageButton btnOverflowMenu;

    // Multi-select Views
    private View selectionActionBar, searchFilterBar;
    private TextView tvSelectionCount;
    private Button btnSelectAll, btnDeleteSelected;
    private ImageButton btnCloseSelection;

    // Recently Watched Carousel & History Views
    private View sectionHistory;
    private RecyclerView recyclerHistory;
    private Button btnOpenHistorySheet;
    private HistoryAdapter historyAdapter;

    private List<MediaItem> allVideos = new ArrayList<>();
    private boolean isFoldersMode = true;
    private String currentSelectedFolder = null;
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

        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentViewMode = prefs.getInt(KEY_VIEW_MODE, VideoAdapter.VIEW_MODE_GRID_2);
            currentSortMode = prefs.getInt(KEY_SORT_MODE, SORT_DATE_DESC);
        }

        adapter = new VideoAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);
        applyCurrentViewMode();

        chipFolders.setOnClickListener(v -> {
            isFoldersMode = true;
            currentSelectedFolder = null;
            updateChipsUI();
            filterAndDisplay();
        });

        chipAllVideos.setOnClickListener(v -> {
            isFoldersMode = false;
            currentSelectedFolder = null;
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
        btnDeleteSelected.setOnClickListener(v -> confirmDeleteSelected());

        loadVideos();
        updateHistorySection();
        return view;
    }

    private void updateHistorySection() {
        if (getContext() == null || sectionHistory == null || historyAdapter == null) return;
        
        // Show recently watched ONLY on the root home page (not inside any opened folder or active search)
        String query = etSearch != null ? etSearch.getText().toString().trim() : "";
        if (currentSelectedFolder != null || !query.isEmpty()) {
            sectionHistory.setVisibility(View.GONE);
            return;
        }

        List<PlaybackHistoryManager.HistoryItem> history = PlaybackHistoryManager.getHistoryList(getContext());
        if (history != null && !history.isEmpty()) {
            sectionHistory.setVisibility(View.VISIBLE);
            historyAdapter.setItems(history);
        } else {
            sectionHistory.setVisibility(View.GONE);
        }
    }

    private void onHistoryItemClick(PlaybackHistoryManager.HistoryItem item) {
        if (getContext() == null || item == null) return;
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
        if (selected.isEmpty()) return;

        int count = selected.size();
        NothingDialogHelper.showConfirmDialog(
            getContext(),
            "DELETE " + count + " ITEM" + (count > 1 ? "S" : "") + "?",
            "These files will be permanently deleted from device storage.",
            "DELETE",
            () -> deleteSelectedItems(selected),
            "CANCEL",
            null
        );
    }

    private void deleteSelectedItems(Set<Object> selected) {
        if (getActivity() == null) return;
        List<MediaItem> itemsToDelete = new ArrayList<>();

        for (Object item : selected) {
            if (item instanceof MediaItem) {
                itemsToDelete.add((MediaItem) item);
            } else if (item instanceof VideoAdapter.FolderItem) {
                VideoAdapter.FolderItem folder = (VideoAdapter.FolderItem) item;
                for (MediaItem v : allVideos) {
                    if (folder.name.equalsIgnoreCase(v.folder)) {
                        itemsToDelete.add(v);
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
        if (isFoldersMode && currentSelectedFolder == null) {
            chipFolders.setBackgroundResource(R.drawable.bg_chip_selected);
            chipFolders.setTextColor(getResources().getColor(R.color.nothing_black));
            chipAllVideos.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipAllVideos.setTextColor(getResources().getColor(R.color.nothing_white_70));
        } else {
            chipAllVideos.setBackgroundResource(R.drawable.bg_chip_selected);
            chipAllVideos.setTextColor(getResources().getColor(R.color.nothing_black));
            chipFolders.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipFolders.setTextColor(getResources().getColor(R.color.nothing_white_70));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHistorySection();
        if (adapter != null) {
            filterAndDisplay();
        }
    }

    private void filterAndDisplay() {
        updateHistorySection();
        applyCurrentViewMode();

        String query = etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        List<Object> displayItems = new ArrayList<>();

        if (isFoldersMode && currentSelectedFolder == null && query.isEmpty()) {
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
                folderItems.add(new VideoAdapter.FolderItem(folderName, count, hasNew, false));
            }

            // Sort folders
            if (currentSortMode == SORT_NAME_DESC) {
                Collections.sort(folderItems, (a, b) -> b.name.compareToIgnoreCase(a.name));
            } else {
                Collections.sort(folderItems, (a, b) -> a.name.compareToIgnoreCase(b.name));
            }
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
    }

    private void applyCurrentViewMode() {
        if (getContext() == null || recyclerView == null) return;
        String query = etSearch != null ? etSearch.getText().toString().trim() : "";
        if (isFoldersMode && currentSelectedFolder == null && query.isEmpty()) {
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

    private void showVideosOptionsMenu() {
        if (getContext() == null) return;
        List<NothingDialogHelper.MenuItemOption> options = new ArrayList<>();

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
        } else if (allVideos != null) {
            for (MediaItem v : allVideos) {
                if (currentSelectedFolder != null && !currentSelectedFolder.equalsIgnoreCase(v.folder)) {
                    continue;
                }
                activeList.add(v);
            }
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
    public void onFolderClick(String folderName) {
        currentSelectedFolder = folderName;
        isFoldersMode = false;
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
        if (currentSelectedFolder != null) {
            currentSelectedFolder = null;
            isFoldersMode = true;
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
