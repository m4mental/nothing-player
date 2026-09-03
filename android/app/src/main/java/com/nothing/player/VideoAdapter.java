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

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_VIDEO = 1;

    public interface OnItemClickListener {
        void onVideoClick(MediaItem video);
        void onFolderClick(String folderName);
        void onMenuClick(MediaItem video);
        void onSelectionChanged(int selectedCount);
    }

    private final Context context;
    private final OnItemClickListener listener;
    private List<Object> items = new ArrayList<>();
    private boolean isFolderView = true;
    private final Set<Object> selectedItems = new HashSet<>();
    private boolean isSelectionMode = false;

    public static class FolderItem {
        public String name;
        public int count;
        public boolean hasNew;

        public FolderItem(String name, int count, boolean hasNew) {
            this.name = name;
            this.count = count;
            this.hasNew = hasNew;
        }
    }

    public VideoAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Object> items, boolean isFolderView) {
        this.items = items;
        this.isFolderView = isFolderView;
        clearSelection();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public Set<Object> getSelectedItems() {
        return selectedItems;
    }

    public void clearSelection() {
        selectedItems.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(0);
    }

    public void selectAll() {
        selectedItems.clear();
        selectedItems.addAll(items);
        isSelectionMode = true;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedItems.size());
    }

    private void toggleSelection(Object item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        isSelectionMode = !selectedItems.isEmpty();
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedItems.size());
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof FolderItem ? TYPE_FOLDER : TYPE_VIDEO;
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
        boolean isSelected = selectedItems.contains(item);

        if (holder instanceof FolderViewHolder) {
            FolderItem folder = (FolderItem) item;
            FolderViewHolder fHolder = (FolderViewHolder) holder;
            fHolder.folderName.setText(folder.name);
            fHolder.folderCount.setText(folder.count + " Videos");
            if (fHolder.badgeNew != null) {
                fHolder.badgeNew.setVisibility(folder.hasNew ? View.VISIBLE : View.GONE);
            }

            if (fHolder.selectionCheck != null) {
                fHolder.selectionCheck.setVisibility(isSelectionMode ? (isSelected ? View.VISIBLE : View.INVISIBLE) : View.GONE);
                if (fHolder.folderArrow != null) {
                    fHolder.folderArrow.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
                }
            }

            fHolder.itemView.setOnClickListener(v -> {
                if (isSelectionMode) {
                    toggleSelection(folder);
                } else if (listener != null) {
                    listener.onFolderClick(folder.name);
                }
            });

            fHolder.itemView.setOnLongClickListener(v -> {
                toggleSelection(folder);
                return true;
            });
        } else if (holder instanceof VideoViewHolder) {
            MediaItem video = (MediaItem) item;
            VideoViewHolder vHolder = (VideoViewHolder) holder;
            vHolder.title.setText(video.title);
            vHolder.subtext.setText(video.folder + " • " + formatSize(video.size));
            vHolder.duration.setText(formatDuration(video.duration));
            vHolder.badgeRes.setText(video.resolution != null ? video.resolution : video.format);

            if (vHolder.badgeNew != null) {
                String path = video.path != null ? video.path : video.contentUri;
                boolean isNew = MediaStateManager.isVideoNew(context, path);
                vHolder.badgeNew.setVisibility(isNew ? View.VISIBLE : View.GONE);
            }

            if (vHolder.selectionCheck != null) {
                vHolder.selectionCheck.setVisibility(isSelectionMode ? (isSelected ? View.VISIBLE : View.INVISIBLE) : View.GONE);
            }

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
                if (isSelectionMode) {
                    toggleSelection(video);
                } else if (listener != null) {
                    listener.onVideoClick(video);
                }
            });

            vHolder.itemView.setOnLongClickListener(v -> {
                toggleSelection(video);
                return true;
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
        TextView folderName, folderCount, folderArrow, badgeNew;
        ImageView selectionCheck;

        FolderViewHolder(View v) {
            super(v);
            folderName = v.findViewById(R.id.folder_name);
            folderCount = v.findViewById(R.id.folder_video_count);
            folderArrow = v.findViewById(R.id.folder_arrow);
            selectionCheck = v.findViewById(R.id.selection_check);
            badgeNew = v.findViewById(R.id.folder_badge_new);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb, selectionCheck;
        TextView title, subtext, duration, badgeRes, badgeSurround, badgeNew;
        ImageButton btnMenu;

        VideoViewHolder(View v) {
            super(v);
            thumb = v.findViewById(R.id.video_thumb);
            title = v.findViewById(R.id.video_title);
            subtext = v.findViewById(R.id.video_subtext);
            duration = v.findViewById(R.id.video_duration);
            badgeRes = v.findViewById(R.id.badge_resolution);
            badgeSurround = v.findViewById(R.id.badge_surround);
            badgeNew = v.findViewById(R.id.badge_new);
            btnMenu = v.findViewById(R.id.btn_video_menu);
            selectionCheck = v.findViewById(R.id.selection_check);
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 MB";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024.0) {
            return String.format(Locale.getDefault(), "%.1f GB", mb / 1024.0);
        }
        return String.format(Locale.getDefault(), "%.1f MB", mb);
    }
}
