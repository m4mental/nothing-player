package com.nothing.player;

import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExoVideoPlayerActivity extends AppCompatActivity {

    // ExoPlayer Elements
    private ExoPlayer exoPlayer;
    private DefaultTrackSelector trackSelector;
    private PlayerView exoPlayerView;

    // LibVLC Universal Codec Elements (Universal EAC3, AC3, DTS, TrueHD, 10-Bit)
    private LibVLC libVLC;
    private MediaPlayer vlcPlayer;
    private VLCVideoLayout vlcVideoLayout;
    private boolean isVlcActive = false;

    // UI Controls
    private View topControlsBar;
    private View bottomControlsBar;
    private ImageButton btnScreenLock;
    private View gestureHudContainer;
    private TextView gestureHudIcon;
    private TextView gestureHudText;
    private ProgressBar gestureHudProgress;

    private TextView videoTitleText;
    private TextView videoSubtitleCodec;
    private TextView timeCurrentText;
    private TextView timeTotalText;
    private SeekBar videoSeekBar;
    private ImageButton btnPlayPause;
    private Button btnDecoderMode;
    private Button btnSpeed;
    private Button btnAspectRatio;
    private Button btnAudioTrack;
    private Button btnSubtitleTrack;
    private Button btnRotateScreen;
    private Button btnAudioBoost;
    private Button btnPip;
    private ImageButton btnMute;

    private boolean isLocked = false;
    private boolean areControlsVisible = true;
    private boolean isMuted = false;
    private boolean isAudioBoosted = false;
    private String currentDecoder = "HW+";

    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    private AudioManager audioManager;
    private int maxVolume = 15;
    private float currentBrightness = 0.5f;
    private float playbackSpeed = 1.0f;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;

    private String videoPath;
    private String videoUriStr;
    private String videoTitle;
    private long currentPositionMs = 0;
    private long totalDurationMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always Open in Landscape Cinema Mode by Default
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // Keep Screen On & Hide System Status / Nav Bars
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        setContentView(R.layout.activity_exo_player);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        // Initialize Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        currentBrightness = lp.screenBrightness > 0 ? lp.screenBrightness : 0.5f;

        videoTitle = getIntent().getStringExtra("title");
        videoPath = getIntent().getStringExtra("path");
        videoUriStr = getIntent().getStringExtra("contentUri");
        currentPositionMs = getIntent().getLongExtra("position", 0);

        initViews();
        
        // Auto-select optimal engine: If Dolby 5.1 / EAC3 / AC3 / DTS is in title/path, use VLC Engine directly
        if (isSurroundOrEac3File()) {
            startVlcPlayer(currentPositionMs);
        } else {
            startExoPlayer(currentPositionMs);
        }
        
        setupGestures();
        setupListeners();
    }

    private boolean isSurroundOrEac3File() {
        String testStr = ((videoTitle != null ? videoTitle : "") + " " + (videoPath != null ? videoPath : "")).toLowerCase();
        return testStr.contains("eac3") || testStr.contains("dd5.1") || testStr.contains("ac3") 
            || testStr.contains("dts") || testStr.contains("truehd") || testStr.contains("atmos")
            || testStr.contains("5.1");
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void initViews() {
        exoPlayerView = findViewById(R.id.exo_player_view);
        vlcVideoLayout = findViewById(R.id.vlc_video_layout);

        topControlsBar = findViewById(R.id.top_controls_bar);
        bottomControlsBar = findViewById(R.id.bottom_controls_bar);
        btnScreenLock = findViewById(R.id.btn_screen_lock);
        
        gestureHudContainer = findViewById(R.id.gesture_hud_container);
        gestureHudIcon = findViewById(R.id.gesture_hud_icon);
        gestureHudText = findViewById(R.id.gesture_hud_text);
        gestureHudProgress = findViewById(R.id.gesture_hud_progress);

        videoTitleText = findViewById(R.id.video_title_text);
        videoSubtitleCodec = findViewById(R.id.video_subtitle_codec);
        timeCurrentText = findViewById(R.id.time_current_text);
        timeTotalText = findViewById(R.id.time_total_text);
        videoSeekBar = findViewById(R.id.video_seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnDecoderMode = findViewById(R.id.btn_decoder_mode);
        btnSpeed = findViewById(R.id.btn_speed);
        btnAspectRatio = findViewById(R.id.btn_aspect_ratio);
        btnAudioTrack = findViewById(R.id.btn_audio_track);
        btnSubtitleTrack = findViewById(R.id.btn_subtitle_track);
        btnRotateScreen = findViewById(R.id.btn_rotate_screen);
        btnAudioBoost = findViewById(R.id.btn_audio_boost);
        btnPip = findViewById(R.id.btn_pip);
        btnMute = findViewById(R.id.btn_mute);

        if (videoTitle != null) {
            videoTitleText.setText(videoTitle);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // ==========================================
    // EXOPLAYER ENGINE (Hardware Accelerated)
    // ==========================================
    private void startExoPlayer(long resumePos) {
        releaseVlcPlayer();
        isVlcActive = false;
        vlcVideoLayout.setVisibility(View.GONE);
        exoPlayerView.setVisibility(View.VISIBLE);

        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true);

        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowAudioNonSeamlessAdaptiveness(true)
                .setTunnelingEnabled(false)
        );

        exoPlayer = new ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build();
        exoPlayer.setAudioAttributes(audioAttributes, true);

        exoPlayerView.setPlayer(exoPlayer);
        exoPlayerView.setResizeMode(currentResizeMode);

        Uri targetUri = resolveMediaUri();

        if (targetUri != null) {
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);
            MediaItem mediaItem = MediaItem.fromUri(targetUri);
            
            exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem));
            
            if (resumePos > 0) {
                exoPlayer.seekTo(resumePos);
            }
            exoPlayer.prepare();
            exoPlayer.play();
            exoPlayer.setPlaybackSpeed(playbackSpeed);
            currentDecoder = "HW+ (Exo)";
            btnDecoderMode.setText("HW+");
            updateCodecInfo();
        }

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isVlcActive) {
                    btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    if (isPlaying) {
                        startProgressTracker();
                        scheduleHideControls();
                    } else {
                        stopProgressTracker();
                    }
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (!isVlcActive) {
                    if (playbackState == Player.STATE_READY) {
                        totalDurationMs = exoPlayer.getDuration();
                        timeTotalText.setText(formatTime(totalDurationMs));
                        videoSeekBar.setMax((int) totalDurationMs);
                        updateCodecInfo();
                    } else if (playbackState == Player.STATE_ENDED) {
                        finish();
                    }
                }
            }

            @Override
            public void onTracksChanged(Tracks tracks) {
                if (!isVlcActive) updateCodecInfo();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                long currentPos = exoPlayer != null ? exoPlayer.getCurrentPosition() : currentPositionMs;
                startVlcPlayer(currentPos);
            }
        });
    }

    // =======================================================
    // LIBVLC NATIVE ENGINE (Universal EAC3, AC3, DTS, TrueHD)
    // =======================================================
    private void startVlcPlayer(long resumePos) {
        releaseExoPlayer();
        isVlcActive = true;
        exoPlayerView.setVisibility(View.GONE);
        vlcVideoLayout.setVisibility(View.VISIBLE);

        ArrayList<String> options = new ArrayList<>();
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        options.add("--audio-time-stretch");
        options.add("--aout=opensles");
        options.add("--audio-resampler=soxr");

        libVLC = new LibVLC(this, options);
        vlcPlayer = new MediaPlayer(libVLC);
        
        // Use TextureView for instant zero-black-screen rendering
        vlcPlayer.attachViews(vlcVideoLayout, null, true, true);

        Media media = createVlcMedia();

        if (media != null) {
            media.setHWDecoderEnabled(true, false);
            vlcPlayer.setMedia(media);
            media.release();

            vlcPlayer.setEventListener(event -> {
                runOnUiThread(() -> {
                    if (event.type == MediaPlayer.Event.Playing) {
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                        startProgressTracker();
                        scheduleHideControls();
                    } else if (event.type == MediaPlayer.Event.Paused) {
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    } else if (event.type == MediaPlayer.Event.LengthChanged) {
                        totalDurationMs = vlcPlayer.getLength();
                        timeTotalText.setText(formatTime(totalDurationMs));
                        videoSeekBar.setMax((int) totalDurationMs);
                    } else if (event.type == MediaPlayer.Event.TimeChanged) {
                        long pos = event.getTimeChanged();
                        currentPositionMs = pos;
                        videoSeekBar.setProgress((int) pos);
                        timeCurrentText.setText(formatTime(pos));
                    } else if (event.type == MediaPlayer.Event.EndReached) {
                        finish();
                    }
                });
            });

            vlcPlayer.play();
            if (resumePos > 0) {
                vlcPlayer.setTime(resumePos);
            }
            vlcPlayer.setRate(playbackSpeed);
            currentDecoder = "SW+ (VLC)";
            btnDecoderMode.setText("SW+");
            videoSubtitleCodec.setText(currentDecoder + " • DOLBY EAC3 / 5.1 STEREO");
        } else {
            Toast.makeText(this, "Could not open video stream", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Media createVlcMedia() {
        if (libVLC == null) return null;
        try {
            if (videoPath != null && !videoPath.isEmpty()) {
                if (videoPath.startsWith("http://") || videoPath.startsWith("https://") || videoPath.startsWith("rtsp://")) {
                    return new Media(libVLC, Uri.parse(videoPath));
                }
                File f = new File(videoPath);
                if (f.exists()) {
                    return new Media(libVLC, f.getAbsolutePath());
                }
            }
            if (videoUriStr != null && !videoUriStr.isEmpty()) {
                Uri u = Uri.parse(videoUriStr);
                try {
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(u, "r");
                    if (pfd != null) {
                        return new Media(libVLC, pfd.getFileDescriptor());
                    }
                } catch (Exception ignored) {}
                return new Media(libVLC, u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void releaseExoPlayer() {
        if (exoPlayer != null) {
            currentPositionMs = exoPlayer.getCurrentPosition();
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void releaseVlcPlayer() {
        if (vlcPlayer != null) {
            currentPositionMs = vlcPlayer.getTime();
            vlcPlayer.stop();
            vlcPlayer.detachViews();
            vlcPlayer.release();
            vlcPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }

    private void updateCodecInfo() {
        if (isVlcActive) {
            videoSubtitleCodec.setText(currentDecoder + " • DOLBY 5.1 / EAC3 STEREO");
            return;
        }
        if (exoPlayer == null) return;
        Tracks tracks = exoPlayer.getCurrentTracks();
        String audioDesc = "AUDIO TRACK";
        
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_AUDIO && group.isSelected()) {
                for (int i = 0; i < group.length; i++) {
                    if (group.isTrackSelected(i)) {
                        Format f = group.getTrackFormat(i);
                        String mime = f.sampleMimeType != null ? f.sampleMimeType.replace("audio/", "").toUpperCase() : "AUDIO";
                        String lang = f.language != null ? (" • " + f.language.toUpperCase()) : "";
                        int channels = f.channelCount;
                        String chStr = channels == 6 ? " (5.1 Surround)" : channels == 2 ? " (Stereo)" : "";
                        audioDesc = mime + chStr + lang;
                        break;
                    }
                }
            }
        }
        videoSubtitleCodec.setText(currentDecoder + " • " + audioDesc);
    }

    private Uri resolveMediaUri() {
        if (videoUriStr != null && !videoUriStr.isEmpty()) {
            return Uri.parse(videoUriStr);
        }
        if (videoPath != null && !videoPath.isEmpty()) {
            if (videoPath.startsWith("http://") || videoPath.startsWith("https://") || videoPath.startsWith("rtsp://")) {
                return Uri.parse(videoPath);
            }
            File file = new File(videoPath);
            if (file.exists()) {
                return Uri.fromFile(file);
            }
            return Uri.parse(videoPath);
        }
        return null;
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (isVlcActive) {
                if (vlcPlayer != null) {
                    if (vlcPlayer.isPlaying()) vlcPlayer.pause();
                    else vlcPlayer.play();
                }
            } else {
                if (exoPlayer != null) {
                    if (exoPlayer.isPlaying()) exoPlayer.pause();
                    else exoPlayer.play();
                }
            }
        });

        findViewById(R.id.btn_rewind).setOnClickListener(v -> {
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            long target = Math.max(0, cur - 10000);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
            else if (exoPlayer != null) exoPlayer.seekTo(target);
            showGestureHud("⏪ -10s", "REWIND", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });

        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            long target = Math.min(totalDurationMs, cur + 10000);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
            else if (exoPlayer != null) exoPlayer.seekTo(target);
            showGestureHud("⏩ +10s", "FORWARD", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });

        btnDecoderMode.setOnClickListener(v -> {
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            if (isVlcActive) {
                Toast.makeText(this, "Switching to Hardware (HW+) Decoder", Toast.LENGTH_SHORT).show();
                startExoPlayer(cur);
            } else {
                Toast.makeText(this, "Switching to Universal VLC (SW+) Engine", Toast.LENGTH_SHORT).show();
                startVlcPlayer(cur);
            }
        });

        btnAudioTrack.setOnClickListener(v -> {
            if (isVlcActive) showVlcAudioTrackDialog();
            else showExoAudioTrackDialog();
        });

        btnSubtitleTrack.setOnClickListener(v -> {
            if (isVlcActive) showVlcSubtitleTrackDialog();
            else showExoSubtitleTrackDialog();
        });

        btnScreenLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            btnScreenLock.setImageResource(isLocked ? android.R.drawable.ic_lock_lock : android.R.drawable.ic_lock_power_off);
            topControlsBar.setVisibility(isLocked ? View.GONE : View.VISIBLE);
            bottomControlsBar.setVisibility(isLocked ? View.GONE : View.VISIBLE);
            showGestureHud(isLocked ? "🔒" : "🔓", isLocked ? "LOCKED" : "UNLOCKED", 100);
        });

        btnRotateScreen.setOnClickListener(v -> {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        });

        btnAspectRatio.setOnClickListener(v -> {
            if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                btnAspectRatio.setText("FILL");
                showGestureHud("📺", "STRETCH / FILL", 100);
            } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                btnAspectRatio.setText("ZOOM");
                showGestureHud("🔍", "ZOOM / CROP", 100);
            } else {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                btnAspectRatio.setText("FIT");
                showGestureHud("📐", "FIT TO SCREEN", 100);
            }
            if (exoPlayerView != null) exoPlayerView.setResizeMode(currentResizeMode);
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
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setRate(playbackSpeed);
            else if (exoPlayer != null) exoPlayer.setPlaybackSpeed(playbackSpeed);
            btnSpeed.setText(playbackSpeed + "x");
            showGestureHud("⚡", "SPEED: " + playbackSpeed + "x", (int)(playbackSpeed * 50));
        });

        btnAudioBoost.setOnClickListener(v -> {
            isAudioBoosted = !isAudioBoosted;
            if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.setVolume(isAudioBoosted ? 200 : 100);
            } else if (exoPlayer != null) {
                exoPlayer.setVolume(isAudioBoosted ? 2.0f : 1.0f);
            }
            btnAudioBoost.setText(isAudioBoosted ? "BOOST ON" : "200%");
            showGestureHud("🔊", isAudioBoosted ? "AUDIO BOOST 200%" : "AUDIO 100%", isAudioBoosted ? 100 : 50);
        });

        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.setVolume(isMuted ? 0 : (isAudioBoosted ? 200 : 100));
            } else if (exoPlayer != null) {
                exoPlayer.setVolume(isMuted ? 0.0f : (isAudioBoosted ? 2.0f : 1.0f));
            }
            btnMute.setImageResource(isMuted ? android.R.drawable.ic_lock_silent_mode : android.R.drawable.ic_lock_silent_mode_off);
            showGestureHud(isMuted ? "🔇" : "🔊", isMuted ? "MUTED" : "UNMUTED", isMuted ? 0 : 100);
        });

        btnPip.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    int w = getWindow().getDecorView().getWidth();
                    int h = getWindow().getDecorView().getHeight();
                    Rational aspectRatio = new Rational(w > 0 ? w : 16, h > 0 ? h : 9);
                    PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
                    pipBuilder.setAspectRatio(aspectRatio);
                    enterPictureInPictureMode(pipBuilder.build());
                } catch (Exception e) {
                    Toast.makeText(this, "PiP Mode not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    timeCurrentText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressTracker();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int target = seekBar.getProgress();
                if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setTime(target);
                } else if (exoPlayer != null) {
                    exoPlayer.seekTo(target);
                }
                startProgressTracker();
            }
        });
    }

    private void showExoAudioTrackDialog() {
        if (exoPlayer == null || trackSelector == null) return;
        Tracks tracks = exoPlayer.getCurrentTracks();

        final List<TrackGroup> audioGroups = new ArrayList<>();
        final List<Integer> trackIndices = new ArrayList<>();
        final List<String> trackLabels = new ArrayList<>();
        int selectedIndex = -1;

        int trackCounter = 1;
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_AUDIO) {
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getTrackFormat(i);
                    audioGroups.add(group.getMediaTrackGroup());
                    trackIndices.add(i);

                    String mime = format.sampleMimeType != null ? format.sampleMimeType.replace("audio/", "").toUpperCase() : "AUDIO";
                    String lang = format.language != null ? (" [" + format.language.toUpperCase() + "]") : "";
                    String label = format.label != null ? format.label : ("Track " + trackCounter);
                    int channels = format.channelCount;
                    String chInfo = channels == 6 ? " (5.1 Surround)" : channels == 2 ? " (Stereo)" : (" (" + channels + " ch)");

                    trackLabels.add(label + lang + " • " + mime + chInfo);

                    if (group.isTrackSelected(i)) {
                        selectedIndex = trackLabels.size() - 1;
                    }
                    trackCounter++;
                }
            }
        }

        if (trackLabels.isEmpty()) {
            Toast.makeText(this, "No audio tracks found", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = trackLabels.toArray(new CharSequence[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Select Audio Track (ExoPlayer)");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            TrackGroup selectedGroup = audioGroups.get(which);
            int trackIndex = trackIndices.get(which);

            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setOverrideForType(new TrackSelectionOverride(selectedGroup, trackIndex))
            );

            Toast.makeText(this, "Selected: " + trackLabels.get(which), Toast.LENGTH_SHORT).show();
            updateCodecInfo();
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showVlcAudioTrackDialog() {
        if (vlcPlayer == null) return;
        MediaPlayer.TrackDescription[] tracks = vlcPlayer.getAudioTracks();
        if (tracks == null || tracks.length == 0) {
            Toast.makeText(this, "No audio tracks found", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentTrackId = vlcPlayer.getAudioTrack();
        int selectedIndex = 0;
        final List<String> labels = new ArrayList<>();
        final List<Integer> trackIds = new ArrayList<>();

        for (int i = 0; i < tracks.length; i++) {
            labels.add(tracks[i].name);
            trackIds.add(tracks[i].id);
            if (tracks[i].id == currentTrackId) {
                selectedIndex = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Select Audio Track (VLC Dolby Engine)");
        builder.setSingleChoiceItems(labels.toArray(new CharSequence[0]), selectedIndex, (dialog, which) -> {
            vlcPlayer.setAudioTrack(trackIds.get(which));
            Toast.makeText(this, "Selected: " + labels.get(which), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showExoSubtitleTrackDialog() {
        if (exoPlayer == null || trackSelector == null) return;
        Tracks tracks = exoPlayer.getCurrentTracks();

        final List<TrackGroup> textGroups = new ArrayList<>();
        final List<Integer> trackIndices = new ArrayList<>();
        final List<String> trackLabels = new ArrayList<>();
        int selectedIndex = 0;

        trackLabels.add("Off / Disable Subtitles");

        int subCounter = 1;
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_TEXT) {
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getTrackFormat(i);
                    textGroups.add(group.getMediaTrackGroup());
                    trackIndices.add(i);

                    String lang = format.language != null ? (" [" + format.language.toUpperCase() + "]") : "";
                    String label = format.label != null ? format.label : ("Subtitle " + subCounter);

                    trackLabels.add(label + lang);

                    if (group.isTrackSelected(i)) {
                        selectedIndex = trackLabels.size() - 1;
                    }
                    subCounter++;
                }
            }
        }

        CharSequence[] items = trackLabels.toArray(new CharSequence[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Select Subtitle (ESub / CC)");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            if (which == 0) {
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                );
                Toast.makeText(this, "Subtitles Disabled", Toast.LENGTH_SHORT).show();
            } else {
                TrackGroup selectedGroup = textGroups.get(which - 1);
                int trackIndex = trackIndices.get(which - 1);

                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(new TrackSelectionOverride(selectedGroup, trackIndex))
                );
                Toast.makeText(this, "Subtitle: " + trackLabels.get(which), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showVlcSubtitleTrackDialog() {
        if (vlcPlayer == null) return;
        MediaPlayer.TrackDescription[] tracks = vlcPlayer.getSpuTracks();
        if (tracks == null || tracks.length == 0) {
            Toast.makeText(this, "No subtitles found", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentTrackId = vlcPlayer.getSpuTrack();
        int selectedIndex = 0;
        final List<String> labels = new ArrayList<>();
        final List<Integer> trackIds = new ArrayList<>();

        for (int i = 0; i < tracks.length; i++) {
            labels.add(tracks[i].name);
            trackIds.add(tracks[i].id);
            if (tracks[i].id == currentTrackId) {
                selectedIndex = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Select Subtitle (VLC Engine)");
        builder.setSingleChoiceItems(labels.toArray(new CharSequence[0]), selectedIndex, (dialog, which) -> {
            vlcPlayer.setSpuTrack(trackIds.get(which));
            Toast.makeText(this, "Selected: " + labels.get(which), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setupGestures() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!isLocked) {
                    toggleControlsVisibility();
                } else {
                    btnScreenLock.setVisibility(btnScreenLock.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked) return false;
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);

                if (e.getX() < screenWidth / 2) {
                    // Double Tap Left: Rewind 10s
                    long target = Math.max(0, cur - 10000);
                    if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("⏪ -10s", "REWIND", (int) (target * 100 / Math.max(1, totalDurationMs)));
                } else {
                    // Double Tap Right: Forward 10s
                    long target = Math.min(totalDurationMs, cur + 10000);
                    if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("⏩ +10s", "FORWARD", (int) (target * 100 / Math.max(1, totalDurationMs)));
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isLocked || e1 == null || e2 == null) return false;

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                float deltaY = distanceY / screenHeight;

                if (Math.abs(distanceY) > Math.abs(distanceX)) {
                    if (e1.getX() > screenWidth / 2) {
                        // Right Side: Volume Swipe
                        if (audioManager != null) {
                            int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            int change = (int) (deltaY * maxVolume * 2.5f);
                            int newVol = Math.max(0, Math.min(maxVolume, currentVol + (distanceY > 0 ? 1 : -1)));
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                            int percent = (int) ((float) newVol / maxVolume * 100);
                            showGestureHud(newVol == 0 ? "🔇" : "🔊", "VOLUME: " + percent + "%", percent);
                        }
                    } else {
                        // Left Side: Brightness Swipe
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        currentBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness + deltaY * 0.8f));
                        lp.screenBrightness = currentBrightness;
                        getWindow().setAttributes(lp);
                        int percent = (int) (currentBrightness * 100);
                        showGestureHud("☀️", "BRIGHTNESS: " + percent + "%", percent);
                    }
                    return true;
                }
                return false;
            }
        });

        View.OnTouchListener touchListener = (v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        };

        exoPlayerView.setOnTouchListener(touchListener);
        vlcVideoLayout.setOnTouchListener(touchListener);
    }

    private void showGestureHud(String icon, String text, int progress) {
        gestureHudIcon.setText(icon);
        gestureHudText.setText(text);
        gestureHudProgress.setProgress(Math.max(0, Math.min(100, progress)));
        gestureHudContainer.setVisibility(View.VISIBLE);

        hideHandler.removeCallbacks(hideHudRunnable);
        hideHandler.postDelayed(hideHudRunnable, 1200);
    }

    private final Runnable hideHudRunnable = () -> gestureHudContainer.setVisibility(View.GONE);

    private void toggleControlsVisibility() {
        if (areControlsVisible) {
            topControlsBar.setVisibility(View.GONE);
            bottomControlsBar.setVisibility(View.GONE);
            areControlsVisible = false;
        } else {
            topControlsBar.setVisibility(View.VISIBLE);
            bottomControlsBar.setVisibility(View.VISIBLE);
            areControlsVisible = true;
            scheduleHideControls();
        }
    }

    private void scheduleHideControls() {
        hideHandler.removeCallbacks(hideControlsRunnable);
        hideHandler.postDelayed(hideControlsRunnable, 4500);
    }

    private final Runnable hideControlsRunnable = () -> {
        boolean isPlaying = isVlcActive ? (vlcPlayer != null && vlcPlayer.isPlaying()) : (exoPlayer != null && exoPlayer.isPlaying());
        if (isPlaying && !isLocked) {
            topControlsBar.setVisibility(View.GONE);
            bottomControlsBar.setVisibility(View.GONE);
            areControlsVisible = false;
        }
    };

    private void startProgressTracker() {
        progressHandler.post(progressRunnable);
    }

    private void stopProgressTracker() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isVlcActive && exoPlayer != null && exoPlayer.isPlaying()) {
                long pos = exoPlayer.getCurrentPosition();
                videoSeekBar.setProgress((int) pos);
                timeCurrentText.setText(formatTime(pos));
                progressHandler.postDelayed(this, 500);
            }
        }
    };

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

    @Override
    protected void onPause() {
        super.onPause();
        if (isVlcActive && vlcPlayer != null && vlcPlayer.isPlaying()) {
            vlcPlayer.pause();
        } else if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressTracker();
        hideHandler.removeCallbacksAndMessages(null);
        releaseExoPlayer();
        releaseVlcPlayer();
    }
}
