package com.nothing.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    public interface OnMusicClickListener {
        void onTrackClick(MediaItem track, int position);
        void onMenuClick(MediaItem track);
        void onSelectionChanged(int selectedCount);
    }

    private final Context context;
    private final OnMusicClickListener listener;
    private List<MediaItem> tracks = new ArrayList<>();
    private String currentPlayingId = null;
    private final Set<MediaItem> selectedTracks = new HashSet<>();
    private boolean isSelectionMode = false;

    public MusicAdapter(Context context, OnMusicClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTracks(List<MediaItem> tracks) {
        this.tracks = tracks;
        clearSelection();
        notifyDataSetChanged();
    }

    public void setCurrentPlayingId(String id) {
        this.currentPlayingId = id;
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public Set<MediaItem> getSelectedTracks() {
        return selectedTracks;
    }

    public void clearSelection() {
        selectedTracks.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(0);
    }

    public void selectAll() {
        selectedTracks.clear();
        selectedTracks.addAll(tracks);
        isSelectionMode = true;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedTracks.size());
    }

    private void toggleSelection(MediaItem track) {
        if (selectedTracks.contains(track)) {
            selectedTracks.remove(track);
        } else {
            selectedTracks.add(track);
        }
        if (selectedTracks.isEmpty()) {
            isSelectionMode = false;
        } else {
            isSelectionMode = true;
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedTracks.size());
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_music_track, parent, false);
        return new MusicViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MediaItem track = tracks.get(position);
        boolean isSelected = selectedTracks.contains(track);

        holder.title.setText(track.title);
        holder.artistAlbum.setText(track.artist + " • " + track.album + " • " + track.format);
        holder.duration.setText(formatDuration(track.duration));

        if (track.id.equals(currentPlayingId)) {
            holder.title.setTextColor(context.getResources().getColor(R.color.nothing_red));
        } else {
            holder.title.setTextColor(context.getResources().getColor(R.color.nothing_white));
        }

        if (holder.selectionCheck != null) {
            holder.selectionCheck.setVisibility(isSelectionMode ? (isSelected ? View.VISIBLE : View.INVISIBLE) : View.GONE);
            if (holder.btnMenu != null) {
                holder.btnMenu.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            }
        }

        Glide.with(context)
                .load(track.path != null ? track.path : track.contentUri)
                .placeholder(android.R.drawable.ic_lock_silent_mode_off)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.art);

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(track);
            } else if (listener != null) {
                listener.onTrackClick(track, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            toggleSelection(track);
            return true;
        });

        holder.btnMenu.setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(track);
        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        ImageView art, selectionCheck;
        TextView title, artistAlbum, duration;
        ImageButton btnMenu;

        MusicViewHolder(View v) {
            super(v);
            art = v.findViewById(R.id.track_art);
            title = v.findViewById(R.id.track_title);
            artistAlbum = v.findViewById(R.id.track_artist_album);
            duration = v.findViewById(R.id.track_duration);
            btnMenu = v.findViewById(R.id.btn_track_menu);
            selectionCheck = v.findViewById(R.id.selection_check);
        }
    }

    private String formatDuration(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / (1000 * 60)) % 60;
        long hours = ms / (1000 * 60 * 60);
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
