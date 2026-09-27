package com.nothing.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnHistoryClickListener {
        void onHistoryItemClick(PlaybackHistoryManager.HistoryItem item);
    }

    private final Context context;
    private final OnHistoryClickListener listener;
    private List<PlaybackHistoryManager.HistoryItem> items = new ArrayList<>();

    public HistoryAdapter(Context context, OnHistoryClickListener listener) {
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
        View v = LayoutInflater.from(context).inflate(R.layout.item_history_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaybackHistoryManager.HistoryItem item = items.get(position);
        holder.title.setText(item.title != null ? item.title : "Video");
        holder.timeBadge.setText(item.getFormattedDate());
        holder.duration.setText(item.getFormattedDuration());

        int percent = item.getProgressPercent();
        holder.progressBar.setProgress(percent);
        holder.subtext.setText(item.getFormattedPosition() + " / " + item.getFormattedDuration() + " • " + percent + "%");

        Glide.with(context)
                .load(item.pathOrUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.thumb);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView timeBadge, duration, title, subtext;
        DotMatrixProgressBar progressBar;

        ViewHolder(View v) {
            super(v);
            thumb = v.findViewById(R.id.history_thumb);
            timeBadge = v.findViewById(R.id.history_time_badge);
            duration = v.findViewById(R.id.history_duration);
            progressBar = v.findViewById(R.id.history_progress_bar);
            title = v.findViewById(R.id.history_title);
            subtext = v.findViewById(R.id.history_subtext);
        }
    }
}
