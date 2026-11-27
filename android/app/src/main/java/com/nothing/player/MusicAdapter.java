package com.nothing.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    public List<MediaItem> getTracks() {
        return tracks;
    }

    public void setTracks(List<MediaItem> tracks) {
        int oldSize = this.tracks.size();
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        selectedTracks.clear();
        isSelectionMode = false;
        if (listener != null) listener.onSelectionChanged(0);
        if (oldSize > 0) notifyItemRangeRemoved(0, oldSize);
        if (!this.tracks.isEmpty()) notifyItemRangeInserted(0, this.tracks.size());
    }

    public void setCurrentPlayingId(String id) {
        this.currentPlayingId = id;
        if (!tracks.isEmpty()) notifyItemRangeChanged(0, tracks.size());
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
        if (!tracks.isEmpty()) notifyItemRangeChanged(0, tracks.size());
        if (listener != null) listener.onSelectionChanged(0);
    }

    public void selectAll() {
        selectedTracks.clear();
        selectedTracks.addAll(tracks);
        isSelectionMode = !tracks.isEmpty();
        if (!tracks.isEmpty()) notifyItemRangeChanged(0, tracks.size());
        if (listener != null) listener.onSelectionChanged(selectedTracks.size());
    }

    private void toggleSelection(MediaItem track) {
        boolean wasSelectionMode = isSelectionMode;
        if (selectedTracks.contains(track)) {
            selectedTracks.remove(track);
        } else {
            selectedTracks.add(track);
        }
        isSelectionMode = !selectedTracks.isEmpty();
        if (wasSelectionMode != isSelectionMode) {
            if (!tracks.isEmpty()) notifyItemRangeChanged(0, tracks.size());
        } else {
            int idx = tracks.indexOf(track);
            if (idx >= 0) {
                notifyItemChanged(idx);
            } else if (!tracks.isEmpty()) {
                notifyItemRangeChanged(0, tracks.size());
            }
        }
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

        if (track.id != null && track.id.equals(currentPlayingId)) {
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.nothing_red));
        } else {
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.nothing_white));
        }

        if (holder.selectionCheck != null) {
            holder.selectionCheck.setVisibility(isSelectionMode ? (isSelected ? View.VISIBLE : View.INVISIBLE) : View.GONE);
            if (holder.btnMenu != null) {
                holder.btnMenu.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            }
        }

        Glide.with(context)
                .load(track.path != null ? track.path : track.contentUri)
                .placeholder(R.drawable.ic_headphones)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.art);

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(track);
            } else if (listener != null) {
                int adapterPos = holder.getBindingAdapterPosition();
                listener.onTrackClick(track, adapterPos != RecyclerView.NO_POSITION ? adapterPos : position);
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
