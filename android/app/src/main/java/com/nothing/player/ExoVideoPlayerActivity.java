package com.nothing.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExoVideoPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private View topControlsBar;
    private View bottomControlsBar;
    private ImageButton btnScreenLock;
    private TextView gestureHudText;
    private TextView videoTitleText;
    private TextView timeCurrentText;
    private TextView timeTotalText;
    private SeekBar videoSeekBar;
    private ImageButton btnPlayPause;
    private Button btnSpeed;
    private Button btnAspectRatio;
    private Button btnAudioTrack;
    private Button btnSubtitleTrack;
    private Button btnRotateScreen;

    private boolean isLocked = false;
    private boolean areControlsVisible = true;
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    private AudioManager audioManager;
    private int maxVolume = 15;
    private float currentBrightness = 0.5f;

    private float playbackSpeed = 1.0f;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Pure black immersive full-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_exo_player);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        initViews();
        setupPlayer();
        setupGestures();
        setupListeners();
    }

    private void initViews() {
        playerView = findViewById(R.id.exo_player_view);
        topControlsBar = findViewById(R.id.top_controls_bar);
        bottomControlsBar = findViewById(R.id.bottom_controls_bar);
        btnScreenLock = findViewById(R.id.btn_screen_lock);
        gestureHudText = findViewById(R.id.gesture_hud_text);
        videoTitleText = findViewById(R.id.video_title_text);
        timeCurrentText = findViewById(R.id.time_current_text);
        timeTotalText = findViewById(R.id.time_total_text);
        videoSeekBar = findViewById(R.id.video_seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnSpeed = findViewById(R.id.btn_speed);
        btnAspectRatio = findViewById(R.id.btn_aspect_ratio);
        btnAudioTrack = findViewById(R.id.btn_audio_track);
        btnSubtitleTrack = findViewById(R.id.btn_subtitle_track);
        btnRotateScreen = findViewById(R.id.btn_rotate_screen);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupPlayer() {
        String title = getIntent().getStringExtra("title");
        String path = getIntent().getStringExtra("path");
        String uriStr = getIntent().getStringExtra("contentUri");
        long startPos = getIntent().getLongExtra("position", 0);

        if (title != null) {
            videoTitleText.setText(title);
        }

        player = new ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build();

        playerView.setPlayer(player);

        Uri videoUri = null;
        if (uriStr != null && !uriStr.isEmpty()) {
            videoUri = Uri.parse(uriStr);
        } else if (path != null && !path.isEmpty()) {
            videoUri = Uri.fromFile(new File(path));
        }

        if (videoUri != null) {
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);
            if (startPos > 0) {
                player.seekTo(startPos);
            }
            player.prepare();
            player.play();
        } else {
            Toast.makeText(this, "Could not open media source", Toast.LENGTH_SHORT).show();
            finish();
        }

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                if (isPlaying) {
                    startProgressTracker();
                    scheduleHideControls();
                } else {
                    stopProgressTracker();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long duration = player.getDuration();
                    if (duration > 0) {
                        videoSeekBar.setMax((int) duration);
                        timeTotalText.setText(formatTime(duration));
                    }
                }
            }
        });
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (player != null) {
                if (player.isPlaying()) player.pause();
                else player.play();
            }
        });

        findViewById(R.id.btn_rewind).setOnClickListener(v -> {
            if (player != null) player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
        });

        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            if (player != null) player.seekTo(Math.min(player.getDuration(), player.getCurrentPosition() + 10000));
        });

        btnScreenLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            btnScreenLock.setImageResource(isLocked ? android.R.drawable.ic_lock_lock : android.R.drawable.ic_lock_power_off);
            topControlsBar.setVisibility(isLocked ? View.GONE : View.VISIBLE);
            bottomControlsBar.setVisibility(isLocked ? View.GONE : View.VISIBLE);
            showGestureHud(isLocked ? "SCREEN LOCKED 🔒" : "SCREEN UNLOCKED 🔓");
        });

        btnRotateScreen.setOnClickListener(v -> {
            int currentOrientation = getRequestedOrientation();
            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });

        btnAspectRatio.setOnClickListener(v -> {
            if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                btnAspectRatio.setText("FILL");
            } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                btnAspectRatio.setText("ZOOM");
            } else {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                btnAspectRatio.setText("FIT");
            }
            playerView.setResizeMode(currentResizeMode);
        });

        btnSpeed.setOnClickListener(v -> {
            float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
            int nextIndex = 0;
            for (int i = 0; i < speeds.length; i++) {
                if (Math.abs(speeds[i] - playbackSpeed) < 0.05) {
                    nextIndex = (i + 1) % speeds.length;
                    break;
                }
            }
            playbackSpeed = speeds[nextIndex];
            if (player != null) player.setPlaybackSpeed(playbackSpeed);
            btnSpeed.setText(playbackSpeed + "x");
            showGestureHud("SPEED: " + playbackSpeed + "x");
        });

        btnAudioTrack.setOnClickListener(v -> showAudioTrackDialog());
        btnSubtitleTrack.setOnClickListener(v -> showSubtitleTrackDialog());

        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    timeCurrentText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                hideHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleHideControls();
            }
        });
    }

    private void showAudioTrackDialog() {
        if (player == null) return;
        Tracks tracks = player.getCurrentTracks();
        List<String> audioNames = new ArrayList<>();
        List<TrackGroup> audioGroups = new ArrayList<>();
        List<Integer> trackIndices = new ArrayList<>();

        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_AUDIO) {
                TrackGroup trackGroup = group.getMediaTrackGroup();
                for (int i = 0; i < trackGroup.length; i++) {
                    String lang = trackGroup.getFormat(i).language;
                    String label = trackGroup.getFormat(i).label;
                    String desc = (label != null ? label : "") + " " + (lang != null ? "[" + lang + "]" : "Audio Track " + (audioNames.size() + 1));
                    audioNames.add(desc.trim());
                    audioGroups.add(trackGroup);
                    trackIndices.add(i);
                }
            }
        }

        if (audioNames.isEmpty()) {
            Toast.makeText(this, "Default Stereo Audio Active", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = audioNames.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
            .setTitle("Select Audio Track")
            .setItems(items, (dialog, which) -> {
                TrackGroup selectedGroup = audioGroups.get(which);
                int trackIdx = trackIndices.get(which);
                player.setTrackSelectionParameters(
                    player.getTrackSelectionParameters()
                        .buildUpon()
                        .setOverrideForType(new TrackSelectionOverride(selectedGroup, trackIdx))
                        .build()
                );
                showGestureHud("AUDIO: " + items[which]);
            })
            .show();
    }

    private void showSubtitleTrackDialog() {
        if (player == null) return;
        Tracks tracks = player.getCurrentTracks();
        List<String> subNames = new ArrayList<>();
        subNames.add("Disable Subtitles (OFF)");

        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_TEXT) {
                TrackGroup trackGroup = group.getMediaTrackGroup();
                for (int i = 0; i < trackGroup.length; i++) {
                    String lang = trackGroup.getFormat(i).language;
                    String label = trackGroup.getFormat(i).label;
                    subNames.add((label != null ? label : "") + " [" + (lang != null ? lang : "Sub") + "]");
                }
            }
        }

        CharSequence[] items = subNames.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
            .setTitle("Subtitles")
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    player.setTrackSelectionParameters(
                        player.getTrackSelectionParameters()
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    );
                    showGestureHud("SUBTITLES: OFF");
                } else {
                    player.setTrackSelectionParameters(
                        player.getTrackSelectionParameters()
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .build()
                    );
                    showGestureHud("SUBTITLES: ON");
                }
            })
            .show();
    }

    private void setupGestures() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isLocked) {
                    btnScreenLock.setVisibility(btnScreenLock.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                } else {
                    toggleControls();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked || player == null) return false;
                float x = e.getX();
                int width = playerView.getWidth();
                if (x < width / 2f) {
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
                    showGestureHud("⏪ -10s");
                } else {
                    player.seekTo(Math.min(player.getDuration(), player.getCurrentPosition() + 10000));
                    showGestureHud("⏩ +10s");
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isLocked || e1 == null || e2 == null) return false;
                float deltaY = e1.getY() - e2.getY();
                float deltaX = e2.getX() - e1.getX();

                if (Math.abs(deltaY) > Math.abs(deltaX)) {
                    if (e1.getX() < playerView.getWidth() / 2f) {
                        // Left side Brightness
                        currentBrightness = Math.max(0.05f, Math.min(1.0f, currentBrightness + (deltaY / 2000f)));
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.screenBrightness = currentBrightness;
                        getWindow().setAttributes(lp);
                        showGestureHud("BRIGHTNESS: " + Math.round(currentBrightness * 100) + "%");
                    } else {
                        // Right side Volume
                        if (audioManager != null) {
                            int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            int change = deltaY > 0 ? 1 : -1;
                            int newVol = Math.max(0, Math.min(maxVolume, currentVol + change));
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                            showGestureHud("VOLUME: " + Math.round((float) newVol / maxVolume * 100) + "%");
                        }
                    }
                }
                return true;
            }
        });

        playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void toggleControls() {
        areControlsVisible = !areControlsVisible;
        topControlsBar.setVisibility(areControlsVisible ? View.VISIBLE : View.GONE);
        bottomControlsBar.setVisibility(areControlsVisible ? View.VISIBLE : View.GONE);
        btnScreenLock.setVisibility(areControlsVisible ? View.VISIBLE : View.GONE);
        if (areControlsVisible) scheduleHideControls();
    }

    private void scheduleHideControls() {
        hideHandler.removeCallbacksAndMessages(null);
        hideHandler.postDelayed(() -> {
            if (!isLocked && player != null && player.isPlaying()) {
                areControlsVisible = false;
                topControlsBar.setVisibility(View.GONE);
                bottomControlsBar.setVisibility(View.GONE);
                btnScreenLock.setVisibility(View.GONE);
            }
        }, 3500);
    }

    private void showGestureHud(String text) {
        gestureHudText.setText(text);
        gestureHudText.setVisibility(View.VISIBLE);
        hideHandler.postDelayed(() -> gestureHudText.setVisibility(View.GONE), 1200);
    }

    private void startProgressTracker() {
        progressHandler.post(new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    long pos = player.getCurrentPosition();
                    videoSeekBar.setProgress((int) pos);
                    timeCurrentText.setText(formatTime(pos));
                    progressHandler.postDelayed(this, 500);
                }
            }
        });
    }

    private void stopProgressTracker() {
        progressHandler.removeCallbacksAndMessages(null);
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, sec);
        }
        return String.format("%02d:%02d", m, sec);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressTracker();
        hideHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
