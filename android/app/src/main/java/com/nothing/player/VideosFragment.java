package com.nothing.player;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideosFragment extends Fragment implements VideoAdapter.OnItemClickListener {
    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private TextView chipFolders, chipAllVideos;

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

        swipeRefresh.setOnRefreshListener(this::loadVideos);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplay();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadVideos();
        return view;
    }

    public void loadVideos() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        MediaRepository.scanMedia(ctx.getApplicationContext(), (videos, audios) -> {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    allVideos = videos != null ? videos : new ArrayList<>();
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    filterAndDisplay();
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
        if (currentSelectedFolder != null) {
            currentSelectedFolder = null;
            isFoldersMode = true;
            updateChipsUI();
            filterAndDisplay();
            return true;
        }
        return false;
    }
}
