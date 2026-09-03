package com.nothing.player;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistQueueAdapter extends RecyclerView.Adapter<PlaylistQueueAdapter.QueueViewHolder> {

    public static class QueueItem {
        public String path;
        public String uri;
        public String title;
        public long duration;

        public QueueItem(String path, String uri, String title, long duration) {
            this.path = path;
            this.uri = uri;
            this.title = title;
            this.duration = duration;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final Context context;
    private final List<QueueItem> items = new ArrayList<>();
    private int activeIndex = 0;
    private final OnItemClickListener listener;

    public PlaylistQueueAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<QueueItem> newItems, int currentActiveIndex) {
        this.items.clear();
        if (newItems != null) this.items.addAll(newItems);
        this.activeIndex = currentActiveIndex;
        notifyDataSetChanged();
    }

    public void setActiveIndex(int currentActiveIndex) {
        int oldIndex = this.activeIndex;
        this.activeIndex = currentActiveIndex;
        notifyItemChanged(oldIndex);
        notifyItemChanged(currentActiveIndex);
    }

    public void updateTitle(int position, String newTitle) {
        if (position >= 0 && position < items.size()) {
            items.get(position).title = newTitle;
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_queue, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        QueueItem item = items.get(position);
        holder.title.setText(item.title != null && !item.title.isEmpty() ? item.title : "Video " + (position + 1));
        
        if (item.duration > 0) {
            holder.duration.setText(formatTime(item.duration));
            holder.duration.setVisibility(View.VISIBLE);
        } else {
            holder.duration.setVisibility(View.GONE);
        }

        boolean isCurrent = position == activeIndex;
        if (isCurrent) {
            holder.status.setVisibility(View.VISIBLE);
            holder.title.setTextColor(Color.WHITE);
            holder.itemView.setBackgroundResource(R.drawable.bg_player_pill_button);
        } else {
            holder.status.setVisibility(View.GONE);
            holder.title.setTextColor(Color.parseColor("#CCFFFFFF"));
            holder.itemView.setBackgroundResource(R.drawable.bg_card_nothing);
        }

        // Load thumbnail
        try {
            String thumbUrl = null;
            if (item.path != null && item.path.contains("youtube.com")) {
                String vid = YouTubeStreamResolver.extractVideoId(item.path);
                if (vid != null && !vid.isEmpty()) {
                    thumbUrl = "https://img.youtube.com/vi/" + vid + "/mqdefault.jpg";
                }
            } else if (item.uri != null && item.uri.contains("youtube.com")) {
                String vid = YouTubeStreamResolver.extractVideoId(item.uri);
                if (vid != null && !vid.isEmpty()) {
                    thumbUrl = "https://img.youtube.com/vi/" + vid + "/mqdefault.jpg";
                }
            }

            if (thumbUrl != null) {
                Glide.with(context)
                        .load(thumbUrl)
                        .placeholder(R.drawable.ic_video)
                        .override(160, 100)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.thumb);
            } else {
                Uri thumbUri = null;
                if (item.path != null && !item.path.isEmpty() && new File(item.path).exists()) {
                    thumbUri = Uri.fromFile(new File(item.path));
                } else if (item.uri != null && !item.uri.isEmpty()) {
                    thumbUri = Uri.parse(item.uri);
                }

                if (thumbUri != null) {
                    Glide.with(context)
                            .asBitmap()
                            .load(thumbUri)
                            .placeholder(R.drawable.ic_video)
                            .override(160, 100)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.thumb);
                } else {
                    holder.thumb.setImageResource(R.drawable.ic_video);
                }
            }
        } catch (Exception ignored) {
            holder.thumb.setImageResource(R.drawable.ic_video);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d", m, s);
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView title;
        TextView status;
        TextView duration;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.queue_item_thumb);
            title = itemView.findViewById(R.id.queue_item_title);
            status = itemView.findViewById(R.id.queue_item_status);
            duration = itemView.findViewById(R.id.queue_item_duration);
        }
    }
}
