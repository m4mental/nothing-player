package com.nothing.player;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.graphics.Point;
import android.graphics.Rect;
import android.content.SharedPreferences;
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
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
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
    private FrameLayout playerRootLayout;
    private float targetCustomAspect = 0f;

    // LibVLC Universal Codec Elements (Universal EAC3, AC3, DTS, TrueHD, 10-Bit)
    private LibVLC libVLC;
    private MediaPlayer vlcPlayer;
    private VLCVideoLayout vlcVideoLayout;
    private boolean isVlcActive = false;

    // UI Controls
    private WebView youtubeStreamView;
    private ImageView videoTransitionOverlay;
    private boolean isYouTubeActive = false;
    private boolean isYouTubePlaying = true;
    private long youtubeDurationMs = 0;
    private long youtubeCurrentPositionMs = 0;

    // PiP Actions
    private static final String ACTION_PIP_PLAY_PAUSE = "com.nothing.player.PIP_PLAY_PAUSE";
    private static final String ACTION_PIP_REWIND = "com.nothing.player.PIP_REWIND";
    private static final String ACTION_PIP_FORWARD = "com.nothing.player.PIP_FORWARD";
    private static final String ACTION_PIP_PREV = "com.nothing.player.PIP_PREV";
    private static final String ACTION_PIP_NEXT = "com.nothing.player.PIP_NEXT";

    private final BroadcastReceiver pipReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (ACTION_PIP_PLAY_PAUSE.equals(action)) {
                togglePlayPause();
            } else if (ACTION_PIP_REWIND.equals(action)) {
                seekBy(-10000);
            } else if (ACTION_PIP_FORWARD.equals(action)) {
                seekBy(10000);
            } else if (ACTION_PIP_PREV.equals(action)) {
                playPreviousVideo();
                updatePipActions();
            } else if (ACTION_PIP_NEXT.equals(action)) {
                playNextVideo();
                updatePipActions();
            }
        }
    };

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
    private final ExecutorService titleExecutor = Executors.newFixedThreadPool(4);
    private volatile long lastRequestedPreviewTime = -1;

    private ImageButton btnPlayPause;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ImageButton btnRewind;
    private ImageButton btnForward;
    private ImageButton btnPlaylistQueue;
    private View playlistQueuePanel;
    private TextView queueHeaderTitle;
    private ImageButton btnCloseQueue;
    private RecyclerView playlistRecyclerView;
    private PlaylistQueueAdapter queueAdapter;
    private ArrayList<String> playlistPaths = new ArrayList<>();
    private ArrayList<String> playlistUris = new ArrayList<>();
    private ArrayList<String> playlistTitles = new ArrayList<>();
    private int playlistIndex = 0;
    private String youtubePlaylistId = null;
    private boolean isYouTubeLoaded = false;

    // Top Controls
    private ImageButton btnCast;
    private Button btnDecoderMode;
    private ImageButton btnSubtitleTrack;
    private ImageButton btnMoreMenu;

    // Quick Action Bar
    private ImageButton btnQuickOrientation;
    private ImageButton btnQuickMute;
    private ImageButton btnQuickBackground;
    private Button btnQuickSpeed;
    private ImageButton btnQuickExpand;
    private View layoutQuickActionsExpanded;
    private ImageButton btnQuickNightMode;
    private ImageButton btnQuickAbRepeat;
    private ImageButton btnQuickPip;
    private ImageButton btnQuickEqualizer;
    private ImageButton btnQuickMirror;
    private ImageButton btnQuickScreenshot;
    private ImageButton btnFloatingLock;
    private String currentRatioLabel = "FIT";

    // Bottom Controls
    private Button btnBottomSpeed;
    private Button btnAspectRatio;

    // Slide-over Panels & Overlays
    private View panelRatio;
    private ImageButton btnCloseRatioPanel;
    private View panelSpeed;
    private ImageButton btnCloseSpeedPanel;
    private View panelMoreMenu;
    private ImageButton btnCloseMoreMenu;
    private View nightModeOverlay;
    private TextView tvSpeedCurrentValue;
    private SeekBar seekbarPlaybackSpeed;

    // Phase 2 Panels & Overlays
    private View panelVisualEnhancer;
    private ImageButton btnCloseVisualEnhancer;
    private ImageButton btnBackVisualEnhancer;
    private View panelSleepTimer;
    private ImageButton btnCloseSleepTimer;
    private ImageButton btnBackSleepTimer;
    private View visualEnhancerOverlay;

    // Phase 3 Others Panel Views
    private View panelOthers;
    private ImageButton btnBackOthers;
    private ImageButton btnCloseOthers;
    private View itemOthersProperties;
    private View itemOthersFaq;
    private View itemOthersCastFaq;
    private View itemOthersPlaybackIssue;

    // Phase 2 Speed Panel Views
    private View layoutSpeedMain;
    private View layoutSpeedAdvanced;
    private View btnOpenSpeedAdvanced;
    private ImageButton btnBackSpeedAdvanced;
    private TextView tvLongPressSpeedTarget;
    private SwitchCompat switchLongPressVibration;
    private float longPressSpeedTarget = 2.0f;
    private boolean longPressVibration = true;
    private Vibrator vibrator;

    // Phase 2 More Menu Elements
    private TextView tvMenuRepeatModeVal;
    private ImageButton btnRepeatOrder, btnRepeatOne, btnRepeatShuffle, btnRepeatAll, btnRepeatOnce;
    private int currentRepeatMode = 3; // 0=Order, 1=Loop One, 2=Shuffle, 3=Loop All, 4=Once
    private SeekBar seekbarMenuBrightness;
    private TextView tvMenuBrightnessVal;
    private SeekBar seekbarMenuVolume;
    private TextView tvMenuVolumeVal;
    private Button btnMenuDecoderHw, btnMenuDecoderSw;
    private SwitchCompat switchMenuScreenshotToggle;
    private ImageView ivMenuBookmarkIcon;
    private ImageView ivMenuFavoriteIcon;
    private TextView tvMenuTimerLabel;

    // Phase 2 Visual Enhancer
    private Button btnFilterOriginal, btnFilterHdr, btnFilterClear, btnFilterUltraClear, btnFilterArcticBlue, btnFilterWarmGlow, btnFilterCinematic;
    private SwitchCompat switchFilterApplyAll;
    private String currentVisualFilter = "ORIGINAL";
    private boolean filterApplyAll = true;

    // Phase 2 Sleep Timer
    private TextView tvSleepTimerStatus;
    private Button btnTimerOff, btnTimer15, btnTimer30, btnTimer60, btnTimer90, btnTimerEndOfVideo;
    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepTimerRunnable;
    private long sleepTimerEndTimeMs = 0;
    private boolean sleepTimerEndOfVideo = false;

    // Toggles & Preferences
    private boolean rememberRatio = true;
    private boolean directRatioSwitch = true;
    private boolean rememberSpeed = false;
    private boolean longPress2xBoost = true;
    private boolean is2xBoostActive = false;
    private float preBoostSpeed = 1.0f;
    private int currentOrientationMode = 0; // 0 = Landscape, 1 = Portrait, 2 = Sensor
    private boolean isNightModeActive = false;
    private boolean isMirrored = false;
    private long repeatPointA = -1;
    private long repeatPointB = -1;
    private int repeatMode = 0; // 0 = All, 1 = One, 2 = Off

    private boolean isLocked = false;
    private boolean areControlsVisible = true;
    private boolean isMuted = false;
    private boolean isAudioBoosted = false;
    private boolean isTrackingTouch = false;
    private String currentDecoder = "HW+";

    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    private AudioManager audioManager;
    private int maxVolume = 15;
    private float currentBrightness = 0.5f;
    private float playbackSpeed = 1.0f;
    private float currentVideoScale = 1.0f;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private int videoWidth = 0;
    private int videoHeight = 0;

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

        // True Edge-to-Edge display past camera cutout / notch (committing LayoutParams to WindowManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            } else {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            getWindow().setAttributes(lp);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Keep Screen On & Hide System Status / Nav Bars
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        setContentView(R.layout.activity_exo_player);

        View rootLayout = findViewById(R.id.player_root_layout);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        // Initialize Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        currentBrightness = lp.screenBrightness > 0 ? lp.screenBrightness : 0.5f;

        initViews();
        setupGestures();
        setupListeners();

        IntentFilter pipFilter = new IntentFilter();
        pipFilter.addAction(ACTION_PIP_PLAY_PAUSE);
        pipFilter.addAction(ACTION_PIP_REWIND);
        pipFilter.addAction(ACTION_PIP_FORWARD);
        pipFilter.addAction(ACTION_PIP_PREV);
        pipFilter.addAction(ACTION_PIP_NEXT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, pipFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pipReceiver, pipFilter);
        }

        processIntent(getIntent(), false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent(intent, true);
    }

    private void processIntent(Intent intent, boolean isNewIntent) {
        if (intent == null) return;

        ArrayList<String> paths = intent.getStringArrayListExtra("playlist_paths");
        ArrayList<String> uris = intent.getStringArrayListExtra("playlist_uris");
        ArrayList<String> titles = intent.getStringArrayListExtra("playlist_titles");
        int index = intent.getIntExtra("playlist_index", 0);

        if (paths != null && !paths.isEmpty()) {
            playlistPaths = paths;
            playlistUris = uris != null ? uris : new ArrayList<>();
            playlistTitles = titles != null ? titles : new ArrayList<>();
            playlistIndex = Math.max(0, Math.min(playlistPaths.size() - 1, index));
            videoPath = playlistPaths.get(playlistIndex);
            videoUriStr = playlistUris.size() > playlistIndex ? playlistUris.get(playlistIndex) : null;
            videoTitle = playlistTitles.size() > playlistIndex ? playlistTitles.get(playlistIndex) : "Nothing Media Player";
        } else {
            String mediaUrl = StreamUrlHelper.extractMediaUriOrUrl(intent);
            String customTitle = StreamUrlHelper.extractTitle(this, intent, mediaUrl);

            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                if (StreamUrlHelper.isOnlineStream(mediaUrl)) {
                    StreamUrlHelper.saveRecentStream(this, mediaUrl);
                    videoUriStr = mediaUrl;
                    videoPath = mediaUrl;
                } else if (mediaUrl.startsWith("content://")) {
                    videoUriStr = mediaUrl;
                    videoPath = null;
                } else {
                    videoPath = mediaUrl;
                    videoUriStr = null;
                }

                videoTitle = customTitle;

                playlistPaths = new ArrayList<>();
                playlistPaths.add(videoPath != null ? videoPath : videoUriStr);
                playlistUris = new ArrayList<>();
                if (videoUriStr != null) playlistUris.add(videoUriStr);
                playlistTitles = new ArrayList<>();
                if (videoTitle != null) playlistTitles.add(videoTitle);
                playlistIndex = 0;
            }
        }

        // Resume & Start Over Support
        currentPositionMs = intent.getLongExtra("position", 0);
        boolean startOver = intent.getBooleanExtra("start_over", false);
        String currentKey = videoPath != null && !videoPath.isEmpty() ? videoPath : videoUriStr;
        if (currentPositionMs <= 0 && !startOver && currentKey != null) {
            PlaybackHistoryManager.HistoryItem saved = PlaybackHistoryManager.getProgress(this, currentKey);
            if (saved != null && saved.positionMs > 2000 && (saved.durationMs <= 0 || saved.positionMs < saved.durationMs - 5000)) {
                currentPositionMs = saved.positionMs;
                showGestureHud("● RESUMED", DotMatrixIconView.TYPE_SEEK_FORWARD, "AT " + saved.getFormattedPosition(), 100);
            }
        }

        youtubePlaylistId = YouTubeStreamResolver.extractPlaylistId(videoUriStr != null ? videoUriStr : videoPath);

        if (videoTitleText != null && videoTitle != null) {
            videoTitleText.setText(videoTitle);
        }
        updateQueueUI();

        if (isNewIntent) {
            // Seamless YouTube stream transition
            if (isYouTubeActive && isYouTubeLoaded && youtubeStreamView != null && isYouTubeStream()) {
                String rawUrl = videoUriStr != null ? videoUriStr : videoPath;
                String vid = YouTubeStreamResolver.extractVideoId(rawUrl);
                if (vid != null && !vid.isEmpty()) {
                    youtubeStreamView.evaluateJavascript("if (player && player.loadVideoById) { player.loadVideoById('" + vid + "'); }", null);
                    fetchSingleYouTubeTitle(rawUrl);
                    showGestureHud("● ONLINE STREAM", DotMatrixIconView.TYPE_SEEK_FORWARD, videoTitle, 100);
                    return;
                }
            }

            // Release any previously playing engines cleanly
            if (isYouTubeActive) {
                if (youtubeStreamView != null) {
                    youtubeStreamView.loadUrl("about:blank");
                    youtubeStreamView.setVisibility(View.GONE);
                }
                isYouTubeActive = false;
                isYouTubeLoaded = false;
            }
            if (isVlcActive) {
                releaseVlcPlayer();
                isVlcActive = false;
                if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(View.GONE);
            }
            if (exoPlayer != null) {
                releaseExoPlayer();
                if (exoPlayerView != null) exoPlayerView.setVisibility(View.GONE);
            }

            // Reset Seekbar and time displays
            if (videoSeekBar != null) {
                videoSeekBar.setProgress(0);
                videoSeekBar.setMax(100);
            }
            if (timeCurrentText != null) timeCurrentText.setText("00:00");
            if (timeTotalText != null) timeTotalText.setText("00:00");
        }

        if (StreamUrlHelper.isOnlineStream(videoUriStr != null ? videoUriStr : videoPath)) {
            showGestureHud("● STREAMING", DotMatrixIconView.TYPE_SEEK_FORWARD, videoTitle, 100);
        }

        // Auto-select optimal engine
        if (isYouTubeStream()) {
            startYouTubePlayer();
            fetchSingleYouTubeTitle(videoUriStr != null ? videoUriStr : videoPath);
        } else if (isSurroundOrEac3File() || isRtspOrLiveStream(videoUriStr != null ? videoUriStr : videoPath)) {
            startVlcPlayer(currentPositionMs);
        } else {
            startExoPlayer(currentPositionMs);
        }
    }

    private boolean isRtspOrLiveStream(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase();
        return lower.startsWith("rtsp://") || lower.startsWith("rtmp://") || lower.startsWith("mms://");
    }

    private String resolveTitleFromUri(Uri uri) {
        if (uri == null) return "Nothing Media Player";
        String displayName = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = uri.getLastPathSegment();
            if (displayName != null && displayName.contains("/")) {
                displayName = displayName.substring(displayName.lastIndexOf('/') + 1);
            }
        }
        return (displayName != null && !displayName.isEmpty()) ? displayName : "Nothing Media Player";
    }

    private boolean isYouTubeStream() {
        String target = videoUriStr != null && !videoUriStr.isEmpty() ? videoUriStr : videoPath;
        return target != null && YouTubeStreamResolver.isYouTubeUrl(target);
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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hideSystemUI();
        applyAspectRatio();
    }

    /**
     * Accurately detects the true physical display resolution (width and height),
     * bypassing status bars, navigation bars, and display cutouts.
     */
    public Point getRealScreenSize() {
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Rect bounds = wm.getCurrentWindowMetrics().getBounds();
                point.set(bounds.width(), bounds.height());
                return point;
            }
        }
        WindowManager wm = getWindowManager();
        if (wm != null) {
            android.view.Display display = wm.getDefaultDisplay();
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            display.getRealMetrics(dm);
            point.set(dm.widthPixels, dm.heightPixels);
        } else {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            point.set(dm.widthPixels, dm.heightPixels);
        }
        return point;
    }

    private float getAspectFromLabel(String label) {
        if (label == null) return 0f;
        switch (label.toUpperCase()) {
            case "16:9": return 16f / 9f;
            case "4:3": return 4f / 3f;
            case "18:9": return 18f / 9f;
            case "19.5:9": return 19.5f / 9f;
            case "20:9": return 20f / 9f;
            case "21:9": return 21f / 9f;
            case "1.85:1": return 1.85f;
            case "2.21:1": return 2.21f;
            case "2.35:1": return 2.35f;
            case "2.39:1": return 2.39f;
            default: return 0f;
        }
    }

    private void applyAspectRatio() {
        Point realSize = getRealScreenSize();
        int rootW = (playerRootLayout != null && playerRootLayout.getWidth() > 0) ? playerRootLayout.getWidth() : realSize.x;
        int rootH = (playerRootLayout != null && playerRootLayout.getHeight() > 0) ? playerRootLayout.getHeight() : realSize.y;

        if (playerRootLayout != null && playerRootLayout.getWidth() == 0) {
            playerRootLayout.post(this::applyAspectRatio);
        }

        if (targetCustomAspect <= 0.05f && currentRatioLabel != null) {
            targetCustomAspect = getAspectFromLabel(currentRatioLabel);
        }

        if (btnAspectRatio != null) {
            if (currentRatioLabel != null && !currentRatioLabel.isEmpty()) {
                btnAspectRatio.setText(currentRatioLabel.toUpperCase());
            } else if (targetCustomAspect > 0.05f) {
                btnAspectRatio.setText("CUSTOM");
            } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                btnAspectRatio.setText("STRETCH");
            } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                btnAspectRatio.setText("FILL");
            } else {
                btnAspectRatio.setText("FIT");
            }
        }

        if (targetCustomAspect > 0.05f) {
            // STANDARD / CINEMA Aspect Ratio Modes (16:9, 4:3, 21:9, 1.85:1, 2.35:1, etc.)
            float containerAspect = (float) rootW / (float) Math.max(1, rootH);
            int targetW, targetH;
            if (targetCustomAspect <= containerAspect) {
                targetH = rootH;
                targetW = Math.max(1, Math.round(targetH * targetCustomAspect));
            } else {
                targetW = rootW;
                targetH = Math.max(1, Math.round(targetW / targetCustomAspect));
            }

            FrameLayout.LayoutParams customLp = new FrameLayout.LayoutParams(targetW, targetH, Gravity.CENTER);

            if (exoPlayerView != null) {
                exoPlayerView.setLayoutParams(customLp);
                exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                if (exoPlayerView.getVideoSurfaceView() != null) {
                    exoPlayerView.getVideoSurfaceView().setScaleX(1.0f);
                    exoPlayerView.getVideoSurfaceView().setScaleY(1.0f);
                }
            }

            if (isVlcActive && vlcPlayer != null) {
                if (vlcVideoLayout != null) {
                    vlcVideoLayout.setLayoutParams(customLp);
                }
                vlcPlayer.setAspectRatio(currentRatioLabel != null ? currentRatioLabel.replace(":", "/") : null);
                vlcPlayer.setScale(0);
            }

            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.setLayoutParams(customLp);
                youtubeStreamView.setScaleX(1.0f);
                youtubeStreamView.setScaleY(1.0f);
            }
        } else {
            // SCREEN Aspect Ratio Modes: FIT, FILL, ORIGINAL, STRETCH
            FrameLayout.LayoutParams fullLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            );

            if (exoPlayerView != null) {
                exoPlayerView.setLayoutParams(fullLp);
                exoPlayerView.setResizeMode(currentResizeMode);
                if (exoPlayerView.getVideoSurfaceView() != null) {
                    exoPlayerView.getVideoSurfaceView().setScaleX(1.0f);
                    exoPlayerView.getVideoSurfaceView().setScaleY(1.0f);
                }
            }

            int sw = Math.max(rootW, rootH);
            int sh = Math.min(rootW, rootH);
            float screenAspect = (float) sw / (float) Math.max(1, sh);
            float videoAspect = (videoWidth > 0 && videoHeight > 0) ? ((float) videoWidth / videoHeight) : (16f / 9f);
            float zoomRatio = screenAspect / videoAspect;
            if (zoomRatio < 1.0f) zoomRatio = videoAspect / screenAspect;
            if (zoomRatio < 1.01f) zoomRatio = 1.25f;

            if (isVlcActive && vlcPlayer != null) {
                if (vlcVideoLayout != null) {
                    vlcVideoLayout.setLayoutParams(fullLp);
                }
                if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                    vlcPlayer.setAspectRatio(sw + ":" + sh);
                    vlcPlayer.setScale(0);
                } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    vlcPlayer.setAspectRatio(null);
                    vlcPlayer.setScale(zoomRatio);
                } else {
                    vlcPlayer.setAspectRatio(null);
                    vlcPlayer.setScale(0);
                }
            }

            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.setLayoutParams(fullLp);
                if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                    youtubeStreamView.setScaleX(zoomRatio);
                    youtubeStreamView.setScaleY(1.0f);
                } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    youtubeStreamView.setScaleX(zoomRatio);
                    youtubeStreamView.setScaleY(zoomRatio);
                } else {
                    youtubeStreamView.setScaleX(1.0f);
                    youtubeStreamView.setScaleY(1.0f);
                }
            }
        }
    }

    private void initViews() {
        playerRootLayout = findViewById(R.id.player_root_layout);
        exoPlayerView = findViewById(R.id.exo_player_view);
        if (exoPlayerView != null) {
            exoPlayerView.setKeepContentOnPlayerReset(true);
            exoPlayerView.setShutterBackgroundColor(Color.TRANSPARENT);
            exoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
        }
        vlcVideoLayout = findViewById(R.id.vlc_video_layout);
        youtubeStreamView = findViewById(R.id.youtube_stream_view);
        videoTransitionOverlay = findViewById(R.id.video_transition_overlay);

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
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnPlaylistQueue = findViewById(R.id.btn_playlist_queue);
        playlistQueuePanel = findViewById(R.id.playlist_queue_panel);
        queueHeaderTitle = findViewById(R.id.queue_header_title);
        btnCloseQueue = findViewById(R.id.btn_close_queue);
        playlistRecyclerView = findViewById(R.id.playlist_recycler_view);

        if (playlistRecyclerView != null) {
            playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            queueAdapter = new PlaylistQueueAdapter(this, pos -> {
                playVideoAtIndex(pos);
                if (playlistQueuePanel != null) {
                    playlistQueuePanel.setVisibility(View.GONE);
                }
            });
            playlistRecyclerView.setAdapter(queueAdapter);
            updateQueueUI();
        }

        btnDecoderMode = findViewById(R.id.btn_decoder_mode);
        btnCast = findViewById(R.id.btn_cast);
        btnSubtitleTrack = findViewById(R.id.btn_subtitle_track);
        btnMoreMenu = findViewById(R.id.btn_more_menu);

        btnQuickOrientation = findViewById(R.id.btn_quick_orientation);
        btnQuickMute = findViewById(R.id.btn_quick_mute);
        btnQuickBackground = findViewById(R.id.btn_quick_background);
        btnQuickSpeed = findViewById(R.id.btn_quick_speed);
        btnQuickExpand = findViewById(R.id.btn_quick_expand);
        layoutQuickActionsExpanded = findViewById(R.id.layout_quick_actions_expanded);
        btnQuickNightMode = findViewById(R.id.btn_quick_night_mode);
        btnQuickAbRepeat = findViewById(R.id.btn_quick_ab_repeat);
        btnQuickPip = findViewById(R.id.btn_quick_pip);
        btnQuickEqualizer = findViewById(R.id.btn_quick_equalizer);
        btnQuickMirror = findViewById(R.id.btn_quick_mirror);
        btnQuickScreenshot = findViewById(R.id.btn_quick_screenshot);
        btnFloatingLock = findViewById(R.id.btn_floating_lock);

        btnBottomSpeed = findViewById(R.id.btn_bottom_speed);
        btnAspectRatio = findViewById(R.id.btn_aspect_ratio);
        btnRewind = findViewById(R.id.btn_rewind);
        btnForward = findViewById(R.id.btn_forward);

        panelRatio = findViewById(R.id.panel_ratio);
        btnCloseRatioPanel = findViewById(R.id.btn_close_ratio_panel);
        panelSpeed = findViewById(R.id.panel_speed);
        btnCloseSpeedPanel = findViewById(R.id.btn_close_speed_panel);
        panelMoreMenu = findViewById(R.id.panel_more_menu);
        btnCloseMoreMenu = findViewById(R.id.btn_close_more_menu);
        nightModeOverlay = findViewById(R.id.night_mode_overlay);
        tvSpeedCurrentValue = findViewById(R.id.tv_speed_current_value);
        seekbarPlaybackSpeed = findViewById(R.id.seekbar_playback_speed);

        // Phase 2 Views Binding
        visualEnhancerOverlay = findViewById(R.id.visual_enhancer_overlay);
        panelVisualEnhancer = findViewById(R.id.panel_visual_enhancer);
        btnCloseVisualEnhancer = findViewById(R.id.btn_close_visual_enhancer);
        btnBackVisualEnhancer = findViewById(R.id.btn_back_visual_enhancer);

        panelSleepTimer = findViewById(R.id.panel_sleep_timer);
        btnCloseSleepTimer = findViewById(R.id.btn_close_sleep_timer);
        btnBackSleepTimer = findViewById(R.id.btn_back_sleep_timer);

        // Phase 3 Views Binding
        panelOthers = findViewById(R.id.panel_others);
        btnBackOthers = findViewById(R.id.btn_back_others);
        btnCloseOthers = findViewById(R.id.btn_close_others);
        itemOthersProperties = findViewById(R.id.item_others_properties);
        itemOthersFaq = findViewById(R.id.item_others_faq);
        itemOthersCastFaq = findViewById(R.id.item_others_cast_faq);
        itemOthersPlaybackIssue = findViewById(R.id.item_others_playback_issue);

        layoutSpeedMain = findViewById(R.id.layout_speed_main);
        layoutSpeedAdvanced = findViewById(R.id.layout_speed_advanced);
        btnOpenSpeedAdvanced = findViewById(R.id.btn_open_speed_advanced);
        btnBackSpeedAdvanced = findViewById(R.id.btn_back_speed_advanced);
        tvLongPressSpeedTarget = findViewById(R.id.tv_long_press_speed_target);
        switchLongPressVibration = findViewById(R.id.switch_long_press_vibration);

        tvMenuRepeatModeVal = findViewById(R.id.tv_menu_repeat_mode_val);
        btnRepeatOrder = findViewById(R.id.btn_repeat_order);
        btnRepeatOne = findViewById(R.id.btn_repeat_one);
        btnRepeatShuffle = findViewById(R.id.btn_repeat_shuffle);
        btnRepeatAll = findViewById(R.id.btn_repeat_all);
        btnRepeatOnce = findViewById(R.id.btn_repeat_once);

        seekbarMenuBrightness = findViewById(R.id.seekbar_menu_brightness);
        tvMenuBrightnessVal = findViewById(R.id.tv_menu_brightness_val);
        seekbarMenuVolume = findViewById(R.id.seekbar_menu_volume);
        tvMenuVolumeVal = findViewById(R.id.tv_menu_volume_val);

        btnMenuDecoderHw = findViewById(R.id.btn_menu_decoder_hw);
        btnMenuDecoderSw = findViewById(R.id.btn_menu_decoder_sw);
        switchMenuScreenshotToggle = findViewById(R.id.switch_menu_screenshot_toggle);
        ivMenuBookmarkIcon = findViewById(R.id.iv_menu_bookmark_icon);
        ivMenuFavoriteIcon = findViewById(R.id.iv_menu_favorite_icon);
        tvMenuTimerLabel = findViewById(R.id.tv_menu_timer_label);

        btnFilterOriginal = findViewById(R.id.btn_filter_original);
        btnFilterHdr = findViewById(R.id.btn_filter_hdr);
        btnFilterClear = findViewById(R.id.btn_filter_clear);
        btnFilterUltraClear = findViewById(R.id.btn_filter_ultra_clear);
        btnFilterArcticBlue = findViewById(R.id.btn_filter_arctic_blue);
        btnFilterWarmGlow = findViewById(R.id.btn_filter_warm_glow);
        btnFilterCinematic = findViewById(R.id.btn_filter_cinematic);
        switchFilterApplyAll = findViewById(R.id.switch_filter_apply_all);

        tvSleepTimerStatus = findViewById(R.id.tv_sleep_timer_status);
        btnTimerOff = findViewById(R.id.btn_timer_off);
        btnTimer15 = findViewById(R.id.btn_timer_15);
        btnTimer30 = findViewById(R.id.btn_timer_30);
        btnTimer60 = findViewById(R.id.btn_timer_60);
        btnTimer90 = findViewById(R.id.btn_timer_90);
        btnTimerEndOfVideo = findViewById(R.id.btn_timer_end_of_video);

        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {}

        SharedPreferences prefs = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        currentResizeMode = prefs.getInt("aspect_ratio_mode", AspectRatioFrameLayout.RESIZE_MODE_FIT);
        rememberRatio = prefs.getBoolean("remember_ratio", true);
        directRatioSwitch = prefs.getBoolean("direct_ratio_switch", true);
        rememberSpeed = prefs.getBoolean("remember_speed", false);
        longPress2xBoost = prefs.getBoolean("long_press_2x_boost", true);
        longPressSpeedTarget = prefs.getFloat("long_press_speed_target", 2.0f);
        longPressVibration = prefs.getBoolean("long_press_vibration", true);
        filterApplyAll = prefs.getBoolean("filter_apply_all", true);
        if (filterApplyAll) {
            currentVisualFilter = prefs.getString("saved_visual_filter", "ORIGINAL");
        }
        currentRepeatMode = prefs.getInt("repeat_mode", 3);
        boolean showScreenshot = prefs.getBoolean("show_screenshot_btn", true);

        if (rememberSpeed) {
            playbackSpeed = prefs.getFloat("saved_playback_speed", 1.0f);
        }
        if (rememberRatio) {
            currentRatioLabel = prefs.getString("saved_ratio_label", "FIT");
            targetCustomAspect = prefs.getFloat("saved_custom_aspect", 0f);
            if (targetCustomAspect <= 0.05f && currentRatioLabel != null) {
                targetCustomAspect = getAspectFromLabel(currentRatioLabel);
            }
        }
        updateSpeedUI(playbackSpeed);
        updateRatioButtonText();
        updateRatioPresetsUI(currentRatioLabel);
        updateRepeatModeUI();
        applyVisualFilter(currentVisualFilter);

        if (btnQuickScreenshot != null) {
            btnQuickScreenshot.setVisibility(showScreenshot ? View.VISIBLE : View.GONE);
        }
        if (switchMenuScreenshotToggle != null) {
            switchMenuScreenshotToggle.setChecked(showScreenshot);
        }
        if (tvLongPressSpeedTarget != null) {
            tvLongPressSpeedTarget.setText(String.format(java.util.Locale.US, "%.1fX >", longPressSpeedTarget));
        }
        if (switchLongPressVibration != null) {
            switchLongPressVibration.setChecked(longPressVibration);
        }
        if (switchFilterApplyAll != null) {
            switchFilterApplyAll.setChecked(filterApplyAll);
        }

        // Initialize Brightness & Volume Sliders in More Menu
        int brightPercent = (int) (currentBrightness * 100);
        if (seekbarMenuBrightness != null) seekbarMenuBrightness.setProgress(brightPercent);
        if (tvMenuBrightnessVal != null) tvMenuBrightnessVal.setText(String.valueOf(brightPercent));

        if (audioManager != null) {
            int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int volPercent = (int) ((float) curVol / Math.max(1, maxVolume) * 100);
            if (seekbarMenuVolume != null) seekbarMenuVolume.setProgress(volPercent);
            if (tvMenuVolumeVal != null) tvMenuVolumeVal.setText(String.valueOf(volPercent));
        }

        // Restore Favorite Icon state
        String favKey = videoPath != null ? videoPath : videoUriStr;
        if (favKey != null && ivMenuFavoriteIcon != null) {
            boolean isFav = prefs.getBoolean("fav_" + favKey, false);
            ivMenuFavoriteIcon.setColorFilter(isFav ? Color.parseColor("#D71921") : Color.WHITE);
        }

        if (videoTitle != null) {
            videoTitleText.setText(videoTitle);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        initPreviewRetriever();
    }

    // ==========================================
    // YOUTUBE STREAM ENGINE (Native Inside Default Player)
    // ==========================================
    private void startYouTubePlayer() {
        releaseExoPlayer();
        releaseVlcPlayer();
        isVlcActive = false;
        isYouTubeActive = true;

        if (exoPlayerView != null) exoPlayerView.setVisibility(View.GONE);
        if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(View.GONE);
        if (youtubeStreamView != null) youtubeStreamView.setVisibility(View.VISIBLE);

        currentDecoder = "YouTube Engine";
        btnDecoderMode.setText("YT");
        videoSubtitleCodec.setText(currentDecoder + " • DIRECT STREAM");

        String rawUrl = videoUriStr != null ? videoUriStr : videoPath;
        String videoId = YouTubeStreamResolver.extractVideoId(rawUrl);
        if (videoId == null || videoId.isEmpty()) {
            videoId = "dQw4w9WgXcQ";
        }

        if (isYouTubeLoaded && youtubeStreamView != null) {
            youtubeStreamView.evaluateJavascript("if (player && player.loadVideoById) { player.loadVideoById('" + videoId + "'); }", null);
            scheduleHideControls();
            return;
        }

        if (youtubeStreamView != null) {
            WebSettings ws = youtubeStreamView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setUseWideViewPort(true);
            ws.setLoadWithOverviewMode(true);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setCacheMode(WebSettings.LOAD_DEFAULT);
            youtubeStreamView.setBackgroundColor(0xFF000000);
            youtubeStreamView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            youtubeStreamView.setWebChromeClient(new WebChromeClient());
            youtubeStreamView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    isYouTubeLoaded = true;
                    scheduleHideControls();
                }
            });

            youtubeStreamView.addJavascriptInterface(new Object() {
                @android.webkit.JavascriptInterface
                public void onTimeUpdate(float current, float duration) {
                    runOnUiThread(() -> {
                        youtubeCurrentPositionMs = (long) (current * 1000f);
                        youtubeDurationMs = (long) (duration * 1000f);
                        totalDurationMs = youtubeDurationMs;
                        if (!isTrackingTouch) {
                            updateTimeDisplay(youtubeCurrentPositionMs);
                            if (youtubeDurationMs > 0) {
                                videoSeekBar.setMax((int) youtubeDurationMs);
                                videoSeekBar.setProgress((int) youtubeCurrentPositionMs);
                            }
                        }
                    });
                }

                @android.webkit.JavascriptInterface
                public void onVideoTitle(String title) {
                    runOnUiThread(() -> {
                        if (title != null && !title.isEmpty() && !title.equals("undefined")) {
                            videoTitle = title;
                            if (videoTitleText != null) videoTitleText.setText(title);
                            if (playlistTitles != null && playlistTitles.size() > playlistIndex) {
                                playlistTitles.set(playlistIndex, title);
                            }
                            updateQueueUI();
                        }
                    });
                }

                @android.webkit.JavascriptInterface
                public void onPlaylistLoaded(String jsonIds, int activeIdx) {
                    runOnUiThread(() -> {
                        try {
                            JSONArray arr = new JSONArray(jsonIds);
                            if (arr.length() > 0) {
                                boolean countChanged = (playlistPaths == null || playlistPaths.size() != arr.length());
                                if (countChanged) {
                                    playlistPaths = new ArrayList<>();
                                    playlistUris = new ArrayList<>();
                                    playlistTitles = new ArrayList<>();
                                    List<String> vids = new ArrayList<>();
                                    for (int i = 0; i < arr.length(); i++) {
                                        String vid = arr.getString(i);
                                        vids.add(vid);
                                        String fullUrl = "https://www.youtube.com/watch?v=" + vid;
                                        playlistPaths.add(fullUrl);
                                        playlistUris.add(fullUrl);
                                        playlistTitles.add("Track #" + (i + 1) + " • " + vid);
                                    }
                                    fetchYouTubePlaylistTitles(vids);
                                }
                                playlistIndex = Math.max(0, Math.min(arr.length() - 1, activeIdx));
                                updateQueueUI();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                @android.webkit.JavascriptInterface
                public void onPlaylistIndexChange(int activeIdx) {
                    runOnUiThread(() -> {
                        if (activeIdx >= 0 && playlistPaths != null && activeIdx < playlistPaths.size() && playlistIndex != activeIdx) {
                            playlistIndex = activeIdx;
                            if (playlistTitles.size() > playlistIndex && videoTitleText != null) {
                                videoTitle = playlistTitles.get(playlistIndex);
                                videoTitleText.setText(videoTitle);
                            }
                            updateQueueUI();
                        }
                    });
                }

                @android.webkit.JavascriptInterface
                public void onStateChange(int state) {
                    runOnUiThread(() -> {
                        // state: 1 = PLAYING, 2 = PAUSED, 0 = ENDED, 3 = BUFFERING
                        if (state == 1) {
                            hideTransitionOverlay();
                            isYouTubePlaying = true;
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                            applyAspectRatio();
                        } else if (state == 2) {
                            isYouTubePlaying = false;
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        } else if (state == 0) {
                            isYouTubePlaying = false;
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                            handleVideoCompletion();
                        }
                    });
                }
            }, "AndroidYT");

            String ytListParam = (youtubePlaylistId != null && !youtubePlaylistId.isEmpty()) ?
                    ", 'listType': 'playlist', 'list': '" + youtubePlaylistId + "'" : "";

            String html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                    "<style>" +
                    "* { margin:0; padding:0; box-sizing:border-box; background:#000000; overflow:hidden; user-select:none; -webkit-user-select:none; }" +
                    "html, body { width:100%; height:100%; background:#000000; overflow:hidden; }" +
                    "#player { width:100% !important; height:100% !important; pointer-events:none !important; border:none !important; }" +
                    "iframe { width:100% !important; height:100% !important; pointer-events:none !important; border:none !important; }" +
                    "</style></head><body>" +
                    "<div id='player'></div>" +
                    "<script src='https://www.youtube.com/iframe_api'></script>" +
                    "<script>" +
                    "var player;" +
                    "var lastTitle = '';" +
                    "var lastIdx = -1;" +
                    "function onYouTubeIframeAPIReady() {" +
                    "  player = new YT.Player('player', {" +
                    "    videoId: '" + videoId + "'," +
                    "    playerVars: { 'autoplay': 1, 'controls': 0, 'playsinline': 1, 'rel': 0, 'modestbranding': 1, 'fs': 0, 'iv_load_policy': 3, 'disablekb': 1, 'showinfo': 0, 'showsearch': 0, 'enablejsapi': 1, 'origin': 'https://www.youtube-nocookie.com'" + ytListParam + " }," +
                    "    events: {" +
                    "      'onReady': onPlayerReady," +
                    "      'onStateChange': onPlayerStateChange" +
                    "    }" +
                    "  });" +
                    "}" +
                    "function syncPlaylist() {" +
                    "  try {" +
                    "    if (player && player.getPlaylist) {" +
                    "      var pl = player.getPlaylist();" +
                    "      if (pl && pl.length) {" +
                    "        var idx = (player.getPlaylistIndex && player.getPlaylistIndex() >= 0) ? player.getPlaylistIndex() : 0;" +
                    "        AndroidYT.onPlaylistLoaded(JSON.stringify(pl), idx);" +
                    "      }" +
                    "    }" +
                    "  } catch(e) {}" +
                    "}" +
                    "function onPlayerReady(event) {" +
                    "  event.target.playVideo();" +
                    "  syncPlaylist();" +
                    "  setInterval(function() {" +
                    "    if (player && player.getCurrentTime) {" +
                    "      AndroidYT.onTimeUpdate(player.getCurrentTime(), player.getDuration());" +
                    "      if (player.getVideoData) {" +
                    "        var vd = player.getVideoData();" +
                    "        if (vd && vd.title && vd.title !== lastTitle) {" +
                    "          lastTitle = vd.title;" +
                    "          AndroidYT.onVideoTitle(lastTitle);" +
                    "        }" +
                    "      }" +
                    "      if (player.getPlaylistIndex) {" +
                    "        var idx = player.getPlaylistIndex();" +
                    "        if (idx !== undefined && idx >= 0 && idx !== lastIdx) {" +
                    "          lastIdx = idx;" +
                    "          AndroidYT.onPlaylistIndexChange(idx);" +
                    "        }" +
                    "      }" +
                    "    }" +
                    "  }, 400);" +
                    "}" +
                    "function onPlayerStateChange(event) {" +
                    "  AndroidYT.onStateChange(event.data);" +
                    "  syncPlaylist();" +
                    "}" +
                    "</script></body></html>";

            youtubeStreamView.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null);
        }
        scheduleHideControls();
    }

    private void fetchYouTubePlaylistTitles(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) return;
        for (int i = 0; i < videoIds.size(); i++) {
            final int index = i;
            final String vid = videoIds.get(i);
            titleExecutor.execute(() -> {
                try {
                    String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" + vid + "&format=json";
                    java.net.URL url = new java.net.URL(oembedUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    if (conn.getResponseCode() == 200) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        org.json.JSONObject obj = new org.json.JSONObject(sb.toString());
                        String realTitle = obj.optString("title", "");
                        if (!realTitle.isEmpty()) {
                            runOnUiThread(() -> {
                                if (playlistTitles != null && playlistTitles.size() > index) {
                                    playlistTitles.set(index, realTitle);
                                    if (playlistIndex == index && videoTitleText != null) {
                                        videoTitle = realTitle;
                                        videoTitleText.setText(realTitle);
                                    }
                                    if (queueAdapter != null) {
                                        queueAdapter.updateTitle(index, realTitle);
                                    }
                                }
                            });
                        }
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            });
        }
    }

    private void fetchSingleYouTubeTitle(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return;
        String videoId = YouTubeStreamResolver.extractVideoId(rawUrl);
        if (videoId == null || videoId.isEmpty()) return;
        titleExecutor.execute(() -> {
            try {
                String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" + videoId + "&format=json";
                java.net.URL url = new java.net.URL(oembedUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    org.json.JSONObject obj = new org.json.JSONObject(sb.toString());
                    String realTitle = obj.optString("title", "");
                    if (!realTitle.isEmpty()) {
                        runOnUiThread(() -> {
                            videoTitle = realTitle;
                            if (videoTitleText != null) {
                                videoTitleText.setText(realTitle);
                            }
                            if (playlistTitles != null && !playlistTitles.isEmpty()) {
                                playlistTitles.set(playlistIndex, realTitle);
                            }
                            if (queueAdapter != null) {
                                queueAdapter.updateTitle(playlistIndex, realTitle);
                            }
                        });
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        });
    }

    private void showTransitionOverlay() {
        if (videoTransitionOverlay == null) return;
        videoTransitionOverlay.animate().cancel();
        videoTransitionOverlay.setAlpha(1.0f);
        videoTransitionOverlay.setVisibility(View.VISIBLE);

        if (isYouTubeStream()) {
            String rawUrl = videoUriStr != null ? videoUriStr : videoPath;
            String vid = YouTubeStreamResolver.extractVideoId(rawUrl);
            if (vid != null && !vid.isEmpty()) {
                Glide.with(this)
                    .load("https://img.youtube.com/vi/" + vid + "/mqdefault.jpg")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(videoTransitionOverlay);
            }
        } else {
            Uri uri = resolveMediaUri();
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(videoTransitionOverlay);
            }
        }
    }

    private void hideTransitionOverlay() {
        if (videoTransitionOverlay != null && videoTransitionOverlay.getVisibility() == View.VISIBLE) {
            videoTransitionOverlay.animate()
                .alpha(0f)
                .setDuration(220)
                .withEndAction(() -> videoTransitionOverlay.setVisibility(View.GONE))
                .start();
        }
    }

    // ==========================================
    // EXOPLAYER ENGINE (Hardware Accelerated)
    // ==========================================
    private void startExoPlayer(long resumePos) {
        showTransitionOverlay();
        releaseVlcPlayer();
        isVlcActive = false;
        isYouTubeActive = false;
        if (youtubeStreamView != null) youtubeStreamView.setVisibility(View.GONE);
        if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(View.GONE);
        if (exoPlayerView != null) exoPlayerView.setVisibility(View.VISIBLE);

        if (exoPlayer != null) {
            Uri targetUri = resolveMediaUri();
            if (targetUri != null) {
                MediaItem mediaItem = MediaItem.fromUri(targetUri);
                exoPlayer.setMediaItem(mediaItem, resumePos > 0 ? resumePos : 0);
                exoPlayer.prepare();
                exoPlayer.play();
                exoPlayer.setPlaybackSpeed(playbackSpeed);
                currentDecoder = "HW+ (Exo)";
                btnDecoderMode.setText("HW+");
                updateCodecInfo();
                scheduleHideControls();
            }
            return;
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

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                20000,
                60000,
                1000,
                2000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build();

        exoPlayer = new ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build();

        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build();
        exoPlayer.setAudioAttributes(audioAttributes, true);

        exoPlayerView.setPlayer(exoPlayer);
        applyAspectRatio();

        Uri targetUri = resolveMediaUri();

        if (targetUri != null) {
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);
            
            MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(targetUri);
            String urlLower = targetUri.toString().toLowerCase();
            if (urlLower.contains(".m3u8") || urlLower.contains("m3u8")) {
                mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);
            } else if (urlLower.contains(".mpd") || urlLower.contains("dash")) {
                mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD);
            }
            MediaItem mediaItem = mediaItemBuilder.build();
            
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
            public void onRenderedFirstFrame() {
                hideTransitionOverlay();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isVlcActive) {
                    btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    if (isPlaying) {
                        startProgressTracker();
                        scheduleHideControls();
                        hideTransitionOverlay();
                    } else {
                        stopProgressTracker();
                    }
                }
            }

            @Override
            public void onVideoSizeChanged(androidx.media3.common.VideoSize videoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width;
                    videoHeight = videoSize.height;
                    applyAspectRatio();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (!isVlcActive) {
                    if (playbackState == Player.STATE_READY) {
                        hideTransitionOverlay();
                        totalDurationMs = exoPlayer.getDuration();
                        videoSeekBar.setMax((int) totalDurationMs);
                        updateTimeDisplay(exoPlayer.getCurrentPosition());
                        AudioEffectManager.getInstance().attachAudioSession(exoPlayer.getAudioSessionId(), ExoVideoPlayerActivity.this);
                        updateCodecInfo();
                        applyAspectRatio();
                    } else if (playbackState == Player.STATE_ENDED) {
                        handleVideoCompletion();
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
        showTransitionOverlay();
        releaseExoPlayer();
        isVlcActive = true;
        isYouTubeActive = false;
        if (youtubeStreamView != null) youtubeStreamView.setVisibility(View.GONE);
        exoPlayerView.setVisibility(View.GONE);
        vlcVideoLayout.setVisibility(View.VISIBLE);

        if (vlcPlayer != null) {
            Media media = createVlcMedia();
            if (media != null) {
                media.setHWDecoderEnabled(true, true);
                media.addOption(":file-caching=2000");
                vlcPlayer.setMedia(media);
                media.release();
                vlcPlayer.play();
                if (resumePos > 0) {
                    vlcPlayer.setTime(resumePos);
                }
                vlcPlayer.setRate(playbackSpeed);
                currentDecoder = "Universal VLC";
                btnDecoderMode.setText("VLC");
                videoSubtitleCodec.setText(currentDecoder + " • DOLBY 5.1 / EAC3");
                scheduleHideControls();
            }
            return;
        }

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
                        hideTransitionOverlay();
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                        startProgressTracker();
                        scheduleHideControls();
                        applyAspectRatio();
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
                        if (Math.abs(pos - lastSavedProgressMs) >= 2000) {
                            lastSavedProgressMs = pos;
                            saveCurrentPlaybackProgress(pos, totalDurationMs);
                        }
                    } else if (event.type == MediaPlayer.Event.EndReached) {
                        handleVideoCompletion();
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
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        if (btnPrev != null) btnPrev.setOnClickListener(v -> playPreviousVideo());
        if (btnNext != null) btnNext.setOnClickListener(v -> playNextVideo());
        if (btnPlaylistQueue != null) btnPlaylistQueue.setOnClickListener(v -> togglePlaylistQueuePanel());
        if (btnCloseQueue != null) btnCloseQueue.setOnClickListener(v -> {
            if (playlistQueuePanel != null) playlistQueuePanel.setVisibility(View.GONE);
        });

        findViewById(R.id.btn_rewind).setOnClickListener(v -> {
            if (isYouTubeActive) {
                long target = Math.max(0, youtubeCurrentPositionMs - 10000);
                if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                showGestureHud("● 10s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-10s", (int) (target * 100 / Math.max(1, youtubeDurationMs)));
                return;
            }
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            long target = Math.max(0, cur - 10000);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
            else if (exoPlayer != null) exoPlayer.seekTo(target);
            showGestureHud("● 10s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-10s", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });

        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            if (isYouTubeActive) {
                long target = Math.min(youtubeDurationMs, youtubeCurrentPositionMs + 10000);
                if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                showGestureHud("● 10s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+10s", (int) (target * 100 / Math.max(1, youtubeDurationMs)));
                return;
            }
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            long target = Math.min(totalDurationMs, cur + 10000);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
            else if (exoPlayer != null) exoPlayer.seekTo(target);
            showGestureHud("● 10s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+10s", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });

        if (btnCast != null) {
            btnCast.setOnClickListener(v -> {
                try {
                    Intent castIntent = new Intent("android.settings.CAST_SETTINGS");
                    startActivity(castIntent);
                } catch (Exception e) {
                    showGestureHud("● CAST SCREEN", DotMatrixIconView.TYPE_SEEK_FORWARD, "CAST READY", 100);
                    Toast.makeText(this, "Cast Screen: Connect to wireless display in settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnDecoderMode != null) {
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
        }

        if (btnSubtitleTrack != null) {
            btnSubtitleTrack.setOnClickListener(v -> {
                if (isVlcActive) showVlcSubtitleTrackDialog();
                else showExoSubtitleTrackDialog();
            });
        }

        if (btnMoreMenu != null) {
            btnMoreMenu.setOnClickListener(v -> togglePanel(panelMoreMenu));
        }

        // Quick Actions Row
        if (btnQuickOrientation != null) {
            btnQuickOrientation.setOnClickListener(v -> cycleOrientation());
        }

        if (btnQuickMute != null) {
            btnQuickMute.setOnClickListener(v -> toggleMute());
        }

        if (btnQuickBackground != null) {
            btnQuickBackground.setOnClickListener(v -> toggleBackgroundPlay());
        }

        if (btnQuickSpeed != null) {
            btnQuickSpeed.setOnClickListener(v -> togglePanel(panelSpeed));
        }

        if (btnQuickExpand != null) {
            btnQuickExpand.setOnClickListener(v -> toggleQuickExpand());
        }

        if (btnQuickNightMode != null) {
            btnQuickNightMode.setOnClickListener(v -> toggleNightMode());
        }

        if (btnQuickAbRepeat != null) {
            btnQuickAbRepeat.setOnClickListener(v -> toggleAbRepeat());
        }

        if (btnQuickPip != null) {
            btnQuickPip.setOnClickListener(v -> enterPipMode());
        }

        if (btnQuickEqualizer != null) {
            btnQuickEqualizer.setOnClickListener(v -> {
                Intent eqIntent = new Intent(this, EqualizerActivity.class);
                if (exoPlayer != null) {
                    eqIntent.putExtra("audio_session_id", exoPlayer.getAudioSessionId());
                }
                startActivity(eqIntent);
            });
        }

        if (btnQuickMirror != null) {
            btnQuickMirror.setOnClickListener(v -> toggleMirror());
        }

        if (btnQuickScreenshot != null) {
            btnQuickScreenshot.setOnClickListener(v -> takeQuickScreenshot());
        }

        // Bottom Transport
        if (btnScreenLock != null) {
            btnScreenLock.setOnClickListener(v -> toggleScreenLock());
        }
        if (btnFloatingLock != null) {
            btnFloatingLock.setOnClickListener(v -> toggleScreenLock());
        }

        if (btnBottomSpeed != null) {
            btnBottomSpeed.setOnClickListener(v -> togglePanel(panelSpeed));
        }

        if (btnAspectRatio != null) {
            btnAspectRatio.setOnClickListener(v -> {
                if (directRatioSwitch) {
                    cycleAspectRatioDirect();
                } else {
                    togglePanel(panelRatio);
                }
            });
            btnAspectRatio.setOnLongClickListener(v -> {
                togglePanel(panelRatio);
                return true;
            });
        }

        setupRatioPanelListeners();
        setupSpeedPanelListeners();
        setupMoreMenuPanelListeners();
        setupVisualEnhancerListeners();
        setupSleepTimerListeners();
        setupOthersPanelListeners();

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
                isTrackingTouch = true;
                stopProgressTracker();
                if (seekbarPreviewCard != null) {
                    seekbarPreviewCard.setVisibility(View.VISIBLE);
                    updatePreviewPosition(seekBar, seekBar.getProgress());
                    extractAndShowPreviewFrame(seekBar.getProgress());
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTrackingTouch = false;
                if (seekbarPreviewCard != null) {
                    seekbarPreviewCard.setVisibility(View.GONE);
                }
                int target = seekBar.getProgress();
                if (isYouTubeActive) {
                    if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                } else if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setTime(target);
                } else if (exoPlayer != null) {
                    exoPlayer.seekTo(target);
                }
                startProgressTracker();
                scheduleHideControls();
            }
        });
    }

    // ==========================================
    // PHASE 1 & 2 QUICK ACTION & PANEL HELPERS
    // ==========================================
    private void toggleScreenLock() {
        isLocked = !isLocked;
        if (btnScreenLock != null) {
            btnScreenLock.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
        }
        if (isLocked) {
            topControlsBar.setVisibility(View.GONE);
            if (layoutQuickActionsExpanded != null) layoutQuickActionsExpanded.setVisibility(View.GONE);
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.GONE);
            closeAllPanels();
            bottomControlsBar.setVisibility(View.GONE);
            if (btnFloatingLock != null) {
                btnFloatingLock.setVisibility(View.VISIBLE);
                btnFloatingLock.setImageResource(R.drawable.ic_lock_closed);
                btnFloatingLock.setColorFilter(Color.parseColor("#D71921"));
                hideHandler.removeCallbacksAndMessages(null);
                hideHandler.postDelayed(() -> {
                    if (isLocked && btnFloatingLock != null) {
                        btnFloatingLock.setVisibility(View.GONE);
                    }
                }, 3000);
            }
            showGestureHud("● SCREEN LOCK", DotMatrixIconView.TYPE_LOCK, "LOCKED", 100);
        } else {
            if (btnFloatingLock != null) btnFloatingLock.setVisibility(View.GONE);
            topControlsBar.setVisibility(View.VISIBLE);
            bottomControlsBar.setVisibility(View.VISIBLE);
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.VISIBLE);
            showGestureHud("● SCREEN LOCK", DotMatrixIconView.TYPE_LOCK, "UNLOCKED", 100);
            scheduleHideControls();
        }
    }

    private void cycleOrientation() {
        currentOrientationMode = (currentOrientationMode + 1) % 3;
        if (currentOrientationMode == 0) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            showGestureHud("● ORIENTATION", DotMatrixIconView.TYPE_SEEK_FORWARD, "LANDSCAPE", 100);
        } else if (currentOrientationMode == 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            showGestureHud("● ORIENTATION", DotMatrixIconView.TYPE_SEEK_FORWARD, "PORTRAIT", 100);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            showGestureHud("● ORIENTATION", DotMatrixIconView.TYPE_SEEK_FORWARD, "AUTO ROTATE", 100);
        }
        scheduleHideControls();
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (isVlcActive && vlcPlayer != null) {
            vlcPlayer.setVolume(isMuted ? 0 : (isAudioBoosted ? 200 : 100));
        } else if (exoPlayer != null) {
            exoPlayer.setVolume(isMuted ? 0.0f : (isAudioBoosted ? 2.0f : 1.0f));
        }
        if (btnQuickMute != null) {
            btnQuickMute.setImageResource(isMuted ? R.drawable.ic_volume_mute : R.drawable.ic_volume_high);
        }
        int dotType = isMuted ? DotMatrixIconView.TYPE_VOLUME_MUTE : DotMatrixIconView.TYPE_VOLUME;
        showGestureHud("● AUDIO", dotType, isMuted ? "MUTED" : "UNMUTED", isMuted ? 0 : 100);
        scheduleHideControls();
    }

    private void toggleBackgroundPlay() {
        Toast.makeText(this, "Background Audio Playback Enabled", Toast.LENGTH_SHORT).show();
        showGestureHud("● BACKGROUND", DotMatrixIconView.TYPE_SEEK_FORWARD, "AUDIO PLAY ON", 100);
        if (videoPath != null || videoUriStr != null) {
            Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
            serviceIntent.putExtra("title", videoTitle != null ? videoTitle : "Video Audio");
            serviceIntent.putExtra("path", videoPath != null ? videoPath : videoUriStr);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } catch (Exception ignored) {}
        }
        scheduleHideControls();
    }

    private void toggleQuickExpand() {
        if (layoutQuickActionsExpanded != null) {
            boolean isExpanded = layoutQuickActionsExpanded.getVisibility() == View.VISIBLE;
            layoutQuickActionsExpanded.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            if (btnQuickExpand != null) {
                btnQuickExpand.setImageResource(isExpanded ? R.drawable.ic_arrow_expand : R.drawable.ic_arrow_collapse);
            }
        }
        scheduleHideControls();
    }

    private void toggleNightMode() {
        isNightModeActive = !isNightModeActive;
        if (nightModeOverlay != null) {
            nightModeOverlay.setVisibility(isNightModeActive ? View.VISIBLE : View.GONE);
        }
        if (btnQuickNightMode != null) {
            btnQuickNightMode.setColorFilter(isNightModeActive ? Color.parseColor("#D71921") : Color.WHITE);
        }
        showGestureHud("● NIGHT MODE", DotMatrixIconView.TYPE_SEEK_FORWARD, isNightModeActive ? "ACTIVE" : "OFF", isNightModeActive ? 100 : 0);
        scheduleHideControls();
    }

    private void toggleAbRepeat() {
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        TextView tvAb = findViewById(R.id.tv_menu_ab_repeat_status);
        if (repeatPointA < 0) {
            repeatPointA = cur;
            showGestureHud("● A-B REPEAT", DotMatrixIconView.TYPE_SEEK_FORWARD, "POINT A (" + formatTime(repeatPointA) + ")", 50);
            Toast.makeText(this, "A-B Repeat: Point A set to " + formatTime(repeatPointA), Toast.LENGTH_SHORT).show();
            if (tvAb != null) tvAb.setText("A: " + formatTime(repeatPointA));
        } else if (repeatPointB < 0) {
            if (cur > repeatPointA) {
                repeatPointB = cur;
                showGestureHud("● A-B REPEAT", DotMatrixIconView.TYPE_SEEK_FORWARD, "LOOPING A-B", 100);
                Toast.makeText(this, "A-B Repeat: Point B set (" + formatTime(repeatPointA) + " - " + formatTime(repeatPointB) + ")", Toast.LENGTH_SHORT).show();
                if (tvAb != null) tvAb.setText(formatTime(repeatPointA) + "-" + formatTime(repeatPointB));
            } else {
                repeatPointA = cur;
                Toast.makeText(this, "Point B must be after Point A. Point A reset.", Toast.LENGTH_SHORT).show();
                if (tvAb != null) tvAb.setText("A: " + formatTime(repeatPointA));
            }
        } else {
            repeatPointA = -1;
            repeatPointB = -1;
            showGestureHud("● A-B REPEAT", DotMatrixIconView.TYPE_SEEK_FORWARD, "CLEARED / OFF", 0);
            Toast.makeText(this, "A-B Repeat Cleared", Toast.LENGTH_SHORT).show();
            if (tvAb != null) tvAb.setText("OFF");
        }
        scheduleHideControls();
    }

    private void toggleMirror() {
        isMirrored = !isMirrored;
        float scaleX = isMirrored ? -1.0f : 1.0f;
        if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() != null) {
            exoPlayerView.getVideoSurfaceView().setScaleX(scaleX);
        }
        if (vlcVideoLayout != null) {
            vlcVideoLayout.setScaleX(scaleX);
        }
        if (youtubeStreamView != null) {
            youtubeStreamView.setScaleX(scaleX);
        }
        showGestureHud("● SCREEN", DotMatrixIconView.TYPE_SEEK_FORWARD, isMirrored ? "MIRRORED" : "NORMAL", 100);
        scheduleHideControls();
    }

    private void takeQuickScreenshot() {
        try {
            Bitmap bitmap = null;
            if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() instanceof android.view.TextureView) {
                bitmap = ((android.view.TextureView) exoPlayerView.getVideoSurfaceView()).getBitmap();
            }
            if (bitmap == null && previewRetriever != null) {
                long pos = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
                bitmap = previewRetriever.getFrameAtTime(pos * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
            }
            if (bitmap != null) {
                File dir = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "Screenshots");
                if (!dir.exists()) dir.mkdirs();
                String fileName = "NothingPlayer_" + System.currentTimeMillis() + ".jpg";
                File file = new File(dir, fileName);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.flush();
                fos.close();
                android.media.MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
                showGestureHud("● SCREENSHOT", DotMatrixIconView.TYPE_SEEK_FORWARD, "SAVED TO GALLERY", 100);
                Toast.makeText(this, "Screenshot saved to Pictures/Screenshots", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Screenshot captured", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Screenshot error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        scheduleHideControls();
    }

    private void togglePanel(View targetPanel) {
        if (targetPanel == null) return;
        boolean isVisible = targetPanel.getVisibility() == View.VISIBLE;
        closeAllPanels();
        if (!isVisible) {
            targetPanel.setVisibility(View.VISIBLE);
            if (targetPanel == panelRatio) {
                updateRatioPresetsUI(currentRatioLabel);
            }
            hideHandler.removeCallbacksAndMessages(null);
            targetPanel.post(() -> targetPanel.scrollTo(0, 0));
        } else {
            targetPanel.setVisibility(View.GONE);
            scheduleHideControls();
        }
    }

    private boolean isAnyPanelOpen() {
        return (playlistQueuePanel != null && playlistQueuePanel.getVisibility() == View.VISIBLE)
            || (panelRatio != null && panelRatio.getVisibility() == View.VISIBLE)
            || (panelSpeed != null && panelSpeed.getVisibility() == View.VISIBLE)
            || (panelMoreMenu != null && panelMoreMenu.getVisibility() == View.VISIBLE)
            || (panelVisualEnhancer != null && panelVisualEnhancer.getVisibility() == View.VISIBLE)
            || (panelSleepTimer != null && panelSleepTimer.getVisibility() == View.VISIBLE)
            || (panelOthers != null && panelOthers.getVisibility() == View.VISIBLE);
    }

    private void closeAllPanels() {
        if (playlistQueuePanel != null) playlistQueuePanel.setVisibility(View.GONE);
        if (panelRatio != null) panelRatio.setVisibility(View.GONE);
        if (panelSpeed != null) panelSpeed.setVisibility(View.GONE);
        if (panelMoreMenu != null) panelMoreMenu.setVisibility(View.GONE);
        if (panelVisualEnhancer != null) panelVisualEnhancer.setVisibility(View.GONE);
        if (panelSleepTimer != null) panelSleepTimer.setVisibility(View.GONE);
        if (panelOthers != null) panelOthers.setVisibility(View.GONE);
    }

    private void cycleAspectRatioDirect() {
        currentVideoScale = 1.0f;
        if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() != null) {
            exoPlayerView.getVideoSurfaceView().setScaleX(1.0f);
            exoPlayerView.getVideoSurfaceView().setScaleY(1.0f);
        }

        if (targetCustomAspect > 0.05f || currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            applyScreenRatio(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "FILL");
        } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            applyScreenRatio(AspectRatioFrameLayout.RESIZE_MODE_FILL, "STRETCH");
        } else {
            applyScreenRatio(AspectRatioFrameLayout.RESIZE_MODE_FIT, "FIT");
        }
    }

    private void updateRatioButtonText() {
        if (btnAspectRatio == null) return;
        if (currentRatioLabel != null && !currentRatioLabel.isEmpty()) {
            btnAspectRatio.setText(currentRatioLabel.toUpperCase());
        } else if (targetCustomAspect > 0.05f) {
            btnAspectRatio.setText("CUSTOM");
        } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            btnAspectRatio.setText("FILL");
        } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
            btnAspectRatio.setText("STRETCH");
        } else {
            btnAspectRatio.setText("FIT");
        }
    }

    private void updateRatioPresetsUI(String activeLabel) {
        if (activeLabel == null) return;
        currentRatioLabel = activeLabel;
        int active = R.drawable.bg_chip_red_active;
        int inactive = R.drawable.bg_chip_dark_inactive;

        int[] btnIds = {
            R.id.btn_ratio_fit, R.id.btn_ratio_fill, R.id.btn_ratio_original, R.id.btn_ratio_stretch,
            R.id.btn_ratio_16_9, R.id.btn_ratio_4_3, R.id.btn_ratio_18_9, R.id.btn_ratio_195_9, R.id.btn_ratio_20_9, R.id.btn_ratio_21_9,
            R.id.btn_ratio_185_1, R.id.btn_ratio_221_1, R.id.btn_ratio_235_1, R.id.btn_ratio_239_1
        };

        String[] labels = {
            "FIT", "FILL", "ORIGINAL", "STRETCH",
            "16:9", "4:3", "18:9", "19.5:9", "20:9", "21:9",
            "1.85:1", "2.21:1", "2.35:1", "2.39:1"
        };

        for (int i = 0; i < btnIds.length; i++) {
            Button btn = findViewById(btnIds[i]);
            if (btn != null) {
                boolean isMatch = labels[i].equalsIgnoreCase(activeLabel)
                        || ("ZOOM".equalsIgnoreCase(activeLabel) && "FILL".equalsIgnoreCase(labels[i]));
                btn.setBackgroundResource(isMatch ? active : inactive);
            }
        }
    }

    private void applyScreenRatio(int resizeMode, String modeName) {
        targetCustomAspect = 0f;
        currentResizeMode = resizeMode;
        currentRatioLabel = modeName;
        currentVideoScale = 1.0f;
        if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() != null) {
            exoPlayerView.getVideoSurfaceView().setScaleX(1.0f);
            exoPlayerView.getVideoSurfaceView().setScaleY(1.0f);
        }
        updateRatioButtonText();
        updateRatioPresetsUI(modeName);
        if (rememberRatio) {
            SharedPreferences p = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
            p.edit().putInt("aspect_ratio_mode", currentResizeMode)
                    .putFloat("saved_custom_aspect", 0f)
                    .putString("saved_ratio_label", modeName)
                    .apply();
        }
        applyAspectRatio();
        showGestureHud("● RATIO: " + modeName, DotMatrixIconView.TYPE_SEEK_FORWARD, modeName, 100);
        if (panelRatio != null) panelRatio.setVisibility(View.GONE);
        scheduleHideControls();
    }

    private void applyCustomAspectRatio(float targetAspect, String label) {
        targetCustomAspect = targetAspect;
        currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
        currentRatioLabel = label;
        currentVideoScale = 1.0f;
        if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() != null) {
            exoPlayerView.getVideoSurfaceView().setScaleX(1.0f);
            exoPlayerView.getVideoSurfaceView().setScaleY(1.0f);
        }
        updateRatioButtonText();
        updateRatioPresetsUI(label);
        if (rememberRatio) {
            SharedPreferences p = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
            p.edit().putInt("aspect_ratio_mode", currentResizeMode)
                    .putFloat("saved_custom_aspect", targetAspect)
                    .putString("saved_ratio_label", label)
                    .apply();
        }
        applyAspectRatio();
        showGestureHud("● RATIO: " + label, DotMatrixIconView.TYPE_SEEK_FORWARD, label, 100);
        if (panelRatio != null) panelRatio.setVisibility(View.GONE);
        scheduleHideControls();
    }

    private void setupRatioPanelListeners() {
        if (panelRatio == null) return;

        // Screen Buttons
        findViewById(R.id.btn_ratio_fit).setOnClickListener(v -> applyScreenRatio(AspectRatioFrameLayout.RESIZE_MODE_FIT, "FIT"));
        findViewById(R.id.btn_ratio_fill).setOnClickListener(v -> applyScreenRatio(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "FILL"));
        findViewById(R.id.btn_ratio_original).setOnClickListener(v -> applyScreenRatio(AspectRatioFrameLayout.RESIZE_MODE_FIT, "ORIGINAL"));
        findViewById(R.id.btn_ratio_stretch).setOnClickListener(v -> applyScreenRatio(AspectRatioFrameLayout.RESIZE_MODE_FILL, "STRETCH"));

        // Standard Buttons
        findViewById(R.id.btn_ratio_16_9).setOnClickListener(v -> applyCustomAspectRatio(16f / 9f, "16:9"));
        findViewById(R.id.btn_ratio_4_3).setOnClickListener(v -> applyCustomAspectRatio(4f / 3f, "4:3"));
        findViewById(R.id.btn_ratio_18_9).setOnClickListener(v -> applyCustomAspectRatio(18f / 9f, "18:9"));
        findViewById(R.id.btn_ratio_195_9).setOnClickListener(v -> applyCustomAspectRatio(19.5f / 9f, "19.5:9"));
        findViewById(R.id.btn_ratio_20_9).setOnClickListener(v -> applyCustomAspectRatio(20f / 9f, "20:9"));
        findViewById(R.id.btn_ratio_21_9).setOnClickListener(v -> applyCustomAspectRatio(21f / 9f, "21:9"));

        // Cinema Buttons
        findViewById(R.id.btn_ratio_185_1).setOnClickListener(v -> applyCustomAspectRatio(1.85f, "1.85:1"));
        findViewById(R.id.btn_ratio_221_1).setOnClickListener(v -> applyCustomAspectRatio(2.21f, "2.21:1"));
        findViewById(R.id.btn_ratio_235_1).setOnClickListener(v -> applyCustomAspectRatio(2.35f, "2.35:1"));
        findViewById(R.id.btn_ratio_239_1).setOnClickListener(v -> applyCustomAspectRatio(2.39f, "2.39:1"));

        // Switches
        SwitchCompat switchRemember = findViewById(R.id.switch_remember_ratio);
        if (switchRemember != null) {
            switchRemember.setChecked(rememberRatio);
            switchRemember.setOnCheckedChangeListener((btn, isChecked) -> {
                rememberRatio = isChecked;
                getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putBoolean("remember_ratio", isChecked).apply();
            });
        }

        SwitchCompat switchDirect = findViewById(R.id.switch_direct_ratio_switch);
        if (switchDirect != null) {
            switchDirect.setChecked(directRatioSwitch);
            switchDirect.setOnCheckedChangeListener((btn, isChecked) -> {
                directRatioSwitch = isChecked;
                getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putBoolean("direct_ratio_switch", isChecked).apply();
            });
        }

        if (btnCloseRatioPanel != null) {
            btnCloseRatioPanel.setOnClickListener(v -> {
                panelRatio.setVisibility(View.GONE);
                scheduleHideControls();
            });
        }
    }

    private void updateSpeedPresetsUI(float speed) {
        int active = R.drawable.bg_chip_red_active;
        int inactive = R.drawable.bg_chip_dark_inactive;
        Button b025 = findViewById(R.id.btn_preset_speed_025);
        Button b05 = findViewById(R.id.btn_preset_speed_05);
        Button b10 = findViewById(R.id.btn_preset_speed_10);
        Button b125 = findViewById(R.id.btn_preset_speed_125);
        Button b15 = findViewById(R.id.btn_preset_speed_15);
        Button b20 = findViewById(R.id.btn_preset_speed_20);
        Button b40 = findViewById(R.id.btn_preset_speed_40);
        Button b80 = findViewById(R.id.btn_preset_speed_80);

        if (b025 != null) b025.setBackgroundResource(Math.abs(speed - 0.25f) < 0.03f ? active : inactive);
        if (b05 != null) b05.setBackgroundResource(Math.abs(speed - 0.5f) < 0.03f ? active : inactive);
        if (b10 != null) b10.setBackgroundResource(Math.abs(speed - 1.0f) < 0.03f ? active : inactive);
        if (b125 != null) b125.setBackgroundResource(Math.abs(speed - 1.25f) < 0.03f ? active : inactive);
        if (b15 != null) b15.setBackgroundResource(Math.abs(speed - 1.5f) < 0.03f ? active : inactive);
        if (b20 != null) b20.setBackgroundResource(Math.abs(speed - 2.0f) < 0.03f ? active : inactive);
        if (b40 != null) b40.setBackgroundResource(Math.abs(speed - 4.0f) < 0.03f ? active : inactive);
        if (b80 != null) b80.setBackgroundResource(Math.abs(speed - 8.0f) < 0.03f ? active : inactive);
    }

    private void updateSpeedUI(float speed) {
        playbackSpeed = Math.round(speed * 100f) / 100f;
        String speedStr = String.format(java.util.Locale.US, "%.2fX", playbackSpeed);
        if (tvSpeedCurrentValue != null) {
            tvSpeedCurrentValue.setText(speedStr);
        }
        if (btnQuickSpeed != null) {
            btnQuickSpeed.setText(String.format(java.util.Locale.US, "%.1fX", playbackSpeed));
        }
        if (seekbarPlaybackSpeed != null) {
            int progress = Math.max(0, Math.min(155, Math.round((playbackSpeed - 0.25f) / 0.05f)));
            seekbarPlaybackSpeed.setProgress(progress);
        }
        updateSpeedPresetsUI(playbackSpeed);
    }

    private void applySpeed(float speed) {
        playbackSpeed = Math.max(0.25f, Math.min(8.0f, Math.round(speed * 100f) / 100f));
        if (isYouTubeActive && youtubeStreamView != null) {
            youtubeStreamView.evaluateJavascript("if (player && player.setPlaybackRate) player.setPlaybackRate(" + playbackSpeed + ");", null);
        } else if (isVlcActive && vlcPlayer != null) {
            vlcPlayer.setRate(playbackSpeed);
        } else if (exoPlayer != null) {
            exoPlayer.setPlaybackSpeed(playbackSpeed);
        }
        updateSpeedUI(playbackSpeed);
        if (rememberSpeed) {
            getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putFloat("saved_playback_speed", playbackSpeed).apply();
        }
        showGestureHud("● SPEED: " + playbackSpeed + "X", DotMatrixIconView.TYPE_SEEK_FORWARD, playbackSpeed + "X", (int) (playbackSpeed * 25));
    }

    private void triggerHapticBoost() {
        if (!longPressVibration || vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(35);
            }
        } catch (Exception ignored) {}
    }

    private void setupSpeedPanelListeners() {
        if (panelSpeed == null) return;

        findViewById(R.id.btn_speed_step_down).setOnClickListener(v -> applySpeed(playbackSpeed - 0.05f));
        findViewById(R.id.btn_speed_step_up).setOnClickListener(v -> applySpeed(playbackSpeed + 0.05f));

        // 8 Discrete Presets
        View b025 = findViewById(R.id.btn_preset_speed_025);
        if (b025 != null) b025.setOnClickListener(v -> applySpeed(0.25f));
        View b05 = findViewById(R.id.btn_preset_speed_05);
        if (b05 != null) b05.setOnClickListener(v -> applySpeed(0.5f));
        View b10 = findViewById(R.id.btn_preset_speed_10);
        if (b10 != null) b10.setOnClickListener(v -> applySpeed(1.0f));
        View b125 = findViewById(R.id.btn_preset_speed_125);
        if (b125 != null) b125.setOnClickListener(v -> applySpeed(1.25f));
        View b15 = findViewById(R.id.btn_preset_speed_15);
        if (b15 != null) b15.setOnClickListener(v -> applySpeed(1.5f));
        View b20 = findViewById(R.id.btn_preset_speed_20);
        if (b20 != null) b20.setOnClickListener(v -> applySpeed(2.0f));
        View b40 = findViewById(R.id.btn_preset_speed_40);
        if (b40 != null) b40.setOnClickListener(v -> applySpeed(4.0f));
        View b80 = findViewById(R.id.btn_preset_speed_80);
        if (b80 != null) b80.setOnClickListener(v -> applySpeed(8.0f));

        if (seekbarPlaybackSpeed != null) {
            seekbarPlaybackSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float speed = 0.25f + (progress * 0.05f);
                        applySpeed(speed);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Subview Switcher: Main -> Advanced
        if (btnOpenSpeedAdvanced != null) {
            btnOpenSpeedAdvanced.setOnClickListener(v -> {
                if (layoutSpeedMain != null) layoutSpeedMain.setVisibility(View.GONE);
                if (layoutSpeedAdvanced != null) layoutSpeedAdvanced.setVisibility(View.VISIBLE);
            });
        }

        // Subview Switcher: Advanced -> Main
        if (btnBackSpeedAdvanced != null) {
            btnBackSpeedAdvanced.setOnClickListener(v -> {
                if (layoutSpeedAdvanced != null) layoutSpeedAdvanced.setVisibility(View.GONE);
                if (layoutSpeedMain != null) layoutSpeedMain.setVisibility(View.VISIBLE);
            });
        }

        // Remember Speed Toggle
        SwitchCompat switchRememberSpeed = findViewById(R.id.switch_remember_speed);
        if (switchRememberSpeed != null) {
            switchRememberSpeed.setChecked(rememberSpeed);
            switchRememberSpeed.setOnCheckedChangeListener((btn, isChecked) -> {
                rememberSpeed = isChecked;
                SharedPreferences.Editor editor = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit();
                editor.putBoolean("remember_speed", isChecked);
                if (isChecked) editor.putFloat("saved_playback_speed", playbackSpeed);
                editor.apply();
            });
        }

        // Long Press Speed Up Toggle
        SwitchCompat switch2x = findViewById(R.id.switch_long_press_2x);
        if (switch2x != null) {
            switch2x.setChecked(longPress2xBoost);
            switch2x.setOnCheckedChangeListener((btn, isChecked) -> {
                longPress2xBoost = isChecked;
                getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putBoolean("long_press_2x_boost", isChecked).apply();
            });
        }

        // Long Press Speed Target Cycle
        View layoutLongPressVal = findViewById(R.id.layout_long_press_speed_val);
        if (layoutLongPressVal != null) {
            layoutLongPressVal.setOnClickListener(v -> {
                if (longPressSpeedTarget == 2.0f) longPressSpeedTarget = 3.0f;
                else if (longPressSpeedTarget == 3.0f) longPressSpeedTarget = 4.0f;
                else if (longPressSpeedTarget == 4.0f) longPressSpeedTarget = 5.0f;
                else longPressSpeedTarget = 2.0f;

                if (tvLongPressSpeedTarget != null) {
                    tvLongPressSpeedTarget.setText(String.format(java.util.Locale.US, "%.1fX >", longPressSpeedTarget));
                }
                getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putFloat("long_press_speed_target", longPressSpeedTarget).apply();
                showGestureHud("● BOOST SPEED", DotMatrixIconView.TYPE_SEEK_FORWARD, longPressSpeedTarget + "X TARGET", 100);
            });
        }

        // Long Press Vibration Toggle
        if (switchLongPressVibration != null) {
            switchLongPressVibration.setChecked(longPressVibration);
            switchLongPressVibration.setOnCheckedChangeListener((btn, isChecked) -> {
                longPressVibration = isChecked;
                getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putBoolean("long_press_vibration", isChecked).apply();
                if (isChecked) triggerHapticBoost();
            });
        }

        if (btnCloseSpeedPanel != null) {
            btnCloseSpeedPanel.setOnClickListener(v -> {
                panelSpeed.setVisibility(View.GONE);
                scheduleHideControls();
            });
        }
    }

    private void updateRepeatModeUI() {
        if (tvMenuRepeatModeVal != null) {
            String[] names = {"ORDER", "LOOP ONE", "SHUFFLE", "LOOP ALL", "PLAY ONCE"};
            tvMenuRepeatModeVal.setText(names[Math.max(0, Math.min(4, currentRepeatMode))]);
        }
        int active = R.drawable.bg_circle_red_active;
        int trans = android.R.color.transparent;
        if (btnRepeatOrder != null) btnRepeatOrder.setBackgroundResource(currentRepeatMode == 0 ? active : trans);
        if (btnRepeatOne != null) btnRepeatOne.setBackgroundResource(currentRepeatMode == 1 ? active : trans);
        if (btnRepeatShuffle != null) btnRepeatShuffle.setBackgroundResource(currentRepeatMode == 2 ? active : trans);
        if (btnRepeatAll != null) btnRepeatAll.setBackgroundResource(currentRepeatMode == 3 ? active : trans);
        if (btnRepeatOnce != null) btnRepeatOnce.setBackgroundResource(currentRepeatMode == 4 ? active : trans);
    }

    private void setRepeatMode(int mode, String label) {
        currentRepeatMode = mode;
        updateRepeatModeUI();
        getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putInt("repeat_mode", mode).apply();
        if (exoPlayer != null) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        }
        showGestureHud("● REPEAT: " + label, DotMatrixIconView.TYPE_SEEK_FORWARD, label, 100);
    }

    private void setupMoreMenuPanelListeners() {
        if (panelMoreMenu == null) return;

        // Top 4x2 Grid Actions
        View audioItem = findViewById(R.id.item_menu_audio_track);
        if (audioItem != null) {
            audioItem.setOnClickListener(v -> {
                if (isVlcActive) showVlcAudioTrackDialog();
                else showExoAudioTrackDialog();
            });
        }

        View subItem = findViewById(R.id.item_menu_subtitle_track);
        if (subItem != null) {
            subItem.setOnClickListener(v -> {
                if (isVlcActive) showVlcSubtitleTrackDialog();
                else showExoSubtitleTrackDialog();
            });
        }

        View bgItem = findViewById(R.id.item_menu_background_play);
        if (bgItem != null) {
            bgItem.setOnClickListener(v -> toggleBackgroundPlay());
        }

        View pipItem = findViewById(R.id.item_menu_pip);
        if (pipItem != null) {
            pipItem.setOnClickListener(v -> enterPipMode());
        }

        View castItem = findViewById(R.id.item_menu_cast);
        if (castItem != null) {
            castItem.setOnClickListener(v -> {
                try {
                    startActivity(new Intent("android.settings.CAST_SETTINGS"));
                } catch (Exception e) {
                    Toast.makeText(this, "Connect to Cast/Wireless Display in Settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View deleteItem = findViewById(R.id.item_menu_delete);
        if (deleteItem != null) {
            deleteItem.setOnClickListener(v -> promptDeleteVideo());
        }

        View bookmarkItem = findViewById(R.id.item_menu_bookmark);
        if (bookmarkItem != null) {
            bookmarkItem.setOnClickListener(v -> saveBookmark());
        }

        View favItem = findViewById(R.id.item_menu_favorites);
        if (favItem != null) {
            favItem.setOnClickListener(v -> toggleFavorite());
        }

        // Play Option Row (AB Repeat, Equalizer, Sleep Timer, Visual Enhancer)
        View abItem = findViewById(R.id.item_playopt_ab_repeat);
        if (abItem != null) {
            abItem.setOnClickListener(v -> toggleAbRepeat());
        }

        View eqItem = findViewById(R.id.item_playopt_equalizer);
        if (eqItem != null) {
            eqItem.setOnClickListener(v -> {
                Intent intent = new Intent(this, EqualizerActivity.class);
                if (exoPlayer != null) {
                    intent.putExtra("audio_session_id", exoPlayer.getAudioSessionId());
                }
                startActivity(intent);
            });
        }

        View timerItem = findViewById(R.id.item_playopt_timer);
        if (timerItem != null) {
            timerItem.setOnClickListener(v -> {
                panelMoreMenu.setVisibility(View.GONE);
                if (panelSleepTimer != null) panelSleepTimer.setVisibility(View.VISIBLE);
            });
        }

        View visualItem = findViewById(R.id.item_playopt_visual_enhancer);
        if (visualItem != null) {
            visualItem.setOnClickListener(v -> {
                panelMoreMenu.setVisibility(View.GONE);
                if (panelVisualEnhancer != null) panelVisualEnhancer.setVisibility(View.VISIBLE);
            });
        }

        // 5-Pill Repeat Mode
        if (btnRepeatOrder != null) btnRepeatOrder.setOnClickListener(v -> setRepeatMode(0, "ORDER"));
        if (btnRepeatOne != null) btnRepeatOne.setOnClickListener(v -> setRepeatMode(1, "LOOP ONE"));
        if (btnRepeatShuffle != null) btnRepeatShuffle.setOnClickListener(v -> setRepeatMode(2, "SHUFFLE"));
        if (btnRepeatAll != null) btnRepeatAll.setOnClickListener(v -> setRepeatMode(3, "LOOP ALL"));
        if (btnRepeatOnce != null) btnRepeatOnce.setOnClickListener(v -> setRepeatMode(4, "PLAY ONCE"));

        // Brightness & Volume Sliders
        if (seekbarMenuBrightness != null) {
            seekbarMenuBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentBrightness = Math.max(0.01f, progress / 100f);
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.screenBrightness = currentBrightness;
                        getWindow().setAttributes(lp);
                        if (tvMenuBrightnessVal != null) tvMenuBrightnessVal.setText(String.valueOf(progress));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (seekbarMenuVolume != null) {
            seekbarMenuVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && audioManager != null) {
                        int vol = Math.round((progress / 100f) * maxVolume);
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
                        if (tvMenuVolumeVal != null) tvMenuVolumeVal.setText(String.valueOf(progress));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Decoder Pills (HW vs SW)
        if (btnMenuDecoderHw != null) {
            btnMenuDecoderHw.setOnClickListener(v -> {
                if (isVlcActive) {
                    long cur = vlcPlayer != null ? vlcPlayer.getTime() : 0;
                    startExoPlayer(cur);
                }
                btnMenuDecoderHw.setBackgroundResource(R.drawable.bg_chip_red_active);
                if (btnMenuDecoderSw != null) btnMenuDecoderSw.setBackgroundResource(R.drawable.bg_chip_dark_inactive);
                Toast.makeText(this, "Hardware (HW+) Decoder Active", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnMenuDecoderSw != null) {
            btnMenuDecoderSw.setOnClickListener(v -> {
                if (!isVlcActive) {
                    long cur = exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
                    startVlcPlayer(cur);
                }
                btnMenuDecoderSw.setBackgroundResource(R.drawable.bg_chip_red_active);
                if (btnMenuDecoderHw != null) btnMenuDecoderHw.setBackgroundResource(R.drawable.bg_chip_dark_inactive);
                Toast.makeText(this, "Universal VLC (SW+) Engine Active", Toast.LENGTH_SHORT).show();
            });
        }

        // Custom Section: Screenshot Toggle & Action Row
        if (switchMenuScreenshotToggle != null) {
            switchMenuScreenshotToggle.setOnCheckedChangeListener((btn, isChecked) -> {
                if (btnQuickScreenshot != null) {
                    btnQuickScreenshot.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
                getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putBoolean("show_screenshot_btn", isChecked).apply();
            });
        }

        View customEdit = findViewById(R.id.btn_custom_edit);
        if (customEdit != null) {
            customEdit.setOnClickListener(v -> {
                Uri uri = resolveMediaUri();
                if (uri != null) {
                    try {
                        Intent editIntent = new Intent(Intent.ACTION_EDIT);
                        editIntent.setDataAndType(uri, "video/*");
                        editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(editIntent, "Edit Video"));
                    } catch (Exception e) {
                        Toast.makeText(this, "No video editor found on device", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        View customFilter = findViewById(R.id.btn_custom_color_filter);
        if (customFilter != null) {
            customFilter.setOnClickListener(v -> {
                panelMoreMenu.setVisibility(View.GONE);
                if (panelVisualEnhancer != null) panelVisualEnhancer.setVisibility(View.VISIBLE);
            });
        }

        View customShare = findViewById(R.id.btn_custom_share);
        if (customShare != null) {
            customShare.setOnClickListener(v -> shareCurrentVideo());
        }

        View customOthers = findViewById(R.id.btn_custom_others);
        if (customOthers != null) {
            customOthers.setOnClickListener(v -> {
                panelMoreMenu.setVisibility(View.GONE);
                if (panelOthers != null) {
                    panelOthers.setVisibility(View.VISIBLE);
                    panelOthers.post(() -> panelOthers.scrollTo(0, 0));
                }
            });
        }

        if (btnCloseMoreMenu != null) {
            btnCloseMoreMenu.setOnClickListener(v -> {
                panelMoreMenu.setVisibility(View.GONE);
                scheduleHideControls();
            });
        }
    }

    // ==========================================
    // PHASE 2: VISUAL ENHANCER LOGIC
    // ==========================================
    private void setupVisualEnhancerListeners() {
        if (panelVisualEnhancer == null) return;

        if (btnBackVisualEnhancer != null) {
            btnBackVisualEnhancer.setOnClickListener(v -> {
                panelVisualEnhancer.setVisibility(View.GONE);
                if (panelMoreMenu != null) panelMoreMenu.setVisibility(View.VISIBLE);
            });
        }

        if (btnCloseVisualEnhancer != null) {
            btnCloseVisualEnhancer.setOnClickListener(v -> {
                panelVisualEnhancer.setVisibility(View.GONE);
                scheduleHideControls();
            });
        }

        if (btnFilterOriginal != null) btnFilterOriginal.setOnClickListener(v -> applyVisualFilter("ORIGINAL"));
        if (btnFilterHdr != null) btnFilterHdr.setOnClickListener(v -> applyVisualFilter("HDR"));
        if (btnFilterClear != null) btnFilterClear.setOnClickListener(v -> applyVisualFilter("CLEAR"));
        if (btnFilterUltraClear != null) btnFilterUltraClear.setOnClickListener(v -> applyVisualFilter("ULTRA CLEAR"));
        if (btnFilterArcticBlue != null) btnFilterArcticBlue.setOnClickListener(v -> applyVisualFilter("ARCTIC BLUE"));
        if (btnFilterWarmGlow != null) btnFilterWarmGlow.setOnClickListener(v -> applyVisualFilter("WARM GLOW"));
        if (btnFilterCinematic != null) btnFilterCinematic.setOnClickListener(v -> applyVisualFilter("CINEMATIC"));

        if (switchFilterApplyAll != null) {
            switchFilterApplyAll.setOnCheckedChangeListener((btn, isChecked) -> {
                filterApplyAll = isChecked;
                SharedPreferences.Editor editor = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit();
                editor.putBoolean("filter_apply_all", isChecked);
                if (isChecked) editor.putString("saved_visual_filter", currentVisualFilter);
                editor.apply();
            });
        }
    }

    private void applyVisualFilter(String filterName) {
        currentVisualFilter = filterName != null ? filterName.toUpperCase() : "ORIGINAL";
        int tintColor = Color.TRANSPARENT;

        switch (currentVisualFilter) {
            case "HDR":
                tintColor = Color.argb(24, 255, 120, 0); // Warm vibrancy & contrast
                break;
            case "CLEAR":
                tintColor = Color.argb(24, 0, 229, 255); // Cool crisp clarity
                break;
            case "ULTRA CLEAR":
                tintColor = Color.argb(20, 255, 255, 255); // High luminescence
                break;
            case "ARCTIC BLUE":
                tintColor = Color.argb(38, 0, 102, 255); // Deep cool cinematic
                break;
            case "WARM GLOW":
                tintColor = Color.argb(35, 255, 102, 0); // Golden hour amber
                break;
            case "CINEMATIC":
                tintColor = Color.argb(35, 0, 51, 102); // Blockbuster teal contrast
                break;
            case "ORIGINAL":
            default:
                tintColor = Color.TRANSPARENT;
                break;
        }

        if (visualEnhancerOverlay != null) {
            visualEnhancerOverlay.setBackgroundColor(tintColor);
        }

        int sel = R.drawable.bg_filter_card_selected;
        int unsel = R.drawable.bg_filter_card;
        if (btnFilterOriginal != null) btnFilterOriginal.setBackgroundResource("ORIGINAL".equals(currentVisualFilter) ? sel : unsel);
        if (btnFilterHdr != null) btnFilterHdr.setBackgroundResource("HDR".equals(currentVisualFilter) ? sel : unsel);
        if (btnFilterClear != null) btnFilterClear.setBackgroundResource("CLEAR".equals(currentVisualFilter) ? sel : unsel);
        if (btnFilterUltraClear != null) btnFilterUltraClear.setBackgroundResource("ULTRA CLEAR".equals(currentVisualFilter) ? sel : unsel);
        if (btnFilterArcticBlue != null) btnFilterArcticBlue.setBackgroundResource("ARCTIC BLUE".equals(currentVisualFilter) ? sel : unsel);
        if (btnFilterWarmGlow != null) btnFilterWarmGlow.setBackgroundResource("WARM GLOW".equals(currentVisualFilter) ? sel : unsel);
        if (btnFilterCinematic != null) btnFilterCinematic.setBackgroundResource("CINEMATIC".equals(currentVisualFilter) ? sel : unsel);

        if (filterApplyAll) {
            getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putString("saved_visual_filter", currentVisualFilter).apply();
        }
        showGestureHud("● FILTER", DotMatrixIconView.TYPE_SEEK_FORWARD, currentVisualFilter, 100);
    }

    // ==========================================
    // PHASE 2: SLEEP TIMER LOGIC
    // ==========================================
    private void setupSleepTimerListeners() {
        if (panelSleepTimer == null) return;

        if (btnBackSleepTimer != null) {
            btnBackSleepTimer.setOnClickListener(v -> {
                panelSleepTimer.setVisibility(View.GONE);
                if (panelMoreMenu != null) panelMoreMenu.setVisibility(View.VISIBLE);
            });
        }

        if (btnCloseSleepTimer != null) {
            btnCloseSleepTimer.setOnClickListener(v -> {
                panelSleepTimer.setVisibility(View.GONE);
                scheduleHideControls();
            });
        }

        if (btnTimerOff != null) btnTimerOff.setOnClickListener(v -> cancelSleepTimer());
        if (btnTimer15 != null) btnTimer15.setOnClickListener(v -> startSleepTimer(15));
        if (btnTimer30 != null) btnTimer30.setOnClickListener(v -> startSleepTimer(30));
        if (btnTimer60 != null) btnTimer60.setOnClickListener(v -> startSleepTimer(60));
        if (btnTimer90 != null) btnTimer90.setOnClickListener(v -> startSleepTimer(90));
        if (btnTimerEndOfVideo != null) btnTimerEndOfVideo.setOnClickListener(v -> setSleepTimerEndOfVideo());
    }

    private void updateSleepTimerButtons(int selectedMinutes, boolean isEndVid) {
        int act = R.drawable.bg_chip_red_active;
        int inact = R.drawable.bg_chip_dark_inactive;
        if (btnTimerOff != null) btnTimerOff.setBackgroundResource(!isEndVid && selectedMinutes == 0 ? act : inact);
        if (btnTimer15 != null) btnTimer15.setBackgroundResource(!isEndVid && selectedMinutes == 15 ? act : inact);
        if (btnTimer30 != null) btnTimer30.setBackgroundResource(!isEndVid && selectedMinutes == 30 ? act : inact);
        if (btnTimer60 != null) btnTimer60.setBackgroundResource(!isEndVid && selectedMinutes == 60 ? act : inact);
        if (btnTimer90 != null) btnTimer90.setBackgroundResource(!isEndVid && selectedMinutes == 90 ? act : inact);
        if (btnTimerEndOfVideo != null) btnTimerEndOfVideo.setBackgroundResource(isEndVid ? act : inact);
    }

    private void cancelSleepTimer() {
        sleepTimerEndOfVideo = false;
        sleepTimerEndTimeMs = 0;
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        if (tvSleepTimerStatus != null) tvSleepTimerStatus.setText("STATUS: OFF");
        if (tvMenuTimerLabel != null) tvMenuTimerLabel.setText("TIMER");
        updateSleepTimerButtons(0, false);
        showGestureHud("● SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, "TIMER OFF", 0);
    }

    private void startSleepTimer(int minutes) {
        sleepTimerEndOfVideo = false;
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        }
        sleepTimerEndTimeMs = System.currentTimeMillis() + (minutes * 60L * 1000L);
        updateSleepTimerButtons(minutes, false);

        sleepTimerRunnable = new Runnable() {
            @Override
            public void run() {
                long remainingMs = sleepTimerEndTimeMs - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    cancelSleepTimer();
                    togglePlayPause();
                    showGestureHud("● SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, "PLAYBACK PAUSED", 100);
                    Toast.makeText(ExoVideoPlayerActivity.this, "Sleep timer expired. Playback paused.", Toast.LENGTH_SHORT).show();
                    return;
                }
                long sec = remainingMs / 1000;
                String countdown = String.format(java.util.Locale.US, "%02d:%02d", sec / 60, sec % 60);
                if (tvSleepTimerStatus != null) {
                    tvSleepTimerStatus.setText("STATUS: " + countdown + " REMAINING");
                }
                if (tvMenuTimerLabel != null) {
                    tvMenuTimerLabel.setText(countdown);
                }
                sleepTimerHandler.postDelayed(this, 1000);
            }
        };
        sleepTimerHandler.post(sleepTimerRunnable);
        showGestureHud("● SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, minutes + " MINUTES SET", 100);
    }

    private void setSleepTimerEndOfVideo() {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        sleepTimerEndOfVideo = true;
        sleepTimerEndTimeMs = 0;
        if (tvSleepTimerStatus != null) tvSleepTimerStatus.setText("STATUS: END OF CURRENT VIDEO");
        if (tvMenuTimerLabel != null) tvMenuTimerLabel.setText("END VID");
        updateSleepTimerButtons(0, true);
        showGestureHud("● SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, "END OF VIDEO", 100);
    }

    // ==========================================
    // PHASE 2: AUXILIARY ACTIONS (Bookmark, Fav, Delete, Share, Properties)
    // ==========================================
    private void saveBookmark() {
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        String key = videoPath != null ? videoPath : videoUriStr;
        if (key != null) {
            SharedPreferences p = getSharedPreferences("nothing_player_bookmarks", MODE_PRIVATE);
            String existing = p.getString(key, "");
            String newEntry = formatTime(cur);
            String updated = existing.isEmpty() ? newEntry : existing + " • " + newEntry;
            p.edit().putString(key, updated).apply();
            showGestureHud("● BOOKMARK ADDED", DotMatrixIconView.TYPE_SEEK_FORWARD, newEntry, 100);
            Toast.makeText(this, "Bookmark created at " + newEntry, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        String key = videoPath != null ? videoPath : videoUriStr;
        if (key == null) return;
        SharedPreferences p = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        boolean isFav = !p.getBoolean("fav_" + key, false);
        p.edit().putBoolean("fav_" + key, isFav).apply();
        if (ivMenuFavoriteIcon != null) {
            ivMenuFavoriteIcon.setColorFilter(isFav ? Color.parseColor("#D71921") : Color.WHITE);
        }
        showGestureHud("● FAVORITE", DotMatrixIconView.TYPE_SEEK_FORWARD, isFav ? "ADDED TO FAVORITES" : "REMOVED FROM FAVORITES", isFav ? 100 : 0);
    }

    private void promptDeleteVideo() {
        NothingDialogHelper.showConfirmDialog(
                this,
                "DELETE VIDEO",
                "Permanently delete this video file from storage?\n\n" + (videoTitle != null ? videoTitle : ""),
                "DELETE",
                () -> {
                    boolean deleted = false;
                    try {
                        if (videoPath != null) {
                            File f = new File(videoPath);
                            if (f.exists()) deleted = f.delete();
                        }
                        if (!deleted && videoUriStr != null) {
                            getContentResolver().delete(Uri.parse(videoUriStr), null, null);
                            deleted = true;
                        }
                    } catch (Exception ignored) {}

                    if (deleted) {
                        Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
                        if (playlistPaths != null && !playlistPaths.isEmpty() && playlistIndex < playlistPaths.size()) {
                            playlistPaths.remove(playlistIndex);
                            if (playlistUris.size() > playlistIndex) playlistUris.remove(playlistIndex);
                            if (playlistTitles.size() > playlistIndex) playlistTitles.remove(playlistIndex);
                            if (!playlistPaths.isEmpty()) {
                                playlistIndex = Math.min(playlistIndex, playlistPaths.size() - 1);
                                playVideoAtIndex(playlistIndex);
                            } else {
                                finish();
                            }
                        } else {
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Could not delete video file", Toast.LENGTH_SHORT).show();
                    }
                },
                "CANCEL",
                null
        );
    }

    private void shareCurrentVideo() {
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("video/*");
            Uri uri = resolveMediaUri();
            if (uri != null) {
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            share.putExtra(Intent.EXTRA_TEXT, videoTitle != null ? videoTitle : "Watch this video");
            startActivity(Intent.createChooser(share, "Share Video via"));
        } catch (Exception e) {
            Toast.makeText(this, "Sharing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // PHASE 3: OTHERS SUBPANEL & FAQ LISTENERS
    // ==========================================
    private void setupOthersPanelListeners() {
        if (panelOthers == null) return;

        if (btnBackOthers != null) {
            btnBackOthers.setOnClickListener(v -> {
                panelOthers.setVisibility(View.GONE);
                if (panelMoreMenu != null) {
                    panelMoreMenu.setVisibility(View.VISIBLE);
                    panelMoreMenu.post(() -> panelMoreMenu.scrollTo(0, 0));
                }
            });
        }

        if (btnCloseOthers != null) {
            btnCloseOthers.setOnClickListener(v -> {
                panelOthers.setVisibility(View.GONE);
                scheduleHideControls();
            });
        }

        if (itemOthersProperties != null) {
            itemOthersProperties.setOnClickListener(v -> showVideoPropertiesDialog());
        }

        if (itemOthersFaq != null) {
            itemOthersFaq.setOnClickListener(v -> showFaqDialog());
        }

        if (itemOthersCastFaq != null) {
            itemOthersCastFaq.setOnClickListener(v -> showCastFaqDialog());
        }

        if (itemOthersPlaybackIssue != null) {
            itemOthersPlaybackIssue.setOnClickListener(v -> showPlaybackIssueDialog());
        }
    }

    private interface OnTrackItemSelectedListener {
        void onTrackSelected(int index);
    }

    private void showNothingInfoDialog(String title, String message) {
        try {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_nothing_info);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            TextView tvTitle = dialog.findViewById(R.id.tv_nothing_dialog_title);
            TextView tvMsg = dialog.findViewById(R.id.tv_nothing_dialog_message);
            Button btnClose = dialog.findViewById(R.id.btn_nothing_dialog_close);

            if (tvTitle != null) tvTitle.setText(title.toUpperCase());
            if (tvMsg != null) tvMsg.setText(message);
            if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        } catch (Exception e) {
            NothingDialogHelper.showConfirmDialog(this, title, message, "CLOSE", null, null, null);
        }
    }

    private void showNothingSelectionDialog(String title, List<String> items, int selectedIndex, OnTrackItemSelectedListener listener) {
        try {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_nothing_selection);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            TextView tvTitle = dialog.findViewById(R.id.tv_selection_dialog_title);
            if (tvTitle != null) tvTitle.setText(title.toUpperCase());

            LinearLayout container = dialog.findViewById(R.id.layout_track_items_container);
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < items.size(); i++) {
                final int idx = i;
                View itemView = inflater.inflate(R.layout.item_nothing_track_selection, container, false);
                View indicator = itemView.findViewById(R.id.view_track_indicator);
                TextView tvTrack = itemView.findViewById(R.id.tv_track_name);

                tvTrack.setText(items.get(i).toUpperCase());
                if (idx == selectedIndex) {
                    indicator.setBackgroundResource(R.drawable.bg_circle_red_active);
                    tvTrack.setTextColor(Color.parseColor("#D71921"));
                } else {
                    indicator.setBackgroundResource(R.drawable.bg_circle_dark);
                    tvTrack.setTextColor(Color.parseColor("#FFFFFF"));
                }

                itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onTrackSelected(idx);
                });

                container.addView(itemView);
            }

            Button btnCancel = dialog.findViewById(R.id.btn_selection_dialog_cancel);
            if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showVideoPropertiesDialog() {
        Point real = getRealScreenSize();
        String res = (videoWidth > 0 && videoHeight > 0) ? (videoWidth + " x " + videoHeight) : (real.x + " x " + real.y);
        String dur = formatTime(totalDurationMs);
        String sizeStr = "Unknown";
        String pathStr = videoPath != null ? videoPath : (videoUriStr != null ? videoUriStr : "Stream");

        if (videoPath != null) {
            File f = new File(videoPath);
            if (f.exists()) {
                double mb = f.length() / (1024.0 * 1024.0);
                if (mb >= 1024) {
                    sizeStr = String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0);
                } else {
                    sizeStr = String.format(java.util.Locale.US, "%.1f MB", mb);
                }
            }
        }

        String details = "TITLE: " + (videoTitle != null ? videoTitle : "N/A") + "\n\n"
                + "RESOLUTION: " + res + "\n"
                + "DURATION: " + dur + "\n"
                + "SIZE: " + sizeStr + "\n"
                + "DECODER: " + currentDecoder + "\n"
                + "PATH: " + pathStr;

        showNothingInfoDialog("VIDEO PROPERTIES", details);
    }

    private void showFaqDialog() {
        String faqText = "● SWIPE LEFT SIDE VERTICALLY\n"
                + "Adjust screen brightness smoothly from 1% to 100%.\n\n"
                + "● SWIPE RIGHT SIDE VERTICALLY\n"
                + "Adjust media volume with real-time dot matrix HUD feedback.\n\n"
                + "● DOUBLE TAP SIDES\n"
                + "Double tap left for 10s Rewind, right for 10s Fast Forward.\n\n"
                + "● LONG PRESS ANYWHERE\n"
                + "Instantly triggers fast playback boost with haptic feedback.\n\n"
                + "● PINCH TO ZOOM\n"
                + "Smoothly scale video up to 400% or fit to screen.\n\n"
                + "● A-B REPEAT\n"
                + "Set loop points A and B to loop any video segment.";
        showNothingInfoDialog("GESTURE & PLAYBACK FAQ", faqText);
    }

    private void showCastFaqDialog() {
        String castFaqText = "● WIRELESS CASTING & MIRRORING\n"
                + "1. Ensure Phone and TV are on the same Wi-Fi network.\n"
                + "2. Tap the CAST button in the top header or More Menu.\n"
                + "3. Select Chromecast, Android TV, Fire TV, or DLNA receiver.\n\n"
                + "● CAST TROUBLESHOOTING\n"
                + "• Restart Wi-Fi on both devices.\n"
                + "• Ensure AP Isolation / Guest Mode is disabled on Wi-Fi router.\n"
                + "• Enable 'Wireless Display' in Android Cast settings.";
        showNothingInfoDialog("CAST & WIRELESS DISPLAY", castFaqText);
    }

    private void showPlaybackIssueDialog() {
        String issueText = "● AUDIO FORMAT NOT SUPPORTED (EAC3 / DTS)\n"
                + "Switch decoder from HW+ to 'UNIVERSAL VLC' in More Menu for full Dolby Digital Plus & DTS 5.1 decode.\n\n"
                + "● VIDEO STUTTERING OR LAG (10-BIT / 4K HEVC)\n"
                + "Switch to HW+ (Hardware Decoder) for GPU acceleration.\n\n"
                + "● AUDIO & VIDEO OUT OF SYNC\n"
                + "Tap 10s Rewind or toggle between HW+ and SW+ in More Menu.\n\n"
                + "● SUBTITLES NOT SHOWING\n"
                + "Open SUBTITLE in the top header to select embedded or external tracks.";
        showNothingInfoDialog("PLAYBACK TROUBLESHOOTING", issueText);
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

        showNothingSelectionDialog("SELECT AUDIO TRACK", trackLabels, selectedIndex, which -> {
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
        });
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

        showNothingSelectionDialog("SELECT AUDIO TRACK (VLC)", labels, selectedIndex, which -> {
            try {
                vlcPlayer.setAudioTrack(trackIds.get(which));
                Toast.makeText(this, "Selected: " + labels.get(which), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

        showNothingSelectionDialog("SELECT SUBTITLE (ESUB / CC)", trackLabels, selectedIndex, which -> {
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
        });
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

        showNothingSelectionDialog("SELECT SUBTITLE (VLC)", labels, selectedIndex, which -> {
            vlcPlayer.setSpuTrack(trackIds.get(which));
            Toast.makeText(this, "Selected: " + labels.get(which), Toast.LENGTH_SHORT).show();
        });
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
                    if (isAnyPanelOpen()) {
                        closeAllPanels();
                        scheduleHideControls();
                    } else {
                        toggleControlsVisibility();
                    }
                } else {
                    if (btnFloatingLock != null) {
                        if (btnFloatingLock.getVisibility() == View.VISIBLE) {
                            btnFloatingLock.setVisibility(View.GONE);
                        } else {
                            btnFloatingLock.setVisibility(View.VISIBLE);
                            btnFloatingLock.setImageResource(R.drawable.ic_lock_closed);
                            btnFloatingLock.setColorFilter(Color.parseColor("#D71921"));
                            hideHandler.removeCallbacksAndMessages(null);
                            hideHandler.postDelayed(() -> {
                                if (isLocked && btnFloatingLock != null) {
                                    btnFloatingLock.setVisibility(View.GONE);
                                }
                            }, 3000);
                        }
                    }
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked) return false;
                Point realSize = getRealScreenSize();
                int screenWidth = Math.max(realSize.x, realSize.y);
                long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
                long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;

                if (e.getX() < screenWidth / 2f) {
                    // Double Tap Left: Rewind 10s
                    long target = Math.max(0, cur - 10000);
                    if (isYouTubeActive && youtubeStreamView != null) {
                        youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                    } else if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("● 10s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-10s", (int) (target * 100 / Math.max(1, dur)));
                } else {
                    // Double Tap Right: Forward 10s
                    long target = Math.min(dur, cur + 10000);
                    if (isYouTubeActive && youtubeStreamView != null) {
                        youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                    } else if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("● 10s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+10s", (int) (target * 100 / Math.max(1, dur)));
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

                Point realSize = getRealScreenSize();
                int screenWidth = Math.max(realSize.x, realSize.y);
                int screenHeight = Math.min(realSize.x, realSize.y);

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

            @Override
            public void onLongPress(MotionEvent e) {
                if (isLocked) return;
                if (longPress2xBoost && !is2xBoostActive) {
                    is2xBoostActive = true;
                    preBoostSpeed = playbackSpeed;
                    triggerHapticBoost();
                    float targetSpeed = longPressSpeedTarget > 0 ? longPressSpeedTarget : 2.0f;
                    if (isYouTubeActive && youtubeStreamView != null) {
                        youtubeStreamView.evaluateJavascript("if (player && player.setPlaybackRate) player.setPlaybackRate(" + targetSpeed + ");", null);
                    } else if (isVlcActive && vlcPlayer != null) {
                        vlcPlayer.setRate(targetSpeed);
                    } else if (exoPlayer != null) {
                        exoPlayer.setPlaybackSpeed(targetSpeed);
                    }
                    showGestureHud("● " + targetSpeed + "X SPEED BOOST", DotMatrixIconView.TYPE_SEEK_FORWARD, "HOLDING " + targetSpeed + "X", 100);
                }
            }
        });

        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (isLocked) return false;
                float scaleFactor = detector.getScaleFactor();
                currentVideoScale = Math.max(0.5f, Math.min(3.5f, currentVideoScale * scaleFactor));

                if (isYouTubeActive && youtubeStreamView != null) {
                    youtubeStreamView.setScaleX(currentVideoScale);
                    youtubeStreamView.setScaleY(currentVideoScale);
                } else if (isVlcActive && vlcPlayer != null) {
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
            if (isLocked) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
            scaleGestureDetector.onTouchEvent(event);
            if (!scaleGestureDetector.isInProgress()) {
                gestureDetector.onTouchEvent(event);
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (is2xBoostActive) {
                    is2xBoostActive = false;
                    applySpeed(preBoostSpeed);
                    showGestureHud("● SPEED RESTORED", DotMatrixIconView.TYPE_SEEK_FORWARD, preBoostSpeed + "X", (int) (preBoostSpeed * 25));
                }
                if (gestureMode[0] == GESTURE_HORIZONTAL) {
                    long target = targetSeekPosition[0];
                    if (isYouTubeActive && youtubeStreamView != null) {
                        youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                    } else if (isVlcActive && vlcPlayer != null) {
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
        if (youtubeStreamView != null) {
            youtubeStreamView.setOnTouchListener(touchListener);
        }
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
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.GONE);
            if (layoutQuickActionsExpanded != null) layoutQuickActionsExpanded.setVisibility(View.GONE);
            closeAllPanels();
            areControlsVisible = false;
        } else {
            topControlsBar.setVisibility(View.VISIBLE);
            bottomControlsBar.setVisibility(View.VISIBLE);
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.VISIBLE);
            if (btnScreenLock != null) {
                btnScreenLock.setVisibility(View.VISIBLE);
                btnScreenLock.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
            }
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
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.GONE);
            if (layoutQuickActionsExpanded != null) layoutQuickActionsExpanded.setVisibility(View.GONE);
            closeAllPanels();
            areControlsVisible = false;
        }
    };

    private void startProgressTracker() {
        progressHandler.post(progressRunnable);
    }

    private void stopProgressTracker() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private long lastSavedProgressMs = 0;

    private void saveCurrentPlaybackProgress(long currentPos, long dur) {
        if (currentPos > 0 && dur > 0) {
            String key = videoPath != null && !videoPath.isEmpty() ? videoPath : videoUriStr;
            if (key != null && !key.isEmpty()) {
                PlaybackHistoryManager.saveProgress(this, key, videoTitle, currentPos, dur);
            }
        }
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isVlcActive && exoPlayer != null && exoPlayer.isPlaying()) {
                long pos = exoPlayer.getCurrentPosition();
                if (repeatPointA >= 0 && repeatPointB > repeatPointA && pos >= repeatPointB) {
                    exoPlayer.seekTo(repeatPointA);
                    pos = repeatPointA;
                }
                videoSeekBar.setProgress((int) pos);
                updateTimeDisplay(pos);
                if (Math.abs(pos - lastSavedProgressMs) >= 2000) {
                    lastSavedProgressMs = pos;
                    saveCurrentPlaybackProgress(pos, totalDurationMs);
                }
                progressHandler.postDelayed(this, 500);
            } else if (isVlcActive && vlcPlayer != null && vlcPlayer.isPlaying()) {
                long pos = vlcPlayer.getTime();
                if (repeatPointA >= 0 && repeatPointB > repeatPointA && pos >= repeatPointB) {
                    vlcPlayer.setTime(repeatPointA);
                    pos = repeatPointA;
                }
                videoSeekBar.setProgress((int) pos);
                updateTimeDisplay(pos);
                progressHandler.postDelayed(this, 500);
            }
        }
    };

    private void updateTimeDisplay(long currentPos) {
        timeCurrentText.setText(formatTime(currentPos));
        if (totalDurationMs <= 0) {
            timeTotalText.setText("LIVE");
            return;
        }
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
        Point realSize = getRealScreenSize();
        int realWidth = Math.max(realSize.x, realSize.y);
        float maxX = parent != null ? parent.getWidth() - cardWidth - dpToPx(8) : realWidth - cardWidth;
        targetX = Math.max(minX, Math.min(maxX, targetX));

        seekbarPreviewCard.setTranslationX(targetX);
        if (previewTimeText != null) {
            previewTimeText.setText(formatTime(progress));
        }
    }

    private void initPreviewRetriever() {
        previewExecutor.execute(() -> {
            try {
                if (previewRetriever != null) {
                    try {
                        previewRetriever.release();
                    } catch (Exception ignored) {}
                    previewRetriever = null;
                }
                previewRetriever = new MediaMetadataRetriever();
                if (videoPath != null && new File(videoPath).exists()) {
                    previewRetriever.setDataSource(videoPath);
                } else if (videoUriStr != null && !videoUriStr.isEmpty()) {
                    previewRetriever.setDataSource(getApplicationContext(), Uri.parse(videoUriStr));
                } else if (getIntent() != null && getIntent().getData() != null) {
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

        Uri uri = resolveMediaUri();
        if (uri != null) {
            Glide.with(ExoVideoPlayerActivity.this)
                .asBitmap()
                .load(uri)
                .override(300, 180)
                .frame(timeMs * 1000)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(previewThumbnail);
        }
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

    private void playNextVideo() {
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        saveCurrentPlaybackProgress(cur, dur);

        if (isYouTubeActive && youtubePlaylistId != null && youtubeStreamView != null) {
            youtubeStreamView.evaluateJavascript("if (player && player.nextVideo) player.nextVideo();", null);
            showGestureHud("● PLAYLIST NEXT", DotMatrixIconView.TYPE_SEEK_FORWARD, "NEXT VIDEO", 100);
            return;
        }

        if (playlistPaths != null && playlistIndex < playlistPaths.size() - 1) {
            playVideoAtIndex(playlistIndex + 1);
        } else {
            showGestureHud("● PLAYLIST END", DotMatrixIconView.TYPE_SEEK_FORWARD, "LAST VIDEO", 100);
        }
    }

    private void playPreviousVideo() {
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        saveCurrentPlaybackProgress(cur, dur);

        if (cur > 3500) {
            seekBy(-cur);
            showGestureHud("● RESTART", DotMatrixIconView.TYPE_SEEK_REWIND, "RESTARTING", 0);
            return;
        }

        if (isYouTubeActive && youtubePlaylistId != null && youtubeStreamView != null) {
            youtubeStreamView.evaluateJavascript("if (player && player.previousVideo) player.previousVideo();", null);
            showGestureHud("● PLAYLIST PREV", DotMatrixIconView.TYPE_SEEK_REWIND, "PREV VIDEO", 100);
            return;
        }

        if (playlistPaths != null && playlistIndex > 0) {
            playVideoAtIndex(playlistIndex - 1);
        } else {
            seekBy(-cur);
            showGestureHud("● PLAYLIST START", DotMatrixIconView.TYPE_SEEK_REWIND, "FIRST VIDEO", 0);
        }
    }

    private void playVideoAtIndex(int index) {
        if (playlistPaths == null || index < 0 || index >= playlistPaths.size()) return;
        playlistIndex = index;
        showTransitionOverlay();

        videoPath = playlistPaths.get(index);
        videoUriStr = playlistUris.size() > index ? playlistUris.get(index) : "";
        videoTitle = playlistTitles.size() > index ? playlistTitles.get(index) : new File(videoPath).getName();

        String openedTarget = videoPath != null && !videoPath.isEmpty() ? videoPath : videoUriStr;
        MediaStateManager.markVideoOpened(this, openedTarget);

        if (videoTitleText != null) {
            videoTitleText.setText(videoTitle);
        }
        updateQueueUI();
        initPreviewRetriever();

        if (isYouTubeActive && isYouTubeLoaded && youtubeStreamView != null) {
            if (youtubePlaylistId != null && !youtubePlaylistId.isEmpty()) {
                youtubeStreamView.evaluateJavascript("if (player && player.playVideoAt) player.playVideoAt(" + index + ");", null);
            } else {
                String rawUrl = videoUriStr != null ? videoUriStr : videoPath;
                String vid = YouTubeStreamResolver.extractVideoId(rawUrl);
                if (vid != null && !vid.isEmpty()) {
                    youtubeStreamView.evaluateJavascript("if (player && player.loadVideoById) player.loadVideoById('" + vid + "');", null);
                }
            }
            showGestureHud("● NOW PLAYING", DotMatrixIconView.TYPE_SEEK_FORWARD, videoTitle, (playlistIndex + 1) * 100 / Math.max(1, playlistPaths.size()));
            return;
        }

        if (isYouTubeStream()) {
            startYouTubePlayer();
        } else if (isSurroundOrEac3File()) {
            startVlcPlayer(0);
        } else {
            startExoPlayer(0);
        }

        showGestureHud("● NOW PLAYING", DotMatrixIconView.TYPE_SEEK_FORWARD, videoTitle, (playlistIndex + 1) * 100 / Math.max(1, playlistPaths.size()));
    }

    private void handleVideoCompletion() {
        if (sleepTimerEndOfVideo) {
            cancelSleepTimer();
            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
            } else if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.pause();
            } else if (exoPlayer != null) {
                exoPlayer.pause();
            }
            if (btnPlayPause != null) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
            showGestureHud("● SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, "PLAYBACK STOPPED", 100);
            Toast.makeText(this, "Sleep timer reached end of video. Playback stopped.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Repeat mode logic
        // 0 = Order, 1 = Loop One, 2 = Shuffle, 3 = Loop All, 4 = Play Once
        if (currentRepeatMode == 1) { // LOOP ONE
            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.evaluateJavascript("if (player && player.seekTo) { player.seekTo(0); player.playVideo(); }", null);
            } else if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.setTime(0);
                vlcPlayer.play();
            } else if (exoPlayer != null) {
                exoPlayer.seekTo(0);
                exoPlayer.play();
            }
            if (btnPlayPause != null) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }
            showGestureHud("● LOOP ONE", DotMatrixIconView.TYPE_SEEK_FORWARD, "REPLAYING", 100);
            return;
        }

        if (currentRepeatMode == 4) { // PLAY ONCE
            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
            } else if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.pause();
            } else if (exoPlayer != null) {
                exoPlayer.pause();
            }
            if (btnPlayPause != null) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
            showGestureHud("● PLAY ONCE", DotMatrixIconView.TYPE_SEEK_FORWARD, "FINISHED", 100);
            return;
        }

        if (currentRepeatMode == 2) { // SHUFFLE
            if (playlistPaths != null && playlistPaths.size() > 1) {
                int nextIdx = playlistIndex;
                java.util.Random rnd = new java.util.Random();
                int attempts = 0;
                while (nextIdx == playlistIndex && attempts < 10) {
                    nextIdx = rnd.nextInt(playlistPaths.size());
                    attempts++;
                }
                playVideoAtIndex(nextIdx);
                showGestureHud("● SHUFFLE", DotMatrixIconView.TYPE_SEEK_FORWARD, "RANDOM NEXT", 100);
            } else {
                if (isYouTubeActive && youtubeStreamView != null) {
                    youtubeStreamView.evaluateJavascript("if (player && player.seekTo) { player.seekTo(0); player.playVideo(); }", null);
                } else if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setTime(0);
                    vlcPlayer.play();
                } else if (exoPlayer != null) {
                    exoPlayer.seekTo(0);
                    exoPlayer.play();
                }
            }
            return;
        }

        if (currentRepeatMode == 3) { // LOOP ALL
            if (playlistPaths != null && !playlistPaths.isEmpty()) {
                if (playlistIndex < playlistPaths.size() - 1) {
                    playNextVideo();
                } else {
                    playVideoAtIndex(0);
                }
            } else {
                if (isYouTubeActive && youtubeStreamView != null) {
                    youtubeStreamView.evaluateJavascript("if (player && player.seekTo) { player.seekTo(0); player.playVideo(); }", null);
                } else if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setTime(0);
                    vlcPlayer.play();
                } else if (exoPlayer != null) {
                    exoPlayer.seekTo(0);
                    exoPlayer.play();
                }
            }
            return;
        }

        // Default: ORDER (0)
        if (playlistPaths != null && playlistIndex < playlistPaths.size() - 1) {
            playNextVideo();
        } else {
            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
            } else if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.pause();
            } else if (exoPlayer != null) {
                exoPlayer.pause();
            }
            if (btnPlayPause != null) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
            showGestureHud("● PLAYLIST END", DotMatrixIconView.TYPE_SEEK_FORWARD, "ORDER COMPLETE", 100);
            if (totalDurationMs > 5000 && !isFinishing()) {
                finish();
            }
        }
    }

    private void togglePlaylistQueuePanel() {
        if (playlistQueuePanel == null) return;
        boolean isVisible = playlistQueuePanel.getVisibility() == View.VISIBLE;
        if (isVisible) {
            playlistQueuePanel.animate()
                    .translationX(playlistQueuePanel.getWidth() > 0 ? playlistQueuePanel.getWidth() : dpToPx(320))
                    .setDuration(200)
                    .withEndAction(() -> playlistQueuePanel.setVisibility(View.GONE))
                    .start();
        } else {
            updateQueueUI();
            if (playlistRecyclerView != null && playlistPaths != null && playlistIndex >= 0 && playlistIndex < playlistPaths.size()) {
                playlistRecyclerView.scrollToPosition(playlistIndex);
            }
            playlistQueuePanel.setTranslationX(dpToPx(320));
            playlistQueuePanel.setVisibility(View.VISIBLE);
            playlistQueuePanel.animate()
                    .translationX(0)
                    .setDuration(220)
                    .start();
        }
    }

    private void updateQueueUI() {
        if (queueHeaderTitle != null) {
            int total = playlistPaths != null ? playlistPaths.size() : 1;
            int current = playlistIndex + 1;
            queueHeaderTitle.setText(String.format("PLAYLIST • %02d / %02d", current, total));
        }
        if (queueAdapter != null && playlistPaths != null) {
            List<PlaylistQueueAdapter.QueueItem> items = new ArrayList<>();
            for (int i = 0; i < playlistPaths.size(); i++) {
                String p = playlistPaths.get(i);
                String u = playlistUris.size() > i ? playlistUris.get(i) : "";
                String t = playlistTitles.size() > i ? playlistTitles.get(i) : new File(p).getName();
                items.add(new PlaylistQueueAdapter.QueueItem(p, u, t, 0));
            }
            queueAdapter.setItems(items, playlistIndex);
        }
    }

    private void togglePlayPause() {
        if (isYouTubeActive) {
            if (isYouTubePlaying) {
                if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
                isYouTubePlaying = false;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            } else {
                if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.playVideo) player.playVideo();", null);
                isYouTubePlaying = true;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }
        } else if (isVlcActive) {
            if (vlcPlayer != null) {
                if (vlcPlayer.isPlaying()) {
                    vlcPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    vlcPlayer.play();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        } else {
            if (exoPlayer != null) {
                if (exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    exoPlayer.play();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        }
        updatePipActions();
    }

    private void seekBy(long deltaMs) {
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        long target = Math.max(0, Math.min(dur > 0 ? dur : Long.MAX_VALUE, cur + deltaMs));

        if (isYouTubeActive && youtubeStreamView != null) {
            youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
        } else if (isVlcActive && vlcPlayer != null) {
            vlcPlayer.setTime(target);
        } else if (exoPlayer != null) {
            exoPlayer.seekTo(target);
        }
        updateTimeDisplay(target);
        videoSeekBar.setProgress((int) target);
    }

    private void updatePipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                boolean isPlaying = isYouTubeActive ? isYouTubePlaying : (isVlcActive ? (vlcPlayer != null && vlcPlayer.isPlaying()) : (exoPlayer != null && exoPlayer.isPlaying()));
                boolean hasMultiple = (playlistPaths != null && playlistPaths.size() > 1) || youtubePlaylistId != null;
                java.util.ArrayList<RemoteAction> actions = new java.util.ArrayList<>();

                // Left Action: Previous Track or Rewind 10s
                if (hasMultiple) {
                    Intent prevIntent = new Intent(ACTION_PIP_PREV).setPackage(getPackageName());
                    PendingIntent prevPending = PendingIntent.getBroadcast(this, 100, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Icon prevIcon = Icon.createWithResource(this, android.R.drawable.ic_media_previous);
                    actions.add(new RemoteAction(prevIcon, "Previous Video", "Previous Video", prevPending));
                } else {
                    Intent rewindIntent = new Intent(ACTION_PIP_REWIND).setPackage(getPackageName());
                    PendingIntent rewindPending = PendingIntent.getBroadcast(this, 101, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Icon rewindIcon = Icon.createWithResource(this, android.R.drawable.ic_media_rew);
                    actions.add(new RemoteAction(rewindIcon, "Rewind 10s", "Rewind 10s", rewindPending));
                }

                // Center Action: Play / Pause
                Intent playPauseIntent = new Intent(ACTION_PIP_PLAY_PAUSE).setPackage(getPackageName());
                PendingIntent playPausePending = PendingIntent.getBroadcast(this, 102, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                Icon playPauseIcon = Icon.createWithResource(this, isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                actions.add(new RemoteAction(playPauseIcon, isPlaying ? "Pause" : "Play", isPlaying ? "Pause" : "Play", playPausePending));

                // Right Action: Next Track or Forward 10s
                if (hasMultiple) {
                    Intent nextIntent = new Intent(ACTION_PIP_NEXT).setPackage(getPackageName());
                    PendingIntent nextPending = PendingIntent.getBroadcast(this, 104, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Icon nextIcon = Icon.createWithResource(this, android.R.drawable.ic_media_next);
                    actions.add(new RemoteAction(nextIcon, "Next Video", "Next Video", nextPending));
                } else {
                    Intent forwardIntent = new Intent(ACTION_PIP_FORWARD).setPackage(getPackageName());
                    PendingIntent forwardPending = PendingIntent.getBroadcast(this, 103, forwardIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Icon forwardIcon = Icon.createWithResource(this, android.R.drawable.ic_media_ff);
                    actions.add(new RemoteAction(forwardIcon, "Forward 10s", "Forward 10s", forwardPending));
                }

                int w = getWindow().getDecorView().getWidth();
                int h = getWindow().getDecorView().getHeight();
                Rational aspectRatio = new Rational(w > 0 ? w : 16, h > 0 ? h : 9);

                PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
                pipBuilder.setAspectRatio(aspectRatio);
                pipBuilder.setActions(actions);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    pipBuilder.setAutoEnterEnabled(true);
                    pipBuilder.setSeamlessResizeEnabled(true);
                }
                setPictureInPictureParams(pipBuilder.build());
            } catch (Exception ignored) {}
        }
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                updatePipActions();
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
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            if (topControlsBar != null) topControlsBar.setVisibility(View.GONE);
            if (bottomControlsBar != null) bottomControlsBar.setVisibility(View.GONE);
            if (btnScreenLock != null) btnScreenLock.setVisibility(View.GONE);
            if (btnFloatingLock != null) btnFloatingLock.setVisibility(View.GONE);
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.GONE);
            if (layoutQuickActionsExpanded != null) layoutQuickActionsExpanded.setVisibility(View.GONE);
            if (gestureHudContainer != null) gestureHudContainer.setVisibility(View.GONE);
            closeAllPanels();
            updatePipActions();
        } else {
            hideSystemUI();
            if (areControlsVisible) {
                if (topControlsBar != null) topControlsBar.setVisibility(View.VISIBLE);
                if (bottomControlsBar != null) bottomControlsBar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isLocked) {
            if (btnFloatingLock != null) {
                btnFloatingLock.setVisibility(View.VISIBLE);
                btnFloatingLock.setImageResource(R.drawable.ic_lock_closed);
                btnFloatingLock.setColorFilter(Color.parseColor("#D71921"));
                hideHandler.removeCallbacksAndMessages(null);
                hideHandler.postDelayed(() -> {
                    if (isLocked && btnFloatingLock != null) {
                        btnFloatingLock.setVisibility(View.GONE);
                    }
                }, 3000);
            }
            return;
        }
        if (isAnyPanelOpen()) {
            closeAllPanels();
            scheduleHideControls();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                updatePipActions();
                enterPipMode();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        saveCurrentPlaybackProgress(cur, dur);

        if (!isInPictureInPictureMode()) {
            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
            } else if (isVlcActive && vlcPlayer != null && vlcPlayer.isPlaying()) {
                vlcPlayer.pause();
            } else if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.pause();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        saveCurrentPlaybackProgress(cur, dur);

        stopProgressTracker();
        hideHandler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(pipReceiver);
        } catch (Exception ignored) {}
        if (youtubeStreamView != null) {
            youtubeStreamView.destroy();
            youtubeStreamView = null;
        }
        try {
            if (previewRetriever != null) {
                previewRetriever.release();
                previewRetriever = null;
            }
            previewExecutor.shutdownNow();
            titleExecutor.shutdownNow();
        } catch (Exception ignored) {}
        releaseExoPlayer();
        releaseVlcPlayer();
    }
}
