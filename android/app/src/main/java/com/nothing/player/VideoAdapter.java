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

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_VIDEO = 1;

    public interface OnItemClickListener {
        void onVideoClick(MediaItem video);
        void onFolderClick(String folderName);
        void onMenuClick(MediaItem video);
    }

    private final Context context;
    private final OnItemClickListener listener;
    private List<Object> items = new ArrayList<>();
    private boolean isFolderView = true;

    public static class FolderItem {
        public String name;
        public int count;

        public FolderItem(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    public VideoAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Object> items, boolean isFolderView) {
        this.items = items;
        this.isFolderView = isFolderView;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof FolderItem) {
            return TYPE_FOLDER;
        }
        return TYPE_VIDEO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOLDER) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_folder_card, parent, false);
            return new FolderViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_video_card, parent, false);
            return new VideoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof FolderViewHolder) {
            FolderItem folder = (FolderItem) item;
            FolderViewHolder fHolder = (FolderViewHolder) holder;
            fHolder.folderName.setText(folder.name);
            fHolder.folderCount.setText(folder.count + " Videos");
            fHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFolderClick(folder.name);
            });
        } else if (holder instanceof VideoViewHolder) {
            MediaItem video = (MediaItem) item;
            VideoViewHolder vHolder = (VideoViewHolder) holder;
            vHolder.title.setText(video.title);
            vHolder.subtext.setText(video.folder + " • " + formatSize(video.size));
            vHolder.duration.setText(formatDuration(video.duration));
            vHolder.badgeRes.setText(video.resolution != null ? video.resolution : video.format);

            if (video.audioCodec != null && (video.audioCodec.contains("Dolby") || video.audioCodec.contains("5.1") || video.audioCodec.contains("E-AC-3"))) {
                vHolder.badgeSurround.setVisibility(View.VISIBLE);
            } else {
                vHolder.badgeSurround.setVisibility(View.GONE);
            }

            Glide.with(context)
                    .load(video.path != null ? video.path : video.contentUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(vHolder.thumb);

            vHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onVideoClick(video);
            });

            vHolder.btnMenu.setOnClickListener(v -> {
                if (listener != null) listener.onMenuClick(video);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName, folderCount;
        FolderViewHolder(View v) {
            super(v);
            folderName = v.findViewById(R.id.folder_name);
            folderCount = v.findViewById(R.id.folder_video_count);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView title, subtext, duration, badgeRes, badgeSurround;
        ImageButton btnMenu;

        VideoViewHolder(View v) {
            super(v);
            thumb = v.findViewById(R.id.video_thumb);
            title = v.findViewById(R.id.video_title);
            subtext = v.findViewById(R.id.video_subtext);
            duration = v.findViewById(R.id.video_duration);
            badgeRes = v.findViewById(R.id.badge_resolution);
            badgeSurround = v.findViewById(R.id.badge_surround);
            btnMenu = v.findViewById(R.id.btn_video_menu);
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

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 MB";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024.0) {
            return String.format(Locale.US, "%.2f GB", mb / 1024.0);
        }
        return String.format(Locale.US, "%.1f MB", mb);
    }
}
