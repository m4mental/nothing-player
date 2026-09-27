package com.nothing.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.ViewHolder> {

    public interface OnHistoryActionListener {
        void onResume(PlaybackHistoryManager.HistoryItem item);
        void onStartOver(PlaybackHistoryManager.HistoryItem item);
        void onDelete(PlaybackHistoryManager.HistoryItem item);
    }

    private final Context context;
    private final OnHistoryActionListener listener;
    private List<PlaybackHistoryManager.HistoryItem> items = new ArrayList<>();

    public HistoryListAdapter(Context context, OnHistoryActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<PlaybackHistoryManager.HistoryItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_history_list_entry, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaybackHistoryManager.HistoryItem item = items.get(position);

        holder.title.setText(item.title != null ? item.title : "Video");
        holder.watchedTime.setText("Watched: " + item.getExactDateTime());
        holder.duration.setText(item.getFormattedDuration());

        int percent = item.getProgressPercent();
        holder.progressBar.setProgress(percent);
        holder.progressText.setText(item.getFormattedPosition() + " / " + item.getFormattedDuration() + " (" + percent + "% watched)");

        Glide.with(context)
                .load(item.pathOrUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.thumb);

        holder.btnResume.setOnClickListener(v -> {
            if (listener != null) listener.onResume(item);
        });

        holder.btnStartOver.setOnClickListener(v -> {
            if (listener != null) listener.onStartOver(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onResume(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView duration, title, watchedTime, progressText;
        DotMatrixProgressBar progressBar;
        ImageButton btnDelete;
        AppCompatButton btnStartOver, btnResume;

        ViewHolder(View v) {
            super(v);
            thumb = v.findViewById(R.id.entry_thumb);
            duration = v.findViewById(R.id.entry_duration);
            progressBar = v.findViewById(R.id.entry_progress_bar);
            title = v.findViewById(R.id.entry_title);
            watchedTime = v.findViewById(R.id.entry_watched_time);
            progressText = v.findViewById(R.id.entry_progress_text);
            btnDelete = v.findViewById(R.id.btn_delete_entry);
            btnStartOver = v.findViewById(R.id.btn_start_over);
            btnResume = v.findViewById(R.id.btn_resume);
        }
    }
}
