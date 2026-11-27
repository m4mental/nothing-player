package com.nothing.player;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NowPlayingBottomSheet extends BottomSheetDialogFragment {
    private MediaItem currentTrack;
    private boolean isPlaying = false;
    private long duration = 0;
    private long currentPosition = 0;

    private ImageView art;
    private TextView title, artist, tvCurrent, tvTotal, tvLyrics;
    private ScrollView layoutLyricsOverlay;
    private Button btnToggleLyrics;
    private SeekBar seekbar;
    private DotMatrixSpectrumView spectrumView;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnShuffle, btnRepeat, btnEq, btnClose;
    private boolean isUserSeeking = false;
    private boolean isShuffleOn = false;
    private int repeatMode = 0; // 0=Off, 1=All, 2=One
    private boolean isLyricsVisible = false;
    private List<LrcParser.LrcLine> lrcLines = null;
    private int lastActiveLrcIndex = -2;
    private final ExecutorService lrcExecutor = Executors.newSingleThreadExecutor();

    public static NowPlayingBottomSheet newInstance(MediaItem track) {
        boolean playing = MusicPlaybackService.instance != null && MusicPlaybackService.instance.isPlaying();
        long pos = MusicPlaybackService.instance != null ? MusicPlaybackService.instance.getCurrentPosition() : 0L;
        long dur = track != null && track.duration > 0 ? track.duration
                : (MusicPlaybackService.instance != null ? MusicPlaybackService.instance.getDuration() : 0L);
        return newInstance(track, playing, pos, dur);
    }

    public static NowPlayingBottomSheet newInstance(MediaItem track, boolean isPlaying, long positionMs, long durationMs) {
        NowPlayingBottomSheet sheet = new NowPlayingBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("track", track);
        args.putBoolean("is_playing", isPlaying);
        args.putLong("position", positionMs);
        args.putLong("duration", durationMs);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currentTrack = getArguments().getSerializable("track", MediaItem.class);
            } else {
                currentTrack = getLegacyTrackArg(getArguments());
            }
            if (getArguments().containsKey("is_playing")) {
                isPlaying = getArguments().getBoolean("is_playing", false);
            } else if (MusicPlaybackService.instance != null) {
                isPlaying = MusicPlaybackService.instance.isPlaying();
            }
            currentPosition = getArguments().getLong("position", 0L);
            duration = getArguments().getLong("duration", 0L);
        }
    }

    @SuppressWarnings("deprecation")
    private static MediaItem getLegacyTrackArg(Bundle args) {
        return (MediaItem) args.getSerializable("track");
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
        spectrumView = view.findViewById(R.id.now_playing_spectrum);
        btnPlayPause = view.findViewById(R.id.btn_full_play_pause);
        btnPrev = view.findViewById(R.id.btn_full_prev);
        btnNext = view.findViewById(R.id.btn_full_next);
        btnShuffle = view.findViewById(R.id.btn_full_shuffle);
        btnRepeat = view.findViewById(R.id.btn_full_repeat);
        btnEq = view.findViewById(R.id.btn_open_eq_from_sheet);
        btnClose = view.findViewById(R.id.btn_collapse_player);
        btnToggleLyrics = view.findViewById(R.id.btn_toggle_lyrics);
        layoutLyricsOverlay = view.findViewById(R.id.layout_lyrics_overlay);
        tvLyrics = view.findViewById(R.id.tv_now_playing_lyrics);

        if (btnToggleLyrics != null) {
            btnToggleLyrics.setOnClickListener(v -> {
                isLyricsVisible = !isLyricsVisible;
                updateLyricsToggleUi();
                if (isLyricsVisible) {
                    renderLyricsAt(currentPosition, true);
                }
            });
        }

        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
            isShuffleOn = prefs.getBoolean("music_shuffle", false);
            repeatMode = prefs.getInt("music_repeat_mode", 1);
        }

        updateTrackUI();
        updateProgress(currentPosition, duration > 0 ? duration : (currentTrack != null ? currentTrack.duration : 0L));
        updateShuffleRepeatUI();

        btnClose.setOnClickListener(v -> dismiss());

        btnEq.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EqualizerActivity.class);
            if (MusicPlaybackService.instance != null && MusicPlaybackService.instance.getAudioSessionId() > 0) {
                intent.putExtra("audio_session_id", MusicPlaybackService.instance.getAudioSessionId());
            }
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

        if (btnShuffle != null) {
            btnShuffle.setOnClickListener(v -> {
                isShuffleOn = !isShuffleOn;
                if (getContext() != null) {
                    getContext().getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE)
                            .edit().putBoolean("music_shuffle", isShuffleOn).apply();
                    Toast.makeText(getContext(), isShuffleOn ? "SHUFFLE: ON" : "SHUFFLE: OFF", Toast.LENGTH_SHORT).show();
                }
                updateShuffleRepeatUI();
            });
        }

        if (btnRepeat != null) {
            btnRepeat.setOnClickListener(v -> {
                repeatMode = (repeatMode + 1) % 3;
                if (getContext() != null) {
                    getContext().getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE)
                            .edit().putInt("music_repeat_mode", repeatMode).apply();
                    String label = repeatMode == 2 ? "REPEAT ONE" : (repeatMode == 1 ? "REPEAT ALL" : "REPEAT OFF");
                    Toast.makeText(getContext(), label, Toast.LENGTH_SHORT).show();
                }
                updateShuffleRepeatUI();
            });
        }

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

    private void updateShuffleRepeatUI() {
        int red = Color.parseColor("#D71921");
        int dim = Color.parseColor("#66FFFFFF");
        if (btnShuffle != null) {
            btnShuffle.setColorFilter(isShuffleOn ? red : dim);
        }
        if (btnRepeat != null) {
            if (repeatMode == 2) {
                btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnRepeat.setColorFilter(red);
            } else if (repeatMode == 1) {
                btnRepeat.setImageResource(R.drawable.ic_repeat_all);
                btnRepeat.setColorFilter(red);
            } else {
                btnRepeat.setImageResource(R.drawable.ic_repeat_all);
                btnRepeat.setColorFilter(dim);
            }
        }
    }

    public void updateTrack(MediaItem track, boolean playing) {
        this.currentTrack = track;
        this.isPlaying = playing;
        if (isAdded() && getView() != null) {
            updateTrackUI();
        }
    }

    public void updateProgress(long pos, long dur) {
        if (isUserSeeking || !isAdded()) return;
        this.currentPosition = pos;
        this.duration = dur;
        if (tvCurrent != null && tvTotal != null && seekbar != null) {
            tvCurrent.setText(formatTime(pos));
            tvTotal.setText(formatTime(dur));
            if (dur > 0) seekbar.setMax((int) dur);
            seekbar.setProgress((int) pos);
        }
        if (spectrumView != null) {
            int sid = MusicPlaybackService.instance != null ? MusicPlaybackService.instance.getAudioSessionId() : 0;
            spectrumView.attachSession(sid, isPlaying);
        }
        if (isLyricsVisible) {
            renderLyricsAt(pos, false);
        }
    }

    private void updateLyricsToggleUi() {
        if (layoutLyricsOverlay != null) {
            layoutLyricsOverlay.setVisibility(isLyricsVisible ? View.VISIBLE : View.GONE);
        }
        if (btnToggleLyrics != null) {
            btnToggleLyrics.setBackgroundResource(isLyricsVisible ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
            btnToggleLyrics.setTextColor(isLyricsVisible ? Color.WHITE : Color.parseColor("#B3FFFFFF"));
        }
    }

    private void loadLyricsForCurrentTrack() {
        lrcLines = null;
        lastActiveLrcIndex = -2;
        if (currentTrack == null || currentTrack.path == null || lrcExecutor.isShutdown()) {
            if (isLyricsVisible) renderLyricsAt(0, true);
            return;
        }
        final String pathSnapshot = currentTrack.path;
        lrcExecutor.execute(() -> {
            List<LrcParser.LrcLine> parsed = LrcParser.findAndParseLrc(pathSnapshot);
            android.app.Activity act = getActivity();
            if (act != null) {
                act.runOnUiThread(() -> {
                    if (!isAdded() || currentTrack == null || !pathSnapshot.equals(currentTrack.path)) return;
                    lrcLines = parsed;
                    lastActiveLrcIndex = -2;
                    if (isLyricsVisible) {
                        renderLyricsAt(currentPosition, true);
                    }
                });
            }
        });
    }

    private void renderLyricsAt(long posMs, boolean force) {
        if (tvLyrics == null) return;
        if (lrcLines == null || lrcLines.isEmpty()) {
            if (force) {
                tvLyrics.setText("NO .LRC LYRICS FOUND\nPLACE A MATCHING .LRC FILE NEXT TO THIS TRACK");
            }
            return;
        }
        int activeIdx = LrcParser.getActiveLineIndex(lrcLines, posMs);
        if (!force && activeIdx == lastActiveLrcIndex) return;
        lastActiveLrcIndex = activeIdx;

        int start = Math.max(0, activeIdx - 2);
        int end = Math.min(lrcLines.size() - 1, Math.max(0, activeIdx) + 4);
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (int i = start; i <= end; i++) {
            String line = lrcLines.get(i).text != null ? lrcLines.get(i).text.trim() : "";
            if (line.isEmpty()) continue;
            int lineStart = ssb.length();
            if (i == activeIdx) {
                ssb.append("● ").append(line.toUpperCase(Locale.US));
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#D71921")), lineStart, lineStart + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(Color.WHITE), lineStart + 1, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                ssb.append(line.toUpperCase(Locale.US));
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#66FFFFFF")), lineStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (i < end) ssb.append("\n");
        }
        tvLyrics.setText(ssb);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (spectrumView != null) {
            spectrumView.releaseVisualizer();
        }
        try { lrcExecutor.shutdownNow(); } catch (Exception ignored) {}
    }

    private void updateTrackUI() {
        if (currentTrack == null || title == null || artist == null || tvTotal == null || seekbar == null || btnPlayPause == null) return;
        if (spectrumView != null) {
            int sid = MusicPlaybackService.instance != null ? MusicPlaybackService.instance.getAudioSessionId() : 0;
            spectrumView.attachSession(sid, isPlaying);
        }
        title.setText(currentTrack.title != null && !currentTrack.title.isEmpty() ? currentTrack.title : "NOTHING TRACK");
        String artistName = (currentTrack.artist != null && !currentTrack.artist.trim().isEmpty())
                ? currentTrack.artist.trim() : "Unknown Artist";
        String albumName = (currentTrack.album != null && !currentTrack.album.trim().isEmpty())
                ? currentTrack.album.trim() : "";
        artist.setText(albumName.isEmpty() ? artistName : (artistName + " • " + albumName));
        tvTotal.setText(formatTime(currentTrack.duration));
        if (currentTrack.duration > 0) seekbar.setMax((int) currentTrack.duration);

        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_dotmatrix : R.drawable.ic_play_dotmatrix);

        if (getContext() != null && art != null) {
            Glide.with(this)
                    .load(currentTrack.path != null ? currentTrack.path : currentTrack.contentUri)
                    .placeholder(R.drawable.ic_headphones)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(art);
        }
        loadLyricsForCurrentTrack();
    }

    private String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
