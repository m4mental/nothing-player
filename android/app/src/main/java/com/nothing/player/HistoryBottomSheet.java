package com.nothing.player;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class HistoryBottomSheet extends BottomSheetDialogFragment implements HistoryListAdapter.OnHistoryActionListener {

    public interface OnHistoryChangedListener {
        void onHistoryChanged();
    }

    private RecyclerView recyclerView;
    private View layoutEmpty;
    private AppCompatButton btnClear;
    private HistoryListAdapter adapter;
    private OnHistoryChangedListener changeListener;

    public static HistoryBottomSheet newInstance() {
        return new HistoryBottomSheet();
    }

    public void setOnHistoryChangedListener(OnHistoryChangedListener listener) {
        this.changeListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_history, container, false);

        recyclerView = view.findViewById(R.id.recycler_history_list);
        layoutEmpty = view.findViewById(R.id.layout_empty_history);
        btnClear = view.findViewById(R.id.btn_clear_history);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryListAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);

        btnClear.setOnClickListener(v -> confirmClearAll());

        loadHistoryData();
        return view;
    }

    private void loadHistoryData() {
        if (getContext() == null) return;
        List<PlaybackHistoryManager.HistoryItem> items = PlaybackHistoryManager.getHistoryList(getContext());
        if (items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            btnClear.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            btnClear.setVisibility(View.VISIBLE);
            adapter.setItems(items);
        }
    }

    private void confirmClearAll() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Clear Watch History?")
                .setMessage("All playback progress and watch history entries will be cleared.")
                .setPositiveButton("CLEAR", (dialog, which) -> {
                    PlaybackHistoryManager.clearHistory(getContext());
                    loadHistoryData();
                    if (changeListener != null) changeListener.onHistoryChanged();
                    Toast.makeText(getContext(), "Watch history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    public void onResume(PlaybackHistoryManager.HistoryItem item) {
        launchPlayer(item, item.positionMs, false);
    }

    @Override
    public void onStartOver(PlaybackHistoryManager.HistoryItem item) {
        launchPlayer(item, 0, true);
    }

    @Override
    public void onDelete(PlaybackHistoryManager.HistoryItem item) {
        if (getContext() == null) return;
        PlaybackHistoryManager.deleteEntry(getContext(), item.pathOrUri);
        loadHistoryData();
        if (changeListener != null) changeListener.onHistoryChanged();
    }

    private void launchPlayer(PlaybackHistoryManager.HistoryItem item, long startPosition, boolean startOver) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(item.pathOrUri));
        intent.putExtra("path", item.pathOrUri);
        intent.putExtra("video_path", item.pathOrUri);
        intent.putExtra("contentUri", item.pathOrUri);
        intent.putExtra("video_uri", item.pathOrUri);
        intent.putExtra("title", item.title);
        intent.putExtra("video_title", item.title);
        intent.putExtra("position", startPosition);
        intent.putExtra("start_over", startOver);
        startActivity(intent);
        dismiss();
    }
}
