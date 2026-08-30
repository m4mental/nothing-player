package com.nothing.player;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MusicFragment extends Fragment implements MusicAdapter.OnMusicClickListener {
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private TextView chipTracks, chipArtists, chipFavorites;

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

        loadTracks();
        return view;
    }

    public void loadTracks() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        MediaRepository.scanMedia(ctx.getApplicationContext(), (videos, audios) -> {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    allTracks = audios != null ? audios : new ArrayList<>();
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    filterTracks();
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
}
