package com.nothing.player;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VideosFragment extends Fragment implements VideoAdapter.OnItemClickListener {
    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private TextView chipFolders, chipAllVideos, chipNetworkStream;

    // Multi-select Views
    private View selectionActionBar, searchFilterBar;
    private TextView tvSelectionCount;
    private Button btnSelectAll, btnDeleteSelected;
    private ImageButton btnCloseSelection;

    private List<MediaItem> allVideos = new ArrayList<>();
    private boolean isFoldersMode = true;
    private String currentSelectedFolder = null;

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

        selectionActionBar = view.findViewById(R.id.selection_action_bar);
        searchFilterBar = view.findViewById(R.id.search_filter_bar);
        tvSelectionCount = view.findViewById(R.id.tv_selection_count);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected);
        btnCloseSelection = view.findViewById(R.id.btn_close_selection);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new VideoAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);

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
        Set<Object> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) return;

        int count = selected.size();
        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Delete " + count + " Item" + (count > 1 ? "s" : "") + "?")
            .setMessage("These files will be permanently deleted from device storage.")
            .setPositiveButton("DELETE", (dialog, which) -> deleteSelectedItems(selected))
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void deleteSelectedItems(Set<Object> selected) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        ContentResolver resolver = ctx.getContentResolver();
        int deletedCount = 0;
        Set<String> deletedPaths = new HashSet<>();

        for (Object item : selected) {
            if (item instanceof MediaItem) {
                MediaItem video = (MediaItem) item;
                boolean deleted = false;
                if (video.contentUri != null) {
                    try {
                        int rows = resolver.delete(Uri.parse(video.contentUri), null, null);
                        if (rows > 0) deleted = true;
                    } catch (Exception ignored) {}
                }
                if (!deleted && video.path != null) {
                    try {
                        File f = new File(video.path);
                        if (f.exists() && f.delete()) deleted = true;
                    } catch (Exception ignored) {}
                }
                if (deleted) {
                    deletedCount++;
                    if (video.path != null) deletedPaths.add(video.path);
                }
            } else if (item instanceof VideoAdapter.FolderItem) {
                VideoAdapter.FolderItem folder = (VideoAdapter.FolderItem) item;
                // Delete all videos in this folder
                for (MediaItem v : allVideos) {
                    if (folder.name.equalsIgnoreCase(v.folder)) {
                        try {
                            if (v.contentUri != null) resolver.delete(Uri.parse(v.contentUri), null, null);
                            if (v.path != null) {
                                new File(v.path).delete();
                                deletedPaths.add(v.path);
                            }
                            deletedCount++;
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        // Immediately update cache and local list
        MediaRepository.removeItemsFromCache(ctx.getApplicationContext(), deletedPaths);
        List<MediaItem> remaining = new ArrayList<>();
        for (MediaItem v : allVideos) {
            if (!deletedPaths.contains(v.path)) remaining.add(v);
        }
        allVideos = remaining;
        adapter.clearSelection();
        filterAndDisplay();

        Toast.makeText(ctx, "Deleted " + deletedCount + " item(s)", Toast.LENGTH_SHORT).show();
        loadVideos(false);
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

    private void filterAndDisplay() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        List<Object> displayItems = new ArrayList<>();

        if (isFoldersMode && currentSelectedFolder == null && query.isEmpty()) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
            Map<String, Integer> folderMap = new HashMap<>();
            for (MediaItem v : allVideos) {
                String folder = v.folder != null ? v.folder : "Storage";
                folderMap.put(folder, folderMap.getOrDefault(folder, 0) + 1);
            }
            for (Map.Entry<String, Integer> entry : folderMap.entrySet()) {
                displayItems.add(new VideoAdapter.FolderItem(entry.getKey(), entry.getValue()));
            }
            adapter.setItems(displayItems, true);
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            for (MediaItem v : allVideos) {
                if (currentSelectedFolder != null && !currentSelectedFolder.equalsIgnoreCase(v.folder)) {
                    continue;
                }
                if (!query.isEmpty() && !v.title.toLowerCase().contains(query)) {
                    continue;
                }
                displayItems.add(v);
            }
            adapter.setItems(displayItems, false);
        }
    }

    @Override
    public void onVideoClick(MediaItem video) {
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
        if (allVideos != null) {
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
        Button btnPasteUrl = dialogView.findViewById(R.id.btn_paste_url);
        Button btnPlayDirect = dialogView.findViewById(R.id.btn_play_direct);
        Button btnPlayStream = dialogView.findViewById(R.id.btn_play_stream);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_stream);
        View btnCloseDialog = dialogView.findViewById(R.id.btn_close_dialog);
        LinearLayout containerRecent = dialogView.findViewById(R.id.container_recent_streams);
        TextView tvRecentLabel = dialogView.findViewById(R.id.tv_recent_streams_label);

        SharedPreferences prefs = requireContext().getSharedPreferences("nothing_streams_prefs", Context.MODE_PRIVATE);
        String recentRaw = prefs.getString("recent_streams", "");

        // Check clipboard for video stream links
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            if (item != null && item.getText() != null) {
                String text = item.getText().toString().trim();
                if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("rtsp://") || text.startsWith("rtmp://")) {
                    etStreamUrl.setText(text);
                    etStreamUrl.setSelection(text.length());
                }
            }
        }

        btnPasteUrl.setOnClickListener(v -> {
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                if (item != null && item.getText() != null) {
                    etStreamUrl.setText(item.getText().toString().trim());
                    etStreamUrl.setSelection(etStreamUrl.getText().length());
                }
            }
        });

        Runnable startStreamRunnable = () -> {
            String url = etStreamUrl.getText().toString().trim();
            if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("rtsp://") && !url.startsWith("rtmp://"))) {
                Toast.makeText(getContext(), "Please enter or paste a valid stream URL", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to recent
            String updated = url + "\n" + recentRaw.replace(url + "\n", "");
            prefs.edit().putString("recent_streams", updated).apply();

            dialog.dismiss();

            Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
            intent.setData(Uri.parse(url));
            intent.putExtra("video_uri", url);
            intent.putExtra("contentUri", url);
            intent.putExtra("path", url);
            String title = YouTubeStreamResolver.isYouTubeUrl(url) ? "YouTube Stream" : getFileNameFromUrl(url);
            intent.putExtra("title", title);
            intent.putExtra("video_title", title);
            startActivity(intent);
        };

        if (btnPlayDirect != null) btnPlayDirect.setOnClickListener(v -> startStreamRunnable.run());
        if (btnPlayStream != null) btnPlayStream.setOnClickListener(v -> startStreamRunnable.run());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        etStreamUrl.setOnEditorActionListener((v, actionId, event) -> {
            startStreamRunnable.run();
            return true;
        });

        // Load recent streams
        if (!recentRaw.isEmpty()) {
            String[] streams = recentRaw.split("\n");
            for (String s : streams) {
                if (s.trim().isEmpty()) continue;
                TextView chip = new TextView(getContext());
                chip.setText(s);
                chip.setTextColor(getResources().getColor(R.color.nothing_white_70));
                chip.setTextSize(11);
                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setPadding(24, 14, 24, 14);
                chip.setSingleLine(true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(0, 8, 0, 8);
                chip.setLayoutParams(lp);
                chip.setOnClickListener(cv -> {
                    etStreamUrl.setText(s);
                    etStreamUrl.setSelection(s.length());
                });
                containerRecent.addView(chip);
            }
        } else {
            if (tvRecentLabel != null) tvRecentLabel.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private String getFileNameFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String last = uri.getLastPathSegment();
            if (last != null && !last.isEmpty()) return last;
        } catch (Exception ignored) {}
        return "Network Stream";
    }
}
