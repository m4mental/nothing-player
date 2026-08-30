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
import java.util.List;
import java.util.Locale;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    public interface OnMusicClickListener {
        void onTrackClick(MediaItem track, int position);
        void onMenuClick(MediaItem track);
    }

    private final Context context;
    private final OnMusicClickListener listener;
    private List<MediaItem> tracks = new ArrayList<>();
    private String currentPlayingId = null;

    public MusicAdapter(Context context, OnMusicClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTracks(List<MediaItem> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
    }

    public void setCurrentPlayingId(String id) {
        this.currentPlayingId = id;
        notifyDataSetChanged();
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
        holder.title.setText(track.title);
        holder.artistAlbum.setText(track.artist + " • " + track.album + " • " + track.format);
        holder.duration.setText(formatDuration(track.duration));

        if (track.id.equals(currentPlayingId)) {
            holder.title.setTextColor(context.getResources().getColor(R.color.nothing_red));
        } else {
            holder.title.setTextColor(context.getResources().getColor(R.color.nothing_white));
        }

        Glide.with(context)
                .load(track.path != null ? track.path : track.contentUri)
                .placeholder(android.R.drawable.ic_lock_silent_mode_off)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.art);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTrackClick(track, position);
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
        ImageView art;
        TextView title, artistAlbum, duration;
        ImageButton btnMenu;

        MusicViewHolder(View v) {
            super(v);
            art = v.findViewById(R.id.track_art);
            title = v.findViewById(R.id.track_title);
            artistAlbum = v.findViewById(R.id.track_artist_album);
            duration = v.findViewById(R.id.track_duration);
            btnMenu = v.findViewById(R.id.btn_track_menu);
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
