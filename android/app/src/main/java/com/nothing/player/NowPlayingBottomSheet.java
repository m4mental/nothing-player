package com.nothing.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class NowPlayingBottomSheet extends BottomSheetDialogFragment {
    private MediaItem currentTrack;
    private boolean isPlaying = true;
    private long duration = 0;
    private long currentPosition = 0;

    private ImageView art;
    private TextView title, artist, tvCurrent, tvTotal;
    private SeekBar seekbar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnEq, btnClose;
    private boolean isUserSeeking = false;

    public static NowPlayingBottomSheet newInstance(MediaItem track) {
        NowPlayingBottomSheet sheet = new NowPlayingBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("track", track);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTrack = (MediaItem) getArguments().getSerializable("track");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_now_playing, container, false);

        art = view.findViewById(R.id.full_player_art);
        title = view.findViewById(R.id.full_player_title);
        artist = view.findViewById(R.id.full_player_artist);
        tvCurrent = view.findViewById(R.id.full_player_time_current);
        tvTotal = view.findViewById(R.id.full_player_time_total);
        seekbar = view.findViewById(R.id.full_player_seekbar);
        btnPlayPause = view.findViewById(R.id.btn_full_play_pause);
        btnPrev = view.findViewById(R.id.btn_full_prev);
        btnNext = view.findViewById(R.id.btn_full_next);
        btnEq = view.findViewById(R.id.btn_open_eq_from_sheet);
        btnClose = view.findViewById(R.id.btn_collapse_player);

        updateTrackUI();

        btnClose.setOnClickListener(v -> dismiss());

        btnEq.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EqualizerActivity.class);
            startActivity(intent);
        });

        btnPlayPause.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).togglePlayPause();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playNext();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playPrevious();
            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && tvCurrent != null) {
                    tvCurrent.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).seekTo(seekBar.getProgress());
                }
            }
        });

        return view;
    }

    public void updateTrack(MediaItem track, boolean playing) {
        this.currentTrack = track;
        this.isPlaying = playing;
        if (getView() != null) {
            updateTrackUI();
        }
    }

    public void updateProgress(long pos, long dur) {
        if (isUserSeeking) return;
        this.currentPosition = pos;
        this.duration = dur;
        if (tvCurrent != null && tvTotal != null && seekbar != null) {
            tvCurrent.setText(formatTime(pos));
            tvTotal.setText(formatTime(dur));
            if (dur > 0) seekbar.setMax((int) dur);
            seekbar.setProgress((int) pos);
        }
    }

    private void updateTrackUI() {
        if (currentTrack == null) return;
        title.setText(currentTrack.title);
        artist.setText(currentTrack.artist + " • " + currentTrack.album);
        tvTotal.setText(formatTime(currentTrack.duration));
        seekbar.setMax((int) currentTrack.duration);

        if (isPlaying) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }

        if (getContext() != null) {
            Glide.with(this)
                    .load(currentTrack.path != null ? currentTrack.path : currentTrack.contentUri)
                    .placeholder(android.R.drawable.ic_lock_silent_mode_off)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(art);
        }
    }

    private String formatTime(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / (1000 * 60)) % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
