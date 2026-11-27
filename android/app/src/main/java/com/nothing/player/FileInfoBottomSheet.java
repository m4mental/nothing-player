package com.nothing.player;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FileInfoBottomSheet extends BottomSheetDialogFragment {
    public interface OnMetadataUpdatedListener {
        void onMetadataUpdated();
    }

    private MediaItem item;
    private OnMetadataUpdatedListener metadataUpdatedListener;

    public void setOnMetadataUpdatedListener(OnMetadataUpdatedListener listener) {
        this.metadataUpdatedListener = listener;
    }

    public static FileInfoBottomSheet newInstance(MediaItem item) {
        FileInfoBottomSheet sheet = new FileInfoBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("media_item", item);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            item = (MediaItem) getArguments().getSerializable("media_item");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_file_info, container, false);

        if (item == null) return view;

        TextView tvName = view.findViewById(R.id.info_file_name);
        TextView tvPath = view.findViewById(R.id.info_storage_path);
        TextView tvCodec = view.findViewById(R.id.info_audio_codec);
        TextView tvChannels = view.findViewById(R.id.info_audio_channels);
        TextView badgeFormat = view.findViewById(R.id.badge_audio_format);
        Button btnCopy = view.findViewById(R.id.btn_copy_path);
        Button btnMatch = view.findViewById(R.id.btn_match_metadata);
        Button btnPlay = view.findViewById(R.id.btn_play_from_info);
        View btnClose = view.findViewById(R.id.btn_close_info);

        tvName.setText(item.title);
        tvPath.setText(item.path != null ? item.path : item.contentUri);
        tvCodec.setText(item.audioCodec != null ? item.audioCodec : (item.format + " Audio"));
        tvChannels.setText("Configuration: " + (item.audioChannels != null ? item.audioChannels : "Stereo (48 kHz)"));

        if (item.audioCodec != null && (item.audioCodec.contains("Dolby") || item.audioCodec.contains("5.1") || item.audioCodec.contains("E-AC-3"))) {
            badgeFormat.setText("DOLBY 5.1");
            badgeFormat.setTextColor(ContextCompat.getColor(requireContext(), R.color.nothing_red));
        } else {
            badgeFormat.setText(item.format);
            badgeFormat.setTextColor(ContextCompat.getColor(requireContext(), R.color.nothing_white));
        }

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Path", item.path != null ? item.path : item.contentUri);
            cm.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Path Copied to Clipboard", Toast.LENGTH_SHORT).show();
        });

        if (btnMatch != null) {
            btnMatch.setOnClickListener(v -> showMatchDialog());
        }

        Button btnPlaylist = view.findViewById(R.id.btn_add_to_playlist);
        if (btnPlaylist != null) {
            btnPlaylist.setOnClickListener(v -> showAddToPlaylistDialog());
        }

        btnPlay.setOnClickListener(v -> {
            Context ctx = getContext();
            dismiss();
            if ("video".equals(item.type)) {
                if (ctx != null) {
                    Intent intent = new Intent(ctx, ExoVideoPlayerActivity.class);
                    intent.putExtra("path", item.path);
                    intent.putExtra("video_path", item.path != null ? item.path : item.contentUri);
                    intent.putExtra("contentUri", item.contentUri);
                    intent.putExtra("title", item.title);
                    intent.putExtra("video_title", item.title);
                    ctx.startActivity(intent);
                }
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playAudioTrack(item, Collections.singletonList(item), 0);
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }

    private void showMatchDialog() {
        if (getContext() == null || item == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_match_metadata, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.EditText etQuery = view.findViewById(R.id.et_match_query);
        android.widget.Button btnSeries = view.findViewById(R.id.btn_type_series);
        android.widget.Button btnMovie = view.findViewById(R.id.btn_type_movie);
        android.widget.Button btnSearch = view.findViewById(R.id.btn_match_search);
        android.widget.Button btnCancel = view.findViewById(R.id.btn_match_cancel);

        final String[] selectedType = {"series"};
        if ("movie".equalsIgnoreCase(item.mediaCategory)) selectedType[0] = "movie";

        Runnable updateButtons = () -> {
            if (!isAdded() || getContext() == null) return;
            boolean isSeries = "series".equalsIgnoreCase(selectedType[0]);
            btnSeries.setBackgroundResource(isSeries ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            btnSeries.setTextColor(ContextCompat.getColor(requireContext(), isSeries ? R.color.nothing_black : R.color.nothing_white));
            btnMovie.setBackgroundResource(!isSeries ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            btnMovie.setTextColor(ContextCompat.getColor(requireContext(), !isSeries ? R.color.nothing_black : R.color.nothing_white));
        };
        updateButtons.run();

        btnSeries.setOnClickListener(v -> {
            selectedType[0] = "series";
            updateButtons.run();
        });
        btnMovie.setOnClickListener(v -> {
            selectedType[0] = "movie";
            updateButtons.run();
        });

        String initial = item.cleanTitle != null ? item.cleanTitle : (item.seriesName != null ? item.seriesName : item.title);
        etQuery.setText(initial);
        etQuery.setSelection(etQuery.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSearch.setOnClickListener(v -> {
            String query = etQuery.getText().toString().trim();
            if (query.isEmpty() || getContext() == null) return;

            Toast.makeText(getContext(), "SEARCHING ONLINE REGISTRY...", Toast.LENGTH_SHORT).show();
            Context appCtx = getContext().getApplicationContext();
            OnlineMetadataManager.searchAndApply(appCtx, item, query, selectedType[0], info -> {
                if (info != null) {
                    MediaRepository.updateVideoMetadataInCache(appCtx, item);
                    if (metadataUpdatedListener != null) {
                        metadataUpdatedListener.onMetadataUpdated();
                    }
                }
                if (!isAdded() || getContext() == null) return;
                if (info != null) {
                    Toast.makeText(getContext(), "MATCHED: " + info.title, Toast.LENGTH_SHORT).show();
                    if (dialog.isShowing()) dialog.dismiss();
                    dismissAllowingStateLoss();
                } else {
                    Toast.makeText(getContext(), "NO ONLINE MATCH FOUND", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showAddToPlaylistDialog() {
        if (getContext() == null || item == null) return;
        List<String> names = VirtualPlaylistManager.getPlaylistNames(getContext());
        List<String> displayList = new ArrayList<>(names);
        displayList.add("+ CREATE NEW PLAYLIST");

        NothingDialogHelper.showSelectionDialog(
            requireActivity(),
            "ADD TO PLAYLIST",
            displayList,
            -1,
            (which, selectedText) -> {
                if (getContext() == null) return;
                if (which == displayList.size() - 1) {
                    showCreatePlaylistDialog();
                } else {
                    String chosen = names.get(which);
                    String targetPath = item.path != null ? item.path : item.contentUri;
                    VirtualPlaylistManager.addToPlaylist(getContext(), chosen, targetPath);
                    Toast.makeText(getContext(), "Added to " + chosen, Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss();
                }
            }
        );
    }

    private void showCreatePlaylistDialog() {
        if (getContext() == null || item == null) return;
        NothingDialogHelper.showInputDialog(
            getContext(),
            "CREATE PLAYLIST",
            null,
            "PLAYLIST NAME (ALL CAPS)",
            "CREATE & ADD",
            android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
            rawName -> {
                if (!isAdded() || getContext() == null) return;
                String name = rawName.toUpperCase(Locale.US);
                if (!name.isEmpty()) {
                    VirtualPlaylistManager.createPlaylist(getContext(), name);
                    String targetPath = item.path != null ? item.path : item.contentUri;
                    VirtualPlaylistManager.addToPlaylist(getContext(), name, targetPath);
                    Toast.makeText(getContext(), "Created & Added to " + name, Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss();
                }
            }
        );
    }
}