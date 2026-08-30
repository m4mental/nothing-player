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

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FileInfoBottomSheet extends BottomSheetDialogFragment {
    private MediaItem item;

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
        Button btnPlay = view.findViewById(R.id.btn_play_from_info);
        View btnClose = view.findViewById(R.id.btn_close_info);

        tvName.setText(item.title);
        tvPath.setText(item.path != null ? item.path : item.contentUri);
        tvCodec.setText(item.audioCodec != null ? item.audioCodec : (item.format + " Audio"));
        tvChannels.setText("Configuration: " + (item.audioChannels != null ? item.audioChannels : "Stereo (48 kHz)"));

        if (item.audioCodec != null && (item.audioCodec.contains("Dolby") || item.audioCodec.contains("5.1") || item.audioCodec.contains("E-AC-3"))) {
            badgeFormat.setText("DOLBY 5.1");
            badgeFormat.setTextColor(getResources().getColor(R.color.nothing_red));
        } else {
            badgeFormat.setText(item.format);
            badgeFormat.setTextColor(getResources().getColor(R.color.nothing_white));
        }

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Path", item.path != null ? item.path : item.contentUri);
            cm.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Path Copied to Clipboard", Toast.LENGTH_SHORT).show();
        });

        btnPlay.setOnClickListener(v -> {
            dismiss();
            if ("video".equals(item.type)) {
                Intent intent = new Intent(getContext(), ExoVideoPlayerActivity.class);
                intent.putExtra("video_path", item.path != null ? item.path : item.contentUri);
                intent.putExtra("video_title", item.title);
                startActivity(intent);
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playAudioTrack(item, null, -1);
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }
}
