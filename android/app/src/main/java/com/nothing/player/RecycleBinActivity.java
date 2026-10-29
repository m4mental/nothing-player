package com.nothing.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecycleBinActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private Button btnEmptyTrash;

    private LinearLayout layoutTrashEmpty;
    private RecyclerView rvTrashVideos;
    private TrashVideosAdapter trashAdapter;
    private List<TrashManager.TrashItem> trashItemList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_bin);

        initViews();
        setupListeners();
        loadTrashVideos();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_trash);
        tvHeaderTitle = findViewById(R.id.tv_trash_header_title);
        tvHeaderSubtitle = findViewById(R.id.tv_trash_header_subtitle);
        btnEmptyTrash = findViewById(R.id.btn_empty_trash);

        layoutTrashEmpty = findViewById(R.id.layout_trash_empty);
        rvTrashVideos = findViewById(R.id.rv_trash_videos);

        rvTrashVideos.setLayoutManager(new LinearLayoutManager(this));
        trashAdapter = new TrashVideosAdapter();
        rvTrashVideos.setAdapter(trashAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEmptyTrash.setOnClickListener(v -> {
            if (trashItemList.isEmpty()) {
                Toast.makeText(this, "Recycle Bin is already empty", Toast.LENGTH_SHORT).show();
                return;
            }

            NothingDialogHelper.showConfirmDialog(
                    this,
                    "EMPTY RECYCLE BIN",
                    "Permanently delete all " + trashItemList.size() + " videos?\n\nThis action cannot be undone.",
                    "EMPTY BIN",
                    () -> {
                        TrashManager.emptyTrash(this);
                        Toast.makeText(this, "Recycle Bin Emptied", Toast.LENGTH_SHORT).show();
                        loadTrashVideos();
                    },
                    "CANCEL",
                    null
            );
        });
    }

    private void loadTrashVideos() {
        trashItemList = TrashManager.getTrashItems(this);
        if (trashItemList.isEmpty()) {
            layoutTrashEmpty.setVisibility(View.VISIBLE);
            rvTrashVideos.setVisibility(View.GONE);
            tvHeaderSubtitle.setText("0 VIDEOS • AUTO-PURGED AFTER 30 DAYS");
            btnEmptyTrash.setVisibility(View.GONE);
        } else {
            layoutTrashEmpty.setVisibility(View.GONE);
            rvTrashVideos.setVisibility(View.VISIBLE);
            btnEmptyTrash.setVisibility(View.VISIBLE);
            int count = trashItemList.size();
            tvHeaderSubtitle.setText(count + " VIDEO" + (count > 1 ? "S" : "") + " • AUTO-PURGED AFTER 30 DAYS");
            trashAdapter.notifyDataSetChanged();
        }
    }

    private class TrashVideosAdapter extends RecyclerView.Adapter<TrashVideosAdapter.TrashViewHolder> {

        @NonNull
        @Override
        public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle_video, parent, false);
            return new TrashViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
            TrashManager.TrashItem item = trashItemList.get(position);

            holder.tvTitle.setText(item.title);
            String sizeStr = formatFileSize(item.size);
            holder.tvInfo.setText(sizeStr);

            int daysLeft = item.getDaysRemaining();
            holder.tvDaysLeft.setText(daysLeft + " DAY" + (daysLeft > 1 ? "S" : "") + " LEFT");

            holder.tvDuration.setText(formatDuration(item.duration));

            // Load Video Thumbnail
            if (item.trashedPath != null) {
                Glide.with(RecycleBinActivity.this)
                        .load(new File(item.trashedPath))
                        .centerCrop()
                        .placeholder(R.color.nothing_black)
                        .into(holder.ivThumb);
            }

            // Restore Video
            holder.btnRestore.setOnClickListener(v -> {
                NothingDialogHelper.showConfirmDialog(
                        RecycleBinActivity.this,
                        "RESTORE VIDEO",
                        "Restore \"" + item.title + "\" back to original folder?",
                        "RESTORE",
                        () -> {
                            if (TrashManager.restoreItem(RecycleBinActivity.this, item)) {
                                Toast.makeText(RecycleBinActivity.this, "Video Restored Successfully", Toast.LENGTH_SHORT).show();
                                loadTrashVideos();
                            } else {
                                Toast.makeText(RecycleBinActivity.this, "Failed to restore video", Toast.LENGTH_SHORT).show();
                            }
                        },
                        "CANCEL",
                        null
                );
            });

            // Permanently Delete Video
            holder.btnDelete.setOnClickListener(v -> {
                NothingDialogHelper.showConfirmDialog(
                        RecycleBinActivity.this,
                        "PERMANENT DELETE",
                        "Permanently delete \"" + item.title + "\"?\n\nThis video cannot be recovered.",
                        "DELETE FOREVER",
                        () -> {
                            if (TrashManager.deletePermanently(RecycleBinActivity.this, item)) {
                                Toast.makeText(RecycleBinActivity.this, "Video deleted permanently", Toast.LENGTH_SHORT).show();
                                loadTrashVideos();
                            }
                        },
                        "CANCEL",
                        null
                );
            });
        }

        @Override
        public int getItemCount() {
            return trashItemList.size();
        }

        class TrashViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView tvDuration, tvTitle, tvInfo, tvDaysLeft;
            Button btnRestore, btnDelete;

            TrashViewHolder(@NonNull View v) {
                super(v);
                ivThumb = v.findViewById(R.id.iv_trash_thumb);
                tvDuration = v.findViewById(R.id.tv_trash_duration);
                tvTitle = v.findViewById(R.id.tv_trash_title);
                tvInfo = v.findViewById(R.id.tv_trash_info);
                tvDaysLeft = v.findViewById(R.id.tv_trash_days_left);
                btnRestore = v.findViewById(R.id.btn_trash_restore_item);
                btnDelete = v.findViewById(R.id.btn_trash_delete_item);
            }
        }
    }

    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String formatDuration(long millis) {
        if (millis <= 0) return "00:00";
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
}

