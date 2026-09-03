package com.nothing.player;

import android.app.AlertDialog;
import android.content.ContentResolver;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MusicFragment extends Fragment implements MusicAdapter.OnMusicClickListener {
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private TextView chipTracks, chipArtists, chipFavorites;

    // Multi-select Views
    private View selectionActionBar;
    private TextView tvSelectionCount;
    private Button btnSelectAll, btnDeleteSelected;
    private ImageButton btnCloseSelection;

    private List<MediaItem> allTracks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);

        recyclerView = view.findViewById(R.id.recycler_music);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_music);
        etSearch = view.findViewById(R.id.et_search_music);
        chipTracks = view.findViewById(R.id.chip_music_tracks);
        chipArtists = view.findViewById(R.id.chip_music_artists);
        chipFavorites = view.findViewById(R.id.chip_music_favorites);

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
        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Delete " + count + " Track" + (count > 1 ? "s" : "") + "?")
            .setMessage("These audio files will be permanently deleted from device storage.")
            .setPositiveButton("DELETE", (dialog, which) -> deleteSelectedTracks(selected))
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void deleteSelectedTracks(Set<MediaItem> selected) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        ContentResolver resolver = ctx.getContentResolver();
        int deletedCount = 0;
        Set<String> deletedPaths = new HashSet<>();

        for (MediaItem track : selected) {
            boolean deleted = false;
            if (track.contentUri != null) {
                try {
                    int rows = resolver.delete(Uri.parse(track.contentUri), null, null);
                    if (rows > 0) deleted = true;
                } catch (Exception ignored) {}
            }
            if (!deleted && track.path != null) {
                try {
                    File f = new File(track.path);
                    if (f.exists() && f.delete()) deleted = true;
                } catch (Exception ignored) {}
            }
                if (deleted) {
                    deletedCount++;
                    if (track.path != null) deletedPaths.add(track.path);
                }
            }

            MediaRepository.removeItemsFromCache(ctx.getApplicationContext(), deletedPaths);
            List<MediaItem> remaining = new ArrayList<>();
            for (MediaItem m : allTracks) {
                if (!deletedPaths.contains(m.path)) remaining.add(m);
            }
            allTracks = remaining;
            adapter.clearSelection();
            filterTracks();

            Toast.makeText(ctx, "Deleted " + deletedCount + " track(s)", Toast.LENGTH_SHORT).show();
            loadTracks(false);
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

    private void filterTracks() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        List<MediaItem> filtered = new ArrayList<>();
        for (MediaItem item : allTracks) {
            if (query.isEmpty() || item.title.toLowerCase().contains(query) || item.artist.toLowerCase().contains(query)) {
                filtered.add(item);
            }
        }
        adapter.setTracks(filtered);
    }

    @Override
    public void onTrackClick(MediaItem track, int position) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).playAudioTrack(track, allTracks, position);
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
        if (adapter.isSelectionMode()) {
            adapter.clearSelection();
            return true;
        }
        return false;
    }
}
