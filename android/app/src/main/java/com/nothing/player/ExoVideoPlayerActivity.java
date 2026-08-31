package com.nothing.player;

import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
    private TextView gestureHudTag;
    private DotMatrixIconView gestureHudDotMatrixIcon;
    private DotMatrixTextView gestureHudDotMatrixText;
    private ImageView gestureHudIconImg;
    private TextView gestureHudIcon;
    private TextView gestureHudText;
    private DotMatrixProgressBar gestureHudDotMatrixProgress;
    private ProgressBar gestureHudProgress;

    private TextView videoTitleText;
    private TextView videoSubtitleCodec;
    private DotMatrixTextView timeCurrentText;
    private DotMatrixTextView timeTotalText;
    private DotMatrixSeekBar videoSeekBar;
    private FrameLayout seekbarPreviewCard;
    private ImageView previewThumbnail;
    private DotMatrixTextView previewTimeText;
    private boolean showRemainingTime = true;
    private MediaMetadataRetriever previewRetriever;
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
    private volatile long lastRequestedPreviewTime = -1;

    private ImageButton btnPlayPause;
    private Button btnDecoderMode;
    private Button btnSpeed;
    private Button btnAspectRatio;
    private Button btnAudioTrack;
    private Button btnSubtitleTrack;
    private Button btnRotateScreen;
    private Button btnAudioBoost;
    private Button btnPip;
    private Button btnMute;
    private ImageButton btnExpandControls;
    private View topExpandableControlsBar;

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
    private float currentVideoScale = 1.0f;
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

        // Edge-to-Edge display past camera cutout / notch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

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
        if (videoTitle == null) videoTitle = getIntent().getStringExtra("video_title");

        videoPath = getIntent().getStringExtra("path");
        if (videoPath == null) videoPath = getIntent().getStringExtra("video_path");

        videoUriStr = getIntent().getStringExtra("contentUri");
        if (videoUriStr == null) videoUriStr = getIntent().getStringExtra("video_uri");
        if (videoUriStr == null && videoPath != null && videoPath.startsWith("content://")) {
            videoUriStr = videoPath;
        }

        if (getIntent().getData() != null) {
            Uri data = getIntent().getData();
            if (videoUriStr == null) videoUriStr = data.toString();
            if (videoPath == null) videoPath = data.getPath();
            if (videoTitle == null) videoTitle = data.getLastPathSegment();
        }

        currentPositionMs = getIntent().getLongExtra("position", 0);

        initViews();
        
        // Auto-select optimal engine: If Dolby 5.1 / EAC3 / AC3 / DTS / MKV is detected, use Universal VLC directly
        if (isSurroundOrEac3File()) {
            startVlcPlayer(currentPositionMs);
        } else {
            startExoPlayer(currentPositionMs);
        }
        
        setupGestures();
        setupListeners();
    }

    private boolean isSurroundOrEac3File() {
        String testStr = ((videoTitle != null ? videoTitle : "") + " " + (videoPath != null ? videoPath : "") + " " + (videoUriStr != null ? videoUriStr : "")).toLowerCase();
        return testStr.contains("eac3") || testStr.contains("e-ac-3") || testStr.contains("dd5.1") 
            || testStr.contains("ddp") || testStr.contains("ac3") || testStr.contains("ac-3")
            || testStr.contains("dts") || testStr.contains("truehd") || testStr.contains("atmos")
            || testStr.contains("5.1") || testStr.contains("7.1") || testStr.contains(".mkv");
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void initViews() {
        exoPlayerView = findViewById(R.id.exo_player_view);
        vlcVideoLayout = findViewById(R.id.vlc_video_layout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        topControlsBar = findViewById(R.id.top_controls_bar);
        bottomControlsBar = findViewById(R.id.bottom_controls_bar);
        btnScreenLock = findViewById(R.id.btn_screen_lock);
        
        gestureHudContainer = findViewById(R.id.gesture_hud_container);
        gestureHudTag = findViewById(R.id.gesture_hud_tag);
        gestureHudDotMatrixIcon = findViewById(R.id.gesture_hud_dot_matrix_icon);
        gestureHudDotMatrixText = findViewById(R.id.gesture_hud_dot_matrix_text);
        gestureHudIconImg = findViewById(R.id.gesture_hud_icon_img);
        gestureHudIcon = findViewById(R.id.gesture_hud_icon);
        gestureHudText = findViewById(R.id.gesture_hud_text);
        gestureHudDotMatrixProgress = findViewById(R.id.gesture_hud_dot_matrix_progress);
        gestureHudProgress = findViewById(R.id.gesture_hud_progress);

        videoTitleText = findViewById(R.id.video_title_text);
        videoSubtitleCodec = findViewById(R.id.video_subtitle_codec);
        timeCurrentText = findViewById(R.id.time_current_text);
        timeTotalText = findViewById(R.id.time_total_text);
        videoSeekBar = findViewById(R.id.video_seek_bar);
        seekbarPreviewCard = findViewById(R.id.seekbar_preview_card);
        previewThumbnail = findViewById(R.id.preview_thumbnail);
        previewTimeText = findViewById(R.id.preview_time_text);
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
        btnExpandControls = findViewById(R.id.btn_expand_controls);
        topExpandableControlsBar = findViewById(R.id.top_expandable_controls_bar);

        if (videoTitle != null) {
            videoTitleText.setText(videoTitle);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        initPreviewRetriever();
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
                        videoSeekBar.setMax((int) totalDurationMs);
                        updateTimeDisplay(exoPlayer.getCurrentPosition());
                        AudioEffectManager.getInstance().attachAudioSession(exoPlayer.getAudioSessionId(), ExoVideoPlayerActivity.this);
                        updateCodecInfo();
                    } else if (playbackState == Player.STATE_ENDED) {
                        if (totalDurationMs > 5000 && exoPlayer.getCurrentPosition() >= totalDurationMs - 2500) {
                            finish();
                        }
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
        options.add("--audio-resampler=soxr");
        options.add("--avcodec-threads=0");
        options.add("--network-caching=3000");

        libVLC = new LibVLC(this, options);
        vlcPlayer = new MediaPlayer(libVLC);
        
        vlcVideoLayout.post(() -> {
            if (vlcPlayer != null && !isFinishing()) {
                try {
                    vlcPlayer.attachViews(vlcVideoLayout, null, true, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Media media = createVlcMedia();

        if (media != null) {
            media.setHWDecoderEnabled(true, true);
            media.addOption(":file-caching=2000");
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
                        videoSeekBar.setMax((int) totalDurationMs);
                        updateTimeDisplay(currentPositionMs);
                    } else if (event.type == MediaPlayer.Event.TimeChanged) {
                        long pos = event.getTimeChanged();
                        currentPositionMs = pos;
                        updateTimeDisplay(pos);
                        videoSeekBar.setProgress((int) pos);
                    } else if (event.type == MediaPlayer.Event.EndReached) {
                        if (totalDurationMs > 5000 && currentPositionMs >= totalDurationMs - 2500) {
                            finish();
                        }
                    } else if (event.type == MediaPlayer.Event.EncounteredError) {
                        Toast.makeText(this, "Playback Warning", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            vlcPlayer.play();
            if (resumePos > 0) {
                vlcPlayer.setTime(resumePos);
            }
            vlcPlayer.setRate(playbackSpeed);
            currentDecoder = "Universal VLC";
            btnDecoderMode.setText("VLC");
            videoSubtitleCodec.setText(currentDecoder + " • DOLBY 5.1 / EAC3");
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
                    Media m = new Media(libVLC, Uri.parse(videoPath));
                    m.setHWDecoderEnabled(true, true);
                    return m;
                }
                File f = new File(videoPath);
                if (f.exists()) {
                    Media m = new Media(libVLC, f.getAbsolutePath());
                    m.setHWDecoderEnabled(true, true);
                    return m;
                }
            }
            if (videoUriStr != null && !videoUriStr.isEmpty()) {
                Uri u = Uri.parse(videoUriStr);
                try {
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(u, "r");
                    if (pfd != null) {
                        Media m = new Media(libVLC, pfd.getFileDescriptor());
                        m.setHWDecoderEnabled(true, true);
                        return m;
                    }
                } catch (Exception ignored) {}
                Media m = new Media(libVLC, u);
                m.setHWDecoderEnabled(true, true);
                return m;
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
            showGestureHud("● 10s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-10s", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });

        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            long target = Math.min(totalDurationMs, cur + 10000);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
            else if (exoPlayer != null) exoPlayer.seekTo(target);
            showGestureHud("● 10s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+10s", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });

        if (btnExpandControls != null) {
            btnExpandControls.setOnClickListener(v -> {
                if (topExpandableControlsBar != null) {
                    boolean isExpanded = topExpandableControlsBar.getVisibility() == View.VISIBLE;
                    topExpandableControlsBar.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                }
                scheduleHideControls();
            });
        }

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
            btnScreenLock.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
            if (isLocked) {
                topControlsBar.setVisibility(View.GONE);
                bottomControlsBar.setVisibility(View.GONE);
                if (topExpandableControlsBar != null) topExpandableControlsBar.setVisibility(View.GONE);
                showGestureHud("● SCREEN LOCK", DotMatrixIconView.TYPE_LOCK, "LOCKED", 100);
                hideHandler.postDelayed(() -> {
                    if (isLocked) btnScreenLock.setVisibility(View.GONE);
                }, 2500);
            } else {
                topControlsBar.setVisibility(View.VISIBLE);
                bottomControlsBar.setVisibility(View.VISIBLE);
                btnScreenLock.setVisibility(View.VISIBLE);
                showGestureHud("● SCREEN LOCK", DotMatrixIconView.TYPE_LOCK, "UNLOCKED", 100);
                scheduleHideControls();
            }
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
            currentVideoScale = 1.0f;
            if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() != null) {
                exoPlayerView.getVideoSurfaceView().setScaleX(1.0f);
                exoPlayerView.getVideoSurfaceView().setScaleY(1.0f);
            }
            if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                btnAspectRatio.setText("FILL");
                showGestureHud("● ASPECT RATIO", DotMatrixIconView.TYPE_SEEK_FORWARD, "STRETCH / FULL", 100);
                if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setAspectRatio("16:9");
                    vlcPlayer.setScale(0);
                }
            } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                btnAspectRatio.setText("ZOOM");
                showGestureHud("● ASPECT RATIO", DotMatrixIconView.TYPE_SEEK_FORWARD, "ZOOM / CROP", 100);
                if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setAspectRatio(null);
                    vlcPlayer.setScale(1.25f);
                }
            } else {
                currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                btnAspectRatio.setText("FIT");
                showGestureHud("● ASPECT RATIO", DotMatrixIconView.TYPE_SEEK_FORWARD, "FIT TO SCREEN", 100);
                if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setAspectRatio(null);
                    vlcPlayer.setScale(0);
                }
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
            showGestureHud("● PLAY SPEED", DotMatrixIconView.TYPE_SEEK_FORWARD, playbackSpeed + "x", (int)(playbackSpeed * 50));
        });

        btnAudioBoost.setOnClickListener(v -> {
            isAudioBoosted = !isAudioBoosted;
            if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.setVolume(isAudioBoosted ? 200 : 100);
            } else if (exoPlayer != null) {
                exoPlayer.setVolume(isAudioBoosted ? 2.0f : 1.0f);
            }
            btnAudioBoost.setText(isAudioBoosted ? "BOOST ON" : "200%");
            int dotType = isAudioBoosted ? DotMatrixIconView.TYPE_AUDIO_BOOST : DotMatrixIconView.TYPE_VOLUME;
            showGestureHud("● AUDIO BOOST", dotType, isAudioBoosted ? "200% BOOST ON" : "100% STANDARD", isAudioBoosted ? 100 : 50);
        });

        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.setVolume(isMuted ? 0 : (isAudioBoosted ? 200 : 100));
            } else if (exoPlayer != null) {
                exoPlayer.setVolume(isMuted ? 0.0f : (isAudioBoosted ? 2.0f : 1.0f));
            }
            btnMute.setText(isMuted ? "MUTED" : "MUTE");
            btnMute.setCompoundDrawablesWithIntrinsicBounds(isMuted ? android.R.drawable.ic_lock_silent_mode : android.R.drawable.ic_lock_silent_mode_off, 0, 0, 0);
            int dotType = isMuted ? DotMatrixIconView.TYPE_VOLUME_MUTE : DotMatrixIconView.TYPE_VOLUME;
            showGestureHud("● AUDIO OUTPUT", dotType, isMuted ? "MUTED" : "UNMUTED", isMuted ? 0 : 100);
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

        timeTotalText.setOnClickListener(v -> {
            showRemainingTime = !showRemainingTime;
            updateTimeDisplay(videoSeekBar.getProgress());
            Toast.makeText(this, showRemainingTime ? "Showing Remaining Time" : "Showing Total Duration", Toast.LENGTH_SHORT).show();
        });

        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateTimeDisplay(progress);
                    updatePreviewPosition(seekBar, progress);
                    extractAndShowPreviewFrame(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressTracker();
                if (seekbarPreviewCard != null) {
                    seekbarPreviewCard.setVisibility(View.VISIBLE);
                    updatePreviewPosition(seekBar, seekBar.getProgress());
                    extractAndShowPreviewFrame(seekBar.getProgress());
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekbarPreviewCard != null) {
                    seekbarPreviewCard.setVisibility(View.GONE);
                }
                int target = seekBar.getProgress();
                if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setTime(target);
                } else if (exoPlayer != null) {
                    exoPlayer.seekTo(target);
                }
                startProgressTracker();
                scheduleHideControls();
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
        builder.setTitle("Select Audio Track");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            try {
                TrackGroup selectedGroup = audioGroups.get(which);
                int trackIndex = trackIndices.get(which);
                Format format = selectedGroup.getFormat(trackIndex);
                String mime = format.sampleMimeType != null ? format.sampleMimeType.toLowerCase() : "";

                // If Dolby / DTS multichannel is selected, switch directly to Universal VLC engine without crash
                if (mime.contains("eac3") || mime.contains("ac3") || mime.contains("dts") || mime.contains("truehd") || format.channelCount > 2) {
                    long cur = exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
                    Toast.makeText(this, "Activating Universal Dolby 5.1 Engine", Toast.LENGTH_SHORT).show();
                    startVlcPlayer(cur);
                    dialog.dismiss();
                    return;
                }

                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setOverrideForType(new TrackSelectionOverride(selectedGroup, trackIndex))
                );

                Toast.makeText(this, "Selected: " + trackLabels.get(which), Toast.LENGTH_SHORT).show();
                updateCodecInfo();
            } catch (Exception e) {
                long cur = exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
                startVlcPlayer(cur);
            }
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
        builder.setTitle("Select Audio Track (Dolby 5.1 Universal)");
        builder.setSingleChoiceItems(labels.toArray(new CharSequence[0]), selectedIndex, (dialog, which) -> {
            try {
                vlcPlayer.setAudioTrack(trackIds.get(which));
                Toast.makeText(this, "Selected: " + labels.get(which), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        final int GESTURE_NONE = 0;
        final int GESTURE_VERTICAL = 1;
        final int GESTURE_HORIZONTAL = 2;
        final int[] gestureMode = {GESTURE_NONE};
        final long[] seekStartPosition = {0};
        final long[] targetSeekPosition = {0};
        final float[] volumeAccumulator = {0f};

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!isLocked) {
                    toggleControlsVisibility();
                } else {
                    if (btnScreenLock.getVisibility() == View.VISIBLE) {
                        btnScreenLock.setVisibility(View.GONE);
                    } else {
                        btnScreenLock.setVisibility(View.VISIBLE);
                        hideHandler.postDelayed(() -> {
                            if (isLocked) btnScreenLock.setVisibility(View.GONE);
                        }, 3000);
                    }
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked) return false;
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);

                if (e.getX() < screenWidth / 2f) {
                    // Double Tap Left: Rewind 10s
                    long target = Math.max(0, cur - 10000);
                    if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("● 10s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-10s", (int) (target * 100 / Math.max(1, totalDurationMs)));
                } else {
                    // Double Tap Right: Forward 10s
                    long target = Math.min(totalDurationMs, cur + 10000);
                    if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("● 10s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+10s", (int) (target * 100 / Math.max(1, totalDurationMs)));
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isLocked || e1 == null || e2 == null) return false;

                float totalDx = Math.abs(e2.getX() - e1.getX());
                float totalDy = Math.abs(e2.getY() - e1.getY());

                // Lock gesture direction once movement exceeds threshold
                if (gestureMode[0] == GESTURE_NONE) {
                    if (totalDy > 25 && totalDy > totalDx * 1.25f) {
                        gestureMode[0] = GESTURE_VERTICAL;
                    } else if (totalDx > 35 && totalDx > totalDy * 1.25f) {
                        gestureMode[0] = GESTURE_HORIZONTAL;
                        seekStartPosition[0] = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
                    } else {
                        return false;
                    }
                }

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;

                if (gestureMode[0] == GESTURE_VERTICAL) {
                    // Vertical Swipe ONLY: Volume / Brightness
                    float deltaY = distanceY / (float) screenHeight;

                    if (e1.getX() > screenWidth / 2f) {
                        // Right Side: Smooth Volume Swipe
                        if (audioManager != null) {
                            volumeAccumulator[0] += deltaY;
                            float threshold = 1.0f / (maxVolume * 1.6f);
                            if (Math.abs(volumeAccumulator[0]) >= threshold) {
                                int step = volumeAccumulator[0] > 0 ? 1 : -1;
                                volumeAccumulator[0] = 0;
                                int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                int newVol = Math.max(0, Math.min(maxVolume, currentVol + step));
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                                int percent = (int) ((float) newVol / maxVolume * 100);
                                int dotType = newVol == 0 ? DotMatrixIconView.TYPE_VOLUME_MUTE : (isAudioBoosted ? DotMatrixIconView.TYPE_AUDIO_BOOST : DotMatrixIconView.TYPE_VOLUME);
                                showGestureHud("● VOLUME", dotType, percent + "%", percent);
                            }
                        }
                    } else {
                        // Left Side: Smooth Brightness Swipe
                        currentBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness + deltaY * 0.25f));
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.screenBrightness = currentBrightness;
                        getWindow().setAttributes(lp);
                        int percent = (int) (currentBrightness * 100);
                        showGestureHud("● BRIGHTNESS", DotMatrixIconView.TYPE_BRIGHTNESS, percent + "%", percent);
                    }
                    return true;
                } else if (gestureMode[0] == GESTURE_HORIZONTAL) {
                    // Horizontal Swipe ONLY: Fast Forward / Rewind
                    float totalDeltaX = (e2.getX() - e1.getX()) / (float) screenWidth;
                    long maxSeekSpan = Math.max(60000, Math.min(180000, totalDurationMs / 5));
                    long deltaMs = (long) (totalDeltaX * maxSeekSpan);
                    targetSeekPosition[0] = Math.max(0, Math.min(totalDurationMs, seekStartPosition[0] + deltaMs));

                    long diffSec = (targetSeekPosition[0] - seekStartPosition[0]) / 1000;
                    String sign = diffSec >= 0 ? "+" : "";
                    String text = sign + diffSec + "s";
                    String tag = "● " + formatTime(targetSeekPosition[0]) + " / " + formatTime(totalDurationMs);
                    int progress = (int) (targetSeekPosition[0] * 100 / Math.max(1, totalDurationMs));
                    int dotType = diffSec >= 0 ? DotMatrixIconView.TYPE_SEEK_FORWARD : DotMatrixIconView.TYPE_SEEK_REWIND;

                    showGestureHud(tag, dotType, text, progress);
                    return true;
                }
                return false;
            }
        });

        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (isLocked) return false;
                float scaleFactor = detector.getScaleFactor();
                currentVideoScale = Math.max(0.5f, Math.min(3.5f, currentVideoScale * scaleFactor));

                if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setScale(currentVideoScale);
                } else if (exoPlayerView != null) {
                    View surface = exoPlayerView.getVideoSurfaceView();
                    if (surface != null) {
                        surface.setScaleX(currentVideoScale);
                        surface.setScaleY(currentVideoScale);
                    }
                }

                int percent = (int) (currentVideoScale * 100);
                showGestureHud("● PINCH ZOOM", DotMatrixIconView.TYPE_SEEK_FORWARD, percent + "%", Math.min(100, (int) ((currentVideoScale - 0.5f) / 3.0f * 100)));
                return true;
            }
        });

        View.OnTouchListener touchListener = (v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            if (!scaleGestureDetector.isInProgress()) {
                gestureDetector.onTouchEvent(event);
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (gestureMode[0] == GESTURE_HORIZONTAL) {
                    long target = targetSeekPosition[0];
                    if (isVlcActive && vlcPlayer != null) {
                        vlcPlayer.setTime(target);
                    } else if (exoPlayer != null) {
                        exoPlayer.seekTo(target);
                    }
                    videoSeekBar.setProgress((int) target);
                    timeCurrentText.setText(formatTime(target));
                }
                gestureMode[0] = GESTURE_NONE;
                volumeAccumulator[0] = 0f;
            }
            return true;
        };

        exoPlayerView.setOnTouchListener(touchListener);
        vlcVideoLayout.setOnTouchListener(touchListener);
    }

    private void showGestureHud(String tag, int dotMatrixType, String text, int progress) {
        if (gestureHudTag != null) {
            gestureHudTag.setText(tag);
        }
        if (gestureHudDotMatrixIcon != null) {
            gestureHudDotMatrixIcon.setIconType(dotMatrixType);
        }
        if (gestureHudDotMatrixText != null) {
            gestureHudDotMatrixText.setText(text);
            if (progress > 100 || (tag != null && tag.contains("BOOST"))) {
                gestureHudDotMatrixText.setTextColor(Color.parseColor("#D71921"));
            } else {
                gestureHudDotMatrixText.setTextColor(Color.WHITE);
            }
        }
        if (gestureHudText != null) {
            gestureHudText.setText(text);
        }
        if (gestureHudDotMatrixProgress != null) {
            gestureHudDotMatrixProgress.setProgress(progress);
        }
        if (gestureHudProgress != null) {
            gestureHudProgress.setProgress(Math.max(0, Math.min(100, progress)));
        }

        if (gestureHudContainer != null) {
            if (gestureHudContainer.getVisibility() != View.VISIBLE) {
                gestureHudContainer.setAlpha(0f);
                gestureHudContainer.setScaleX(0.92f);
                gestureHudContainer.setScaleY(0.92f);
                gestureHudContainer.setVisibility(View.VISIBLE);
                gestureHudContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start();
            }
        }

        hideHandler.removeCallbacks(hideHudRunnable);
        hideHandler.postDelayed(hideHudRunnable, 1200);
    }

    private void showGestureHud(String icon, String text, int progress) {
        showGestureHud("● CONTROLS", DotMatrixIconView.TYPE_VOLUME, text, progress);
    }

    private final Runnable hideHudRunnable = () -> {
        if (gestureHudContainer != null && gestureHudContainer.getVisibility() == View.VISIBLE) {
            gestureHudContainer.animate()
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(160)
                .withEndAction(() -> gestureHudContainer.setVisibility(View.GONE))
                .start();
        }
    };

    private void toggleControlsVisibility() {
        if (areControlsVisible) {
            topControlsBar.setVisibility(View.GONE);
            bottomControlsBar.setVisibility(View.GONE);
            btnScreenLock.setVisibility(View.GONE);
            if (topExpandableControlsBar != null) topExpandableControlsBar.setVisibility(View.GONE);
            areControlsVisible = false;
        } else {
            topControlsBar.setVisibility(View.VISIBLE);
            bottomControlsBar.setVisibility(View.VISIBLE);
            btnScreenLock.setVisibility(View.VISIBLE);
            btnScreenLock.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
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
            btnScreenLock.setVisibility(View.GONE);
            if (topExpandableControlsBar != null) topExpandableControlsBar.setVisibility(View.GONE);
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
                updateTimeDisplay(pos);
                progressHandler.postDelayed(this, 500);
            }
        }
    };

    private void updateTimeDisplay(long currentPos) {
        timeCurrentText.setText(formatTime(currentPos));
        if (showRemainingTime) {
            long remaining = Math.max(0, totalDurationMs - currentPos);
            timeTotalText.setText("-" + formatTime(remaining));
        } else {
            timeTotalText.setText(formatTime(totalDurationMs));
        }
    }

    private void updatePreviewPosition(SeekBar seekBar, int progress) {
        if (seekbarPreviewCard == null) return;
        int usableWidth = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        float ratio = totalDurationMs > 0 ? (float) progress / (float) totalDurationMs : 0f;
        float thumbX = seekBar.getX() + seekBar.getPaddingLeft() + (usableWidth * ratio);

        int cardWidth = seekbarPreviewCard.getWidth() > 0 ? seekbarPreviewCard.getWidth() : dpToPx(140);
        float targetX = thumbX - (cardWidth / 2f);

        float minX = dpToPx(8);
        View parent = (View) seekbarPreviewCard.getParent();
        float maxX = parent != null ? parent.getWidth() - cardWidth - dpToPx(8) : getResources().getDisplayMetrics().widthPixels - cardWidth;
        targetX = Math.max(minX, Math.min(maxX, targetX));

        seekbarPreviewCard.setTranslationX(targetX);
        if (previewTimeText != null) {
            previewTimeText.setText(formatTime(progress));
        }
    }

    private void initPreviewRetriever() {
        previewExecutor.execute(() -> {
            try {
                if (previewRetriever == null) {
                    previewRetriever = new MediaMetadataRetriever();
                }
                if (videoPath != null && new File(videoPath).exists()) {
                    previewRetriever.setDataSource(videoPath);
                } else if (videoUriStr != null) {
                    previewRetriever.setDataSource(getApplicationContext(), Uri.parse(videoUriStr));
                } else if (getIntent().getData() != null) {
                    previewRetriever.setDataSource(getApplicationContext(), getIntent().getData());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void extractAndShowPreviewFrame(long timeMs) {
        if (previewThumbnail == null) return;
        lastRequestedPreviewTime = timeMs;

        previewExecutor.execute(() -> {
            if (timeMs != lastRequestedPreviewTime) return;
            Bitmap frame = null;
            try {
                if (previewRetriever == null) {
                    if (videoPath != null && new File(videoPath).exists()) {
                        previewRetriever = new MediaMetadataRetriever();
                        previewRetriever.setDataSource(videoPath);
                    } else if (videoUriStr != null) {
                        previewRetriever = new MediaMetadataRetriever();
                        previewRetriever.setDataSource(getApplicationContext(), Uri.parse(videoUriStr));
                    }
                }
                if (previewRetriever != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        frame = previewRetriever.getScaledFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 300, 180);
                    }
                    if (frame == null) {
                        frame = previewRetriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    }
                }
            } catch (Exception ignored) {}

            if (frame != null && timeMs == lastRequestedPreviewTime) {
                final Bitmap finalFrame = frame;
                runOnUiThread(() -> {
                    if (previewThumbnail != null && seekbarPreviewCard != null && seekbarPreviewCard.getVisibility() == View.VISIBLE) {
                        previewThumbnail.setImageBitmap(finalFrame);
                    }
                });
            } else {
                runOnUiThread(() -> {
                    try {
                        Uri uri = resolveMediaUri();
                        if (uri != null && previewThumbnail != null) {
                            Glide.with(ExoVideoPlayerActivity.this)
                                .asBitmap()
                                .load(uri)
                                .override(300, 180)
                                .frame(timeMs * 1000)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(previewThumbnail);
                        }
                    } catch (Exception ignored) {}
                });
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

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
        try {
            if (previewRetriever != null) {
                previewRetriever.release();
                previewRetriever = null;
            }
            previewExecutor.shutdownNow();
        } catch (Exception ignored) {}
        releaseExoPlayer();
        releaseVlcPlayer();
    }
}
