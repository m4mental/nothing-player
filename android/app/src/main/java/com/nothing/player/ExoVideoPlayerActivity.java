
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
import java.util.Locale;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.media.VolumeProviderCompat;

import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
import androidx.media3.common.Effect;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.effect.Brightness;
import androidx.media3.effect.Contrast;
import androidx.media3.effect.HslAdjustment;
import androidx.media3.effect.RgbAdjustment;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.SubtitleView;
import android.widget.ListView;
import android.text.TextWatcher;
import android.text.Editable;
import android.graphics.Typeface;
import android.util.TypedValue;

import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.RendererItem;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
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
    private FrameLayout youtubeStreamContainer;
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

    private boolean wasPlayingBeforeHeadsetUnplug = false;
    private final BroadcastReceiver noisyAudioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                boolean autoPause = prefs == null || prefs.getBoolean("headset_auto_pause", true);
                if (autoPause) {
                    boolean isPlaying = isYouTubeActive ? isYouTubePlaying : (isVlcActive ? isVlcPlayingSafe() : (exoPlayer != null && exoPlayer.isPlaying()));
                    if (isPlaying) {
                        wasPlayingBeforeHeadsetUnplug = true;
                        if (isYouTubeActive && youtubeStreamView != null) {
                            youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
                        } else if (isVlcActive && vlcPlayer != null) {
                            vlcPlayer.pause();
                            updatePlayPauseButtonState();
                        } else if (exoPlayer != null) {
                            exoPlayer.pause();
                            updatePlayPauseButtonState();
                        }
                        showGestureHud("● HEADSET UNPLUGGED", DotMatrixIconView.TYPE_SEEK_REWIND, "PAUSED", 100);
                    }
                }
            } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                int state = intent.getIntExtra("state", -1);
                if (state == 1) { // Headset plugged in
                    boolean autoResume = prefs != null && prefs.getBoolean("headset_auto_resume", false);
                    if (autoResume && wasPlayingBeforeHeadsetUnplug) {
                        wasPlayingBeforeHeadsetUnplug = false;
                        if (isYouTubeActive && youtubeStreamView != null) {
                            youtubeStreamView.evaluateJavascript("if (player && player.playVideo) player.playVideo();", null);
                        } else if (isVlcActive && vlcPlayer != null) {
                            vlcPlayer.play();
                            updatePlayPauseButtonState();
                        } else if (exoPlayer != null) {
                            exoPlayer.play();
                            updatePlayPauseButtonState();
                        }
                        showGestureHud("● HEADSET CONNECTED", DotMatrixIconView.TYPE_SEEK_FORWARD, "RESUMED", 100);
                    }
                }
            }
        }
    };
    private boolean isNoisyReceiverRegistered = false;

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
    private final Object previewLock = new Object();
    private volatile boolean previewRetrieverAttempted = false;
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService titleExecutor = Executors.newFixedThreadPool(4);
    private final ExecutorService subtitleIoExecutor = Executors.newSingleThreadExecutor();
    private boolean userForcedHwDecoder = false;
    private boolean hasAutoFallenBackToVlc = false;
    private static volatile boolean isVlcFontconfigReady = false;
    private final List<Dialog> activeDialogs = new ArrayList<>();
    private ParcelFileDescriptor activeVlcPfd = null;
    private android.media.AudioFocusRequest vlcAudioFocusRequest = null;
    private boolean vlcPausedByTransientFocus = false;
    private long pendingVlcResumePos = 0L;
    private long lastSavedProgressMs = 0L;
    private Player.Listener exoPlayerListener;
    private boolean prePodcastBgPlayState = false;
    private volatile long lastRequestedPreviewTime = -1;
    private final android.util.LruCache<Long, Bitmap> previewBitmapCache = new android.util.LruCache<Long, Bitmap>(30) {
        @Override
        protected int sizeOf(Long key, Bitmap value) {
            return 1;
        }
    };
    private final java.util.concurrent.atomic.AtomicLong pendingPreviewTime = new java.util.concurrent.atomic.AtomicLong(-1);
    private final java.util.concurrent.atomic.AtomicBoolean isDecodingFrame = new java.util.concurrent.atomic.AtomicBoolean(false);

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

    // Floating Equalizer Window
    private View windowFloatingEqualizer;
    private View eqDragHeader;
    private View eqBodyContainer;
    private View eqMinimizedPill;
    private TextView tvMinimizedEqStatus;
    private View floatEqPowerLed;
    private androidx.appcompat.widget.SwitchCompat switchFloatEqPower;
    private Button btnFloatEqReset;
    private ImageButton btnMinimizeFloatEq, btnCloseFloatEq, btnExpandFloatEq, btnCloseMinimizedEq;
    private SeekBar[] floatBandSeekbars = new SeekBar[5];
    private TextView[] floatBandValTexts = new TextView[5];
    private SeekBar seekbarFloatBass, seekbarFloatVirt, seekbarFloatPreamp;
    private TextView tvFloatBassVal, tvFloatVirtVal, tvFloatPreampVal;
    private TextView[] floatPresetChips = new TextView[8];
    private boolean isFloatEqInitialized = false;
    private String currentRatioLabel = "FIT";
    // Next Episode Countdown Overlay
    private View layoutNextEpisodeCard;
    private TextView tvNextEpisodeTag;
    private TextView tvNextEpisodeTitle;
    private Button btnCancelNextEpisode;
    private Button btnPlayNowNextEpisode;
    private boolean hasCancelledNextEpisodeForCurrent = false;

    // Skip Intro Pill Button
    private Button btnSkipIntro;
    private boolean hasSkippedIntroForCurrent = false;
    private long introEndMs = 85000L;

    // Per-Series Remembered Preferences & Cast Renderer
    private String currentSeriesKey = "";
    private RendererItem activeCastRenderer = null;

    // Online Subtitles
    private Uri activeSubtitleUri = null;

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
    private View amoledPureBlackOverlay;
    private View eyeComfortOverlay;

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
    private Button btnTimerOff, btnTimer15, btnTimer30, btnTimer45, btnTimer60, btnTimer90, btnTimerEndOfVideo;
    private ChannelMixingAudioProcessor channelMixingAudioProcessor;
    private boolean isMonoDownmixEnabled = false;
    private SwitchCompat switchMenuMonoDownmix;
    private SwitchCompat switchMenuNightCompressor;
    private long subtitleDelayMs = 0L;
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
    private boolean isBackgroundPlayActive = false;
    private ImageView ivMenuBackgroundPlay;
    private TextView tvMenuBackgroundPlay;
    private long repeatPointA = -1;
    private long repeatPointB = -1;
    private int repeatMode = 0; // 0 = All, 1 = One, 2 = Off

    private boolean isLocked = false;
    private boolean areControlsVisible = true;
    private boolean isMuted = false;
    private boolean isAudioBoosted = false;
    private int volumeBoostPercent = 0;
    private SharedPreferences prefs;
    private TextView tvSecondarySubtitle;
    private List<SubtitleParser.SubtitleCue> secondarySubtitleCues = null;
    private List<SubtitleParser.SubtitleCue> searchableSubtitleCues = null;
    private boolean isPodcastModeActive = false;
    private boolean isPalmTouchIgnored = false;
    private final Handler amoledPixelShiftHandler = new Handler(Looper.getMainLooper());
    private final Runnable amoledPixelShiftRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPreferences p = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
            if (p.getBoolean("pref_amoled_pixel_shift", true) && !isFinishing()) {
                int shiftX = (int) (Math.random() * 3 - 1);
                int shiftY = (int) (Math.random() * 3 - 1);
                if (topControlsBar != null) {
                    topControlsBar.setTranslationX(shiftX);
                    topControlsBar.setTranslationY(shiftY);
                }
                if (bottomControlsBar != null) {
                    bottomControlsBar.setTranslationX(shiftX);
                    bottomControlsBar.setTranslationY(shiftY);
                }
            }
            amoledPixelShiftHandler.postDelayed(this, 180000);
        }
    };
    private float previousScreenBrightness = -1f;
    private View layoutAudioOnlyOverlay;
    private TextView tvPodcastTitle;
    private SwitchCompat switchMenuPodcastMode;
    private String secondarySubtitleTitle = null;
    private boolean isTrackingTouch = false;
    private String currentDecoder = "HW+";

    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    private AudioManager audioManager;
    private int maxVolume = 15;
    private float currentBrightness = 0.5f;
    private float playbackSpeed = 1.0f;
    private float currentVideoScale = 1.0f;
    private float videoPanOffsetX = 0f;
    private float videoPanOffsetY = 0f;
    private boolean isSubtitleGuardEnabled = true;
    private float twoFingerStartX = 0f;
    private float twoFingerStartY = 0f;
    private boolean isTwoFingerPanning = false;
    private TextView tvPanOffsetLabel;
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
        ContextCompat.registerReceiver(this, pipReceiver, pipFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        if (!isNoisyReceiverRegistered) {
            try {
                IntentFilter noisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
                noisyFilter.addAction(Intent.ACTION_HEADSET_PLUG);
                ContextCompat.registerReceiver(this, noisyAudioReceiver, noisyFilter, ContextCompat.RECEIVER_EXPORTED);
                isNoisyReceiverRegistered = true;
            } catch (Exception ignored) {}
        }
        if (!isScreenStateReceiverRegistered) {
            try {
                IntentFilter screenFilter = new IntentFilter();
                screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
                screenFilter.addAction(Intent.ACTION_SCREEN_ON);
                ContextCompat.registerReceiver(this, screenStateReceiver, screenFilter, ContextCompat.RECEIVER_EXPORTED);
                isScreenStateReceiverRegistered = true;
            } catch (Exception ignored) {}
        }

        processIntent(getIntent(), false);
        refreshLiveSettings();
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

        boolean resumeActive = intent.getBooleanExtra("resume_active_playback", false);

        if (isNewIntent && !resumeActive) {
            userForcedHwDecoder = false;
            hasAutoFallenBackToVlc = false;
            secondarySubtitleCues = null;
            secondarySubtitleTitle = null;
            searchableSubtitleCues = null;
            subtitleDelayMs = 0L;
            activeSubtitleUri = null;
            repeatPointA = -1;
            repeatPointB = -1;
            totalDurationMs = 0;
            videoWidth = 0;
            videoHeight = 0;
            hasCancelledNextEpisodeForCurrent = false;
            hasSkippedIntroForCurrent = false;
            introEndMs = 85000L;
            is2xBoostActive = false;
            wasPlayingBeforeHeadsetUnplug = false;
            if (tvSecondarySubtitle != null) {
                tvSecondarySubtitle.setText("");
                tvSecondarySubtitle.setVisibility(View.GONE);
            }
            if (videoSeekBar != null) {
                videoSeekBar.clearAbRepeatRange();
                videoSeekBar.setChapterPositions(new ArrayList<>(), 1);
            }
            if (btnSkipIntro != null) btnSkipIntro.setVisibility(View.GONE);
            if (layoutNextEpisodeCard != null) layoutNextEpisodeCard.setVisibility(View.GONE);
        }

        // Load Per-Series Remembered Playback Settings
        currentSeriesKey = SeriesSettingsManager.deriveSeriesKey(intent.getStringExtra("folder"), videoPath, videoTitle);
        if (!resumeActive && !currentSeriesKey.isEmpty()) {
            SeriesSettingsManager.SeriesSettings s = SeriesSettingsManager.load(this, currentSeriesKey);
            if (s.subtitleDelayMs != 0L) subtitleDelayMs = s.subtitleDelayMs;
            if (s.playbackSpeed >= 0.25f && s.playbackSpeed <= 8.0f && Math.abs(s.playbackSpeed - 1.0f) > 0.01f) {
                playbackSpeed = s.playbackSpeed;
            }
            if (s.introSkipSec > 0) {
                introEndMs = s.introSkipSec * 1000L;
            }
            if (prefs != null && s.audioLang != null && !s.audioLang.isEmpty()) {
                prefs.edit().putString("preferred_audio_language", s.audioLang).apply();
            }
        }

        // Resume & Start Over Support
        currentPositionMs = intent.getLongExtra("position", 0);
        boolean startOver = intent.getBooleanExtra("start_over", false);
        String currentKey = videoPath != null && !videoPath.isEmpty() ? videoPath : videoUriStr;
        if (!resumeActive && currentPositionMs <= 0 && !startOver && currentKey != null) {
            PlaybackHistoryManager.HistoryItem saved = PlaybackHistoryManager.getProgress(this, currentKey);
            if (saved != null) {
                if (saved.durationMs > 0) {
                    totalDurationMs = saved.durationMs;
                }
                if (saved.positionMs > 2000 && (saved.durationMs <= 0 || saved.positionMs < saved.durationMs - 5000)) {
                    currentPositionMs = saved.positionMs;
                    showGestureHud("● RESUMED", DotMatrixIconView.TYPE_SEEK_FORWARD, "AT " + saved.getFormattedPosition(), 100);
                }
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
                if (youtubeStreamContainer != null) youtubeStreamContainer.setVisibility(View.GONE);
                isYouTubeActive = false;
                isYouTubeLoaded = false;
            }
            if (!resumeActive) {
                if (isVlcActive || vlcPlayer != null) {
                    releaseVlcPlayer(false);
                    isVlcActive = false;
                    if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(View.GONE);
                }
                if (exoPlayer != null) {
                    releaseExoPlayer(false);
                    if (exoPlayerView != null) exoPlayerView.setVisibility(View.GONE);
                }
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
        if (resumeActive && BackgroundPlaybackManager.getInstance().hasActiveSession()) {
            if (BackgroundPlaybackManager.getInstance().isVlcActive()) {
                startVlcPlayer(currentPositionMs);
            } else {
                startExoPlayer(currentPositionMs);
            }
        } else if (isYouTubeStream()) {
            startYouTubePlayer();
            fetchSingleYouTubeTitle(videoUriStr != null ? videoUriStr : videoPath);
        } else if (isRtspOrLiveStream(videoUriStr != null ? videoUriStr : videoPath)
                || isSpecialCodecRequired(videoPath)
                || isSpecialCodecRequired(videoUriStr)) {
            startVlcPlayer(currentPositionMs);
        } else {
            startExoPlayer(currentPositionMs);
        }
    }

    private boolean isRtspOrLiveStream(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase();
        return lower.startsWith("rtsp://") || lower.startsWith("rtmp://") || lower.startsWith("mms://")
                || lower.startsWith("smb://") || lower.startsWith("ftp://")
                || lower.startsWith("sftp://") || lower.startsWith("ftps://");
    }

    private boolean isSpecialCodecRequired(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase();
        int queryIdx = lower.indexOf('?');
        if (queryIdx != -1) {
            lower = lower.substring(0, queryIdx);
        }
        if (prefs != null) {
            String hevcPref = prefs.getString("pref_hevc_engine", "AUTO");
            if (hevcPref.contains("FORCE VLC") && (lower.contains("hevc") || lower.contains("x265") || lower.contains("h265"))) {
                return true;
            }
        }
        if (lower.contains("hi10p") || lower.contains("10bit") || lower.contains("10-bit")
                || lower.contains("dolby.vision") || lower.contains("dolby vision")
                || lower.contains("dovi") || lower.contains("dvhe")
                || lower.contains("vvc") || lower.contains("h266") || lower.contains("x266")) {
            return true;
        }
        return lower.endsWith(".avi")
                || lower.endsWith(".wmv")
                || lower.endsWith(".flv")
                || lower.endsWith(".f4v")
                || lower.endsWith(".rmvb")
                || lower.endsWith(".rm")
                || lower.endsWith(".vob")
                || lower.endsWith(".ts")
                || lower.endsWith(".m2ts")
                || lower.endsWith(".mts")
                || lower.endsWith(".asf")
                || lower.endsWith(".divx")
                || lower.endsWith(".mpg")
                || lower.endsWith(".mpeg")
                || lower.endsWith(".ogv")
                || lower.endsWith(".ogm")
                || lower.endsWith(".3g2")
                || lower.endsWith(".mxf")
                || lower.endsWith(".trp")
                || lower.endsWith(".dat");
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
        return false; // Hardware-accelerated HW+ (ExoPlayer) is default for all local files (MKV, MP4, AAC 5.1). Auto VLC fallback occurs on error.
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

    private boolean isActivityAlive() {
        return !isFinishing() && !isDestroyed();
    }

    private Point getViewportSize() {
        Point realSize = getRealScreenSize();
        int w = (playerRootLayout != null && playerRootLayout.getWidth() > 0) ? playerRootLayout.getWidth() : realSize.x;
        int h = (playerRootLayout != null && playerRootLayout.getHeight() > 0) ? playerRootLayout.getHeight() : realSize.y;
        return new Point(Math.max(1, w), Math.max(1, h));
    }

    private void trackDialog(Dialog dialog) {
        if (dialog == null) return;
        activeDialogs.add(dialog);
        dialog.setOnDismissListener(d -> activeDialogs.remove(dialog));
    }

    private void dismissActiveDialogs() {
        for (Dialog d : new ArrayList<>(activeDialogs)) {
            try {
                if (d != null && d.isShowing()) {
                    d.dismiss();
                }
            } catch (Exception ignored) {}
        }
        activeDialogs.clear();
    }

    private void restoreActiveVolume() {
        if (exoPlayer != null) {
            try {
                exoPlayer.setVolume(isMuted ? 0.0f : 1.0f);
            } catch (Exception ignored) {}
        }
        if (vlcPlayer != null) {
            try {
                vlcPlayer.setVolume(isMuted ? 0 : (100 + volumeBoostPercent));
            } catch (Exception ignored) {}
        }
    }

    private final Runnable hideFloatingLockRunnable = () -> {
        if (isLocked && btnFloatingLock != null) {
            btnFloatingLock.setVisibility(View.GONE);
        }
    };

    private void showFloatingLockButton() {
        if (btnFloatingLock == null) return;
        btnFloatingLock.setVisibility(View.VISIBLE);
        btnFloatingLock.setImageResource(R.drawable.ic_lock_closed);
        btnFloatingLock.setColorFilter(Color.parseColor("#D71921"));
        hideHandler.removeCallbacks(hideFloatingLockRunnable);
        hideHandler.postDelayed(hideFloatingLockRunnable, 3000);
    }

    @SuppressWarnings("deprecation")
    private Vibrator getVibratorCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.os.VibratorManager vm = (android.os.VibratorManager) getSystemService(Context.VIBRATOR_SERVICE);
                return vm != null ? vm.getDefaultVibrator() : null;
            }
            return (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {
            return null;
        }
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

    private String getVlcAspectString(String label) {
        if (label == null) return null;
        switch (label.toUpperCase()) {
            case "16:9": return "16:9";
            case "4:3": return "4:3";
            case "18:9": return "18:9";
            case "19.5:9": return "195:90";
            case "20:9": return "20:9";
            case "21:9": return "21:9";
            case "1.85:1": return "185:100";
            case "2.21:1": return "221:100";
            case "2.35:1": return "235:100";
            case "2.39:1": return "239:100";
            default:
                if (label.contains(":")) {
                    return label;
                }
                return null;
        }
    }

    private void applyVlcAspectRatio(FrameLayout.LayoutParams fullLp) {
        if (vlcPlayer == null) return;
        try {
            vlcPlayer.setUseOrientationFromBounds(true);
            if (vlcVideoLayout != null) {
                vlcVideoLayout.setLayoutParams(fullLp);
                vlcVideoLayout.requestLayout();
            }

            if ("ORIGINAL".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_ORIGINAL);
            } else if ("16:9".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_16_9);
            } else if ("4:3".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_4_3);
            } else if ("16:10".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_16_10);
            } else if ("2.21:1".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_221_1);
            } else if ("2.35:1".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_235_1);
            } else if ("2.39:1".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_239_1);
            } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL || "STRETCH".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL);
            } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM || "FILL".equalsIgnoreCase(currentRatioLabel)) {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_FIT_SCREEN);
            } else if (targetCustomAspect > 0.05f) {
                String vlcAspect = getVlcAspectString(currentRatioLabel);
                if (vlcAspect != null) {
                    vlcPlayer.setAspectRatio(vlcAspect);
                    vlcPlayer.setScale(0);
                } else {
                    vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
                }
            } else {
                vlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
            }

            vlcPlayer.updateVideoSurfaces();
        } catch (Exception e) {
            e.printStackTrace();
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
                applyVideoTransforms();
                exoPlayerView.requestLayout();
            }

            if (isVlcActive && vlcPlayer != null) {
                applyVlcAspectRatio(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                ));
            }

            if (isYouTubeActive && youtubeStreamView != null) {
                float mirrorSign = isMirrored ? -1.0f : 1.0f;
                youtubeStreamView.setLayoutParams(customLp);
                youtubeStreamView.setScaleX(currentVideoScale * mirrorSign);
                youtubeStreamView.setScaleY(currentVideoScale);
                youtubeStreamView.requestLayout();
            }
        } else {
            // SCREEN Aspect Ratio Modes: FIT, FILL, ORIGINAL, STRETCH
            FrameLayout.LayoutParams fullLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            );

            if (exoPlayerView != null) {
                if ("ORIGINAL".equalsIgnoreCase(currentRatioLabel) && videoWidth > 0 && videoHeight > 0) {
                    int targetW = Math.min(videoWidth, rootW);
                    int targetH = Math.min(videoHeight, rootH);
                    FrameLayout.LayoutParams origLp = new FrameLayout.LayoutParams(targetW, targetH, Gravity.CENTER);
                    exoPlayerView.setLayoutParams(origLp);
                    exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                } else {
                    exoPlayerView.setLayoutParams(fullLp);
                    exoPlayerView.setResizeMode(currentResizeMode);
                }
                applyVideoTransforms();
                exoPlayerView.requestLayout();
            }

            int sw = Math.max(rootW, rootH);
            int sh = Math.min(rootW, rootH);
            float screenAspect = (float) sw / (float) Math.max(1, sh);
            float videoAspect = (videoWidth > 0 && videoHeight > 0) ? ((float) videoWidth / videoHeight) : (16f / 9f);
            float zoomRatio = screenAspect / videoAspect;
            if (zoomRatio < 1.0f) zoomRatio = videoAspect / screenAspect;
            if (zoomRatio < 1.01f) zoomRatio = 1.25f;

            if (isVlcActive && vlcPlayer != null) {
                applyVlcAspectRatio(fullLp);
            }

            if (isYouTubeActive && youtubeStreamView != null) {
                float mirrorSign = isMirrored ? -1.0f : 1.0f;
                youtubeStreamView.setLayoutParams(fullLp);
                if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                    youtubeStreamView.setScaleX(zoomRatio * currentVideoScale * mirrorSign);
                    youtubeStreamView.setScaleY(currentVideoScale);
                } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    youtubeStreamView.setScaleX(zoomRatio * currentVideoScale * mirrorSign);
                    youtubeStreamView.setScaleY(zoomRatio * currentVideoScale);
                } else {
                    youtubeStreamView.setScaleX(currentVideoScale * mirrorSign);
                    youtubeStreamView.setScaleY(currentVideoScale);
                }
                youtubeStreamView.requestLayout();
            }
        }

        if (playerRootLayout != null) {
            playerRootLayout.requestLayout();
        }
    }

    private void initViews() {
        playerRootLayout = findViewById(R.id.player_root_layout);
        exoPlayerView = findViewById(R.id.exo_player_view);
        if (exoPlayerView != null && playerRootLayout != null) {
            SharedPreferences surfacePrefs = getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
            String renderSurface = surfacePrefs.getString("pref_render_surface_type", "TEXTUREVIEW");
            if (renderSurface != null && renderSurface.toUpperCase(Locale.US).contains("SURFACE")) {
                int idx = playerRootLayout.indexOfChild(exoPlayerView);
                playerRootLayout.removeView(exoPlayerView);
                exoPlayerView = (PlayerView) LayoutInflater.from(this)
                        .inflate(R.layout.view_exo_player_surface, playerRootLayout, false);
                playerRootLayout.addView(exoPlayerView, Math.max(0, idx));
            }
        }
        if (exoPlayerView != null) {
            exoPlayerView.setKeepContentOnPlayerReset(true);
            exoPlayerView.setShutterBackgroundColor(Color.TRANSPARENT);
            exoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
        }
        vlcVideoLayout = findViewById(R.id.vlc_video_layout);
        youtubeStreamContainer = findViewById(R.id.youtube_stream_container);
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
        tvSecondarySubtitle = findViewById(R.id.tv_secondary_subtitle);
        if (tvSecondarySubtitle != null) {
            tvSecondarySubtitle.setClickable(true);
            tvSecondarySubtitle.setFocusable(true);
            tvSecondarySubtitle.setOnClickListener(v -> {
                CharSequence text = tvSecondarySubtitle.getText();
                if (text != null && text.length() > 0) {
                    if (isVlcActive && vlcPlayer != null) {
                        vlcPlayer.pause();
                    } else if (exoPlayer != null) {
                        exoPlayer.pause();
                    }
                    updatePlayPauseButtonState();

                    SubtitleDictionaryManager.showWordSelectorDialog(this, text.toString(), (word, sentence) -> {
                        showDotMatrixHud("● LOOKUP: " + word.toUpperCase(Locale.US));
                        SubtitleDictionaryManager.lookupWord(this, word, sentence, new SubtitleDictionaryManager.DictionaryCallback() {
                            @Override
                            public void onResult(SubtitleDictionaryManager.WordDefinition definition) {
                                SubtitleDictionaryManager.showDictionaryDialog(ExoVideoPlayerActivity.this, definition, () -> {
                                    if (isVlcActive && vlcPlayer != null) {
                                        vlcPlayer.play();
                                    } else if (exoPlayer != null) {
                                        exoPlayer.play();
                                    }
                                    updatePlayPauseButtonState();
                                });
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(ExoVideoPlayerActivity.this, "Lookup: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            });
        }
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
        btnSkipIntro = findViewById(R.id.btn_skip_intro);

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
        amoledPureBlackOverlay = findViewById(R.id.amoled_pure_black_overlay);
        eyeComfortOverlay = findViewById(R.id.eye_comfort_overlay);
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
        btnTimer45 = findViewById(R.id.btn_timer_45);
        btnTimer60 = findViewById(R.id.btn_timer_60);
        btnTimer90 = findViewById(R.id.btn_timer_90);
        btnTimerEndOfVideo = findViewById(R.id.btn_timer_end_of_video);
        switchMenuNightCompressor = findViewById(R.id.switch_menu_night_compressor);
        switchMenuMonoDownmix = findViewById(R.id.switch_menu_mono_downmix);

        layoutNextEpisodeCard = findViewById(R.id.layout_next_episode_card);
        tvNextEpisodeTag = findViewById(R.id.tv_next_episode_tag);
        tvNextEpisodeTitle = findViewById(R.id.tv_next_episode_title);
        btnCancelNextEpisode = findViewById(R.id.btn_cancel_next_episode);
        btnPlayNowNextEpisode = findViewById(R.id.btn_play_now_next_episode);

        if (btnCancelNextEpisode != null) {
            btnCancelNextEpisode.setOnClickListener(v -> {
                hasCancelledNextEpisodeForCurrent = true;
                if (layoutNextEpisodeCard != null) layoutNextEpisodeCard.setVisibility(View.GONE);
            });
        }
        if (btnPlayNowNextEpisode != null) {
            btnPlayNowNextEpisode.setOnClickListener(v -> {
                if (layoutNextEpisodeCard != null) layoutNextEpisodeCard.setVisibility(View.GONE);
                playNextVideo();
            });
        }

        vibrator = getVibratorCompat();
        SubtitleDictionaryManager.initTts(this);

        prefs = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        currentResizeMode = prefs.getInt("aspect_ratio_mode", AspectRatioFrameLayout.RESIZE_MODE_FIT);
        rememberRatio = prefs.getBoolean("remember_ratio", true);
        directRatioSwitch = prefs.getBoolean("direct_ratio_switch", true);
        isSubtitleGuardEnabled = prefs.getBoolean("pref_subtitle_guard", true);
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
        // Background playback is OFF by default for any newly opened video
        isBackgroundPlayActive = false;

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
        isMonoDownmixEnabled = prefs.getBoolean("earbud_mono_downmix", false);
        if (switchMenuMonoDownmix != null) {
            switchMenuMonoDownmix.setChecked(isMonoDownmixEnabled);
            switchMenuMonoDownmix.setOnCheckedChangeListener((btn, isChecked) -> {
                setMonoDownmix(isChecked);
            });
        }

        switchMenuPodcastMode = findViewById(R.id.switch_menu_podcast_mode);
        layoutAudioOnlyOverlay = findViewById(R.id.layout_audio_only_overlay);
        tvPodcastTitle = findViewById(R.id.tv_podcast_title);
        if (switchMenuPodcastMode != null) {
            switchMenuPodcastMode.setOnCheckedChangeListener((btn, isChecked) -> {
                setPodcastAudioMode(isChecked);
            });
        }
        if (layoutAudioOnlyOverlay != null) {
            final android.view.GestureDetector doubleTapDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(android.view.MotionEvent e) {
                    setPodcastAudioMode(false);
                    return true;
                }
            });
            layoutAudioOnlyOverlay.setOnTouchListener((v, event) -> {
                doubleTapDetector.onTouchEvent(event);
                return true;
            });
        }
        applySubtitleStyling();
        applyAudioBalance();
        applyAccentColorTheme();

        View btnOpenSettings = findViewById(R.id.btn_menu_open_settings);
        if (btnOpenSettings != null) {
            btnOpenSettings.setOnClickListener(v -> {
                if (panelMoreMenu != null) panelMoreMenu.setVisibility(View.GONE);
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }
        if (switchMenuNightCompressor != null) {
            switchMenuNightCompressor.setChecked(AudioEffectManager.getInstance().isNightModeCompressorEnabled());
            switchMenuNightCompressor.setOnCheckedChangeListener((btn, isChecked) -> {
                AudioEffectManager.getInstance().setNightModeCompressorEnabled(isChecked, this);
                showGestureHud("🌙 NIGHT COMPRESSOR", DotMatrixIconView.TYPE_SEEK_FORWARD, isChecked ? "NORMALIZER ON" : "NORMALIZER OFF", 100);
            });
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

        findViewById(R.id.btn_back).setOnClickListener(v -> handlePlayerBackExit());
        initPreviewRetriever();
    }

    private WebView ensureYouTubeWebView() {
        if (youtubeStreamView == null && youtubeStreamContainer != null) {
            youtubeStreamView = new WebView(this);
            youtubeStreamView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            ));
            youtubeStreamView.setBackgroundColor(0xFF000000);
            youtubeStreamContainer.addView(youtubeStreamView);
        }
        return youtubeStreamView;
    }

    // ==========================================
    // YOUTUBE STREAM ENGINE (Native Inside Default Player)
    // ==========================================
    private void startYouTubePlayer() {
        releaseExoPlayer(false);
        releaseVlcPlayer(false);
        isVlcActive = false;
        isYouTubeActive = true;
        updateDecoderMenuPills();

        if (exoPlayerView != null) exoPlayerView.setVisibility(View.GONE);
        if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(View.GONE);
        ensureYouTubeWebView();
        if (youtubeStreamContainer != null) youtubeStreamContainer.setVisibility(View.VISIBLE);
        if (youtubeStreamView != null) youtubeStreamView.setVisibility(View.VISIBLE);

        currentDecoder = "YouTube Engine";
        if (btnDecoderMode != null) btnDecoderMode.setText("YT");
        if (videoSubtitleCodec != null) videoSubtitleCodec.setText(currentDecoder + " • DIRECT STREAM");

        String rawUrl = videoUriStr != null ? videoUriStr : videoPath;
        String videoId = YouTubeStreamResolver.extractVideoId(rawUrl);
        String safeVideoId = (videoId != null) ? videoId.replaceAll("[^a-zA-Z0-9_-]", "") : "";
        if (safeVideoId.isEmpty()) {
            safeVideoId = "dQw4w9WgXcQ";
        }
        String safePlaylistId = (youtubePlaylistId != null) ? youtubePlaylistId.replaceAll("[^a-zA-Z0-9_-]", "") : "";

        if (isYouTubeLoaded && youtubeStreamView != null) {
            youtubeStreamView.evaluateJavascript("if (player && player.loadVideoById) { player.loadVideoById('" + safeVideoId + "'); }", null);
            applyAspectRatio();
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
                    if (!isActivityAlive()) return;
                    isYouTubeLoaded = true;
                    applyAspectRatio();
                    scheduleHideControls();
                }
            });

            youtubeStreamView.addJavascriptInterface(new Object() {
                @android.webkit.JavascriptInterface
                public void onTimeUpdate(float current, float duration) {
                    runOnUiThread(() -> {
                        if (!isActivityAlive() || !isYouTubeActive) return;
                        youtubeCurrentPositionMs = Math.max(0L, (long) (current * 1000f));
                        youtubeDurationMs = Math.max(0L, (long) (duration * 1000f));
                        totalDurationMs = youtubeDurationMs;
                        if (!isTrackingTouch) {
                            updateTimeDisplay(youtubeCurrentPositionMs);
                            if (youtubeDurationMs > 0 && videoSeekBar != null) {
                                videoSeekBar.setMax((int) youtubeDurationMs);
                                videoSeekBar.setProgress((int) youtubeCurrentPositionMs);
                            }
                        }
                    });
                }

                @android.webkit.JavascriptInterface
                public void onVideoTitle(String title) {
                    runOnUiThread(() -> {
                        if (!isActivityAlive() || !isYouTubeActive) return;
                        if (title != null && !title.isEmpty() && !title.equals("undefined")) {
                            videoTitle = title;
                            if (videoTitleText != null) videoTitleText.setText(title);
                            if (playlistTitles != null && playlistIndex >= 0 && playlistIndex < playlistTitles.size()) {
                                playlistTitles.set(playlistIndex, title);
                            }
                            updateQueueUI();
                        }
                    });
                }

                @android.webkit.JavascriptInterface
                public void onPlaylistLoaded(String jsonIds, int activeIdx) {
                    runOnUiThread(() -> {
                        if (!isActivityAlive() || !isYouTubeActive) return;
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
                        } catch (Exception ignored) {}
                    });
                }

                @android.webkit.JavascriptInterface
                public void onPlaylistIndexChange(int activeIdx) {
                    runOnUiThread(() -> {
                        if (!isActivityAlive() || !isYouTubeActive) return;
                        if (activeIdx >= 0 && playlistPaths != null && activeIdx < playlistPaths.size() && playlistIndex != activeIdx) {
                            playlistIndex = activeIdx;
                            if (playlistTitles != null && playlistIndex < playlistTitles.size() && videoTitleText != null) {
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
                        if (!isActivityAlive() || !isYouTubeActive) return;
                        // state: 1 = PLAYING, 2 = PAUSED, 0 = ENDED, 3 = BUFFERING
                        if (state == 1) {
                            hideTransitionOverlay();
                            isYouTubePlaying = true;
                            if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
                            applyAspectRatio();
                            updatePipActions();
                        } else if (state == 2) {
                            isYouTubePlaying = false;
                            if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_play_dotmatrix);
                            updatePipActions();
                        } else if (state == 0) {
                            isYouTubePlaying = false;
                            if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_play_dotmatrix);
                            updatePipActions();
                            handleVideoCompletion();
                        }
                    });
                }
            }, "AndroidYT");

            String ytListParam = !safePlaylistId.isEmpty()
                    ? (", 'listType': 'playlist', 'list': '" + safePlaylistId + "'") : "";

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
                    "var progressTimer = null;" +
                    "function onYouTubeIframeAPIReady() {" +
                    "  player = new YT.Player('player', {" +
                    "    videoId: '" + safeVideoId + "'," +
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
                    "  if (progressTimer) clearInterval(progressTimer);" +
                    "  progressTimer = setInterval(function() {" +
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

    private String fetchYouTubeTitleSync(String vid) {
        if (vid == null || vid.isEmpty()) return null;
        java.net.HttpURLConnection conn = null;
        try {
            String encodedUrl = java.net.URLEncoder.encode("https://www.youtube.com/watch?v=" + vid, "UTF-8");
            java.net.URL url = new java.net.URL("https://www.youtube.com/oembed?url=" + encodedUrl + "&format=json");
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            if (conn.getResponseCode() == java.net.HttpURLConnection.HTTP_OK) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    org.json.JSONObject obj = new org.json.JSONObject(sb.toString());
                    String realTitle = obj.optString("title", "");
                    return realTitle.isEmpty() ? null : realTitle;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private void fetchYouTubePlaylistTitles(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) return;
        for (int i = 0; i < videoIds.size(); i++) {
            final int index = i;
            final String vid = videoIds.get(i);
            titleExecutor.execute(() -> {
                String realTitle = fetchYouTubeTitleSync(vid);
                if (realTitle != null) {
                    runOnUiThread(() -> {
                        if (!isActivityAlive()) return;
                        if (playlistTitles != null && index >= 0 && index < playlistTitles.size()) {
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
            });
        }
    }

    private void fetchSingleYouTubeTitle(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return;
        String videoId = YouTubeStreamResolver.extractVideoId(rawUrl);
        if (videoId == null || videoId.isEmpty()) return;
        titleExecutor.execute(() -> {
            String realTitle = fetchYouTubeTitleSync(videoId);
            if (realTitle != null) {
                runOnUiThread(() -> {
                    if (!isActivityAlive()) return;
                    videoTitle = realTitle;
                    if (videoTitleText != null) {
                        videoTitleText.setText(realTitle);
                    }
                    if (playlistTitles != null && playlistIndex >= 0 && playlistIndex < playlistTitles.size()) {
                        playlistTitles.set(playlistIndex, realTitle);
                    }
                    if (queueAdapter != null && playlistIndex >= 0) {
                        queueAdapter.updateTitle(playlistIndex, realTitle);
                    }
                });
            }
        });
    }

    private final Runnable transitionOverlaySafetyRunnable = () -> {
        if (videoTransitionOverlay != null && videoTransitionOverlay.getVisibility() == View.VISIBLE) {
            videoTransitionOverlay.setVisibility(View.GONE);
            videoTransitionOverlay.setAlpha(0f);
        }
    };

    private void showTransitionOverlay() {
        if (videoTransitionOverlay == null) return;
        videoTransitionOverlay.animate().cancel();
        videoTransitionOverlay.removeCallbacks(transitionOverlaySafetyRunnable);
        videoTransitionOverlay.setAlpha(1.0f);
        videoTransitionOverlay.setVisibility(View.VISIBLE);
        // Safety watchdog: auto-hide after 500ms so screen is never permanently blocked
        videoTransitionOverlay.postDelayed(transitionOverlaySafetyRunnable, 500);

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
        if (videoTransitionOverlay != null) {
            videoTransitionOverlay.removeCallbacks(transitionOverlaySafetyRunnable);
            if (videoTransitionOverlay.getVisibility() == View.VISIBLE) {
                videoTransitionOverlay.animate().cancel();
                videoTransitionOverlay.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (videoTransitionOverlay != null) {
                            videoTransitionOverlay.setVisibility(View.GONE);
                            videoTransitionOverlay.setImageDrawable(null);
                        }
                    })
                    .start();
            }
        }
    }

    private void updateDecoderMenuPills() {
        if (btnMenuDecoderHw != null) {
            btnMenuDecoderHw.setBackgroundResource((!isYouTubeActive && !isVlcActive) ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
        }
        if (btnMenuDecoderSw != null) {
            btnMenuDecoderSw.setBackgroundResource((!isYouTubeActive && isVlcActive) ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
        }
    }

    private long applyResumePolicy(long resumePos) {
        if (resumePos <= 0) return 0L;
        SharedPreferences appPrefs = getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
        String resumePolicy = appPrefs.getString("pref_resume_policy", "ALWAYS RESUME");
        if (resumePolicy.contains("< 2 MIN") && totalDurationMs > 0 && (totalDurationMs - resumePos < 120000)) {
            return 0L;
        }
        if (resumePolicy.contains("START")) {
            return 0L;
        }
        return resumePos;
    }

    private androidx.media3.exoplayer.source.MediaSource buildExoMediaSource(Uri targetUri) {
        androidx.media3.datasource.DataSource.Factory dataSourceFactory =
                MediaCacheHelper.createDataSourceFactory(this, targetUri);
        androidx.media3.extractor.DefaultExtractorsFactory extractorsFactory =
                new androidx.media3.extractor.DefaultExtractorsFactory()
                        .setConstantBitrateSeekingEnabled(true);
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory);
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(targetUri);
        String urlLower = targetUri.toString().toLowerCase(Locale.US);
        if (urlLower.contains(".m3u8") || urlLower.contains("m3u8")) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);
        } else if (urlLower.contains(".mpd") || urlLower.contains("dash")) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD);
        } else if (urlLower.contains(".ism") || urlLower.contains(".isml") || urlLower.contains("/manifest")) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SS);
        }
        if (activeSubtitleUri != null) {
            String subLower = activeSubtitleUri.toString().toLowerCase(Locale.US);
            String subMime = androidx.media3.common.MimeTypes.APPLICATION_SUBRIP;
            if (subLower.endsWith(".vtt")) {
                subMime = androidx.media3.common.MimeTypes.TEXT_VTT;
            } else if (subLower.endsWith(".ass") || subLower.endsWith(".ssa")) {
                subMime = androidx.media3.common.MimeTypes.TEXT_SSA;
            }
            MediaItem.SubtitleConfiguration subtitleConfig = new MediaItem.SubtitleConfiguration.Builder(activeSubtitleUri)
                    .setMimeType(subMime)
                    .setLanguage("eng")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build();
            mediaItemBuilder.setSubtitleConfigurations(java.util.Collections.singletonList(subtitleConfig));
        }
        return mediaSourceFactory.createMediaSource(mediaItemBuilder.build());
    }

    private Player.Listener getOrCreateExoPlayerListener() {
        if (exoPlayerListener == null) {
            exoPlayerListener = new Player.Listener() {
                @Override
                public void onRenderedFirstFrame() {
                    if (isActivityAlive()) hideTransitionOverlay();
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (!isActivityAlive() || isVlcActive) return;
                    btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_dotmatrix : R.drawable.ic_play_dotmatrix);
                    if (isPlaying) {
                        startProgressTracker();
                        scheduleHideControls();
                        hideTransitionOverlay();
                    } else {
                        stopProgressTracker();
                    }
                    updatePipActions();
                }

                @Override
                public void onVideoSizeChanged(androidx.media3.common.VideoSize videoSize) {
                    if (!isActivityAlive()) return;
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoWidth = videoSize.width;
                        videoHeight = videoSize.height;
                        applyAspectRatio();
                        checkVideoAdaptiveRotation(videoSize.width, videoSize.height);
                    }
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (!isActivityAlive() || isVlcActive || exoPlayer == null) return;
                    if (playbackState == Player.STATE_READY) {
                        hideTransitionOverlay();
                        totalDurationMs = exoPlayer.getDuration();
                        if (totalDurationMs > 0) {
                            videoSeekBar.setMax((int) totalDurationMs);
                        }
                        updateTimeDisplay(exoPlayer.getCurrentPosition());
                        AudioEffectManager.getInstance().attachAudioSession(exoPlayer.getAudioSessionId(), ExoVideoPlayerActivity.this);
                        updateCodecInfo();
                        applyAspectRatio();
                    } else if (playbackState == Player.STATE_ENDED) {
                        handleVideoCompletion();
                    }
                }

                @Override
                public void onTracksChanged(Tracks tracks) {
                    if (!isActivityAlive() || isVlcActive) return;
                    updateCodecInfo();
                    checkAudioCodecSupport(tracks);
                    autoSelectPreferredAudioTrack(tracks);
                    checkAndApplyRefreshRate();
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    if (!isActivityAlive()) return;
                    if (userForcedHwDecoder || hasAutoFallenBackToVlc) {
                        hideTransitionOverlay();
                        String msg = error != null && error.getMessage() != null ? error.getMessage() : "Playback error";
                        Toast.makeText(ExoVideoPlayerActivity.this, "HW+ error: " + msg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    hasAutoFallenBackToVlc = true;
                    long currentPos = exoPlayer != null ? exoPlayer.getCurrentPosition() : currentPositionMs;
                    startVlcPlayer(currentPos);
                }
            };
        }
        return exoPlayerListener;
    }

    // ==========================================
    // EXOPLAYER ENGINE (Hardware Accelerated)
    // ==========================================
    private void startExoPlayer(long resumePos) {
        showTransitionOverlay();
        releaseVlcPlayer(false);
        isVlcActive = false;
        isYouTubeActive = false;
        updateDecoderMenuPills();
        if (youtubeStreamView != null) youtubeStreamView.setVisibility(View.GONE);
        if (youtubeStreamContainer != null) youtubeStreamContainer.setVisibility(View.GONE);
        if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(View.GONE);
        if (exoPlayerView != null) exoPlayerView.setVisibility(View.VISIBLE);

        boolean resumeActive = getIntent().getBooleanExtra("resume_active_playback", false);
        if (resumeActive && BackgroundPlaybackManager.getInstance().hasActiveSession() && !BackgroundPlaybackManager.getInstance().isVlcActive()) {
            exoPlayer = BackgroundPlaybackManager.getInstance().getActivePlayer();
            trackSelector = BackgroundPlaybackManager.getInstance().getActiveTrackSelector();
            channelMixingAudioProcessor = BackgroundPlaybackManager.getInstance().getActiveChannelMixingProcessor();
            BackgroundPlaybackManager.getInstance().setForegroundUiAttached(true);
            if (exoPlayer != null) {
                Player.Listener listener = getOrCreateExoPlayerListener();
                exoPlayer.removeListener(listener);
                exoPlayer.addListener(listener);
                long dur = exoPlayer.getDuration();
                if (dur > 0 && dur != C.TIME_UNSET) {
                    totalDurationMs = dur;
                    if (videoSeekBar != null) videoSeekBar.setMax((int) dur);
                }
                androidx.media3.common.VideoSize vs = exoPlayer.getVideoSize();
                if (vs != null && vs.width > 0 && vs.height > 0) {
                    videoWidth = vs.width;
                    videoHeight = vs.height;
                }
                updateTimeDisplay(exoPlayer.getCurrentPosition());
                AudioEffectManager.getInstance().attachAudioSession(exoPlayer.getAudioSessionId(), this);
            }
            if (exoPlayerView != null) {
                exoPlayerView.setPlayer(exoPlayer);
            }
            currentDecoder = "HW+ (Exo)";
            btnDecoderMode.setText("HW+");
            isBackgroundPlayActive = true;
            updateBackgroundPlayUi();
            updatePlayPauseButtonState();
            updateCodecInfo();
            applyAspectRatio();
            startProgressTracker();
            scheduleHideControls();
            hideTransitionOverlay();
            return;
        }

        if (exoPlayer != null) {
            Uri targetUri = resolveMediaUri();
            if (targetUri != null) {
                long effectivePos = applyResumePolicy(resumePos);
                exoPlayer.setMediaSource(buildExoMediaSource(targetUri), effectivePos);
                exoPlayer.prepare();
                exoPlayer.play();
                if (btnPlayPause != null) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
                }
                BackgroundPlaybackManager.getInstance().setForegroundUiAttached(true);
                BackgroundPlaybackManager.getInstance().setAudioComponents(trackSelector, channelMixingAudioProcessor);
                BackgroundPlaybackManager.getInstance().initSession(
                    getApplicationContext(),
                    exoPlayer,
                    null,
                    null,
                    false,
                    playlistPaths,
                    playlistTitles,
                    playlistUris,
                    playlistIndex,
                    videoTitle,
                    videoPath,
                    videoUriStr,
                    isBackgroundPlayActive
                );
                startProgressTracker();
                hideTransitionOverlay();
                applyPlaybackSpeedWithPitchPreservation(playbackSpeed);
                currentDecoder = "HW+ (Exo)";
                btnDecoderMode.setText("HW+");
                updateCodecInfo();
                scheduleHideControls();
            }
            return;
        }

        channelMixingAudioProcessor = new ChannelMixingAudioProcessor();
        applyAudioBalance();

        DefaultAudioSink audioSink = new DefaultAudioSink.Builder(this)
            .setAudioProcessors(new AudioProcessor[]{channelMixingAudioProcessor})
            .build();

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this) {
            @Override
            protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams) {
                return audioSink;
            }
        };
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true);

        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowAudioNonSeamlessAdaptiveness(true)
                .setTunnelingEnabled(false)
        );

        SharedPreferences appPrefs = getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
        String bufferMode = appPrefs.getString("pref_buffer_size_mode", "BALANCED");
        int minBuffer = 15000;
        int maxBuffer = 50000;
        int bufForPlayback = 200;
        int bufAfterRebuf = 500;
        if (bufferMode.contains("ULTRA-LOW") || bufferMode.contains("5MB")) {
            minBuffer = 2500;
            maxBuffer = 5000;
            bufForPlayback = 100;
            bufAfterRebuf = 250;
        } else if (bufferMode.contains("HEAVY") || bufferMode.contains("100MB")) {
            minBuffer = 50000;
            maxBuffer = 120000;
            bufForPlayback = 1500;
            bufAfterRebuf = 3000;
        }

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBuffer,
                maxBuffer,
                bufForPlayback,
                bufAfterRebuf
            )
            .setBackBuffer(15000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build();

        exoPlayer = new ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build();

        String scrubMode = appPrefs.getString("pref_scrubbing_mode", "FAST KEYFRAME");
        if (scrubMode.contains("EXACT")) {
            exoPlayer.setSeekParameters(SeekParameters.EXACT);
        } else {
            exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        }

        boolean dacPassthrough = appPrefs.getBoolean("pref_dac_passthrough", false);
        AudioAttributes.Builder audioAttrBuilder = new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(dacPassthrough ? C.AUDIO_CONTENT_TYPE_MUSIC : C.AUDIO_CONTENT_TYPE_MOVIE);
        if (dacPassthrough && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            audioAttrBuilder.setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL);
        }
        exoPlayer.setAudioAttributes(audioAttrBuilder.build(), true);
        exoPlayer.setWakeMode(C.WAKE_MODE_LOCAL);

        exoPlayerView.setPlayer(exoPlayer);
        applyAspectRatio();
        applyVisualFilterEffectOnExo();

        Uri targetUri = resolveMediaUri();

        if (targetUri != null) {
            exoPlayer.setMediaSource(buildExoMediaSource(targetUri));
            long effectivePos = applyResumePolicy(resumePos);
            if (effectivePos > 0) {
                exoPlayer.seekTo(effectivePos);
            }
            exoPlayer.prepare();
            exoPlayer.play();
            applyPlaybackSpeedWithPitchPreservation(playbackSpeed);
            currentDecoder = "HW+ (Exo)";
            btnDecoderMode.setText("HW+");
            updateCodecInfo();
        }

        BackgroundPlaybackManager.getInstance().setForegroundUiAttached(true);
        BackgroundPlaybackManager.getInstance().setAudioComponents(trackSelector, channelMixingAudioProcessor);
        BackgroundPlaybackManager.getInstance().initSession(
            getApplicationContext(),
            exoPlayer,
            null,
            null,
            false,
            playlistPaths,
            playlistTitles,
            playlistUris,
            playlistIndex,
            videoTitle,
            videoPath,
            videoUriStr,
            isBackgroundPlayActive
        );

        exoPlayer.addListener(getOrCreateExoPlayerListener());
    }

    private void checkAudioCodecSupport(Tracks tracks) {
        if (tracks == null || isVlcActive || userForcedHwDecoder || hasAutoFallenBackToVlc) return;
        boolean hasVideoTrack = false;
        boolean hasSupportedVideo = false;
        boolean hasAudioTrack = false;
        boolean hasSupportedAudio = false;
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_VIDEO) {
                hasVideoTrack = true;
                if (group.isSupported()) {
                    hasSupportedVideo = true;
                }
            } else if (group.getType() == C.TRACK_TYPE_AUDIO) {
                hasAudioTrack = true;
                if (group.isSupported()) {
                    hasSupportedAudio = true;
                }
            }
        }
        if ((hasVideoTrack && !hasSupportedVideo) || (hasAudioTrack && !hasSupportedAudio)) {
            hasAutoFallenBackToVlc = true;
            long currentPos = exoPlayer != null ? exoPlayer.getCurrentPosition() : currentPositionMs;
            String reason = (hasVideoTrack && !hasSupportedVideo)
                    ? "SWITCHING TO VLC FOR MODERN VIDEO CODEC"
                    : "SWITCHING TO VLC FOR DOLBY/DTS AUDIO";
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
            startVlcPlayer(currentPos);
        }
    }

    private final AudioManager.OnAudioFocusChangeListener vlcAudioFocusListener = focusChange -> {
        if (!isVlcActive || vlcPlayer == null) return;
        try {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                vlcPausedByTransientFocus = false;
                if (!vlcPlayer.isReleased() && vlcPlayer.isPlaying()) {
                    vlcPlayer.pause();
                    updatePlayPauseButtonState();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if (!vlcPlayer.isReleased() && vlcPlayer.isPlaying()) {
                    vlcPausedByTransientFocus = true;
                    vlcPlayer.pause();
                    updatePlayPauseButtonState();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (!vlcPlayer.isReleased() && !isMuted) {
                    vlcPlayer.setVolume(25);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (!vlcPlayer.isReleased()) {
                    vlcPlayer.setVolume(isMuted ? 0 : (100 + volumeBoostPercent));
                    if (vlcPausedByTransientFocus) {
                        vlcPausedByTransientFocus = false;
                        vlcPlayer.play();
                        updatePlayPauseButtonState();
                    }
                }
            }
        } catch (Exception ignored) {}
    };

    @SuppressWarnings("deprecation")
    private void requestVlcAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vlcAudioFocusRequest == null) {
                    vlcAudioFocusRequest = new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(new android.media.AudioAttributes.Builder()
                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                                    .build())
                            .setOnAudioFocusChangeListener(vlcAudioFocusListener)
                            .build();
                }
                audioManager.requestAudioFocus(vlcAudioFocusRequest);
            } else {
                audioManager.requestAudioFocus(vlcAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("deprecation")
    private void abandonVlcAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vlcAudioFocusRequest != null) {
                    audioManager.abandonAudioFocusRequest(vlcAudioFocusRequest);
                }
            } else {
                audioManager.abandonAudioFocus(vlcAudioFocusListener);
            }
        } catch (Exception ignored) {}
    }

    private final MediaPlayer.EventListener vlcEventListener = event -> runOnUiThread(() -> {
        if (!isActivityAlive() || vlcPlayer == null || !isVlcActive) return;
        if (event.type == MediaPlayer.Event.Playing || event.type == MediaPlayer.Event.Vout) {
            hideTransitionOverlay();
            if (pendingVlcResumePos > 0) {
                try {
                    vlcPlayer.setTime(pendingVlcResumePos);
                } catch (Exception ignored) {}
                pendingVlcResumePos = 0L;
            }
            try {
                vlcPlayer.setRate(playbackSpeed);
            } catch (Exception ignored) {}
            if (activeSubtitleUri != null) {
                try {
                    vlcPlayer.addSlave(Media.Slave.Type.Subtitle, activeSubtitleUri, true);
                } catch (Exception ignored) {}
            }
            AudioEffectManager.getInstance().applyVlcEqualizer(vlcPlayer, ExoVideoPlayerActivity.this);
            btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
            startProgressTracker();
            scheduleHideControls();
            try {
                org.videolan.libvlc.interfaces.IMedia.VideoTrack vt = vlcPlayer.getCurrentVideoTrack();
                if (vt != null && vt.width > 0 && vt.height > 0) {
                    videoWidth = vt.width;
                    videoHeight = vt.height;
                }
            } catch (Exception ignored) {}
            applyAspectRatio();
            if (vlcVideoLayout != null) vlcVideoLayout.post(ExoVideoPlayerActivity.this::applyAspectRatio);
            updateChapters();
            updatePipActions();
        } else if (event.type == MediaPlayer.Event.Paused || event.type == MediaPlayer.Event.Stopped) {
            updatePlayPauseButtonState();
            updatePipActions();
        } else if (event.type == MediaPlayer.Event.LengthChanged) {
            totalDurationMs = vlcPlayer.getLength();
            if (totalDurationMs > 0) videoSeekBar.setMax((int) totalDurationMs);
            updateTimeDisplay(currentPositionMs);
            updateChapters();
        } else if (event.type == MediaPlayer.Event.TimeChanged) {
            hideTransitionOverlay();
            long pos = event.getTimeChanged();
            currentPositionMs = pos;
            updateTimeDisplay(pos);
            videoSeekBar.setProgress((int) pos);
            checkSkipIntroButton(pos, totalDurationMs);
            updateSecondarySubtitle(pos);
            if (Math.abs(pos - lastSavedProgressMs) >= 2000) {
                lastSavedProgressMs = pos;
                saveCurrentPlaybackProgress(pos, totalDurationMs);
            }
        } else if (event.type == MediaPlayer.Event.EndReached) {
            handleVideoCompletion();
        } else if (event.type == MediaPlayer.Event.EncounteredError) {
            hideTransitionOverlay();
            Toast.makeText(ExoVideoPlayerActivity.this, "Playback Warning", Toast.LENGTH_SHORT).show();
        }
    });

    /**
     * Points LibVLC's bundled Fontconfig to a single lightweight system TTF and writable cache
     * directory so embedded MKV subtitles do not trigger a ~45s full scan of /system/fonts.
     */
    private static synchronized void ensureFastVlcFontconfig(Context context) {
        if (isVlcFontconfigReady || context == null) return;
        try {
            File cacheDir = context.getCacheDir();
            File minFontDir = new File(cacheDir, "vlc_font_min");
            File fcCacheDir = new File(cacheDir, "vlc_fc_cache");
            File fcConfDir = new File(cacheDir, "vlc_fc_conf");
            minFontDir.mkdirs();
            fcCacheDir.mkdirs();
            fcConfDir.mkdirs();

            String[] candidates = {
                "/system/fonts/Roboto-Regular.ttf",
                "/system/fonts/DroidSans.ttf",
                "/system/fonts/DroidSansMono.ttf"
            };
            File targetFont = new File(minFontDir, "default.ttf");
            if (!targetFont.exists()) {
                for (String path : candidates) {
                    File src = new File(path);
                    if (src.exists() && src.canRead()) {
                        try {
                            if (targetFont.exists()) targetFont.delete();
                            android.system.Os.symlink(src.getAbsolutePath(), targetFont.getAbsolutePath());
                        } catch (Throwable ignored) {}
                        break;
                    }
                }
            }

            File fontsConf = new File(fcConfDir, "fonts.conf");
            String xml = "<?xml version=\"1.0\"?>\n"
                + "<fontconfig>\n"
                + "  <dir>" + minFontDir.getAbsolutePath() + "</dir>\n"
                + "  <cachedir>" + fcCacheDir.getAbsolutePath() + "</cachedir>\n"
                + "</fontconfig>\n";
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(fontsConf, false)) {
                fos.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            android.system.Os.setenv("FONTCONFIG_FILE", fontsConf.getAbsolutePath(), true);
            android.system.Os.setenv("FONTCONFIG_PATH", fcConfDir.getAbsolutePath(), true);
            android.system.Os.setenv("XDG_CACHE_HOME", cacheDir.getAbsolutePath(), true);
            android.system.Os.setenv("HOME", cacheDir.getAbsolutePath(), true);
            isVlcFontconfigReady = true;
        } catch (Throwable ignored) {}
    }

    // =======================================================
    // LIBVLC NATIVE ENGINE (Universal EAC3, AC3, DTS, TrueHD)
    // =======================================================
    private void startVlcPlayer(long resumePos) {
        ensureFastVlcFontconfig(getApplicationContext());
        showTransitionOverlay();
        releaseExoPlayer(false);
        isVlcActive = true;
        isYouTubeActive = false;
        updateDecoderMenuPills();
        if (youtubeStreamView != null) youtubeStreamView.setVisibility(View.GONE);
        if (youtubeStreamContainer != null) youtubeStreamContainer.setVisibility(View.GONE);
        exoPlayerView.setVisibility(View.GONE);
        vlcVideoLayout.setVisibility(View.VISIBLE);
        vlcVideoLayout.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        ));
        vlcVideoLayout.requestLayout();
        requestVlcAudioFocus();

        boolean resumeActive = getIntent().getBooleanExtra("resume_active_playback", false);
        if (resumeActive && BackgroundPlaybackManager.getInstance().hasActiveSession() && BackgroundPlaybackManager.getInstance().isVlcActive()) {
            vlcPlayer = BackgroundPlaybackManager.getInstance().getActiveVlcPlayer();
            libVLC = BackgroundPlaybackManager.getInstance().getLibVLC();
            BackgroundPlaybackManager.getInstance().setForegroundVlcListener(vlcEventListener);
            BackgroundPlaybackManager.getInstance().setForegroundUiAttached(true);
            if (vlcPlayer != null && !isFinishing()) {
                vlcPlayer.setUseOrientationFromBounds(true);
                try {
                    vlcPlayer.attachViews(vlcVideoLayout, null, true, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long len = vlcPlayer.getLength();
                if (len > 0) {
                    totalDurationMs = len;
                    if (videoSeekBar != null) videoSeekBar.setMax((int) len);
                }
                updateTimeDisplay(vlcPlayer.getTime());
                updateChapters();
                AudioEffectManager.getInstance().applyVlcEqualizer(vlcPlayer, this);
            }
            isBackgroundPlayActive = true;
            updateBackgroundPlayUi();
            updatePlayPauseButtonState();
            currentDecoder = "Universal VLC";
            btnDecoderMode.setText("VLC");
            videoSubtitleCodec.setText(currentDecoder + " • DOLBY 5.1 / EAC3");
            applyAspectRatio();
            if (vlcVideoLayout != null) vlcVideoLayout.post(this::applyAspectRatio);
            startProgressTracker();
            scheduleHideControls();
            hideTransitionOverlay();
            return;
        }

        if (vlcPlayer != null) {
            vlcPlayer.setUseOrientationFromBounds(true);
            try {
                vlcPlayer.attachViews(vlcVideoLayout, null, true, true);
            } catch (Exception ignored) {}
            Media media = createVlcMedia();
            if (media != null) {
                pendingVlcResumePos = applyResumePolicy(resumePos);
                media.setHWDecoderEnabled(true, false);
                media.addOption(":file-caching=1500");
                media.addOption(":network-caching=3000");
                vlcPlayer.setMedia(media);
                media.release();
                BackgroundPlaybackManager.getInstance().setForegroundVlcListener(vlcEventListener);
                BackgroundPlaybackManager.getInstance().setForegroundUiAttached(true);
                BackgroundPlaybackManager.getInstance().initSession(
                    getApplicationContext(),
                    null,
                    vlcPlayer,
                    libVLC,
                    true,
                    playlistPaths,
                    playlistTitles,
                    playlistUris,
                    playlistIndex,
                    videoTitle,
                    videoPath,
                    videoUriStr,
                    isBackgroundPlayActive
                );
                vlcPlayer.play();
                if (btnPlayPause != null) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
                }
                if (vlcVideoLayout != null) {
                    vlcVideoLayout.postDelayed(this::autoSelectPreferredVlcAudioTrack, 800);
                }
                startProgressTracker();
                if (activeSubtitleUri != null) {
                    try {
                        vlcPlayer.addSlave(Media.Slave.Type.Subtitle, activeSubtitleUri, true);
                    } catch (Exception ignored) {}
                }
                if (pendingVlcResumePos > 0) {
                    vlcPlayer.setTime(pendingVlcResumePos);
                }
                vlcPlayer.setRate(playbackSpeed);
                AudioEffectManager.getInstance().applyVlcEqualizer(vlcPlayer, this);
                currentDecoder = "Universal VLC";
                btnDecoderMode.setText("VLC");
                videoSubtitleCodec.setText(currentDecoder + " • DOLBY 5.1 / EAC3");
                applyAspectRatio();
                if (vlcVideoLayout != null) vlcVideoLayout.post(this::applyAspectRatio);
                scheduleHideControls();
                hideTransitionOverlay();
            }
            return;
        }

        ArrayList<String> options = new ArrayList<>();
        options.add("--drop-late-frames");
        options.add("--audio-time-stretch");
        options.add("--audio-resampler=soxr");
        options.add("--avcodec-threads=0");
        options.add("--file-caching=1500");
        options.add("--network-caching=3000");

        if (prefs != null) {
            int subSize = prefs.getInt("sub_size_choice", 18);
            boolean subBox = prefs.getBoolean("sub_box_choice", true);
            int relFont = subSize >= 24 ? 13 : (subSize >= 20 ? 15 : (subSize >= 16 ? 18 : 22));
            options.add("--freetype-rel-fontsize=" + relFont);
            options.add("--freetype-bold");
            options.add("--freetype-color=16777215");
            options.add("--freetype-shadow-opacity=200");
            options.add("--freetype-background-opacity=" + (subBox ? "180" : "0"));
            options.add("--freetype-background-color=0");
        }

        libVLC = new LibVLC(this, options);
        vlcPlayer = new MediaPlayer(libVLC);
        vlcPlayer.setUseOrientationFromBounds(true);
        if (activeCastRenderer != null) {
            try {
                vlcPlayer.setRenderer(activeCastRenderer);
            } catch (Throwable ignored) {}
        }

        // Synchronously attach views BEFORE playback starts
        try {
            vlcPlayer.attachViews(vlcVideoLayout, null, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Media media = createVlcMedia();

        if (media != null) {
            pendingVlcResumePos = applyResumePolicy(resumePos);
            media.setHWDecoderEnabled(true, false);
            media.addOption(":file-caching=1500");
            media.addOption(":network-caching=3000");
            vlcPlayer.setMedia(media);
            media.release();

            BackgroundPlaybackManager.getInstance().setForegroundVlcListener(vlcEventListener);
            BackgroundPlaybackManager.getInstance().setForegroundUiAttached(true);
            BackgroundPlaybackManager.getInstance().initSession(
                getApplicationContext(),
                null,
                vlcPlayer,
                libVLC,
                true,
                playlistPaths,
                playlistTitles,
                playlistUris,
                playlistIndex,
                videoTitle,
                videoPath,
                videoUriStr,
                isBackgroundPlayActive
            );

            vlcPlayer.play();
            if (btnPlayPause != null) {
                btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
            }
            if (vlcVideoLayout != null) {
                vlcVideoLayout.postDelayed(this::autoSelectPreferredVlcAudioTrack, 800);
            }
            startProgressTracker();
            if (pendingVlcResumePos > 0) {
                vlcPlayer.setTime(pendingVlcResumePos);
            }
            vlcPlayer.setRate(playbackSpeed);
            AudioEffectManager.getInstance().applyVlcEqualizer(vlcPlayer, this);
            currentDecoder = "Universal VLC";
            btnDecoderMode.setText("VLC");
            videoSubtitleCodec.setText(currentDecoder + " • DOLBY 5.1 / EAC3");
            applyAspectRatio();
            if (vlcVideoLayout != null) vlcVideoLayout.post(this::applyAspectRatio);
        } else {
            hideTransitionOverlay();
            Toast.makeText(this, "Could not open video stream", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void closeActiveVlcPfd() {
        if (activeVlcPfd != null) {
            try { activeVlcPfd.close(); } catch (Exception ignored) {}
            activeVlcPfd = null;
        }
    }

    private Media createVlcMedia() {
        if (libVLC == null) return null;
        closeActiveVlcPfd();
        try {
            if (videoPath != null && !videoPath.isEmpty()) {
                if (StreamUrlHelper.isOnlineStream(videoPath)) {
                    Media m = new Media(libVLC, Uri.parse(videoPath));
                    m.setHWDecoderEnabled(true, false);
                    return m;
                }
                String cleanPath = videoPath.startsWith("file://") ? videoPath.substring(7) : videoPath;
                File f = new File(cleanPath);
                if (f.exists() && f.canRead()) {
                    Media m = new Media(libVLC, f.getAbsolutePath());
                    m.setHWDecoderEnabled(true, false);
                    return m;
                }
            }
            if (videoUriStr != null && !videoUriStr.isEmpty()) {
                Uri u = Uri.parse(videoUriStr);
                if ("content".equalsIgnoreCase(u.getScheme())) {
                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(u, "r");
                        if (pfd != null) {
                            activeVlcPfd = pfd;
                            Media m = new Media(libVLC, pfd.getFileDescriptor());
                            m.setHWDecoderEnabled(true, false);
                            return m;
                        }
                    } catch (Exception ignored) {}
                }
                Media m = new Media(libVLC, u);
                m.setHWDecoderEnabled(true, false);
                return m;
            }
            if (videoPath != null && !videoPath.isEmpty()) {
                Media m = new Media(libVLC, Uri.parse(videoPath));
                m.setHWDecoderEnabled(true, false);
                return m;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void releaseExoPlayer(boolean preserveForBackground) {
        if (preserveForBackground && isBackgroundPlayActive && exoPlayer != null) {
            BackgroundPlaybackManager.getInstance().setForegroundUiAttached(false);
            if (exoPlayerListener != null) {
                try { exoPlayer.removeListener(exoPlayerListener); } catch (Exception ignored) {}
            }
            if (exoPlayerView != null) {
                exoPlayerView.setPlayer(null);
            }
            return;
        }
        if (exoPlayer != null) {
            try {
                currentPositionMs = exoPlayer.getCurrentPosition();
                if (exoPlayerListener != null) {
                    exoPlayer.removeListener(exoPlayerListener);
                }
                exoPlayer.stop();
                exoPlayer.release();
            } catch (Exception ignored) {}
            exoPlayer = null;
        }
    }

    private void releaseVlcPlayer(boolean preserveForBackground) {
        if (preserveForBackground && isBackgroundPlayActive && isVlcActive && vlcPlayer != null) {
            BackgroundPlaybackManager.getInstance().setForegroundUiAttached(false);
            BackgroundPlaybackManager.getInstance().setForegroundVlcListener(null);
            try {
                vlcPlayer.detachViews();
            } catch (Exception ignored) {}
            return;
        }
        abandonVlcAudioFocus();
        BackgroundPlaybackManager.getInstance().setForegroundVlcListener(null);
        final org.videolan.libvlc.MediaPlayer playerToRelease = vlcPlayer;
        final org.videolan.libvlc.LibVLC vlcToRelease = libVLC;
        final ParcelFileDescriptor pfdToClose = activeVlcPfd;
        activeVlcPfd = null;
        if (playerToRelease != null) {
            try {
                currentPositionMs = playerToRelease.getTime();
            } catch (Exception ignored) {}
            try {
                playerToRelease.setEventListener(null);
                playerToRelease.detachViews();
            } catch (Exception ignored) {}
        }
        vlcPlayer = null;
        libVLC = null;

        if (playerToRelease != null || vlcToRelease != null || pfdToClose != null) {
            BackgroundPlaybackManager.releaseVlcAsync(playerToRelease, vlcToRelease, pfdToClose);
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
                        String key = videoPath != null && !videoPath.isEmpty() ? videoPath : videoUriStr;
                        String cachedChannels = channels > 0 ? (channels + " Channels" + chStr) : "Stereo";
                        MediaRepository.updateAudioCodecInCache(getApplicationContext(), key, mime, cachedChannels);
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
            if (isCurrentPlayerPaused() && !isYouTubeActive) {
                stepSingleFrame(-1);
                return;
            }
            int stepSec = prefs != null ? prefs.getInt("double_tap_seek_step_sec", 10) : 10;
            long stepMs = stepSec * 1000L;
            if (isYouTubeActive) {
                long target = Math.max(0, youtubeCurrentPositionMs - stepMs);
                if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                showGestureHud("● " + stepSec + "s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-" + stepSec + "s", (int) (target * 100 / Math.max(1, youtubeDurationMs)));
                return;
            }
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            long target = Math.max(0, cur - stepMs);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
            else if (exoPlayer != null) exoPlayer.seekTo(target);
            showGestureHud("● " + stepSec + "s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-" + stepSec + "s", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });
        findViewById(R.id.btn_rewind).setOnLongClickListener(v -> {
            stepSingleFrame(-1);
            return true;
        });

        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            if (isCurrentPlayerPaused() && !isYouTubeActive) {
                stepSingleFrame(1);
                return;
            }
            int stepSec = prefs != null ? prefs.getInt("double_tap_seek_step_sec", 10) : 10;
            long stepMs = stepSec * 1000L;
            if (isYouTubeActive) {
                long target = Math.min(youtubeDurationMs, youtubeCurrentPositionMs + stepMs);
                if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                showGestureHud("● " + stepSec + "s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+" + stepSec + "s", (int) (target * 100 / Math.max(1, youtubeDurationMs)));
                return;
            }
            long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
            long target = Math.min(totalDurationMs, cur + stepMs);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
            else if (exoPlayer != null) exoPlayer.seekTo(target);
            showGestureHud("● " + stepSec + "s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+" + stepSec + "s", (int) (target * 100 / Math.max(1, totalDurationMs)));
        });
        findViewById(R.id.btn_forward).setOnLongClickListener(v -> {
            stepSingleFrame(1);
            return true;
        });

        if (btnCast != null) {
            btnCast.setOnClickListener(v -> NetworkCastHelper.showRendererPickerDialog(this, libVLC, renderer -> {
                if (activeCastRenderer != null && activeCastRenderer != renderer) {
                    try { activeCastRenderer.release(); } catch (Throwable ignored) {}
                }
                activeCastRenderer = renderer;
                long curPos = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : currentPositionMs)
                        : (exoPlayer != null ? exoPlayer.getCurrentPosition() : currentPositionMs);
                if (renderer == null) {
                    if (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased()) {
                        try { vlcPlayer.setRenderer(null); } catch (Throwable ignored) {}
                    }
                    showGestureHud("● CAST OFF", DotMatrixIconView.TYPE_SEEK_FORWARD, "LOCAL DEVICE", 100);
                } else {
                    String name = renderer.displayName != null ? renderer.displayName : renderer.name;
                    if (!isVlcActive) {
                        startVlcPlayer(curPos);
                    } else if (vlcPlayer != null && !vlcPlayer.isReleased()) {
                        try { vlcPlayer.setRenderer(renderer); } catch (Throwable ignored) {}
                    }
                    showGestureHud("● CASTING", DotMatrixIconView.TYPE_SEEK_FORWARD,
                            name != null ? name.toUpperCase(Locale.US) : "WI-FI RENDERER", 100);
                }
            }));
        }

        if (btnDecoderMode != null) {
            btnDecoderMode.setOnClickListener(v -> {
                if (isYouTubeActive) {
                    Toast.makeText(this, "YouTube Web Engine Active", Toast.LENGTH_SHORT).show();
                    updateDecoderMenuPills();
                    return;
                }
                long cur = (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased()) ? vlcPlayer.getTime() : (exoPlayer != null ? exoPlayer.getCurrentPosition() : currentPositionMs);
                if (isVlcActive) {
                    userForcedHwDecoder = true;
                    hasAutoFallenBackToVlc = false;
                    Toast.makeText(this, "Switching to Hardware (HW+) Decoder", Toast.LENGTH_SHORT).show();
                    startExoPlayer(cur);
                } else {
                    userForcedHwDecoder = false;
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
        updateBackgroundPlayUi();

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
            btnQuickEqualizer.setOnClickListener(v -> showFloatingEqualizer());
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

        if (btnSkipIntro != null) {
            btnSkipIntro.setOnClickListener(v -> {
                long targetSeek = introEndMs > 0 ? introEndMs : 85000L;
                if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setTime(targetSeek);
                } else if (exoPlayer != null) {
                    exoPlayer.seekTo(targetSeek);
                }
                videoSeekBar.setProgress((int) targetSeek);
                updateTimeDisplay(targetSeek);
                btnSkipIntro.setVisibility(View.GONE);
                hasSkippedIntroForCurrent = true;
                showGestureHud("● INTRO SKIPPED", DotMatrixIconView.TYPE_SEEK_FORWARD, "+85S", (int) (targetSeek * 100 / Math.max(1, totalDurationMs)));
            });
        }

        setupRatioPanelListeners();
        setupSpeedPanelListeners();
        setupMoreMenuPanelListeners();
        setupVisualEnhancerListeners();
        setupSleepTimerListeners();
        setupOthersPanelListeners();

        if (timeCurrentText != null) {
            timeCurrentText.setClickable(true);
            timeCurrentText.setOnClickListener(v -> showJumpToTimestampDialog());
        }

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

                    // Instant 0ms visual feedback: show cached frame or current TextureView frame immediately
                    if (previewThumbnail != null) {
                        long sec = seekBar.getProgress() / 1000;
                        Bitmap cached = previewBitmapCache.get(sec);
                        if (cached != null) {
                            previewThumbnail.setImageBitmap(cached);
                        } else {
                            try {
                                if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() instanceof android.view.TextureView) {
                                    android.view.TextureView tv = (android.view.TextureView) exoPlayerView.getVideoSurfaceView();
                                    Bitmap liveFrame = tv.getBitmap(240, 135);
                                    if (liveFrame != null) {
                                        previewThumbnail.setImageBitmap(liveFrame);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    extractAndShowPreviewFrame(seekBar.getProgress());
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTrackingTouch = false;
                pendingPreviewTime.set(-1);
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
            if (is2xBoostActive) {
                is2xBoostActive = false;
                applySpeed(preBoostSpeed);
            }
            topControlsBar.setVisibility(View.GONE);
            if (layoutQuickActionsExpanded != null) layoutQuickActionsExpanded.setVisibility(View.GONE);
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.GONE);
            closeAllPanels();
            bottomControlsBar.setVisibility(View.GONE);
            showFloatingLockButton();
            showGestureHud("● SCREEN LOCK", DotMatrixIconView.TYPE_LOCK, "HOLD VOL +/- TO SKIP", 100);
        } else {
            hideHandler.removeCallbacks(hideFloatingLockRunnable);
            if (btnFloatingLock != null) btnFloatingLock.setVisibility(View.GONE);
            topControlsBar.setVisibility(View.VISIBLE);
            bottomControlsBar.setVisibility(View.VISIBLE);
            if (btnQuickScreenshot != null) {
                boolean showShot = prefs == null || prefs.getBoolean("show_screenshot_btn", true);
                btnQuickScreenshot.setVisibility(showShot ? View.VISIBLE : View.GONE);
            }
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

    private void applyVolumeBoost(int boostPercent) {
        this.volumeBoostPercent = Math.max(0, Math.min(100, boostPercent));
        this.isAudioBoosted = (this.volumeBoostPercent > 0);
        if (isVlcActive && vlcPlayer != null) {
            try {
                vlcPlayer.setVolume(isMuted ? 0 : (100 + this.volumeBoostPercent));
            } catch (Exception ignored) {}
        } else if (exoPlayer != null) {
            try {
                AudioEffectManager.getInstance().setVolumeBoostGain(this.volumeBoostPercent, this);
                exoPlayer.setVolume(isMuted ? 0.0f : 1.0f);
            } catch (Exception ignored) {}
        }
    }

    private void updateSecondarySubtitle(long pos) {
        if (tvSecondarySubtitle != null) {
            if (secondarySubtitleCues != null && !secondarySubtitleCues.isEmpty()) {
                long shiftedPos = Math.max(0, pos - subtitleDelayMs);
                String cueText = SubtitleParser.getCueTextAt(secondarySubtitleCues, shiftedPos);
                if (cueText != null && !cueText.isEmpty()) {
                    boolean cleanSdh = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE)
                            .getBoolean("pref_sub_clean_sdh", false);
                    if (cleanSdh) {
                        cueText = SubtitleParser.cleanSdh(cueText);
                    }
                    if (cueText != null && !cueText.isEmpty()) {
                        tvSecondarySubtitle.setText(cueText);
                        tvSecondarySubtitle.setVisibility(View.VISIBLE);
                    } else {
                        tvSecondarySubtitle.setVisibility(View.GONE);
                    }
                } else {
                    tvSecondarySubtitle.setVisibility(View.GONE);
                }
            } else {
                tvSecondarySubtitle.setVisibility(View.GONE);
            }
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (isVlcActive && vlcPlayer != null) {
            vlcPlayer.setVolume(isMuted ? 0 : (100 + this.volumeBoostPercent));
        } else if (exoPlayer != null) {
            AudioEffectManager.getInstance().setVolumeBoostGain(isMuted ? 0 : this.volumeBoostPercent, this);
            exoPlayer.setVolume(isMuted ? 0.0f : 1.0f);
        }
        if (btnQuickMute != null) {
            btnQuickMute.setImageResource(isMuted ? R.drawable.ic_volume_mute : R.drawable.ic_volume_high);
        }
        int dotType = isMuted ? DotMatrixIconView.TYPE_VOLUME_MUTE : DotMatrixIconView.TYPE_VOLUME;
        showGestureHud("● AUDIO", dotType, isMuted ? "MUTED" : "UNMUTED", isMuted ? 0 : 100);
        scheduleHideControls();
    }

    private void toggleBackgroundPlay() {
        isBackgroundPlayActive = !isBackgroundPlayActive;
        BackgroundPlaybackManager.getInstance().setBackgroundPlayActive(isBackgroundPlayActive);
        updateBackgroundPlayUi();
        showGestureHud("● BACKGROUND PLAY", DotMatrixIconView.TYPE_SEEK_FORWARD, isBackgroundPlayActive ? "ACTIVE" : "OFF", isBackgroundPlayActive ? 100 : 0);
        Toast.makeText(this, isBackgroundPlayActive ? "Background Playback: ON" : "Background Playback: OFF", Toast.LENGTH_SHORT).show();
        scheduleHideControls();
    }

    private void updateBackgroundPlayUi() {
        if (btnQuickBackground != null) {
            btnQuickBackground.setImageResource(isBackgroundPlayActive ? R.drawable.ic_headphones_active : R.drawable.ic_headphones);
        }
        if (ivMenuBackgroundPlay != null) {
            ivMenuBackgroundPlay.setImageResource(isBackgroundPlayActive ? R.drawable.ic_headphones_active : R.drawable.ic_headphones);
            ivMenuBackgroundPlay.setBackgroundResource(isBackgroundPlayActive ? R.drawable.bg_circle_red_active : R.drawable.bg_circle_dark);
        }
        if (tvMenuBackgroundPlay != null) {
            tvMenuBackgroundPlay.setTextColor(isBackgroundPlayActive ? Color.parseColor("#D71921") : Color.WHITE);
            tvMenuBackgroundPlay.setText(isBackgroundPlayActive ? "BG: ON" : "BG PLAY");
        }
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
            if (isNightModeActive) {
                btnQuickNightMode.setColorFilter(Color.parseColor("#D71921"));
            } else {
                btnQuickNightMode.clearColorFilter();
            }
        }
        showGestureHud("● NIGHT MODE", DotMatrixIconView.TYPE_SEEK_FORWARD, isNightModeActive ? "ACTIVE" : "OFF", isNightModeActive ? 100 : 0);
        scheduleHideControls();
    }

    private void toggleAbRepeat() {
        long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        TextView tvAb = findViewById(R.id.tv_menu_ab_repeat_status);
        if (repeatPointA < 0) {
            repeatPointA = cur;
            if (videoSeekBar != null) videoSeekBar.setAbRepeatRange(repeatPointA, -1, totalDurationMs);
            showGestureHud("● A-B REPEAT", DotMatrixIconView.TYPE_SEEK_FORWARD, "POINT A (" + formatTime(repeatPointA) + ")", 50);
            Toast.makeText(this, "A-B Repeat: Point A set to " + formatTime(repeatPointA), Toast.LENGTH_SHORT).show();
            if (tvAb != null) tvAb.setText("A: " + formatTime(repeatPointA));
        } else if (repeatPointB < 0) {
            if (cur > repeatPointA) {
                repeatPointB = cur;
                if (videoSeekBar != null) videoSeekBar.setAbRepeatRange(repeatPointA, repeatPointB, totalDurationMs);
                showGestureHud("● A-B REPEAT", DotMatrixIconView.TYPE_SEEK_FORWARD, "LOOPING A-B", 100);
                Toast.makeText(this, "A-B Repeat: Point B set (" + formatTime(repeatPointA) + " - " + formatTime(repeatPointB) + ")", Toast.LENGTH_SHORT).show();
                if (tvAb != null) tvAb.setText(formatTime(repeatPointA) + "-" + formatTime(repeatPointB));
            } else {
                repeatPointA = cur;
                if (videoSeekBar != null) videoSeekBar.setAbRepeatRange(repeatPointA, -1, totalDurationMs);
                Toast.makeText(this, "Point B must be after Point A. Point A reset.", Toast.LENGTH_SHORT).show();
                if (tvAb != null) tvAb.setText("A: " + formatTime(repeatPointA));
            }
        } else {
            repeatPointA = -1;
            repeatPointB = -1;
            if (videoSeekBar != null) videoSeekBar.clearAbRepeatRange();
            showGestureHud("● A-B REPEAT", DotMatrixIconView.TYPE_SEEK_FORWARD, "CLEARED / OFF", 0);
            Toast.makeText(this, "A-B Repeat Cleared", Toast.LENGTH_SHORT).show();
            if (tvAb != null) tvAb.setText("OFF");
        }
        scheduleHideControls();
    }

    private void toggleMirror() {
        isMirrored = !isMirrored;
        applyVideoTransforms();
        applyAspectRatio();
        if (btnQuickMirror != null) {
            if (isMirrored) {
                btnQuickMirror.setColorFilter(Color.parseColor("#D71921"));
            } else {
                btnQuickMirror.clearColorFilter();
            }
        }
        showGestureHud("● SCREEN", DotMatrixIconView.TYPE_SEEK_FORWARD, isMirrored ? "MIRRORED" : "NORMAL", 100);
        scheduleHideControls();
    }

    private void takeQuickScreenshot() {
        final long capturePosMs = isVlcActive
                ? (vlcPlayer != null ? vlcPlayer.getTime() : 0)
                : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
        Bitmap textureBmp = null;
        if (exoPlayerView != null && exoPlayerView.getVideoSurfaceView() instanceof android.view.TextureView) {
            textureBmp = ((android.view.TextureView) exoPlayerView.getVideoSurfaceView()).getBitmap();
        }
        final Bitmap initialBitmap = textureBmp;

        previewExecutor.execute(() -> {
            try {
                Bitmap bitmap = initialBitmap;
                if (bitmap == null) {
                    synchronized (previewLock) {
                        if (previewRetriever != null) {
                            bitmap = previewRetriever.getFrameAtTime(capturePosMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST);
                        }
                    }
                }
                if (bitmap == null) {
                    runOnUiThread(() -> {
                        if (isActivityAlive()) {
                            Toast.makeText(this, "Screenshot unavailable for active stream", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                String fileName = "NothingPlayer_" + System.currentTimeMillis() + ".jpg";
                boolean saved = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                            android.os.Environment.DIRECTORY_PICTURES + "/Screenshots");
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
                    Uri itemUri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (itemUri != null) {
                        try (java.io.OutputStream out = getContentResolver().openOutputStream(itemUri)) {
                            if (out != null) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                                out.flush();
                                saved = true;
                            }
                        } finally {
                            values.clear();
                            values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                            getContentResolver().update(itemUri, values, null, null);
                        }
                    }
                } else {
                    File dir = new File(android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_PICTURES), "Screenshots");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, fileName);
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                        fos.flush();
                        saved = true;
                    }
                    android.media.MediaScannerConnection.scanFile(
                            getApplicationContext(), new String[]{file.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
                }

                if (bitmap != initialBitmap) {
                    bitmap.recycle();
                }

                final boolean ok = saved;
                runOnUiThread(() -> {
                    if (!isActivityAlive()) return;
                    if (ok) {
                        showGestureHud("● SCREENSHOT", DotMatrixIconView.TYPE_SEEK_FORWARD, "SAVED TO GALLERY", 100);
                        Toast.makeText(this, "Screenshot saved to Pictures/Screenshots", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Could not save screenshot", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isActivityAlive()) {
                        Toast.makeText(this, "Screenshot error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
            hideHandler.removeCallbacks(hideControlsRunnable);
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
        if (isSubtitleGuardEnabled && isVideoCroppedOrZoomed()) {
            applySubtitleGuardOffset();
        } else {
            videoPanOffsetX = 0f;
            videoPanOffsetY = 0f;
            applyVideoTransforms();
            updatePanUI();
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
    }

    private void applyCustomAspectRatio(float targetAspect, String label) {
        targetCustomAspect = targetAspect;
        currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
        currentRatioLabel = label;
        currentVideoScale = 1.0f;
        if (isSubtitleGuardEnabled && isVideoCroppedOrZoomed()) {
            applySubtitleGuardOffset();
        } else {
            videoPanOffsetX = 0f;
            videoPanOffsetY = 0f;
            applyVideoTransforms();
            updatePanUI();
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

        // Vertical Pan & Subtitle Guard Controls
        tvPanOffsetLabel = findViewById(R.id.tv_pan_offset_label);
        updatePanUI();

        View btnPanSubtitles = findViewById(R.id.btn_pan_subtitles);
        if (btnPanSubtitles != null) {
            btnPanSubtitles.setOnClickListener(v -> applySubtitleGuardOffset());
        }

        View btnPanCenter = findViewById(R.id.btn_pan_center);
        if (btnPanCenter != null) {
            btnPanCenter.setOnClickListener(v -> resetVideoPan());
        }

        View btnPanStepUp = findViewById(R.id.btn_pan_step_up);
        if (btnPanStepUp != null) {
            btnPanStepUp.setOnClickListener(v -> {
                Point realSize = getRealScreenSize();
                int screenH = Math.min(realSize.x, realSize.y);
                applyPanDelta(0f, -(screenH * 0.05f));
            });
        }

        View btnPanStepDown = findViewById(R.id.btn_pan_step_down);
        if (btnPanStepDown != null) {
            btnPanStepDown.setOnClickListener(v -> {
                Point realSize = getRealScreenSize();
                int screenH = Math.min(realSize.x, realSize.y);
                applyPanDelta(0f, screenH * 0.05f);
            });
        }

        SwitchCompat switchSubtitleGuard = findViewById(R.id.switch_subtitle_guard);
        if (switchSubtitleGuard != null) {
            switchSubtitleGuard.setChecked(isSubtitleGuardEnabled);
            switchSubtitleGuard.setOnCheckedChangeListener((btn, isChecked) -> {
                isSubtitleGuardEnabled = isChecked;
                getSharedPreferences("nothing_player_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("pref_subtitle_guard", isChecked)
                        .apply();
                if (isChecked && isVideoCroppedOrZoomed()) {
                    applySubtitleGuardOffset();
                } else if (!isChecked && Math.abs(videoPanOffsetY) > 0) {
                    resetVideoPan();
                }
            });
        }

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

    private void applyVideoTransforms() {
        float mirrorSign = isMirrored ? -1.0f : 1.0f;
        if (exoPlayerView != null) {
            View surface = exoPlayerView.getVideoSurfaceView();
            if (surface != null) {
                surface.setScaleX(currentVideoScale * mirrorSign);
                surface.setScaleY(currentVideoScale);
                surface.setTranslationX(videoPanOffsetX);
                surface.setTranslationY(videoPanOffsetY);
            } else {
                exoPlayerView.setScaleX(currentVideoScale * mirrorSign);
                exoPlayerView.setScaleY(currentVideoScale);
                exoPlayerView.setTranslationX(videoPanOffsetX);
                exoPlayerView.setTranslationY(videoPanOffsetY);
            }
        }

        if (isVlcActive && vlcVideoLayout != null) {
            vlcVideoLayout.setScaleX(currentVideoScale * mirrorSign);
            vlcVideoLayout.setScaleY(currentVideoScale);
            vlcVideoLayout.setTranslationX(videoPanOffsetX);
            vlcVideoLayout.setTranslationY(videoPanOffsetY);
        }

        if (isYouTubeActive && youtubeStreamView != null) {
            youtubeStreamView.setScaleX(currentVideoScale * mirrorSign);
            youtubeStreamView.setScaleY(currentVideoScale);
            youtubeStreamView.setTranslationX(videoPanOffsetX);
            youtubeStreamView.setTranslationY(videoPanOffsetY);
        }
    }

    private void applyPanDelta(float dx, float dy) {
        Point viewport = getViewportSize();
        int screenW = viewport.x;
        int screenH = viewport.y;

        float maxPanX = screenW * Math.max(0.25f, (currentVideoScale - 1f) * 0.6f);
        float maxPanY = screenH * Math.max(0.45f, (currentVideoScale - 1f) * 0.6f);

        videoPanOffsetX = Math.max(-maxPanX, Math.min(maxPanX, videoPanOffsetX + dx));
        videoPanOffsetY = Math.max(-maxPanY, Math.min(maxPanY, videoPanOffsetY + dy));

        applyVideoTransforms();
        updatePanUI();

        int panPercentY = screenH > 0 ? (int) ((-videoPanOffsetY / (float) screenH) * 100f) : 0;
        String panText = (panPercentY > 0 ? "+" : "") + panPercentY + "%";
        showGestureHud("● PAN OFFSET", DotMatrixIconView.TYPE_SEEK_FORWARD, "PAN Y " + panText, Math.min(100, Math.abs(panPercentY) * 2));
    }

    private void applySubtitleGuardOffset() {
        Point viewport = getViewportSize();
        int rootW = viewport.x;
        int rootH = viewport.y;

        float effectiveVideoAspect = 16f / 9f;
        if (videoWidth > 0 && videoHeight > 0) {
            effectiveVideoAspect = (float) videoWidth / (float) videoHeight;
        }

        float screenAspect = (float) rootW / (float) Math.max(1, rootH);
        float targetHeight;
        if (effectiveVideoAspect <= screenAspect) {
            targetHeight = rootW / effectiveVideoAspect;
        } else {
            targetHeight = rootH;
        }

        if (currentVideoScale > 1.0f) {
            targetHeight *= currentVideoScale;
        }

        float verticalOverflow = Math.max(0, targetHeight - rootH);
        if (verticalOverflow > 10f) {
            videoPanOffsetY = -(verticalOverflow / 2f);
        } else {
            videoPanOffsetY = -(rootH * 0.12f);
        }
        videoPanOffsetX = 0f;

        applyVideoTransforms();
        updatePanUI();
        showGestureHud("● SUBTITLE GUARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "SUBTITLES SAFE", 100);
    }

    private void resetVideoPan() {
        videoPanOffsetX = 0f;
        videoPanOffsetY = 0f;
        applyVideoTransforms();
        updatePanUI();
        showGestureHud("● PAN RESET", DotMatrixIconView.TYPE_SEEK_FORWARD, "CENTERED", 100);
    }

    private void updatePanUI() {
        if (tvPanOffsetLabel != null) {
            int screenH = getViewportSize().y;
            int panPercentY = screenH > 0 ? (int) ((-videoPanOffsetY / (float) screenH) * 100f) : 0;
            String text = "PAN: " + (panPercentY > 0 ? "+" : "") + panPercentY + "%";
            tvPanOffsetLabel.setText(text);
            if (panPercentY != 0) {
                tvPanOffsetLabel.setTextColor(Color.parseColor("#D71921"));
            } else {
                tvPanOffsetLabel.setTextColor(Color.WHITE);
            }
        }
    }

    private boolean isVideoCroppedOrZoomed() {
        return currentVideoScale > 1.05f
                || currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                || "FILL".equalsIgnoreCase(currentRatioLabel)
                || "ZOOM".equalsIgnoreCase(currentRatioLabel)
                || "21:9".equalsIgnoreCase(currentRatioLabel)
                || "2.35:1".equalsIgnoreCase(currentRatioLabel)
                || "2.39:1".equalsIgnoreCase(currentRatioLabel);
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
        } else {
            applyPlaybackSpeedWithPitchPreservation(playbackSpeed);
        }
        updateSpeedUI(playbackSpeed);
        if (rememberSpeed) {
            getSharedPreferences("nothing_player_prefs", MODE_PRIVATE).edit().putFloat("saved_playback_speed", playbackSpeed).apply();
        }
        saveCurrentSeriesSettings();
        showGestureHud("● SPEED: " + playbackSpeed + "X", DotMatrixIconView.TYPE_SEEK_FORWARD, playbackSpeed + "X", (int) (playbackSpeed * 25));
    }

    @SuppressWarnings("deprecation")
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

        View chaptersItem = findViewById(R.id.item_menu_chapters);
        if (chaptersItem != null) {
            chaptersItem.setOnClickListener(v -> {
                panelMoreMenu.setVisibility(View.GONE);
                showChaptersDialog();
            });
        }

        View subDelayItem = findViewById(R.id.item_menu_sub_delay);
        if (subDelayItem != null) {
            subDelayItem.setOnClickListener(v -> {
                panelMoreMenu.setVisibility(View.GONE);
                showSubtitleDelayDialog();
            });
        }

        View bgItem = findViewById(R.id.item_menu_background_play);
        ivMenuBackgroundPlay = findViewById(R.id.iv_menu_background_play);
        tvMenuBackgroundPlay = findViewById(R.id.tv_menu_background_play);
        if (bgItem != null) {
            bgItem.setOnClickListener(v -> toggleBackgroundPlay());
        }
        updateBackgroundPlayUi();

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
                if (panelMoreMenu != null) panelMoreMenu.setVisibility(View.GONE);
                showFloatingEqualizer();
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
            seekbarMenuVolume.setMax(200);
            int curSysVol = audioManager != null ? audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) : 0;
            int initProg = (int) ((float) curSysVol / maxVolume * 100) + volumeBoostPercent;
            seekbarMenuVolume.setProgress(initProg);
            seekbarMenuVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && audioManager != null) {
                        if (progress <= 100) {
                            int vol = Math.round((progress / 100f) * maxVolume);
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
                            applyVolumeBoost(0);
                            if (tvMenuVolumeVal != null) {
                                tvMenuVolumeVal.setText(progress + "%");
                                tvMenuVolumeVal.setTextColor(Color.WHITE);
                            }
                        } else {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
                            applyVolumeBoost(progress - 100);
                            if (tvMenuVolumeVal != null) {
                                tvMenuVolumeVal.setText(progress + "% (BOOST)");
                                tvMenuVolumeVal.setTextColor(Color.parseColor("#D71921"));
                            }
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Decoder Pills (HW vs SW)
        if (btnMenuDecoderHw != null) {
            btnMenuDecoderHw.setOnClickListener(v -> {
                if (isYouTubeActive) {
                    Toast.makeText(this, "YouTube Web Engine Active", Toast.LENGTH_SHORT).show();
                    updateDecoderMenuPills();
                    return;
                }
                if (isVlcActive) {
                    userForcedHwDecoder = true;
                    hasAutoFallenBackToVlc = false;
                    long cur = (vlcPlayer != null && !vlcPlayer.isReleased()) ? vlcPlayer.getTime() : currentPositionMs;
                    startExoPlayer(cur);
                }
                updateDecoderMenuPills();
                Toast.makeText(this, "Hardware (HW+) Decoder Active", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnMenuDecoderSw != null) {
            btnMenuDecoderSw.setOnClickListener(v -> {
                if (isYouTubeActive) {
                    Toast.makeText(this, "YouTube Web Engine Active", Toast.LENGTH_SHORT).show();
                    updateDecoderMenuPills();
                    return;
                }
                userForcedHwDecoder = false;
                if (!isVlcActive) {
                    long cur = exoPlayer != null ? exoPlayer.getCurrentPosition() : currentPositionMs;
                    startVlcPlayer(cur);
                }
                updateDecoderMenuPills();
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

        boolean appliedGlEffects = applyVisualFilterEffectOnExo();
        if (visualEnhancerOverlay != null) {
            if (appliedGlEffects) {
                visualEnhancerOverlay.setVisibility(View.GONE);
            } else {
                visualEnhancerOverlay.setBackgroundColor(tintColor);
                visualEnhancerOverlay.setVisibility(tintColor != Color.TRANSPARENT ? View.VISIBLE : View.GONE);
            }
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

    private boolean applyVisualFilterEffectOnExo() {
        if (isVlcActive || isYouTubeActive || exoPlayer == null) {
            return false;
        }
        try {
            List<Effect> effects = new ArrayList<>();
            switch (currentVisualFilter != null ? currentVisualFilter : "ORIGINAL") {
                case "HDR":
                    effects.add(new Contrast(0.22f));
                    effects.add(new HslAdjustment.Builder().adjustSaturation(18f).build());
                    break;
                case "CLEAR":
                    effects.add(new Contrast(0.14f));
                    effects.add(new Brightness(0.05f));
                    break;
                case "ULTRA CLEAR":
                    effects.add(new Contrast(0.18f));
                    effects.add(new Brightness(0.10f));
                    effects.add(new HslAdjustment.Builder().adjustSaturation(10f).build());
                    break;
                case "ARCTIC BLUE":
                    effects.add(new RgbAdjustment.Builder().setBlueScale(1.14f).setRedScale(0.92f).build());
                    break;
                case "WARM GLOW":
                    effects.add(new RgbAdjustment.Builder().setRedScale(1.14f).setBlueScale(0.88f).build());
                    break;
                case "CINEMATIC":
                    effects.add(new Contrast(0.25f));
                    effects.add(new HslAdjustment.Builder().adjustSaturation(-12f).build());
                    break;
                case "ORIGINAL":
                default:
                    break;
            }
            exoPlayer.setVideoEffects(effects);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
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
        if (btnTimer45 != null) btnTimer45.setOnClickListener(v -> startSleepTimer(45));
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
        if (btnTimer45 != null) btnTimer45.setBackgroundResource(!isEndVid && selectedMinutes == 45 ? act : inact);
        if (btnTimer60 != null) btnTimer60.setBackgroundResource(!isEndVid && selectedMinutes == 60 ? act : inact);
        if (btnTimer90 != null) btnTimer90.setBackgroundResource(!isEndVid && selectedMinutes == 90 ? act : inact);
        if (btnTimerEndOfVideo != null) btnTimerEndOfVideo.setBackgroundResource(isEndVid ? act : inact);
    }

        public void setMonoDownmix(boolean enabled) {
        this.isMonoDownmixEnabled = enabled;
        getSharedPreferences("nothing_player_prefs", MODE_PRIVATE)
            .edit().putBoolean("earbud_mono_downmix", enabled).apply();
        applyAudioBalance();
        showGestureHud("\u25cf EARBUD AUDIO", DotMatrixIconView.TYPE_SEEK_FORWARD, enabled ? "MONO DOWNMIX ON" : "STEREO OFF", 100);
    }

    private void cancelSleepTimer() {
        sleepTimerEndOfVideo = false;
        sleepTimerEndTimeMs = 0;
        BackgroundPlaybackManager.getInstance().cancelSleepTimer();
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        restoreActiveVolume();
        if (tvSleepTimerStatus != null) tvSleepTimerStatus.setText("STATUS: OFF");
        if (tvMenuTimerLabel != null) tvMenuTimerLabel.setText("TIMER");
        updateSleepTimerButtons(0, false);
        showGestureHud("\u25cf SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, "TIMER OFF", 0);
    }

    private void startSleepTimer(int minutes) {
        sleepTimerEndOfVideo = false;
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        }
        sleepTimerEndTimeMs = System.currentTimeMillis() + (minutes * 60L * 1000L);
        BackgroundPlaybackManager.getInstance().startSleepTimer(minutes);
        updateSleepTimerButtons(minutes, false);
        startSleepTimerUiLoop();
        showGestureHud("\u25cf SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, minutes + " MINUTES SET", 100);
    }

    private void startSleepTimerUiLoop() {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        }
        sleepTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isActivityAlive()) return;
                long remainingMs = sleepTimerEndTimeMs - System.currentTimeMillis();
                // Gradual 60-second audio fade-out
                if (remainingMs <= 60000 && remainingMs > 0 && !isMuted) {
                    float fade = Math.max(0.05f, Math.min(1.0f, (float) remainingMs / 60000.0f));
                    if (exoPlayer != null) exoPlayer.setVolume(fade);
                    if (vlcPlayer != null) vlcPlayer.setVolume((int) (fade * (100 + volumeBoostPercent)));
                }
                if (remainingMs <= 0) {
                    cancelSleepTimer();
                    if (exoPlayer != null && exoPlayer.isPlaying()) exoPlayer.pause();
                    if (isVlcPlayingSafe()) { try { vlcPlayer.pause(); } catch (Exception ignored) {} }
                    if (isYouTubeActive && youtubeStreamView != null) {
                        youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
                    }
                    updatePlayPauseButtonState();
                    restoreActiveVolume();
                    showGestureHud("\u25cf SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, "SLEEP TIMEOUT", 100);
                    Toast.makeText(ExoVideoPlayerActivity.this, "Sleep timer expired. Screen sleeping.", Toast.LENGTH_SHORT).show();
                    try {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.screenBrightness = 0.01f;
                        getWindow().setAttributes(lp);
                    } catch (Exception ignored) {}
                    finish();
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
    }

    private void setSleepTimerEndOfVideo() {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        sleepTimerEndOfVideo = true;
        sleepTimerEndTimeMs = 0;
        BackgroundPlaybackManager.getInstance().setSleepTimerEndOfVideo(true);
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
        java.util.List<String> options = java.util.Arrays.asList(
                "MOVE TO RECYCLE BIN (30-DAY RECOVERY)",
                "MOVE TO PRIVATE VAULT (PIN LOCKED)",
                "PERMANENTLY DELETE FROM DEVICE"
        );
        NothingDialogHelper.showSelectionDialog(this, "MANAGE VIDEO", options, 0, (which, item) -> {
            if (which == 0) {
                com.nothing.player.MediaItem mi = new com.nothing.player.MediaItem();
                mi.path = videoPath;
                mi.title = videoTitle;
                mi.duration = totalDurationMs;
                if (TrashManager.moveToTrash(this, mi)) {
                    Toast.makeText(this, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Could not move file to trash", Toast.LENGTH_SHORT).show();
                }
            } else if (which == 1) {
                com.nothing.player.MediaItem mi = new com.nothing.player.MediaItem();
                mi.path = videoPath;
                mi.title = videoTitle;
                mi.duration = totalDurationMs;
                if (VaultManager.moveToVault(this, mi)) {
                    Toast.makeText(this, "Moved to Private Vault", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Could not move file to vault", Toast.LENGTH_SHORT).show();
                }
            } else if (which == 2) {
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
            }
        });
    }

    private void showDualSubtitleDialog() {
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("SELECT SECONDARY SUBTITLE FILE (.SRT)");
        if (secondarySubtitleCues != null) {
            options.add("DISABLE DUAL SUBTITLE");
        }
        NothingDialogHelper.showSelectionDialog(this, "DUAL / TOP SUBTITLE", options, -1, (which, item) -> {
            if (which == 0) {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    String[] mimes = {"application/x-subrip", "text/plain", "text/*"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
                    startActivityForResult(intent, 9002);
                } catch (Exception e) {
                    Toast.makeText(this, "Error selecting file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (which == 1) {
                secondarySubtitleCues = null;
                secondarySubtitleTitle = null;
                if (tvSecondarySubtitle != null) {
                    tvSecondarySubtitle.setVisibility(View.GONE);
                }
                Toast.makeText(this, "Dual Subtitle Disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9002 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            subtitleIoExecutor.execute(() -> {
                List<SubtitleParser.SubtitleCue> parsedCues = null;
                String resolvedName = "Custom Subtitle";
                String err = null;
                try {
                    try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                        if (is != null) {
                            parsedCues = SubtitleParser.parseSrt(is);
                        }
                    }
                    try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                            if (nameIndex >= 0) resolvedName = cursor.getString(nameIndex);
                        }
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    err = e.getMessage();
                }
                final List<SubtitleParser.SubtitleCue> finalCues = parsedCues;
                final String finalName = resolvedName;
                final String finalErr = err;
                runOnUiThread(() -> {
                    if (!isActivityAlive()) return;
                    if (finalErr != null) {
                        Toast.makeText(this, "Failed to load subtitle: " + finalErr, Toast.LENGTH_SHORT).show();
                    } else if (finalCues != null && !finalCues.isEmpty()) {
                        secondarySubtitleCues = finalCues;
                        searchableSubtitleCues = finalCues;
                        secondarySubtitleTitle = finalName;
                        updateSecondarySubtitle(currentPositionMs);
                        Toast.makeText(this, "Dual Subtitle Enabled: " + finalName, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No valid subtitle cues found in file", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
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
        if (!isActivityAlive()) return;
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

            trackDialog(dialog);
            dialog.show();
        } catch (Exception e) {
            NothingDialogHelper.showConfirmDialog(this, title, message, "CLOSE", null, null, null);
        }
    }

    private void showNothingSelectionDialog(String title, List<String> items, int selectedIndex, OnTrackItemSelectedListener listener) {
        if (!isActivityAlive()) return;
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

            trackDialog(dialog);
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
                String langToSave = format.language != null ? format.language : (format.label != null ? format.label : "");
                savePreferredAudioLanguage(langToSave);

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
                savePreferredAudioLanguage(labels.get(which));
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

        final int searchOnlineIndex = trackLabels.size();
        trackLabels.add("● SEARCH ONLINE SUBTITLES");
        final int subDelayIndex = trackLabels.size();
        trackLabels.add("● SUBTITLE DELAY / SYNC TUNER");
        final int searchDialogueIndex = trackLabels.size();
        trackLabels.add("🔍 SEARCH IN-VIDEO DIALOGUES");
        final int stylingStudioIndex = trackLabels.size();
        trackLabels.add("🎨 SUBTITLE STYLING STUDIO");
        final int dualSubIndex = trackLabels.size();
        trackLabels.add(secondarySubtitleCues != null ? ("● DUAL SUBTITLE: ON [" + secondarySubtitleTitle + "]") : "● DUAL / SECONDARY SUBTITLE (TOP)");
        final int dictIndex = trackLabels.size();
        trackLabels.add("📖 IN-SUBTITLE DICTIONARY & TRANSLATOR");
        final int translateSubIndex = trackLabels.size();
        trackLabels.add("🌐 TRANSLATE & REPLACE SUBTITLE (OFFLINE ML)");

        showNothingSelectionDialog("SELECT SUBTITLE (ESUB / CC)", trackLabels, selectedIndex, which -> {
            if (which == translateSubIndex) {
                triggerSubtitleTranslation();
                return;
            }
            if (which == searchOnlineIndex) {
                showOnlineSubtitleSearchDialog();
                return;
            }
            if (which == subDelayIndex) {
                showSubtitleDelayDialog();
                return;
            }
            if (which == searchDialogueIndex) {
                showInVideoDialogueSearchDialog();
                return;
            }
            if (which == stylingStudioIndex) {
                showSubtitleStylingDialog();
                return;
            }
            if (which == dualSubIndex) {
                showDualSubtitleDialog();
                return;
            }
            if (which == dictIndex) {
                CharSequence currentSub = (tvSecondarySubtitle != null && tvSecondarySubtitle.getVisibility() == View.VISIBLE)
                        ? tvSecondarySubtitle.getText() : null;
                if (currentSub != null && currentSub.length() > 0) {
                    SubtitleDictionaryManager.showWordSelectorDialog(this, currentSub.toString(), (word, sentence) -> {
                        showDotMatrixHud("● LOOKUP: " + word.toUpperCase(Locale.US));
                        SubtitleDictionaryManager.lookupWord(this, word, sentence, new SubtitleDictionaryManager.DictionaryCallback() {
                            @Override
                            public void onResult(SubtitleDictionaryManager.WordDefinition def) {
                                if (!isActivityAlive()) return;
                                SubtitleDictionaryManager.showDictionaryDialog(ExoVideoPlayerActivity.this, def, null);
                            }
                            @Override
                            public void onError(String msg) {
                                if (!isActivityAlive()) return;
                                Toast.makeText(ExoVideoPlayerActivity.this, "Lookup: " + msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                } else {
                    showInVideoDialogueSearchDialog();
                }
                return;
            }
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

        int currentTrackId = vlcPlayer.getSpuTrack();
        int selectedIndex = 0;
        final List<String> labels = new ArrayList<>();
        final List<Integer> trackIds = new ArrayList<>();

        if (tracks != null) {
            for (int i = 0; i < tracks.length; i++) {
                labels.add(tracks[i].name);
                trackIds.add(tracks[i].id);
                if (tracks[i].id == currentTrackId) {
                    selectedIndex = i;
                }
            }
        }

        final int searchOnlineIndex = labels.size();
        labels.add("● SEARCH ONLINE SUBTITLES");
        final int subDelayIndex = labels.size();
        labels.add("● SUBTITLE DELAY / SYNC TUNER");
        final int searchDialogueIndex = labels.size();
        labels.add("🔍 SEARCH IN-VIDEO DIALOGUES");
        final int stylingStudioIndex = labels.size();
        labels.add("🎨 SUBTITLE STYLING STUDIO");
        final int vlcDualSubIndex = labels.size();
        labels.add(secondarySubtitleCues != null ? ("● DUAL SUBTITLE: ON [" + secondarySubtitleTitle + "]") : "● DUAL / SECONDARY SUBTITLE (TOP)");
        final int vlcDictIndex = labels.size();
        labels.add("📖 IN-SUBTITLE DICTIONARY & TRANSLATOR");
        final int vlcTranslateSubIndex = labels.size();
        labels.add("🌐 TRANSLATE & REPLACE SUBTITLE (OFFLINE ML)");

        showNothingSelectionDialog("SELECT SUBTITLE (VLC)", labels, selectedIndex, which -> {
            if (which == vlcTranslateSubIndex) {
                triggerSubtitleTranslation();
                return;
            }
            if (which == searchOnlineIndex) {
                showOnlineSubtitleSearchDialog();
                return;
            }
            if (which == subDelayIndex) {
                showSubtitleDelayDialog();
                return;
            }
            if (which == searchDialogueIndex) {
                showInVideoDialogueSearchDialog();
                return;
            }
            if (which == stylingStudioIndex) {
                showSubtitleStylingDialog();
                return;
            }
            if (which == vlcDualSubIndex) {
                showDualSubtitleDialog();
                return;
            }
            if (which == vlcDictIndex) {
                CharSequence currentSub = (tvSecondarySubtitle != null && tvSecondarySubtitle.getVisibility() == View.VISIBLE)
                        ? tvSecondarySubtitle.getText() : null;
                if (currentSub != null && currentSub.length() > 0) {
                    SubtitleDictionaryManager.showWordSelectorDialog(this, currentSub.toString(), (word, sentence) -> {
                        showDotMatrixHud("● LOOKUP: " + word.toUpperCase(Locale.US));
                        SubtitleDictionaryManager.lookupWord(this, word, sentence, new SubtitleDictionaryManager.DictionaryCallback() {
                            @Override
                            public void onResult(SubtitleDictionaryManager.WordDefinition def) {
                                if (!isActivityAlive()) return;
                                SubtitleDictionaryManager.showDictionaryDialog(ExoVideoPlayerActivity.this, def, null);
                            }
                            @Override
                            public void onError(String msg) {
                                if (!isActivityAlive()) return;
                                Toast.makeText(ExoVideoPlayerActivity.this, "Lookup: " + msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                } else {
                    showInVideoDialogueSearchDialog();
                }
                return;
            }
            vlcPlayer.setSpuTrack(trackIds.get(which));
            Toast.makeText(this, "Selected: " + labels.get(which), Toast.LENGTH_SHORT).show();
        });
    }

    private List<SubtitleParser.SubtitleCue> loadAvailableSrtCuesSync() {
        List<SubtitleParser.SubtitleCue> sourceCues = null;
        if (activeSubtitleUri != null) {
            try {
                if ("file".equalsIgnoreCase(activeSubtitleUri.getScheme())) {
                    sourceCues = SubtitleParser.parseSrt(new File(activeSubtitleUri.getPath()));
                } else {
                    try (java.io.InputStream is = getContentResolver().openInputStream(activeSubtitleUri)) {
                        if (is != null) sourceCues = SubtitleParser.parseSrt(is);
                    }
                }
            } catch (Exception ignored) {}
        }
        if ((sourceCues == null || sourceCues.isEmpty()) && videoPath != null) {
            int dot = videoPath.lastIndexOf('.');
            if (dot > 0) {
                String base = videoPath.substring(0, dot);
                String[] candidates = {base + ".srt", base + ".en.srt", base + ".eng.srt", base + ".hi.srt", base + ".hin.srt", base + ".default.srt"};
                for (String c : candidates) {
                    File srt = new File(c);
                    if (srt.exists()) {
                        sourceCues = SubtitleParser.parseSrt(srt);
                        if (sourceCues != null && !sourceCues.isEmpty()) break;
                    }
                }
            }
        }
        return sourceCues;
    }

    private void triggerSubtitleTranslation() {
        if (searchableSubtitleCues != null && !searchableSubtitleCues.isEmpty()) {
            launchTranslationStudio(searchableSubtitleCues, "Active Subtitle");
            return;
        }
        if (secondarySubtitleCues != null && !secondarySubtitleCues.isEmpty()) {
            launchTranslationStudio(secondarySubtitleCues, "Active Subtitle");
            return;
        }

        showDotMatrixHud("● LOADING SUBTITLE CUES...");
        final Uri mediaUri = resolveMediaUri();
        subtitleIoExecutor.execute(() -> {
            List<SubtitleParser.SubtitleCue> loaded = loadAvailableSrtCuesSync();
            runOnUiThread(() -> {
                if (!isActivityAlive()) return;
                if (loaded != null && !loaded.isEmpty()) {
                    searchableSubtitleCues = loaded;
                    launchTranslationStudio(loaded, "Active Subtitle");
                    return;
                }

                // If no external .srt found, scan and extract embedded subtitle tracks from video container (MKV/MP4)
                showDotMatrixHud("● SCANNING EMBEDDED SUBTITLES...");
                EmbeddedSubtitleExtractor.extractSubtitlesAsync(this, mediaUri, videoPath, new EmbeddedSubtitleExtractor.ExtractionCallback() {
                    @Override
                    public void onSuccess(List<EmbeddedSubtitleExtractor.SubtitleTrackInfo> tracks) {
                        if (!isActivityAlive()) return;
                        if (tracks == null || tracks.isEmpty()) {
                            showNoSubtitleActionDialog();
                            return;
                        }

                        if (tracks.size() == 1) {
                            EmbeddedSubtitleExtractor.SubtitleTrackInfo single = tracks.get(0);
                            showDotMatrixHud("● EXTRACTED " + single.cues.size() + " EMBEDDED CUES");
                            searchableSubtitleCues = single.cues;
                            launchTranslationStudio(single.cues, single.getDisplayLabel());
                        } else {
                            // Multiple embedded subtitle tracks: let user choose which track to translate
                            List<String> trackLabels = new ArrayList<>();
                            for (EmbeddedSubtitleExtractor.SubtitleTrackInfo t : tracks) {
                                trackLabels.add(t.getDisplayLabel());
                            }
                            showNothingSelectionDialog("SELECT EMBEDDED SUBTITLE TO TRANSLATE", trackLabels, 0, which -> {
                                if (!isActivityAlive()) return;
                                EmbeddedSubtitleExtractor.SubtitleTrackInfo chosen = tracks.get(which);
                                searchableSubtitleCues = chosen.cues;
                                launchTranslationStudio(chosen.cues, chosen.getDisplayLabel());
                            });
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!isActivityAlive()) return;
                        showNoSubtitleActionDialog();
                    }
                });
            });
        });
    }

    private void launchTranslationStudio(List<SubtitleParser.SubtitleCue> cuesToTranslate, String sourceLabel) {
        if (!isActivityAlive() || cuesToTranslate == null || cuesToTranslate.isEmpty()) return;
        String titleOrPath = (videoPath != null) ? videoPath : (videoTitle != null ? videoTitle : "video");

        SubtitleTranslationManager.showTranslationDialog(
                this,
                titleOrPath,
                cuesToTranslate,
                (srtFile, translatedCues, replaceOriginal) -> {
                    if (!isActivityAlive()) return;
                    if (replaceOriginal) {
                        secondarySubtitleCues = null;
                        secondarySubtitleTitle = null;
                        if (tvSecondarySubtitle != null) {
                            tvSecondarySubtitle.setVisibility(View.GONE);
                        }
                        searchableSubtitleCues = translatedCues;
                        applyExternalSubtitle(srtFile);
                        showDotMatrixHud("● SUBTITLE REPLACED: " + srtFile.getName().toUpperCase(Locale.US));
                    } else {
                        secondarySubtitleCues = cuesToTranslate;
                        secondarySubtitleTitle = "ORIGINAL (TOP)";
                        updateSecondarySubtitle(currentPositionMs);
                        searchableSubtitleCues = translatedCues;
                        applyExternalSubtitle(srtFile);
                        showDotMatrixHud("● DUAL SUBTITLE: ORIGINAL (TOP) + TRANSLATED (BOTTOM)");
                    }
                }
        );
    }

    private void showNoSubtitleActionDialog() {
        if (!isActivityAlive()) return;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_nothing_selection);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_selection_dialog_title);
        if (tvTitle != null) tvTitle.setText("NO TEXT SUBTITLE FOUND");

        List<String> options = new ArrayList<>();
        options.add("🎯 1-TAP FETCH EXACT SUBTITLE BY 64-BIT MOVIE HASH");
        options.add("🔍 SEARCH OPENSUBTITLES ONLINE");
        options.add("📂 BROWSE STORAGE FOR LOCAL .SRT FILE");

        LinearLayout container = dialog.findViewById(R.id.layout_track_items_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < options.size(); i++) {
            final int idx = i;
            View itemView = inflater.inflate(R.layout.item_nothing_track_selection, container, false);
            View indicator = itemView.findViewById(R.id.view_track_indicator);
            TextView tvTrack = itemView.findViewById(R.id.tv_track_name);

            tvTrack.setText(options.get(i).toUpperCase(Locale.US));
            if (idx == 0) {
                indicator.setBackgroundResource(R.drawable.bg_circle_red_active);
                tvTrack.setTextColor(Color.parseColor("#D71921"));
            } else {
                indicator.setBackgroundResource(R.drawable.bg_circle_dark);
                tvTrack.setTextColor(Color.WHITE);
            }

            itemView.setOnClickListener(v -> {
                dialog.dismiss();
                if (idx == 0) {
                    autoFetchSubtitleViaHashAndTranslate();
                } else if (idx == 1) {
                    showOnlineSubtitleSearchDialog();
                } else if (idx == 2) {
                    showDualSubtitleDialog();
                }
            });

            container.addView(itemView);
        }

        Button btnCancel = dialog.findViewById(R.id.btn_selection_dialog_cancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        trackDialog(dialog);
        dialog.show();
    }

    private void autoFetchSubtitleViaHashAndTranslate() {
        if (!isActivityAlive()) return;
        final Uri mediaUri = resolveMediaUri();
        showDotMatrixHud("● COMPUTING 64-BIT MOVIE HASH...");
        final String queryTitle = (videoTitle != null) ? videoTitle : "";

        subtitleIoExecutor.execute(() -> {
            String hash = null;
            long fileSize = 0;
            try {
                if (videoPath != null) {
                    File f = new File(videoPath);
                    if (f.exists()) {
                        fileSize = f.length();
                        hash = OnlineSubtitleManager.computeMovieHash(f);
                    }
                } else if (mediaUri != null) {
                    hash = OnlineSubtitleManager.computeMovieHash(this, mediaUri);
                    try (android.database.Cursor c = getContentResolver().query(mediaUri, null, null, null, null)) {
                        if (c != null && c.moveToFirst()) {
                            int sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                            if (sizeIdx >= 0) fileSize = c.getLong(sizeIdx);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            final String finalHash = hash;
            final long finalSize = fileSize;
            runOnUiThread(() -> {
                if (!isActivityAlive()) return;
                OnlineSubtitleManager.searchSubtitles(this, queryTitle, finalHash, finalSize, "eng", new OnlineSubtitleManager.SubtitleSearchCallback() {
                    @Override
                    public void onResults(List<OnlineSubtitleManager.SubtitleItem> items) {
                        if (!isActivityAlive()) return;
                        if (items != null && !items.isEmpty()) {
                            OnlineSubtitleManager.SubtitleItem best = items.get(0);
                            showDotMatrixHud("● 100% MATCH FOUND: DOWNLOADING...");
                            OnlineSubtitleManager.downloadSubtitle(ExoVideoPlayerActivity.this, best, new OnlineSubtitleManager.SubtitleDownloadCallback() {
                                @Override
                                public void onDownloaded(File srtFile) {
                                    if (!isActivityAlive()) return;
                                    showDotMatrixHud("● EXACT SUBTITLE DOWNLOADED");
                                    subtitleIoExecutor.execute(() -> {
                                        List<SubtitleParser.SubtitleCue> cues = SubtitleParser.parseSrt(srtFile);
                                        runOnUiThread(() -> {
                                            if (!isActivityAlive()) return;
                                            if (cues != null && !cues.isEmpty()) {
                                                searchableSubtitleCues = cues;
                                                launchTranslationStudio(cues, srtFile.getName());
                                            } else {
                                                Toast.makeText(ExoVideoPlayerActivity.this, "Downloaded subtitle file is empty", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    if (!isActivityAlive()) return;
                                    Toast.makeText(ExoVideoPlayerActivity.this, "Download error: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(ExoVideoPlayerActivity.this, "No match found on OpenSubtitles. Please search by name.", Toast.LENGTH_LONG).show();
                            showOnlineSubtitleSearchDialog();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!isActivityAlive()) return;
                        Toast.makeText(ExoVideoPlayerActivity.this, "Lookup error: " + message, Toast.LENGTH_SHORT).show();
                        showOnlineSubtitleSearchDialog();
                    }
                });
            });
        });
    }

    private void applyExternalSubtitle(File srtFile) {
        if (srtFile == null || !srtFile.exists()) return;
        activeSubtitleUri = Uri.fromFile(srtFile);
        subtitleIoExecutor.execute(() -> {
            List<SubtitleParser.SubtitleCue> parsed = SubtitleParser.parseSrt(srtFile);
            runOnUiThread(() -> {
                if (isActivityAlive() && parsed != null && !parsed.isEmpty()) {
                    searchableSubtitleCues = parsed;
                }
            });
        });

        if (isVlcActive && vlcPlayer != null) {
            try {
                vlcPlayer.addSlave(Media.Slave.Type.Subtitle, activeSubtitleUri, true);
                Toast.makeText(this, "Online Subtitle Attached (VLC)", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load subtitle in VLC: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (exoPlayer != null) {
            try {
                long curPos = exoPlayer.getCurrentPosition();
                boolean isPlaying = exoPlayer.isPlaying();
                Uri targetUri = resolveMediaUri();
                if (targetUri != null) {
                    exoPlayer.setMediaSource(buildExoMediaSource(targetUri));
                    exoPlayer.seekTo(curPos);
                    exoPlayer.prepare();
                    if (isPlaying) {
                        exoPlayer.play();
                    }
                    if (trackSelector != null) {
                        trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        );
                    }
                    Toast.makeText(this, "Online Subtitle Attached", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load subtitle in Exo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showOnlineSubtitleSearchDialog() {
        if (!isActivityAlive()) return;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_online_subtitles);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        EditText etQuery = dialog.findViewById(R.id.et_subtitle_query);
        Button btnSearch = dialog.findViewById(R.id.btn_subtitle_search);
        Button btnHashMatch = dialog.findViewById(R.id.btn_subtitle_hash_match);
        Button btnLangAll = dialog.findViewById(R.id.btn_lang_all);
        Button btnLangEng = dialog.findViewById(R.id.btn_lang_eng);
        Button btnLangHin = dialog.findViewById(R.id.btn_lang_hin);
        ProgressBar pbLoading = dialog.findViewById(R.id.pb_subtitles_loading);
        TextView tvStatus = dialog.findViewById(R.id.tv_subtitles_status);
        ScrollView scrollSubtitles = dialog.findViewById(R.id.scroll_subtitles);
        LinearLayout layoutResults = dialog.findViewById(R.id.layout_subtitles_results);
        Button btnCancel = dialog.findViewById(R.id.btn_subtitle_cancel);

        final String[] computedHash = {null};
        final long[] computedSize = {0L};
        final java.util.concurrent.atomic.AtomicBoolean isDownloading = new java.util.concurrent.atomic.AtomicBoolean(false);

        if (btnHashMatch != null) {
            btnHashMatch.setText("🎯 COMPUTING 64-BIT MOVIE HASH...");
            btnHashMatch.setEnabled(false);
            btnHashMatch.setAlpha(0.5f);
        }

        final String pathSnapshot = videoPath;
        final String uriSnapshot = videoUriStr;
        subtitleIoExecutor.execute(() -> {
            String hash = null;
            long size = 0L;
            try {
                if (pathSnapshot != null && !pathSnapshot.isEmpty()) {
                    File f = new File(pathSnapshot);
                    if (f.exists() && f.length() > 131072) {
                        size = f.length();
                        hash = OnlineSubtitleManager.computeMovieHash(f);
                    }
                } else if (uriSnapshot != null) {
                    Uri u = Uri.parse(uriSnapshot);
                    hash = OnlineSubtitleManager.computeMovieHash(this, u);
                    try (android.database.Cursor c = getContentResolver().query(u, null, null, null, null)) {
                        if (c != null && c.moveToFirst()) {
                            int sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                            if (sizeIdx >= 0) size = c.getLong(sizeIdx);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            final String finalHash = hash;
            final long finalSize = size;
            runOnUiThread(() -> {
                if (!isActivityAlive() || !dialog.isShowing()) return;
                computedHash[0] = finalHash;
                computedSize[0] = finalSize;
                if (btnHashMatch != null) {
                    if (finalHash != null) {
                        String shortHash = finalHash.substring(0, Math.min(8, finalHash.length())).toUpperCase(Locale.US);
                        btnHashMatch.setText("🎯 1-TAP HASH MATCH [" + shortHash + "...] (100% SYNC)");
                        btnHashMatch.setEnabled(true);
                        btnHashMatch.setAlpha(1.0f);
                    } else {
                        btnHashMatch.setText("🎯 1-TAP HASH MATCH (STREAM FILE)");
                        btnHashMatch.setEnabled(false);
                        btnHashMatch.setAlpha(0.5f);
                    }
                }
            });
        });

        final String[] currentLang = {"eng"};

        View.OnClickListener langListener = v -> {
            if (v == btnLangAll) {
                currentLang[0] = "all";
                btnLangAll.setBackgroundResource(R.drawable.bg_chip_selected);
                btnLangAll.setTextColor(Color.BLACK);
                btnLangEng.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnLangEng.setTextColor(Color.WHITE);
                btnLangHin.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnLangHin.setTextColor(Color.WHITE);
            } else if (v == btnLangEng) {
                currentLang[0] = "eng";
                btnLangAll.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnLangAll.setTextColor(Color.WHITE);
                btnLangEng.setBackgroundResource(R.drawable.bg_chip_selected);
                btnLangEng.setTextColor(Color.BLACK);
                btnLangHin.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnLangHin.setTextColor(Color.WHITE);
            } else if (v == btnLangHin) {
                currentLang[0] = "hin";
                btnLangAll.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnLangAll.setTextColor(Color.WHITE);
                btnLangEng.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnLangEng.setTextColor(Color.WHITE);
                btnLangHin.setBackgroundResource(R.drawable.bg_chip_selected);
                btnLangHin.setTextColor(Color.BLACK);
            }
        };

        btnLangAll.setOnClickListener(langListener);
        btnLangEng.setOnClickListener(langListener);
        btnLangHin.setOnClickListener(langListener);

        String initialQuery = videoTitle != null ? videoTitle : "";
        initialQuery = initialQuery.replaceAll("(?i)\\b(1080p|720p|480p|2160p|4k|x264|x265|hevc|bluray|webrip|web-dl|mkv|mp4)\\b", " ")
                .replaceAll("[\\[\\]\\(\\)\\._\\-]+", " ")
                .trim();
        etQuery.setText(initialQuery);

        Runnable performSearch = () -> {
            String query = etQuery.getText().toString().trim();
            if (query.isEmpty() && computedHash[0] == null) {
                Toast.makeText(this, "Please enter title to search", Toast.LENGTH_SHORT).show();
                return;
            }

            pbLoading.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.GONE);
            scrollSubtitles.setVisibility(View.GONE);
            layoutResults.removeAllViews();

            OnlineSubtitleManager.searchSubtitles(this, query, computedHash[0], computedSize[0], currentLang[0], new OnlineSubtitleManager.SubtitleSearchCallback() {
                @Override
                public void onResults(List<OnlineSubtitleManager.SubtitleItem> results) {
                    if (!isActivityAlive() || !dialog.isShowing()) return;
                    pbLoading.setVisibility(View.GONE);
                    if (results.isEmpty()) {
                        tvStatus.setText("NO SUBTITLES FOUND FOR THIS QUERY");
                        tvStatus.setVisibility(View.VISIBLE);
                        scrollSubtitles.setVisibility(View.GONE);
                        return;
                    }

                    tvStatus.setVisibility(View.GONE);
                    scrollSubtitles.setVisibility(View.VISIBLE);
                    layoutResults.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(ExoVideoPlayerActivity.this);

                    for (OnlineSubtitleManager.SubtitleItem item : results) {
                        View itemView = inflater.inflate(R.layout.item_nothing_subtitle, layoutResults, false);
                        TextView tvTitle = itemView.findViewById(R.id.tv_sub_title);
                        TextView tvMeta = itemView.findViewById(R.id.tv_sub_meta);
                        TextView tvHashBadge = itemView.findViewById(R.id.tv_sub_hash_badge);

                        tvTitle.setText(item.fileName != null ? item.fileName : "Subtitle");
                        String metaText = "[" + (item.languageCode != null ? item.languageCode.toUpperCase() : "SUB") + "] • ★ "
                                + (item.rating != null ? item.rating : "0.0")
                                + " • " + item.downloadCount + " DOWNLOADS";
                        tvMeta.setText(metaText);

                        if (tvHashBadge != null) {
                            tvHashBadge.setVisibility(item.isExactHashMatch ? View.VISIBLE : View.GONE);
                        }

                        itemView.setOnClickListener(v -> {
                            if (!isDownloading.compareAndSet(false, true)) return;
                            Toast.makeText(ExoVideoPlayerActivity.this, "Downloading subtitle...", Toast.LENGTH_SHORT).show();
                            pbLoading.setVisibility(View.VISIBLE);
                            OnlineSubtitleManager.downloadSubtitle(ExoVideoPlayerActivity.this, item, new OnlineSubtitleManager.SubtitleDownloadCallback() {
                                @Override
                                public void onDownloaded(File srtFile) {
                                    isDownloading.set(false);
                                    if (!isActivityAlive()) return;
                                    pbLoading.setVisibility(View.GONE);
                                    if (dialog.isShowing()) dialog.dismiss();
                                    applyExternalSubtitle(srtFile);
                                }

                                @Override
                                public void onError(String message) {
                                    isDownloading.set(false);
                                    if (!isActivityAlive()) return;
                                    pbLoading.setVisibility(View.GONE);
                                    Toast.makeText(ExoVideoPlayerActivity.this, "Download failed: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        });

                        layoutResults.addView(itemView);
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isActivityAlive() || !dialog.isShowing()) return;
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText("ERROR: " + message.toUpperCase());
                    tvStatus.setVisibility(View.VISIBLE);
                    scrollSubtitles.setVisibility(View.GONE);
                }
            });
        };

        if (btnHashMatch != null) {
            btnHashMatch.setOnClickListener(v -> {
                if (computedHash[0] == null || computedSize[0] <= 0) {
                    Toast.makeText(this, "Movie hash not available for stream", Toast.LENGTH_SHORT).show();
                    return;
                }
                pbLoading.setVisibility(View.VISIBLE);
                tvStatus.setVisibility(View.GONE);
                scrollSubtitles.setVisibility(View.GONE);
                layoutResults.removeAllViews();

                OnlineSubtitleManager.searchSubtitles(this, null, computedHash[0], computedSize[0], currentLang[0], new OnlineSubtitleManager.SubtitleSearchCallback() {
                    @Override
                    public void onResults(List<OnlineSubtitleManager.SubtitleItem> results) {
                        if (!isActivityAlive() || !dialog.isShowing()) return;
                        pbLoading.setVisibility(View.GONE);
                        if (results.isEmpty()) {
                            tvStatus.setText("NO EXACT HASH MATCHES (TRY TITLE SEARCH)");
                            tvStatus.setVisibility(View.VISIBLE);
                            scrollSubtitles.setVisibility(View.GONE);
                            return;
                        }

                        tvStatus.setVisibility(View.GONE);
                        scrollSubtitles.setVisibility(View.VISIBLE);
                        layoutResults.removeAllViews();
                        LayoutInflater inflater = LayoutInflater.from(ExoVideoPlayerActivity.this);

                        for (OnlineSubtitleManager.SubtitleItem item : results) {
                            View itemView = inflater.inflate(R.layout.item_nothing_subtitle, layoutResults, false);
                            TextView tvTitle = itemView.findViewById(R.id.tv_sub_title);
                            TextView tvMeta = itemView.findViewById(R.id.tv_sub_meta);
                            TextView tvHashBadge = itemView.findViewById(R.id.tv_sub_hash_badge);

                            tvTitle.setText(item.fileName != null ? item.fileName : "Subtitle");
                            String metaText = "[" + (item.languageCode != null ? item.languageCode.toUpperCase() : "SUB") + "] • ★ "
                                    + (item.rating != null ? item.rating : "0.0")
                                    + " • " + item.downloadCount + " DOWNLOADS";
                            tvMeta.setText(metaText);

                            if (tvHashBadge != null) {
                                tvHashBadge.setVisibility(View.VISIBLE);
                            }

                            itemView.setOnClickListener(subV -> {
                                if (!isDownloading.compareAndSet(false, true)) return;
                                Toast.makeText(ExoVideoPlayerActivity.this, "Downloading exact subtitle...", Toast.LENGTH_SHORT).show();
                                pbLoading.setVisibility(View.VISIBLE);
                                OnlineSubtitleManager.downloadSubtitle(ExoVideoPlayerActivity.this, item, new OnlineSubtitleManager.SubtitleDownloadCallback() {
                                    @Override
                                    public void onDownloaded(File srtFile) {
                                        isDownloading.set(false);
                                        if (!isActivityAlive()) return;
                                        pbLoading.setVisibility(View.GONE);
                                        if (dialog.isShowing()) dialog.dismiss();
                                        applyExternalSubtitle(srtFile);
                                    }

                                    @Override
                                    public void onError(String message) {
                                        isDownloading.set(false);
                                        if (!isActivityAlive()) return;
                                        pbLoading.setVisibility(View.GONE);
                                        Toast.makeText(ExoVideoPlayerActivity.this, "Download failed: " + message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });

                            layoutResults.addView(itemView);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!isActivityAlive() || !dialog.isShowing()) return;
                        pbLoading.setVisibility(View.GONE);
                        tvStatus.setText("HASH ERROR: " + message.toUpperCase());
                        tvStatus.setVisibility(View.VISIBLE);
                        scrollSubtitles.setVisibility(View.GONE);
                    }
                });
            });
        }

        btnSearch.setOnClickListener(v -> performSearch.run());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        trackDialog(dialog);
        dialog.show();
        if (!initialQuery.isEmpty()) {
            performSearch.run();
        }
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
                    showFloatingLockButton();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked) return false;
                int screenWidth = getViewportSize().x;

                // If video is zoomed or panned, double-tap in center 30% area resets to 100% Fit!
                if (currentVideoScale > 1.05f || Math.abs(videoPanOffsetY) > 20f || Math.abs(videoPanOffsetX) > 20f) {
                    float x = e.getX();
                    if (x >= screenWidth * 0.35f && x <= screenWidth * 0.65f) {
                        currentVideoScale = 1.0f;
                        resetVideoPan();
                        showGestureHud("● ZOOM RESET", DotMatrixIconView.TYPE_SEEK_FORWARD, "FIT (100%)", 100);
                        return true;
                    }
                }

                long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
                long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;

                int stepSec = prefs != null ? prefs.getInt("double_tap_seek_step_sec", 10) : 10;
                long stepMs = stepSec * 1000L;
                if (e.getX() < screenWidth / 2f) {
                    // Double Tap Left: Rewind
                    long target = Math.max(0, cur - stepMs);
                    if (isYouTubeActive && youtubeStreamView != null) {
                        youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                    } else if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("● " + stepSec + "s REWIND", DotMatrixIconView.TYPE_SEEK_REWIND, "-" + stepSec + "s", (int) (target * 100 / Math.max(1, dur)));
                } else {
                    // Double Tap Right: Forward
                    long target = dur > 0 ? Math.min(dur, cur + stepMs) : (cur + stepMs);
                    if (isYouTubeActive && youtubeStreamView != null) {
                        youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (target / 1000f) + ", true);", null);
                    } else if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(target);
                    else if (exoPlayer != null) exoPlayer.seekTo(target);
                    showGestureHud("● " + stepSec + "s FORWARD", DotMatrixIconView.TYPE_SEEK_FORWARD, "+" + stepSec + "s", (int) (target * 100 / Math.max(1, dur)));
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
                        seekStartPosition[0] = isYouTubeActive
                                ? youtubeCurrentPositionMs
                                : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
                        stopProgressTracker();
                    } else {
                        return false;
                    }
                }

                Point viewport = getViewportSize();
                int screenWidth = viewport.x;
                int screenHeight = viewport.y;

                SharedPreferences pSens = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
                String sensStr = pSens.getString("pref_gesture_sensitivity", "NORMAL (1.0X)");
                float sensFactor = 1.0f;
                if (sensStr.contains("LOW") || sensStr.contains("0.5")) sensFactor = 0.5f;
                else if (sensStr.contains("HIGH") || sensStr.contains("1.5")) sensFactor = 1.5f;

                if (gestureMode[0] == GESTURE_VERTICAL) {
                    // Vertical Swipe ONLY: Volume / Brightness
                    float deltaY = (distanceY / (float) screenHeight) * sensFactor;

                    if (e1.getX() > screenWidth / 2f) {
                        // Right Side: Smooth Volume Swipe
                        if (audioManager != null) {
                            volumeAccumulator[0] += deltaY;
                            float threshold = 1.0f / (maxVolume * 1.6f);
                            if (Math.abs(volumeAccumulator[0]) >= threshold) {
                                int step = volumeAccumulator[0] > 0 ? 1 : -1;
                                volumeAccumulator[0] = 0;
                                int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                if (step > 0) {
                                    if (currentVol < maxVolume) {
                                        int newVol = Math.min(maxVolume, currentVol + step);
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                                        applyVolumeBoost(0);
                                        int percent = (int) ((float) newVol / maxVolume * 100);
                                        showGestureHud("● VOLUME", DotMatrixIconView.TYPE_VOLUME, percent + "%", percent);
                                    } else {
                                        applyVolumeBoost(volumeBoostPercent + 10);
                                        int totalPercent = 100 + volumeBoostPercent;
                                        showGestureHud("● VOLUME BOOST", DotMatrixIconView.TYPE_AUDIO_BOOST, totalPercent + "%", totalPercent);
                                    }
                                } else {
                                    if (volumeBoostPercent > 0) {
                                        applyVolumeBoost(volumeBoostPercent - 10);
                                        int totalPercent = 100 + volumeBoostPercent;
                                        if (volumeBoostPercent > 0) {
                                            showGestureHud("● VOLUME BOOST", DotMatrixIconView.TYPE_AUDIO_BOOST, totalPercent + "%", totalPercent);
                                        } else {
                                            showGestureHud("● VOLUME", DotMatrixIconView.TYPE_VOLUME, "100%", 100);
                                        }
                                    } else {
                                        int newVol = Math.max(0, currentVol + step);
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                                        int percent = (int) ((float) newVol / maxVolume * 100);
                                        int dotType = newVol == 0 ? DotMatrixIconView.TYPE_VOLUME_MUTE : DotMatrixIconView.TYPE_VOLUME;
                                        showGestureHud("● VOLUME", dotType, percent + "%", percent);
                                    }
                                }
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
                    long activeDur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
                    float totalDeltaX = (e2.getX() - e1.getX()) / (float) screenWidth;
                    long maxSeekSpan = Math.max(60000, Math.min(180000, activeDur / 5));
                    long deltaMs = (long) (totalDeltaX * maxSeekSpan);
                    targetSeekPosition[0] = Math.max(0, Math.min(activeDur, seekStartPosition[0] + deltaMs));

                    long diffSec = (targetSeekPosition[0] - seekStartPosition[0]) / 1000;
                    String sign = diffSec >= 0 ? "+" : "";
                    String text = sign + diffSec + "s";
                    String tag = "● " + formatTime(targetSeekPosition[0]) + " / " + formatTime(activeDur);
                    int progress = (int) (targetSeekPosition[0] * 100 / Math.max(1, activeDur));
                    int dotType = diffSec >= 0 ? DotMatrixIconView.TYPE_SEEK_FORWARD : DotMatrixIconView.TYPE_SEEK_REWIND;

                    showGestureHud(tag, dotType, text, progress);
                    if (videoSeekBar != null) videoSeekBar.setProgress((int) targetSeekPosition[0]);
                    updateTimeDisplay(targetSeekPosition[0]);
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
                    } else {
                        applyPlaybackSpeedWithPitchPreservation(targetSpeed);
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
                float maxZoom = prefs != null ? prefs.getFloat("max_zoom_limit", 4.0f) : 4.0f;
                currentVideoScale = Math.max(0.5f, Math.min(maxZoom, currentVideoScale * scaleFactor));

                Point viewport = getViewportSize();
                float maxPanX = viewport.x * Math.max(0.25f, (currentVideoScale - 1f) * 0.6f);
                float maxPanY = viewport.y * Math.max(0.45f, (currentVideoScale - 1f) * 0.6f);
                videoPanOffsetX = Math.max(-maxPanX, Math.min(maxPanX, videoPanOffsetX));
                videoPanOffsetY = Math.max(-maxPanY, Math.min(maxPanY, videoPanOffsetY));

                applyVideoTransforms();
                updatePanUI();

                int percent = (int) (currentVideoScale * 100);
                showGestureHud("● PINCH ZOOM", DotMatrixIconView.TYPE_SEEK_FORWARD, percent + "%", Math.min(100, (int) ((currentVideoScale - 0.5f) / 3.0f * 100)));
                return true;
            }
        });

        View.OnTouchListener touchListener = (v, event) -> {
            SharedPreferences pPalm = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
            int palmDeadzone = pPalm.getInt("pref_palm_rejection_dp", 0);
            if (palmDeadzone > 0) {
                float deadzonePx = palmDeadzone * getResources().getDisplayMetrics().density;
                float rawX = event.getRawX();
                int screenW = getViewportSize().x;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (rawX < deadzonePx || rawX > (screenW - deadzonePx)) {
                        isPalmTouchIgnored = true;
                        return true;
                    } else {
                        isPalmTouchIgnored = false;
                    }
                } else if (isPalmTouchIgnored) {
                    if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        isPalmTouchIgnored = false;
                    }
                    return true;
                }
            }
            if (isLocked) {
                gestureDetector.onTouchEvent(event);
                return true;
            }

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                if (event.getPointerCount() == 2) {
                    twoFingerStartX = (event.getX(0) + event.getX(1)) / 2f;
                    twoFingerStartY = (event.getY(0) + event.getY(1)) / 2f;
                    isTwoFingerPanning = true;
                }
            } else if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() >= 2) {
                float midX = (event.getX(0) + event.getX(1)) / 2f;
                float midY = (event.getY(0) + event.getY(1)) / 2f;
                if (isTwoFingerPanning) {
                    float dx = midX - twoFingerStartX;
                    float dy = midY - twoFingerStartY;
                    twoFingerStartX = midX;
                    twoFingerStartY = midY;
                    if (Math.abs(dx) > 1.2f || Math.abs(dy) > 1.2f) {
                        applyPanDelta(dx, dy);
                    }
                } else {
                    twoFingerStartX = midX;
                    twoFingerStartY = midY;
                    isTwoFingerPanning = true;
                }
            } else if (action == MotionEvent.ACTION_POINTER_UP) {
                if (event.getPointerCount() <= 2) {
                    isTwoFingerPanning = false;
                }
            }

            scaleGestureDetector.onTouchEvent(event);
            if (!scaleGestureDetector.isInProgress() && !isTwoFingerPanning && event.getPointerCount() == 1) {
                gestureDetector.onTouchEvent(event);
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isTwoFingerPanning = false;
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
                    if (videoSeekBar != null) videoSeekBar.setProgress((int) target);
                    updateTimeDisplay(target);
                    startProgressTracker();
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
            if (btnQuickScreenshot != null) {
                boolean showShot = prefs == null || prefs.getBoolean("show_screenshot_btn", true);
                btnQuickScreenshot.setVisibility(showShot ? View.VISIBLE : View.GONE);
            }
            if (btnScreenLock != null) {
                btnScreenLock.setVisibility(View.VISIBLE);
                btnScreenLock.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
            }
            areControlsVisible = true;
            scheduleHideControls();
        }
    }

    private void scheduleHideControls() {
        if (isAnyPanelOpen()) {
            hideHandler.removeCallbacks(hideControlsRunnable);
            return;
        }
        hideHandler.removeCallbacks(hideControlsRunnable);
        hideHandler.postDelayed(hideControlsRunnable, 4500);
    }

    private final Runnable hideControlsRunnable = () -> {
        if (isAnyPanelOpen()) {
            return;
        }
        boolean isPlaying = isYouTubeActive ? isYouTubePlaying : (isVlcActive ? isVlcPlayingSafe() : (exoPlayer != null && exoPlayer.isPlaying()));
        if (isPlaying && !isLocked) {
            topControlsBar.setVisibility(View.GONE);
            bottomControlsBar.setVisibility(View.GONE);
            if (btnQuickScreenshot != null) btnQuickScreenshot.setVisibility(View.GONE);
            if (layoutQuickActionsExpanded != null) layoutQuickActionsExpanded.setVisibility(View.GONE);
            closeAllPanels();
            areControlsVisible = false;
        }
    };

    private boolean isVlcPlayingSafe() {
        if (!isVlcActive || vlcPlayer == null) return false;
        try {
            return !vlcPlayer.isReleased() && vlcPlayer.isPlaying();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void updatePlayPauseButtonState() {
        if (btnPlayPause == null) return;
        boolean isPlaying;
        if (isYouTubeActive) {
            isPlaying = isYouTubePlaying;
        } else if (isVlcActive) {
            isPlaying = isVlcPlayingSafe();
        } else {
            isPlaying = (exoPlayer != null && exoPlayer.isPlaying());
        }
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_dotmatrix : R.drawable.ic_play_dotmatrix);
    }

    private void startProgressTracker() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }

    private void stopProgressTracker() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private void saveCurrentPlaybackProgress(long currentPos, long dur) {
        if (currentPos > 0 && dur > 0) {
            String key = videoPath != null && !videoPath.isEmpty() ? videoPath : videoUriStr;
            if (key != null && !key.isEmpty()) {
                PlaybackHistoryManager.saveProgress(this, key, videoTitle, currentPos, dur);
            }
        }
    }

    private void checkNextEpisodeCountdown(long pos, long dur) {
        if (hasCancelledNextEpisodeForCurrent) {
            if (layoutNextEpisodeCard != null && layoutNextEpisodeCard.getVisibility() == View.VISIBLE) {
                layoutNextEpisodeCard.setVisibility(View.GONE);
            }
            return;
        }
        if (playlistPaths != null && playlistIndex < playlistPaths.size() - 1 && dur > 30000) {
            long remainingSec = (dur - pos) / 1000;
            if (remainingSec <= 15 && remainingSec >= 0) {
                if (layoutNextEpisodeCard != null) {
                    layoutNextEpisodeCard.setVisibility(View.VISIBLE);
                    if (tvNextEpisodeTag != null) {
                        tvNextEpisodeTag.setText("● NEXT EPISODE IN " + Math.max(1, remainingSec) + "S");
                    }
                    if (tvNextEpisodeTitle != null) {
                        String nextTitle = playlistTitles.size() > (playlistIndex + 1)
                                ? playlistTitles.get(playlistIndex + 1)
                                : new File(playlistPaths.get(playlistIndex + 1)).getName();
                        tvNextEpisodeTitle.setText(nextTitle.toUpperCase());
                    }
                }
                if (remainingSec <= 0) {
                    if (layoutNextEpisodeCard != null) layoutNextEpisodeCard.setVisibility(View.GONE);
                    playNextVideo();
                }
            } else {
                if (layoutNextEpisodeCard != null && layoutNextEpisodeCard.getVisibility() == View.VISIBLE) {
                    layoutNextEpisodeCard.setVisibility(View.GONE);
                }
            }
        } else {
            if (layoutNextEpisodeCard != null && layoutNextEpisodeCard.getVisibility() == View.VISIBLE) {
                layoutNextEpisodeCard.setVisibility(View.GONE);
            }
        }
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;

            updatePlayPauseButtonState();

            if (isYouTubeActive) {
                // YouTube tracking handled separately
            } else if (isVlcActive && vlcPlayer != null) {
                long pos = vlcPlayer.getTime();
                if (pos >= 0) {
                    if (repeatPointA >= 0 && repeatPointB > repeatPointA && pos >= repeatPointB) {
                        vlcPlayer.setTime(repeatPointA);
                        pos = repeatPointA;
                    }
                    currentPositionMs = pos;
                    videoSeekBar.setProgress((int) pos);
                    updateTimeDisplay(pos);
                    checkNextEpisodeCountdown(pos, totalDurationMs);
                    checkSkipIntroButton(pos, totalDurationMs);
                    updateSecondarySubtitle(pos);
                    if (Math.abs(pos - lastSavedProgressMs) >= 2000) {
                        lastSavedProgressMs = pos;
                        saveCurrentPlaybackProgress(pos, totalDurationMs);
                    }
                }
                long len = vlcPlayer.getLength();
                if (len > 0 && len != totalDurationMs) {
                    totalDurationMs = len;
                    videoSeekBar.setMax((int) len);
                    updateTimeDisplay(currentPositionMs);
                }
            } else if (exoPlayer != null) {
                long pos = exoPlayer.getCurrentPosition();
                if (repeatPointA >= 0 && repeatPointB > repeatPointA && pos >= repeatPointB) {
                    exoPlayer.seekTo(repeatPointA);
                    pos = repeatPointA;
                }
                currentPositionMs = pos;
                videoSeekBar.setProgress((int) pos);
                updateTimeDisplay(pos);
                checkNextEpisodeCountdown(pos, totalDurationMs);
                checkSkipIntroButton(pos, totalDurationMs);
                updateSecondarySubtitle(pos);
                if (Math.abs(pos - lastSavedProgressMs) >= 2000) {
                    lastSavedProgressMs = pos;
                    saveCurrentPlaybackProgress(pos, totalDurationMs);
                }
                long len = exoPlayer.getDuration();
                if (len > 0 && len != C.TIME_UNSET && len != totalDurationMs) {
                    totalDurationMs = len;
                    videoSeekBar.setMax((int) len);
                    updateTimeDisplay(currentPositionMs);
                }
            }

            if (sleepTimerEndOfVideo && totalDurationMs > 0 && currentPositionMs > 0 && !isMuted) {
                long rem = totalDurationMs - currentPositionMs;
                if (rem > 0 && rem <= 60000) {
                    float fade = Math.max(0.05f, Math.min(1.0f, (float) rem / 60000.0f));
                    if (exoPlayer != null) exoPlayer.setVolume(fade);
                    if (vlcPlayer != null) vlcPlayer.setVolume((int) (fade * (100 + volumeBoostPercent)));
                }
            }

            // Always reschedule loop while activity is active so it never freezes
            progressHandler.removeCallbacks(this);
            progressHandler.postDelayed(this, 500);
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
        int realWidth = getViewportSize().x;
        float maxX = parent != null ? parent.getWidth() - cardWidth - dpToPx(8) : realWidth - cardWidth;
        targetX = Math.max(minX, Math.min(maxX, targetX));

        seekbarPreviewCard.setTranslationX(targetX);
        if (previewTimeText != null) {
            previewTimeText.setText(formatTime(progress));
        }
    }

    private void initPreviewRetriever() {
        previewBitmapCache.evictAll();
        previewRetrieverAttempted = false;
        previewExecutor.execute(this::ensurePreviewRetrieverOnWorker);
    }

    private void ensurePreviewRetrieverOnWorker() {
        previewRetrieverAttempted = true;
        MediaMetadataRetriever oldRetriever;
        synchronized (previewLock) {
            oldRetriever = previewRetriever;
            previewRetriever = null;
        }
        if (oldRetriever != null) {
            try { oldRetriever.release(); } catch (Exception ignored) {}
        }
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            boolean ok = false;

            if (videoPath != null && !videoPath.isEmpty() && !videoPath.startsWith("http://") && !videoPath.startsWith("https://")) {
                File f = new File(videoPath);
                if (f.exists()) {
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                        mmr.setDataSource(fis.getFD());
                        ok = true;
                    } catch (Exception e) {
                        try {
                            mmr.setDataSource(videoPath);
                            ok = true;
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (!ok && videoUriStr != null && !videoUriStr.isEmpty()) {
                Uri u = Uri.parse(videoUriStr);
                if ("content".equalsIgnoreCase(u.getScheme())) {
                    try {
                        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(u, "r")) {
                            if (pfd != null) {
                                mmr.setDataSource(pfd.getFileDescriptor());
                                ok = true;
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (!ok) {
                    try {
                        mmr.setDataSource(getApplicationContext(), u);
                        ok = true;
                    } catch (Exception ignored) {}
                }
            }

            if (!ok && getIntent() != null && getIntent().getData() != null) {
                Uri u = getIntent().getData();
                try {
                    if ("content".equalsIgnoreCase(u.getScheme())) {
                        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(u, "r")) {
                            if (pfd != null) {
                                mmr.setDataSource(pfd.getFileDescriptor());
                                ok = true;
                            }
                        }
                    }
                    if (!ok) {
                        mmr.setDataSource(getApplicationContext(), u);
                        ok = true;
                    }
                } catch (Exception ignored) {}
            }

            if (ok) {
                synchronized (previewLock) {
                    previewRetriever = mmr;
                }
            } else {
                try { mmr.release(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            synchronized (previewLock) {
                previewRetriever = null;
            }
        }
    }

    private void extractAndShowPreviewFrame(long timeMs) {
        if (previewThumbnail == null || !isActivityAlive()) return;
        lastRequestedPreviewTime = timeMs;

        long secKey = timeMs / 1000;

        // 1. Direct memory cache hit (0ms Instant display!)
        Bitmap cached = previewBitmapCache.get(secKey);
        if (cached != null) {
            previewThumbnail.setImageBitmap(cached);
            return;
        }

        // 2. Smooth scrubbing fallback: check adjacent cached seconds (+-1s, +-2s)
        Bitmap near = previewBitmapCache.get(secKey - 1);
        if (near == null) near = previewBitmapCache.get(secKey + 1);
        if (near == null) near = previewBitmapCache.get(secKey - 2);
        if (near == null) near = previewBitmapCache.get(secKey + 2);
        if (near != null) {
            previewThumbnail.setImageBitmap(near);
        }

        // 3. YouTube stream preview
        if (isYouTubeActive) {
            String rawUrl = videoUriStr != null ? videoUriStr : videoPath;
            String vid = YouTubeStreamResolver.extractVideoId(rawUrl);
            if (vid != null && !vid.isEmpty()) {
                Glide.with(ExoVideoPlayerActivity.this)
                        .load("https://img.youtube.com/vi/" + vid + "/mqdefault.jpg")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(previewThumbnail);
                return;
            }
        }

        // 4. Atomic asynchronous hardware keyframe decode
        pendingPreviewTime.set(timeMs);
        if (isDecodingFrame.compareAndSet(false, true)) {
            triggerPreviewDecode();
        }
    }

    private void triggerPreviewDecode() {
        previewExecutor.execute(() -> {
            try {
                while (isTrackingTouch && isActivityAlive()) {
                    long targetMs = pendingPreviewTime.getAndSet(-1);
                    if (targetMs < 0) break;

                    long targetSec = targetMs / 1000;
                    Bitmap bmp = previewBitmapCache.get(targetSec);

                    if (bmp == null) {
                        if (previewRetriever == null && !previewRetrieverAttempted) {
                            ensurePreviewRetrieverOnWorker();
                        }
                        synchronized (previewLock) {
                            if (previewRetriever != null) {
                                try {
                                    long timeUs = targetMs * 1000;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                        bmp = previewRetriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180);
                                    } else {
                                        Bitmap raw = previewRetriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                                        if (raw != null) {
                                            bmp = Bitmap.createScaledBitmap(raw, 320, 180, true);
                                            if (raw != bmp) raw.recycle();
                                        }
                                    }
                                    if (bmp != null) {
                                        previewBitmapCache.put(targetSec, bmp);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }

                    // Fallback to Glide if retriever was not available (e.g. online stream)
                    if (bmp == null && isActivityAlive()) {
                        Uri uri = resolveMediaUri();
                        if (uri != null) {
                            try {
                                bmp = Glide.with(getApplicationContext())
                                        .asBitmap()
                                        .load(uri)
                                        .frame(targetMs * 1000)
                                        .override(240, 135)
                                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                        .submit()
                                        .get();
                                if (bmp != null) {
                                    previewBitmapCache.put(targetSec, bmp);
                                }
                            } catch (Throwable ignored) {}
                        }
                    }

                    if (bmp != null) {
                        final Bitmap finalBmp = bmp;
                        runOnUiThread(() -> {
                            if (isActivityAlive() && isTrackingTouch && previewThumbnail != null) {
                                previewThumbnail.setImageBitmap(finalBmp);
                            }
                        });
                    }
                }
            } finally {
                isDecodingFrame.set(false);
                if (pendingPreviewTime.get() >= 0 && isTrackingTouch) {
                    if (isDecodingFrame.compareAndSet(false, true)) {
                        triggerPreviewDecode();
                    }
                }
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
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
        hasCancelledNextEpisodeForCurrent = false;
        hasSkippedIntroForCurrent = false;
        introEndMs = 85000L;
        if (btnSkipIntro != null) btnSkipIntro.setVisibility(View.GONE);
        if (videoSeekBar != null) {
            videoSeekBar.setChapterPositions(new ArrayList<>(), 1);
            videoSeekBar.clearAbRepeatRange();
        }
        activeSubtitleUri = null;
        secondarySubtitleCues = null;
        secondarySubtitleTitle = null;
        searchableSubtitleCues = null;
        subtitleDelayMs = 0L;
        if (tvSecondarySubtitle != null) tvSecondarySubtitle.setVisibility(View.GONE);
        repeatPointA = -1L;
        repeatPointB = -1L;
        totalDurationMs = 0L;
        currentPositionMs = 0L;
        videoWidth = 0;
        videoHeight = 0;
        is2xBoostActive = false;
        userForcedHwDecoder = false;
        hasAutoFallenBackToVlc = false;
        TextView tvAb = findViewById(R.id.tv_menu_ab_repeat_status);
        if (tvAb != null) tvAb.setText("OFF");
        if (layoutNextEpisodeCard != null) layoutNextEpisodeCard.setVisibility(View.GONE);
        showTransitionOverlay();

        videoPath = playlistPaths.get(index);
        videoUriStr = playlistUris.size() > index ? playlistUris.get(index) : "";
        videoTitle = playlistTitles.size() > index ? playlistTitles.get(index)
                : (videoPath != null ? new File(videoPath).getName() : "Nothing Media Player");

        String openedTarget = videoPath != null && !videoPath.isEmpty() ? videoPath : videoUriStr;
        MediaStateManager.markVideoOpened(this, openedTarget);

        BackgroundPlaybackManager.getInstance().updateCurrentIndex(index, videoTitle, videoPath, videoUriStr);
        BackgroundPlaybackManager.getInstance().setPlaybackSpeed(playbackSpeed);

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
                String safeVid = (vid != null) ? vid.replaceAll("[^a-zA-Z0-9_-]", "") : "";
                if (!safeVid.isEmpty()) {
                    youtubeStreamView.evaluateJavascript("if (player && player.loadVideoById) player.loadVideoById('" + safeVid + "');", null);
                }
            }
            showGestureHud("● NOW PLAYING", DotMatrixIconView.TYPE_SEEK_FORWARD, videoTitle, (playlistIndex + 1) * 100 / Math.max(1, playlistPaths.size()));
            return;
        }

        if (isYouTubeStream()) {
            startYouTubePlayer();
        } else if (isRtspOrLiveStream(videoUriStr != null ? videoUriStr : videoPath) || isSpecialCodecRequired(videoUriStr != null ? videoUriStr : videoPath) || isSpecialCodecRequired(videoPath)) {
            startVlcPlayer(0);
        } else {
            startExoPlayer(0);
        }

        showGestureHud("● NOW PLAYING", DotMatrixIconView.TYPE_SEEK_FORWARD, videoTitle, (playlistIndex + 1) * 100 / Math.max(1, playlistPaths.size()));
    }

    private void handleVideoCompletion() {
        if (!isActivityAlive()) return;
        if (sleepTimerEndOfVideo) {
            cancelSleepTimer();
            if (isYouTubeActive && youtubeStreamView != null) {
                youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
            } else if (isVlcActive && vlcPlayer != null) {
                vlcPlayer.pause();
            } else if (exoPlayer != null) {
                exoPlayer.pause();
            }
            updatePlayPauseButtonState();
            restoreActiveVolume();
            showGestureHud("\u25cf SLEEP TIMER", DotMatrixIconView.TYPE_SEEK_FORWARD, "PLAYBACK STOPPED", 100);
            Toast.makeText(this, "Sleep timer reached end of video. Screen sleeping.", Toast.LENGTH_SHORT).show();
            try {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = 0.01f;
                getWindow().setAttributes(lp);
            } catch (Exception ignored) {}
            finish();
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
                btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
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
                btnPlayPause.setImageResource(R.drawable.ic_play_dotmatrix);
            }
            showGestureHud("● PLAY ONCE", DotMatrixIconView.TYPE_SEEK_FORWARD, "FINISHED", 100);
            checkWatchedEpisodeOnEnd(false);
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
                btnPlayPause.setImageResource(R.drawable.ic_play_dotmatrix);
            }
            showGestureHud("● PLAYLIST END", DotMatrixIconView.TYPE_SEEK_FORWARD, "ORDER COMPLETE", 100);
            boolean finishWhenDone = totalDurationMs > 5000 && !isFinishing();
            checkWatchedEpisodeOnEnd(finishWhenDone);
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
                String t = playlistTitles.size() > i ? playlistTitles.get(i) : (p != null ? new File(p).getName() : "Video");
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
                btnPlayPause.setImageResource(R.drawable.ic_play_dotmatrix);
            } else {
                if (youtubeStreamView != null) youtubeStreamView.evaluateJavascript("if (player && player.playVideo) player.playVideo();", null);
                isYouTubePlaying = true;
                btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
            }
        } else if (isVlcActive) {
            if (vlcPlayer != null) {
                if (isVlcPlayingSafe()) {
                    vlcPlayer.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play_dotmatrix);
                } else {
                    vlcPlayer.play();
                    btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
                }
            }
        } else {
            if (exoPlayer != null) {
                if (exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play_dotmatrix);
                } else {
                    exoPlayer.play();
                    btnPlayPause.setImageResource(R.drawable.ic_pause_dotmatrix);
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

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private PictureInPictureParams buildPipParams(boolean isPlaying, boolean autoPip) {
        boolean hasMultiple = (playlistPaths != null && playlistPaths.size() > 1) || youtubePlaylistId != null;
        java.util.ArrayList<RemoteAction> actions = new java.util.ArrayList<>();

        if (hasMultiple) {
            Intent prevIntent = new Intent(ACTION_PIP_PREV).setPackage(getPackageName());
            PendingIntent prevPending = PendingIntent.getBroadcast(this, 100, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon prevIcon = Icon.createWithResource(this, R.drawable.ic_prev_dotmatrix);
            actions.add(new RemoteAction(prevIcon, "Previous Video", "Previous Video", prevPending));
        } else {
            Intent rewindIntent = new Intent(ACTION_PIP_REWIND).setPackage(getPackageName());
            PendingIntent rewindPending = PendingIntent.getBroadcast(this, 101, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon rewindIcon = Icon.createWithResource(this, R.drawable.ic_prev_dotmatrix);
            actions.add(new RemoteAction(rewindIcon, "Rewind 10s", "Rewind 10s", rewindPending));
        }

        Intent playPauseIntent = new Intent(ACTION_PIP_PLAY_PAUSE).setPackage(getPackageName());
        PendingIntent playPausePending = PendingIntent.getBroadcast(this, 102, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Icon playPauseIcon = Icon.createWithResource(this, isPlaying ? R.drawable.ic_pause_dotmatrix : R.drawable.ic_play_dotmatrix);
        actions.add(new RemoteAction(playPauseIcon, isPlaying ? "Pause" : "Play", isPlaying ? "Pause" : "Play", playPausePending));

        if (hasMultiple) {
            Intent nextIntent = new Intent(ACTION_PIP_NEXT).setPackage(getPackageName());
            PendingIntent nextPending = PendingIntent.getBroadcast(this, 104, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon nextIcon = Icon.createWithResource(this, R.drawable.ic_next_dotmatrix);
            actions.add(new RemoteAction(nextIcon, "Next Video", "Next Video", nextPending));
        } else {
            Intent forwardIntent = new Intent(ACTION_PIP_FORWARD).setPackage(getPackageName());
            PendingIntent forwardPending = PendingIntent.getBroadcast(this, 103, forwardIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon forwardIcon = Icon.createWithResource(this, R.drawable.ic_next_dotmatrix);
            actions.add(new RemoteAction(forwardIcon, "Forward 10s", "Forward 10s", forwardPending));
        }

        int rawW = (videoWidth > 0 && videoHeight > 0) ? videoWidth : getWindow().getDecorView().getWidth();
        int rawH = (videoWidth > 0 && videoHeight > 0) ? videoHeight : getWindow().getDecorView().getHeight();
        if (rawW <= 0 || rawH <= 0) {
            rawW = 16;
            rawH = 9;
        }
        float ratio = (float) rawW / (float) rawH;
        Rational aspectRatio;
        if (ratio < 0.418410f) {
            aspectRatio = new Rational(419, 1000);
        } else if (ratio > 2.39f) {
            aspectRatio = new Rational(239, 100);
        } else {
            aspectRatio = new Rational(rawW, rawH);
        }

        PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
        pipBuilder.setAspectRatio(aspectRatio);
        pipBuilder.setActions(actions);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pipBuilder.setAutoEnterEnabled(autoPip && !isBackgroundPlayActive && isPlaying);
            pipBuilder.setSeamlessResizeEnabled(true);
        }
        return pipBuilder.build();
    }

    private void updatePipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isActivityAlive()) {
            try {
                boolean isPlaying = isYouTubeActive ? isYouTubePlaying : (isVlcActive ? isVlcPlayingSafe() : (exoPlayer != null && exoPlayer.isPlaying()));
                boolean autoPip = prefs != null ? prefs.getBoolean("pref_auto_pip_home", true) : true;
                setPictureInPictureParams(buildPipParams(isPlaying, autoPip));
            } catch (Exception ignored) {}
        }
    }

    private void checkVideoAdaptiveRotation(int width, int height) {
        SharedPreferences p = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        if (!p.getBoolean("pref_auto_rotate_by_aspect", false) || isLocked) return;
        try {
            if (width >= height * 1.15f) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else if (height >= width * 1.15f) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } catch (Exception ignored) {}
    }

    private void applyEcoBatterySaver() {
        SharedPreferences p = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        if (p.getBoolean("pref_eco_battery_saver", false)) {
            try {
                android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                if (bm != null) {
                    int pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    if (pct <= 15 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Window window = getWindow();
                        if (window != null) {
                            android.view.WindowManager.LayoutParams params = window.getAttributes();
                            params.preferredRefreshRate = 30.0f;
                            window.setAttributes(params);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isActivityAlive()) {
            try {
                boolean isPlaying = isYouTubeActive ? isYouTubePlaying : (isVlcActive ? isVlcPlayingSafe() : (exoPlayer != null && exoPlayer.isPlaying()));
                boolean autoPip = prefs != null ? prefs.getBoolean("pref_auto_pip_home", true) : true;
                enterPictureInPictureMode(buildPipParams(isPlaying, autoPip));
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
            if (!isLocked && areControlsVisible) {
                if (topControlsBar != null) topControlsBar.setVisibility(View.VISIBLE);
                if (bottomControlsBar != null) bottomControlsBar.setVisibility(View.VISIBLE);
                if (btnScreenLock != null) {
                    btnScreenLock.setVisibility(View.VISIBLE);
                    btnScreenLock.setImageResource(R.drawable.ic_lock_open);
                }
                if (btnQuickScreenshot != null) {
                    boolean showShot = prefs == null || prefs.getBoolean("show_screenshot_btn", true);
                    btnQuickScreenshot.setVisibility(showShot ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (isLocked) {
            showFloatingLockButton();
            return;
        }
        if (windowFloatingEqualizer != null && windowFloatingEqualizer.getVisibility() == View.VISIBLE) {
            closeFloatingEqualizer();
            return;
        }
        if (isAnyPanelOpen()) {
            closeAllPanels();
            scheduleHideControls();
            return;
        }
        handlePlayerBackExit(false);
        super.onBackPressed();
    }

    private void handlePlayerBackExit() {
        handlePlayerBackExit(true);
    }

    private void handlePlayerBackExit(boolean callFinish) {
        long cur = currentPositionMs;
        try {
            if (isYouTubeActive) {
                cur = youtubeCurrentPositionMs;
            } else if (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased()) {
                cur = vlcPlayer.getTime();
            } else if (exoPlayer != null) {
                cur = exoPlayer.getCurrentPosition();
            }
        } catch (Exception ignored) {}
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        saveCurrentPlaybackProgress(cur, dur);

        if (!isBackgroundPlayActive) {
            if (BackgroundPlaybackManager.getInstance().hasActiveSession()) {
                BackgroundPlaybackManager.getInstance().stopAndRelease();
            }
        }
        if (callFinish) {
            finish();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        boolean autoPip = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE)
                .getBoolean("pref_auto_pip_home", true);
        if (autoPip && !isBackgroundPlayActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                boolean isPlaying = isYouTubeActive ? isYouTubePlaying : (isVlcActive ? isVlcPlayingSafe() : (exoPlayer != null && exoPlayer.isPlaying()));
                if (isPlaying) {
                    updatePipActions();
                    enterPipMode();
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && !isBackgroundPlayActive && !isInPictureInPictureMode()) {
            try {
                if (isYouTubeActive && youtubeStreamView != null) {
                    youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
                } else if (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased() && vlcPlayer.isPlaying()) {
                    vlcPlayer.pause();
                } else if (exoPlayer != null && exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        amoledPixelShiftHandler.removeCallbacks(amoledPixelShiftRunnable);
        if (is2xBoostActive) {
            is2xBoostActive = false;
            applySpeed(preBoostSpeed);
        }
        long cur = 0;
        try {
            cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null && !vlcPlayer.isReleased() ? vlcPlayer.getTime() : currentPositionMs) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
        } catch (Exception ignored) {
            cur = currentPositionMs;
        }
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        saveCurrentPlaybackProgress(cur, dur);

        if (!isInPictureInPictureMode() && !isBackgroundPlayActive) {
            try {
                if (isYouTubeActive && youtubeStreamView != null) {
                    youtubeStreamView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null);
                } else if (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased() && vlcPlayer.isPlaying()) {
                    vlcPlayer.pause();
                } else if (exoPlayer != null && exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                }
            } catch (Exception ignored) {}
        }
    }

    private void refreshLiveSettings() {
        SharedPreferences sp = getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        if (amoledPureBlackOverlay != null) {
            amoledPureBlackOverlay.setVisibility(sp.getBoolean("pref_amoled_pure_black", false) ? View.VISIBLE : View.GONE);
        }
        if (eyeComfortOverlay != null) {
            eyeComfortOverlay.setVisibility(sp.getBoolean("pref_eye_comfort", false) ? View.VISIBLE : View.GONE);
        }
        isSubtitleGuardEnabled = sp.getBoolean("pref_subtitle_guard", true);
        longPressSpeedTarget = sp.getFloat("long_press_speed_target", 2.0f);
        longPressVibration = sp.getBoolean("long_press_vibration", true);
        if (tvLongPressSpeedTarget != null) {
            tvLongPressSpeedTarget.setText(String.format(java.util.Locale.US, "%.1fX >", longPressSpeedTarget));
        }
        if (switchLongPressVibration != null) {
            switchLongPressVibration.setChecked(longPressVibration);
        }
        applySubtitleStyling();
        updatePocketMediaSessionState();
        applyAudioBalance();
        applyAccentColorTheme();
        applyEcoBatterySaver();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW || level >= TRIM_MEMORY_UI_HIDDEN) {
            previewBitmapCache.evictAll();
            try { Glide.get(this).trimMemory(level); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLiveSettings();
        BackgroundPlaybackManager bg = BackgroundPlaybackManager.getInstance();
        if (bg.isSleepTimerEndOfVideo()) {
            sleepTimerEndOfVideo = true;
            sleepTimerEndTimeMs = 0L;
            if (tvSleepTimerStatus != null) tvSleepTimerStatus.setText("STATUS: END OF CURRENT VIDEO");
            if (tvMenuTimerLabel != null) tvMenuTimerLabel.setText("END VID");
            updateSleepTimerButtons(0, true);
        } else if (bg.getSleepTimerEndTimeMs() > System.currentTimeMillis()) {
            sleepTimerEndOfVideo = false;
            sleepTimerEndTimeMs = bg.getSleepTimerEndTimeMs();
            startSleepTimerUiLoop();
        }
        amoledPixelShiftHandler.postDelayed(amoledPixelShiftRunnable, 180000);
        updateBackgroundPlayUi();
        if (isBackgroundPlayActive) {
            startProgressTracker();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissActiveDialogs();

        long cur = 0;
        try {
            cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null && !vlcPlayer.isReleased() ? vlcPlayer.getTime() : currentPositionMs) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : currentPositionMs));
        } catch (Exception ignored) {
            cur = currentPositionMs;
        }
        long dur = isYouTubeActive ? youtubeDurationMs : totalDurationMs;
        saveCurrentPlaybackProgress(cur, dur);

        stopProgressTracker();
        hideHandler.removeCallbacksAndMessages(null);
        sleepTimerHandler.removeCallbacksAndMessages(null);
        if (!isBackgroundPlayActive) {
            BackgroundPlaybackManager.getInstance().cancelSleepTimer();
        }
        amoledPixelShiftHandler.removeCallbacksAndMessages(null);
        volumeKeyHandler.removeCallbacksAndMessages(null);

        try {
            unregisterReceiver(pipReceiver);
        } catch (Exception ignored) {}
        if (isNoisyReceiverRegistered) {
            try {
                unregisterReceiver(noisyAudioReceiver);
            } catch (Exception ignored) {}
            isNoisyReceiverRegistered = false;
        }
        if (isScreenStateReceiverRegistered) {
            try {
                unregisterReceiver(screenStateReceiver);
            } catch (Exception ignored) {}
            isScreenStateReceiverRegistered = false;
        }

        if (youtubeStreamView != null) {
            try {
                youtubeStreamView.stopLoading();
                youtubeStreamView.removeJavascriptInterface("AndroidYT");
                youtubeStreamView.loadUrl("about:blank");
                if (youtubeStreamView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) youtubeStreamView.getParent()).removeView(youtubeStreamView);
                }
                youtubeStreamView.destroy();
            } catch (Exception ignored) {}
            youtubeStreamView = null;
        }

        if (pocketMediaSession != null) {
            try {
                pocketMediaSession.setActive(false);
                pocketMediaSession.release();
            } catch (Exception ignored) {}
            pocketMediaSession = null;
        }

        releaseExoPlayer(true);
        releaseVlcPlayer(true);

        previewBitmapCache.evictAll();
        try {
            previewExecutor.shutdownNow();
            titleExecutor.shutdownNow();
            subtitleIoExecutor.shutdownNow();
        } catch (Exception ignored) {}

        synchronized (previewLock) {
            if (previewRetriever != null) {
                try { previewRetriever.release(); } catch (Exception ignored) {}
                previewRetriever = null;
            }
        }
    }

    // ==========================================
    // IMPROVEMENTS 1 - 4 HELPER IMPLEMENTATIONS
    // ==========================================

    private void checkSkipIntroButton(long pos, long dur) {
        if (btnSkipIntro == null) return;
        if (hasSkippedIntroForCurrent || isLocked) {
            if (btnSkipIntro.getVisibility() == View.VISIBLE) {
                btnSkipIntro.setVisibility(View.GONE);
            }
            return;
        }
        // Show between 3s and introEndMs - 2s if video is at least 3 minutes long
        if (dur >= 180000L && pos >= 3000L && pos <= (introEndMs - 2000L)) {
            if (btnSkipIntro.getVisibility() != View.VISIBLE) {
                btnSkipIntro.setVisibility(View.VISIBLE);
                btnSkipIntro.setAlpha(0f);
                btnSkipIntro.animate().alpha(1f).setDuration(220).start();
            }
        } else {
            if (btnSkipIntro.getVisibility() == View.VISIBLE) {
                btnSkipIntro.setVisibility(View.GONE);
            }
        }
    }

    private void updateChapters() {
        if (vlcPlayer == null) return;
        try {
            MediaPlayer.Chapter[] chapters = vlcPlayer.getChapters(-1);
            if (chapters != null && chapters.length > 0) {
                List<Long> offsets = new ArrayList<>();
                for (MediaPlayer.Chapter c : chapters) {
                    if (c != null && c.timeOffset >= 0) {
                        offsets.add(c.timeOffset);
                    }
                }
                if (videoSeekBar != null && totalDurationMs > 0) {
                    videoSeekBar.setChapterPositions(offsets, totalDurationMs);
                }
                // If chapter 1 is intro (ends between 30s and 130s), adapt introEndMs
                if (chapters.length > 1 && chapters[1] != null) {
                    long firstChapEnd = chapters[1].timeOffset;
                    if (firstChapEnd >= 30000L && firstChapEnd <= 130000L) {
                        introEndMs = firstChapEnd;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void showChaptersDialog() {
        if (!isVlcActive) {
            Toast.makeText(this, "Activating Universal VLC Engine for chapters...", Toast.LENGTH_SHORT).show();
            long cur = exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
            startVlcPlayer(cur);
        }
        if (vlcPlayer == null) return;

        MediaPlayer.Chapter[] chapters = vlcPlayer.getChapters(-1);
        if (chapters == null || chapters.length == 0) {
            Toast.makeText(this, "No embedded chapters found in this video", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentChapter = vlcPlayer.getChapter();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < chapters.length; i++) {
            MediaPlayer.Chapter c = chapters[i];
            String name = (c.name != null && !c.name.trim().isEmpty()) ? c.name.trim() : ("CHAPTER " + (i + 1));
            String timeStr = formatTime(c.timeOffset);
            labels.add(String.format(java.util.Locale.US, "[%s]  %s", timeStr, name));
        }

        showNothingSelectionDialog("SELECT CHAPTER", labels, currentChapter, which -> {
            if (which >= 0 && which < chapters.length) {
                vlcPlayer.setChapter(which);
                long target = chapters[which].timeOffset;
                videoSeekBar.setProgress((int) target);
                updateTimeDisplay(target);
                showGestureHud("● CHAPTER", DotMatrixIconView.TYPE_SEEK_FORWARD, "CH " + (which + 1), (int) (target * 100 / Math.max(1, totalDurationMs)));
            }
        });
    }

    private void showSubtitleDelayDialog() {
        if (!isActivityAlive()) return;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_subtitle_delay);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvDisplay = dialog.findViewById(R.id.tv_sub_delay_display);
        Button btnMinus500 = dialog.findViewById(R.id.btn_sub_delay_minus_500);
        Button btnMinus100 = dialog.findViewById(R.id.btn_sub_delay_minus_100);
        Button btnReset = dialog.findViewById(R.id.btn_sub_delay_reset);
        Button btnPlus100 = dialog.findViewById(R.id.btn_sub_delay_plus_100);
        Button btnPlus500 = dialog.findViewById(R.id.btn_sub_delay_plus_500);
        Button btnDone = dialog.findViewById(R.id.btn_sub_delay_done);

        Runnable updateDisplay = () -> {
            subtitleDelayMs = Math.max(-60000L, Math.min(60000L, subtitleDelayMs));
            String text;
            if (Math.abs(subtitleDelayMs) < 10) {
                text = "0 MS";
            } else {
                text = String.format(java.util.Locale.US, "%+d MS", subtitleDelayMs);
            }
            if (tvDisplay != null) tvDisplay.setText(text);
            try {
                if (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased()) {
                    vlcPlayer.setSpuDelay(subtitleDelayMs * 1000L);
                }
            } catch (Exception ignored) {}
            long cur = isYouTubeActive ? youtubeCurrentPositionMs : (isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0));
            updateSecondarySubtitle(cur);
        };
        updateDisplay.run();

        if (btnMinus500 != null) btnMinus500.setOnClickListener(v -> {
            subtitleDelayMs -= 500L;
            updateDisplay.run();
        });
        if (btnMinus100 != null) btnMinus100.setOnClickListener(v -> {
            subtitleDelayMs -= 100L;
            updateDisplay.run();
        });
        if (btnReset != null) btnReset.setOnClickListener(v -> {
            subtitleDelayMs = 0L;
            updateDisplay.run();
        });
        if (btnPlus100 != null) btnPlus100.setOnClickListener(v -> {
            subtitleDelayMs += 100L;
            updateDisplay.run();
        });
        if (btnPlus500 != null) btnPlus500.setOnClickListener(v -> {
            subtitleDelayMs += 500L;
            updateDisplay.run();
        });
        if (btnDone != null) btnDone.setOnClickListener(v -> {
            saveCurrentSeriesSettings();
            showGestureHud("\u25cf SUB DELAY", DotMatrixIconView.TYPE_SEEK_FORWARD, String.format(java.util.Locale.US, "%+d MS", subtitleDelayMs), 100);
            dialog.dismiss();
        });

        trackDialog(dialog);
        dialog.show();
    }

    private void checkWatchedEpisodeOnEnd(boolean finishIfNotShown) {
        if (videoPath != null && !videoPath.isEmpty()) {
            File file = new File(videoPath);
            if (file.exists() && file.length() >= 10L * 1024L * 1024L) {
                showWatchedEpisodeCleanupDialog(videoPath, videoTitle != null ? videoTitle : file.getName(), finishIfNotShown);
                return;
            }
        }
        if (finishIfNotShown && !isFinishing()) {
            finish();
        }
    }

    private void showWatchedEpisodeCleanupDialog(String targetPath, String targetTitle, boolean finishOnDone) {
        if (!isActivityAlive()) {
            if (finishOnDone && !isFinishing()) finish();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_clean_watched);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialog.findViewById(R.id.tv_clean_watched_subtitle);
        LinearLayout layoutList = dialog.findViewById(R.id.layout_clean_episodes_list);
        TextView tvReclaimable = dialog.findViewById(R.id.tv_clean_total_reclaimable);
        Button btnKeep = dialog.findViewById(R.id.btn_clean_cancel);
        Button btnDelete = dialog.findViewById(R.id.btn_clean_confirm);

        if (tvSubtitle != null) {
            tvSubtitle.setText("EPISODE COMPLETED. DELETE TO FREE UP STORAGE?");
        }

        File targetFile = new File(targetPath);
        long bytes = targetFile.exists() ? targetFile.length() : 0L;
        double mb = bytes / (1024.0 * 1024.0);
        String sizeStr = mb >= 1024.0
                ? String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
                : String.format(java.util.Locale.US, "%.1f MB", mb);

        if (tvReclaimable != null) {
            tvReclaimable.setText("TOTAL RECLAIMABLE: " + sizeStr);
        }

        if (layoutList != null) {
            layoutList.removeAllViews();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(6), 0, dpToPx(6));

            TextView dot = new TextView(this);
            dot.setText("● ");
            dot.setTextColor(Color.parseColor("#D71921"));
            dot.setTextSize(10f);
            row.addView(dot);

            TextView name = new TextView(this);
            name.setText(targetTitle.toUpperCase());
            name.setTextColor(Color.WHITE);
            name.setTextSize(8.5f);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(lp);
            row.addView(name);

            TextView sz = new TextView(this);
            sz.setText(sizeStr);
            sz.setTextColor(Color.parseColor("#88FFFFFF"));
            sz.setTextSize(8f);
            row.addView(sz);

            layoutList.addView(row);
        }

        if (finishOnDone) {
            dialog.setOnCancelListener(d -> {
                if (!isFinishing()) finish();
            });
        }

        if (btnKeep != null) {
            btnKeep.setOnClickListener(v -> {
                dialog.dismiss();
                if (finishOnDone && !isFinishing()) finish();
            });
        }
        if (btnDelete != null) btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            final String uriSnapshot = videoUriStr;
            final Context appCtx = getApplicationContext();
            new Thread(() -> {
                boolean deleted = false;
                try {
                    if (targetFile.exists()) {
                        deleted = targetFile.delete();
                    }
                    if (!deleted && uriSnapshot != null && !uriSnapshot.isEmpty()) {
                        appCtx.getContentResolver().delete(Uri.parse(uriSnapshot), null, null);
                        deleted = true;
                    }
                } catch (Exception ignored) {}
                final boolean wasDeleted = deleted;
                runOnUiThread(() -> {
                    if (wasDeleted) {
                        Toast.makeText(appCtx, "Deleted & freed " + sizeStr, Toast.LENGTH_SHORT).show();
                        if (isActivityAlive()) {
                            showGestureHud("● STORAGE SAVED", DotMatrixIconView.TYPE_SEEK_FORWARD, sizeStr + " FREED", 100);
                        }
                    }
                    if (finishOnDone && !isFinishing()) finish();
                });
            }, "DeleteWatchedEpisode").start();
        });

        trackDialog(dialog);
        dialog.show();
    }

    // =========================================================================
    // FLOATING EQUALIZER WINDOW (Nothing OS Dot-Matrix HUD)
    // =========================================================================
    private void initFloatingEqualizer() {
        if (isFloatEqInitialized) return;
        windowFloatingEqualizer = findViewById(R.id.window_floating_equalizer);
        if (windowFloatingEqualizer == null) return;

        eqDragHeader = findViewById(R.id.eq_drag_header);
        eqBodyContainer = findViewById(R.id.eq_body_container);
        eqMinimizedPill = findViewById(R.id.eq_minimized_pill);
        tvMinimizedEqStatus = findViewById(R.id.tv_minimized_eq_status);

        floatEqPowerLed = findViewById(R.id.float_eq_power_led);
        switchFloatEqPower = findViewById(R.id.switch_float_eq_power);
        btnFloatEqReset = findViewById(R.id.btn_float_eq_reset);
        btnMinimizeFloatEq = findViewById(R.id.btn_minimize_float_eq);
        btnCloseFloatEq = findViewById(R.id.btn_close_float_eq);
        btnExpandFloatEq = findViewById(R.id.btn_expand_float_eq);
        btnCloseMinimizedEq = findViewById(R.id.btn_close_minimized_eq);

        floatBandSeekbars[0] = findViewById(R.id.seekbar_float_band_0);
        floatBandSeekbars[1] = findViewById(R.id.seekbar_float_band_1);
        floatBandSeekbars[2] = findViewById(R.id.seekbar_float_band_2);
        floatBandSeekbars[3] = findViewById(R.id.seekbar_float_band_3);
        floatBandSeekbars[4] = findViewById(R.id.seekbar_float_band_4);

        floatBandValTexts[0] = findViewById(R.id.tv_float_band_0_val);
        floatBandValTexts[1] = findViewById(R.id.tv_float_band_1_val);
        floatBandValTexts[2] = findViewById(R.id.tv_float_band_2_val);
        floatBandValTexts[3] = findViewById(R.id.tv_float_band_3_val);
        floatBandValTexts[4] = findViewById(R.id.tv_float_band_4_val);

        seekbarFloatBass = findViewById(R.id.seekbar_float_bass);
        seekbarFloatVirt = findViewById(R.id.seekbar_float_virtualizer);
        seekbarFloatPreamp = findViewById(R.id.seekbar_float_preamp);

        tvFloatBassVal = findViewById(R.id.tv_float_bass_val);
        tvFloatVirtVal = findViewById(R.id.tv_float_virtualizer_val);
        tvFloatPreampVal = findViewById(R.id.tv_float_preamp_val);

        floatPresetChips[0] = findViewById(R.id.preset_float_punch);
        floatPresetChips[1] = findViewById(R.id.preset_float_cinema);
        floatPresetChips[2] = findViewById(R.id.preset_float_bass);
        floatPresetChips[3] = findViewById(R.id.preset_float_edm);
        floatPresetChips[4] = findViewById(R.id.preset_float_rock);
        floatPresetChips[5] = findViewById(R.id.preset_float_pop);
        floatPresetChips[6] = findViewById(R.id.preset_float_vocal);
        floatPresetChips[7] = findViewById(R.id.preset_float_flat);

        // Setup Draggable Movement (Only on handle bar and title, not over buttons)
        View dragBar = findViewById(R.id.layout_drag_handle_bar);
        View titleGroup = findViewById(R.id.eq_title_group);
        setupFloatingEqualizerDraggable(dragBar);
        setupFloatingEqualizerDraggable(titleGroup);
        setupFloatingEqualizerDraggable(tvMinimizedEqStatus);

        // Setup Close & Minimize
        if (btnCloseFloatEq != null) {
            btnCloseFloatEq.setOnClickListener(v -> closeFloatingEqualizer());
        }
        if (btnCloseMinimizedEq != null) {
            btnCloseMinimizedEq.setOnClickListener(v -> closeFloatingEqualizer());
        }
        if (btnMinimizeFloatEq != null) {
            btnMinimizeFloatEq.setOnClickListener(v -> minimizeFloatingEqualizer());
        }
        if (btnExpandFloatEq != null) {
            btnExpandFloatEq.setOnClickListener(v -> expandFloatingEqualizer());
        }

        // Setup Power Switch
        SharedPreferences prefs = getSharedPreferences(AudioEffectManager.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isEqEnabled = prefs.getBoolean(AudioEffectManager.KEY_ENABLED, true);
        if (switchFloatEqPower != null) {
            switchFloatEqPower.setChecked(isEqEnabled);
            switchFloatEqPower.setText(isEqEnabled ? "ON" : "OFF");
            switchFloatEqPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(AudioEffectManager.KEY_ENABLED, isChecked).apply();
                switchFloatEqPower.setText(isChecked ? "ON" : "OFF");
                updateFloatEqPowerUI(isChecked);
                applyFloatingEqualizerAudioSettings();
            });
        }
        updateFloatEqPowerUI(isEqEnabled);

        // Setup Reset Button
        if (btnFloatEqReset != null) {
            btnFloatEqReset.setOnClickListener(v -> {
                applyFloatEqPreset(new int[]{15, 15, 15, 15, 15}, 60, 50, 12);
                selectFloatPresetChip(0);
            });
        }

        // Setup Band SeekBars
        for (int i = 0; i < 5; i++) {
            final int bandIndex = i;
            int savedLevel = prefs.getInt("eq_band_" + bandIndex, 15);
            if (floatBandSeekbars[bandIndex] != null) {
                floatBandSeekbars[bandIndex].setProgress(savedLevel);
                updateFloatBandText(bandIndex, savedLevel);
                floatBandSeekbars[bandIndex].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        updateFloatBandText(bandIndex, progress);
                        if (fromUser) {
                            prefs.edit().putInt("eq_band_" + bandIndex, progress).apply();
                            applyFloatingEqualizerAudioSettings();
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
        }

        // Setup Bass Boost
        int savedBass = prefs.getInt(AudioEffectManager.KEY_BASS, 60);
        if (seekbarFloatBass != null) {
            seekbarFloatBass.setProgress(savedBass);
            if (tvFloatBassVal != null) tvFloatBassVal.setText(savedBass + "%");
            seekbarFloatBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (tvFloatBassVal != null) tvFloatBassVal.setText(progress + "%");
                    if (fromUser) {
                        prefs.edit().putInt(AudioEffectManager.KEY_BASS, progress).apply();
                        applyFloatingEqualizerAudioSettings();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Setup 3D Virtualizer
        int savedVirt = prefs.getInt(AudioEffectManager.KEY_VIRTUALIZER, 50);
        if (seekbarFloatVirt != null) {
            seekbarFloatVirt.setProgress(savedVirt);
            if (tvFloatVirtVal != null) tvFloatVirtVal.setText(savedVirt + "%");
            seekbarFloatVirt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (tvFloatVirtVal != null) tvFloatVirtVal.setText(progress + "%");
                    if (fromUser) {
                        prefs.edit().putInt(AudioEffectManager.KEY_VIRTUALIZER, progress).apply();
                        applyFloatingEqualizerAudioSettings();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Setup Preamp Gain
        int savedPreamp = prefs.getInt(AudioEffectManager.KEY_PREAMP, 12);
        if (seekbarFloatPreamp != null) {
            seekbarFloatPreamp.setProgress(savedPreamp);
            updateFloatPreampText(savedPreamp);
            seekbarFloatPreamp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateFloatPreampText(progress);
                    if (fromUser) {
                        prefs.edit().putInt(AudioEffectManager.KEY_PREAMP, progress).apply();
                        applyFloatingEqualizerAudioSettings();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Setup Presets
        setupFloatPresetChips();

        isFloatEqInitialized = true;
    }

    private void setupFloatingEqualizerDraggable(View dragHandle) {
        if (dragHandle == null) return;
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private float initWindowX, initWindowY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (windowFloatingEqualizer == null) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        initWindowX = windowFloatingEqualizer.getX();
                        initWindowY = windowFloatingEqualizer.getY();
                        isDragging = false;
                        if (v.getParent() != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;
                        if (!isDragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) {
                            isDragging = true;
                        }
                        if (isDragging) {
                            View parent = (View) windowFloatingEqualizer.getParent();
                            if (parent != null) {
                                int maxW = parent.getWidth() - windowFloatingEqualizer.getWidth();
                                int maxH = parent.getHeight() - windowFloatingEqualizer.getHeight();
                                float targetX = Math.max(10, Math.min(Math.max(10, maxW - 10), initWindowX + dx));
                                float targetY = Math.max(10, Math.min(Math.max(10, maxH - 10), initWindowY + dy));
                                windowFloatingEqualizer.setX(targetX);
                                windowFloatingEqualizer.setY(targetY);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (v.getParent() != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        return isDragging;
                }
                return false;
            }
        });
    }

    private void updateFloatBandText(int index, int progress) {
        if (floatBandValTexts[index] != null) {
            int db = progress - 15;
            floatBandValTexts[index].setText((db > 0 ? "+" : "") + db + " dB");
        }
    }

    private void updateFloatPreampText(int progress) {
        if (tvFloatPreampVal != null) {
            int db = progress - 12;
            tvFloatPreampVal.setText((db > 0 ? "+" : "") + db + " dB");
        }
    }

    private void updateFloatEqPowerUI(boolean isEnabled) {
        if (floatEqPowerLed != null) {
            floatEqPowerLed.setAlpha(isEnabled ? 1.0f : 0.2f);
        }
        if (eqBodyContainer != null) {
            eqBodyContainer.setAlpha(isEnabled ? 1.0f : 0.4f);
        }
        for (SeekBar sb : floatBandSeekbars) {
            if (sb != null) sb.setEnabled(isEnabled);
        }
        if (seekbarFloatBass != null) seekbarFloatBass.setEnabled(isEnabled);
        if (seekbarFloatVirt != null) seekbarFloatVirt.setEnabled(isEnabled);
        if (seekbarFloatPreamp != null) seekbarFloatPreamp.setEnabled(isEnabled);
        if (btnFloatEqReset != null) btnFloatEqReset.setEnabled(isEnabled);
        if (tvMinimizedEqStatus != null) {
            tvMinimizedEqStatus.setText(isEnabled ? "EQ: ACTIVE" : "EQ: OFF (BYPASS)");
        }
    }

    private void setupFloatPresetChips() {
        if (floatPresetChips[0] != null) {
            floatPresetChips[0].setOnClickListener(v -> {
                selectFloatPresetChip(0);
                applyFloatEqPreset(new int[]{21, 18, 14, 18, 20}, 80, 60, -1);
            });
        }
        if (floatPresetChips[1] != null) {
            floatPresetChips[1].setOnClickListener(v -> {
                selectFloatPresetChip(1);
                applyFloatEqPreset(new int[]{24, 19, 17, 21, 23}, 90, 100, 14);
            });
        }
        if (floatPresetChips[2] != null) {
            floatPresetChips[2].setOnClickListener(v -> {
                selectFloatPresetChip(2);
                applyFloatEqPreset(new int[]{25, 22, 16, 13, 14}, 100, 40, -1);
            });
        }
        if (floatPresetChips[3] != null) {
            floatPresetChips[3].setOnClickListener(v -> {
                selectFloatPresetChip(3);
                applyFloatEqPreset(new int[]{22, 19, 13, 19, 23}, 85, 70, -1);
            });
        }
        if (floatPresetChips[4] != null) {
            floatPresetChips[4].setOnClickListener(v -> {
                selectFloatPresetChip(4);
                applyFloatEqPreset(new int[]{20, 16, 18, 21, 22}, 70, 50, -1);
            });
        }
        if (floatPresetChips[5] != null) {
            floatPresetChips[5].setOnClickListener(v -> {
                selectFloatPresetChip(5);
                applyFloatEqPreset(new int[]{13, 17, 21, 18, 14}, 50, 45, -1);
            });
        }
        if (floatPresetChips[6] != null) {
            floatPresetChips[6].setOnClickListener(v -> {
                selectFloatPresetChip(6);
                applyFloatEqPreset(new int[]{11, 14, 23, 20, 15}, 30, 30, -1);
            });
        }
        if (floatPresetChips[7] != null) {
            floatPresetChips[7].setOnClickListener(v -> {
                selectFloatPresetChip(7);
                applyFloatEqPreset(new int[]{15, 15, 15, 15, 15}, 0, 0, -1);
            });
        }
    }

    private void selectFloatPresetChip(int index) {
        for (int i = 0; i < floatPresetChips.length; i++) {
            TextView chip = floatPresetChips[i];
            if (chip == null) continue;
            if (i == index) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(getResources().getColor(R.color.nothing_black));
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setTextColor(getResources().getColor(R.color.nothing_white_70));
            }
        }
    }

    private void applyFloatEqPreset(int[] levels, int bass, int virt, int preamp) {
        SharedPreferences.Editor editor = getSharedPreferences(AudioEffectManager.PREFS_NAME, Context.MODE_PRIVATE).edit();
        for (int i = 0; i < 5; i++) {
            if (floatBandSeekbars[i] != null) floatBandSeekbars[i].setProgress(levels[i]);
            updateFloatBandText(i, levels[i]);
            editor.putInt("eq_band_" + i, levels[i]);
        }
        if (seekbarFloatBass != null) seekbarFloatBass.setProgress(bass);
        if (tvFloatBassVal != null) tvFloatBassVal.setText(bass + "%");
        editor.putInt(AudioEffectManager.KEY_BASS, bass);

        if (seekbarFloatVirt != null) seekbarFloatVirt.setProgress(virt);
        if (tvFloatVirtVal != null) tvFloatVirtVal.setText(virt + "%");
        editor.putInt(AudioEffectManager.KEY_VIRTUALIZER, virt);

        if (preamp >= 0) {
            if (seekbarFloatPreamp != null) seekbarFloatPreamp.setProgress(preamp);
            updateFloatPreampText(preamp);
            editor.putInt(AudioEffectManager.KEY_PREAMP, preamp);
        }
        editor.apply();
        applyFloatingEqualizerAudioSettings();
    }

    private void applyFloatingEqualizerAudioSettings() {
        if (isVlcActive && vlcPlayer != null) {
            AudioEffectManager.getInstance().applyVlcEqualizer(vlcPlayer, this);
        } else {
            AudioEffectManager.getInstance().applySettings(this);
        }
    }

    public void showFloatingEqualizer() {
        initFloatingEqualizer();
        if (windowFloatingEqualizer == null) return;

        applyFloatingEqualizerAudioSettings();

        // Expand body by default
        if (eqBodyContainer != null) eqBodyContainer.setVisibility(View.VISIBLE);
        if (eqMinimizedPill != null) eqMinimizedPill.setVisibility(View.GONE);

        if (windowFloatingEqualizer.getVisibility() != View.VISIBLE) {
            windowFloatingEqualizer.setAlpha(0f);
            windowFloatingEqualizer.setVisibility(View.VISIBLE);
            windowFloatingEqualizer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
    }

    public void closeFloatingEqualizer() {
        if (windowFloatingEqualizer != null && windowFloatingEqualizer.getVisibility() == View.VISIBLE) {
            windowFloatingEqualizer.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> windowFloatingEqualizer.setVisibility(View.GONE))
                    .start();
        }
    }

    private void minimizeFloatingEqualizer() {
        if (eqBodyContainer != null) eqBodyContainer.setVisibility(View.GONE);
        if (eqMinimizedPill != null) eqMinimizedPill.setVisibility(View.VISIBLE);
    }

    private void expandFloatingEqualizer() {
        if (eqMinimizedPill != null) eqMinimizedPill.setVisibility(View.GONE);
        if (eqBodyContainer != null) eqBodyContainer.setVisibility(View.VISIBLE);
    }


        private void showDotMatrixHud(String message) {
        showGestureHud("●", message, 100);
    }

    private void applyPlaybackSpeedWithPitchPreservation(float speed) {
        playbackSpeed = speed;
        if (exoPlayer != null) {
            try {
                exoPlayer.setPlaybackParameters(new PlaybackParameters(speed, 1.0f));
            } catch (Exception e) {
                exoPlayer.setPlaybackSpeed(speed);
            }
        }
        if (vlcPlayer != null) {
            try {
                vlcPlayer.setRate(speed);
            } catch (Exception ignored) {}
        }
    }

    private void savePreferredAudioLanguage(String lang) {
        if (lang == null || lang.trim().isEmpty() || prefs == null) return;
        String cleanLang = lang.trim().toLowerCase();
        prefs.edit().putString("preferred_audio_language", cleanLang).apply();
        saveCurrentSeriesSettings();
    }

    private void saveCurrentSeriesSettings() {
        if (currentSeriesKey == null || currentSeriesKey.isEmpty()) return;
        SeriesSettingsManager.SeriesSettings s = SeriesSettingsManager.load(this, currentSeriesKey);
        s.audioLang = prefs != null ? prefs.getString("preferred_audio_language", s.audioLang) : s.audioLang;
        s.subtitleDelayMs = subtitleDelayMs;
        s.playbackSpeed = playbackSpeed;
        s.introSkipSec = introEndMs > 0 ? (int) (introEndMs / 1000L) : -1;
        SeriesSettingsManager.save(this, currentSeriesKey, s);
    }

    private boolean isCurrentPlayerPaused() {
        if (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased()) {
            return !vlcPlayer.isPlaying();
        }
        return exoPlayer != null && !exoPlayer.isPlaying();
    }

    private void stepSingleFrame(int direction) {
        if (isYouTubeActive) return;
        long frameStepMs = 42L; // ~1 frame at 24fps
        if (isVlcActive && vlcPlayer != null && !vlcPlayer.isReleased()) {
            if (vlcPlayer.isPlaying()) vlcPlayer.pause();
            long cur = Math.max(0L, vlcPlayer.getTime());
            long target = Math.max(0L, Math.min(totalDurationMs > 0 ? totalDurationMs : Long.MAX_VALUE, cur + (direction * frameStepMs)));
            vlcPlayer.setTime(target);
            updateTimeDisplay(target);
            if (videoSeekBar != null) videoSeekBar.setProgress((int) target);
        } else if (exoPlayer != null) {
            if (exoPlayer.isPlaying()) exoPlayer.pause();
            long cur = Math.max(0L, exoPlayer.getCurrentPosition());
            long target = Math.max(0L, Math.min(totalDurationMs > 0 ? totalDurationMs : Long.MAX_VALUE, cur + (direction * frameStepMs)));
            exoPlayer.setSeekParameters(SeekParameters.EXACT);
            exoPlayer.seekTo(target);
            updateTimeDisplay(target);
            if (videoSeekBar != null) videoSeekBar.setProgress((int) target);
        }
        updatePlayPauseButtonState();
        showGestureHud("● FRAME STEP", direction > 0 ? DotMatrixIconView.TYPE_SEEK_FORWARD : DotMatrixIconView.TYPE_SEEK_REWIND,
                direction > 0 ? "+1 FRAME" : "-1 FRAME", 100);
    }

    private void showJumpToTimestampDialog() {
        if (!isActivityAlive() || isYouTubeActive) return;
        android.graphics.Typeface ndot = null;
        try {
            ndot = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.ndot);
        } catch (Throwable ignored) {}

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.bg_card_nothing);
        int pad = dpToPx(20);
        container.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("JUMP TO TIMESTAMP");
        title.setTextColor(Color.parseColor("#D71921"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        if (ndot != null) title.setTypeface(ndot, Typeface.BOLD);
        container.addView(title);

        EditText input = new EditText(this);
        input.setHint("HH:MM:SS  OR  MM:SS");
        long cur = isVlcActive ? (vlcPlayer != null ? vlcPlayer.getTime() : 0) : (exoPlayer != null ? exoPlayer.getCurrentPosition() : 0);
        input.setText(formatTime(cur));
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#66FFFFFF"));
        input.setBackgroundResource(R.drawable.bg_input_box);
        input.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        if (ndot != null) input.setTypeface(ndot);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(12);
        lp.bottomMargin = dpToPx(14);
        container.addView(input, lp);

        Button btnGo = new Button(this);
        btnGo.setText("SEEK NOW");
        btnGo.setTextColor(Color.WHITE);
        btnGo.setBackgroundResource(R.drawable.bg_circle_red);
        if (ndot != null) btnGo.setTypeface(ndot, Typeface.BOLD);
        container.addView(btnGo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(42)));

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setView(container).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnGo.setOnClickListener(v -> {
            long targetMs = parseTimestampToMs(input.getText().toString().trim());
            if (targetMs < 0) {
                Toast.makeText(this, "Enter valid time (e.g. 01:15:30 or 45:00)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (totalDurationMs > 0) targetMs = Math.min(totalDurationMs, targetMs);
            if (isVlcActive && vlcPlayer != null) vlcPlayer.setTime(targetMs);
            else if (exoPlayer != null) exoPlayer.seekTo(targetMs);
            if (videoSeekBar != null) videoSeekBar.setProgress((int) targetMs);
            updateTimeDisplay(targetMs);
            showGestureHud("● JUMPED TO", DotMatrixIconView.TYPE_SEEK_FORWARD, formatTime(targetMs),
                    (int) (targetMs * 100 / Math.max(1, totalDurationMs)));
            dialog.dismiss();
        });

        trackDialog(dialog);
        dialog.show();
    }

    public static long parseTimestampToMs(String text) {
        if (text == null || text.trim().isEmpty()) return -1L;
        String[] parts = text.trim().split(":");
        try {
            if (parts.length == 1) {
                return Math.max(0L, Long.parseLong(parts[0]) * 1000L);
            } else if (parts.length == 2) {
                long m = Long.parseLong(parts[0]);
                long s = Long.parseLong(parts[1]);
                return Math.max(0L, (m * 60L + s) * 1000L);
            } else if (parts.length == 3) {
                long h = Long.parseLong(parts[0]);
                long m = Long.parseLong(parts[1]);
                long s = Long.parseLong(parts[2]);
                return Math.max(0L, (h * 3600L + m * 60L + s) * 1000L);
            }
        } catch (NumberFormatException ignored) {}
        return -1L;
    }

    private void autoSelectPreferredAudioTrack(Tracks tracks) {
        if (tracks == null || trackSelector == null || prefs == null) return;
        String preferredLang = prefs.getString("preferred_audio_language", null);
        if (preferredLang == null || preferredLang.isEmpty()) return;

        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_AUDIO) {
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getTrackFormat(i);
                    String lang = format.language != null ? format.language.toLowerCase() : "";
                    String label = format.label != null ? format.label.toLowerCase() : "";
                    if ((!lang.isEmpty() && (lang.contains(preferredLang) || preferredLang.contains(lang))) ||
                        (!label.isEmpty() && (label.contains(preferredLang) || preferredLang.contains(label)))) {
                        if (!group.isTrackSelected(i)) {
                            try {
                                trackSelector.setParameters(
                                    trackSelector.buildUponParameters()
                                        .setPreferredAudioLanguage(preferredLang)
                                        .setOverrideForType(new TrackSelectionOverride(group.getMediaTrackGroup(), i))
                                );
                                showDotMatrixHud("AUDIO: " + (format.label != null ? format.label.toUpperCase() : preferredLang.toUpperCase()));
                            } catch (Exception ignored) {}
                        }
                        return;
                    }
                }
            }
        }
    }

    private void autoSelectPreferredVlcAudioTrack() {
        if (vlcPlayer == null || prefs == null) return;
        String preferredLang = prefs.getString("preferred_audio_language", null);
        if (preferredLang == null || preferredLang.isEmpty()) return;

        try {
            MediaPlayer.TrackDescription[] tracks = vlcPlayer.getAudioTracks();
            if (tracks == null) return;
            for (MediaPlayer.TrackDescription t : tracks) {
                if (t.name != null && t.name.toLowerCase().contains(preferredLang)) {
                    if (vlcPlayer.getAudioTrack() != t.id) {
                        vlcPlayer.setAudioTrack(t.id);
                        showDotMatrixHud("AUDIO: " + t.name.toUpperCase());
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    
    // ==========================================================
    // POCKET PROTECTION & SCREEN-OFF HARDWARE KEYS ENGINE
    // ==========================================================
    private final Handler volumeKeyHandler = new Handler(Looper.getMainLooper());
    private Runnable volumeUpLongPressRunnable = null;
    private Runnable volumeDownLongPressRunnable = null;
    private boolean isVolumeUpLongPressed = false;
    private boolean isVolumeDownLongPressed = false;
    private static final long VOLUME_LONG_PRESS_MS = 450;
    private boolean isScreenOff = false;
    private MediaSessionCompat pocketMediaSession = null;
    private VolumeProviderCompat pocketVolumeProvider = null;
    private boolean isScreenStateReceiverRegistered = false;

    private final BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                isScreenOff = true;
                updatePocketMediaSessionState();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                isScreenOff = false;
                updatePocketMediaSessionState();
            }
        }
    };

    private void initPocketMediaSession() {
        if (pocketMediaSession != null) return;
        try {
            pocketMediaSession = new MediaSessionCompat(this, "NothingPocketSession");
            pocketMediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onSkipToNext() {
                    playNextVideo();
                }

                @Override
                public void onSkipToPrevious() {
                    playPreviousVideo();
                }
            });
            pocketMediaSession.setPlaybackState(new android.support.v4.media.session.PlaybackStateCompat.Builder()
                    .setActions(android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            | android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            | android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .setState(android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING, 0L, 1.0f)
                    .build());
            pocketMediaSession.setActive(true);

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVol = audioManager != null ? audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) : 15;
            int curVol = audioManager != null ? audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) : 7;

            pocketVolumeProvider = new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, maxVol, curVol) {
                private long lastAdjustTime = 0;
                private int consecutiveCount = 0;
                private int lastDirection = 0;
                private final Handler handler = new Handler(Looper.getMainLooper());
                private Runnable singleTapRunnable;

                @Override
                public void onAdjustVolume(int direction) {
                    long now = System.currentTimeMillis();
                    if (direction == lastDirection && (now - lastAdjustTime) < 320) {
                        consecutiveCount++;
                    } else {
                        consecutiveCount = 1;
                        lastDirection = direction;
                    }
                    lastAdjustTime = now;

                    if (consecutiveCount >= 3) {
                        if (singleTapRunnable != null) handler.removeCallbacks(singleTapRunnable);
                        consecutiveCount = 0;
                        triggerPocketHardwareKeySkip(direction > 0);
                    } else {
                        if (singleTapRunnable != null) handler.removeCallbacks(singleTapRunnable);
                        singleTapRunnable = () -> {
                            if (audioManager != null) {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                            }
                            consecutiveCount = 0;
                        };
                        handler.postDelayed(singleTapRunnable, 260);
                    }
                }
            };
        } catch (Exception ignored) {}
    }

    private void updatePocketMediaSessionState() {
        boolean isSkipEnabled = (prefs != null) ? prefs.getBoolean("screen_off_volume_skip", true) : true;
        if (!isSkipEnabled) {
            if (pocketMediaSession != null) {
                pocketMediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
            }
            return;
        }

        if (isScreenOff || isPodcastModeActive) {
            initPocketMediaSession();
            if (pocketMediaSession != null && pocketVolumeProvider != null) {
                pocketMediaSession.setPlaybackState(new android.support.v4.media.session.PlaybackStateCompat.Builder()
                        .setActions(android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                | android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE)
                        .setState(android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING, 0L, 1.0f)
                        .build());
                pocketMediaSession.setActive(true);
                pocketMediaSession.setPlaybackToRemote(pocketVolumeProvider);
            }
        } else {
            if (pocketMediaSession != null) {
                pocketMediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
            }
        }
    }

    private void triggerPocketHardwareKeySkip(boolean isNext) {
        performHardwareKeyHapticFeedback();
        if (isNext) {
            playNextVideo();
            showGestureHud("● NEXT VIDEO", DotMatrixIconView.TYPE_SEEK_FORWARD, "VOL+ HOLD", 100);
        } else {
            playPreviousVideo();
            showGestureHud("● PREV VIDEO", DotMatrixIconView.TYPE_SEEK_REWIND, "VOL- HOLD", 100);
        }
    }

    @SuppressWarnings("deprecation")
    private void performHardwareKeyHapticFeedback() {
        try {
            Vibrator v = getVibratorCompat();
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(new long[]{0, 35, 50, 35}, -1));
                } else {
                    v.vibrate(50);
                }
            }
        } catch (Exception ignored) {}
    }

    private void adjustMediaVolume(int direction) {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int flags = (isPodcastModeActive || isScreenOff) ? 0 : AudioManager.FLAG_SHOW_UI;
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                boolean isSkipEnabled = (prefs != null) ? prefs.getBoolean("screen_off_volume_skip", true) : true;
                if (isSkipEnabled && (isLocked || isPodcastModeActive || isScreenOff)) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (event.getRepeatCount() == 0) {
                            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                                isVolumeUpLongPressed = false;
                                if (volumeUpLongPressRunnable != null) volumeKeyHandler.removeCallbacks(volumeUpLongPressRunnable);
                                volumeUpLongPressRunnable = () -> {
                                    isVolumeUpLongPressed = true;
                                    triggerPocketHardwareKeySkip(true);
                                };
                                volumeKeyHandler.postDelayed(volumeUpLongPressRunnable, VOLUME_LONG_PRESS_MS);
                            } else {
                                isVolumeDownLongPressed = false;
                                if (volumeDownLongPressRunnable != null) volumeKeyHandler.removeCallbacks(volumeDownLongPressRunnable);
                                volumeDownLongPressRunnable = () -> {
                                    isVolumeDownLongPressed = true;
                                    triggerPocketHardwareKeySkip(false);
                                };
                                volumeKeyHandler.postDelayed(volumeDownLongPressRunnable, VOLUME_LONG_PRESS_MS);
                            }
                        }
                        return true;
                    } else if (event.getAction() == KeyEvent.ACTION_UP) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                            if (volumeUpLongPressRunnable != null) {
                                volumeKeyHandler.removeCallbacks(volumeUpLongPressRunnable);
                                volumeUpLongPressRunnable = null;
                            }
                            if (isVolumeUpLongPressed) {
                                isVolumeUpLongPressed = false;
                                return true;
                            }
                            adjustMediaVolume(AudioManager.ADJUST_RAISE);
                            return true;
                        } else {
                            if (volumeDownLongPressRunnable != null) {
                                volumeKeyHandler.removeCallbacks(volumeDownLongPressRunnable);
                                volumeDownLongPressRunnable = null;
                            }
                            if (isVolumeDownLongPressed) {
                                isVolumeDownLongPressed = false;
                                return true;
                            }
                            adjustMediaVolume(AudioManager.ADJUST_LOWER);
                            return true;
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void setPodcastAudioMode(boolean active) {
        if (isPodcastModeActive == active) return;
        isPodcastModeActive = active;
        if (switchMenuPodcastMode != null && switchMenuPodcastMode.isChecked() != active) {
            switchMenuPodcastMode.setChecked(active);
        }
        if (active) {
            prePodcastBgPlayState = isBackgroundPlayActive;
            isBackgroundPlayActive = true;
            BackgroundPlaybackManager.getInstance().setBackgroundPlayActive(true);
            updateBackgroundPlayUi();
            if (tvPodcastTitle != null) {
                tvPodcastTitle.setText(videoTitle != null ? videoTitle.toUpperCase() : "PLAYING AUDIO");
            }
            if (layoutAudioOnlyOverlay != null) layoutAudioOnlyOverlay.setVisibility(View.VISIBLE);
            if (exoPlayerView != null) exoPlayerView.setVisibility(View.INVISIBLE);
            if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(View.INVISIBLE);
            if (youtubeStreamContainer != null) youtubeStreamContainer.setVisibility(View.INVISIBLE);
            if (youtubeStreamView != null) youtubeStreamView.setVisibility(View.INVISIBLE);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            if (previousScreenBrightness < 0) previousScreenBrightness = lp.screenBrightness;
            lp.screenBrightness = 0.01f;
            getWindow().setAttributes(lp);
            showDotMatrixHud("PODCAST MODE ACTIVE ● DOUBLE-TAP TO RESTORE");
            updatePocketMediaSessionState();
        } else {
            isBackgroundPlayActive = prePodcastBgPlayState;
            BackgroundPlaybackManager.getInstance().setBackgroundPlayActive(prePodcastBgPlayState);
            updateBackgroundPlayUi();
            if (layoutAudioOnlyOverlay != null) layoutAudioOnlyOverlay.setVisibility(View.GONE);
            if (exoPlayerView != null) exoPlayerView.setVisibility(!isVlcActive && !isYouTubeActive ? View.VISIBLE : View.GONE);
            if (vlcVideoLayout != null) vlcVideoLayout.setVisibility(isVlcActive && !isYouTubeActive ? View.VISIBLE : View.GONE);
            if (youtubeStreamContainer != null) youtubeStreamContainer.setVisibility(isYouTubeActive ? View.VISIBLE : View.GONE);
            if (youtubeStreamView != null) youtubeStreamView.setVisibility(isYouTubeActive ? View.VISIBLE : View.GONE);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            if (previousScreenBrightness >= 0) {
                lp.screenBrightness = previousScreenBrightness;
                previousScreenBrightness = -1f;
                getWindow().setAttributes(lp);
            }
            showDotMatrixHud("VIDEO MODE RESTORED");
            updatePocketMediaSessionState();
        }
    }

    private void applySubtitleStyling() {
        if (prefs == null) return;
        String fontPref = prefs.getString("sub_font_choice", "NDOT");
        boolean boxPref = prefs.getBoolean("sub_box_choice", true);
        int sizePref = prefs.getInt("sub_size_choice", 18);

        Typeface tf = null;
        if ("NDOT".equalsIgnoreCase(fontPref)) {
            try {
                tf = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.ndot);
            } catch (Exception ignored) {}
        }
        if (tf == null) {
            tf = Typeface.DEFAULT_BOLD;
        }

        if (tvSecondarySubtitle != null) {
            tvSecondarySubtitle.setTypeface(tf);
            tvSecondarySubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizePref);
            if (boxPref) {
                tvSecondarySubtitle.setBackgroundResource(R.drawable.bg_subtitle_capsule);
                int padH = (int) (12 * getResources().getDisplayMetrics().density);
                int padV = (int) (6 * getResources().getDisplayMetrics().density);
                tvSecondarySubtitle.setPadding(padH, padV, padH, padV);
            } else {
                tvSecondarySubtitle.setBackground(null);
                tvSecondarySubtitle.setPadding(0, 0, 0, 0);
            }
        }

        if (exoPlayerView != null) {
            try {
                SubtitleView subView = exoPlayerView.getSubtitleView();
                if (subView != null) {
                    subView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, sizePref);
                    int bg = boxPref ? Color.parseColor("#CC000000") : Color.TRANSPARENT;
                    CaptionStyleCompat style = new CaptionStyleCompat(
                        Color.WHITE,
                        bg,
                        Color.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                        Color.BLACK,
                        tf
                    );
                    subView.setStyle(style);
                }
            } catch (Exception ignored) {}
        }
    }

    private void showSubtitleStylingDialog() {
        if (!isActivityAlive()) return;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_subtitle_styling, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnFontNdot = dialogView.findViewById(R.id.btn_sub_font_ndot);
        Button btnFontSans = dialogView.findViewById(R.id.btn_sub_font_sans);
        Button btnBoxOn = dialogView.findViewById(R.id.btn_sub_box_on);
        Button btnBoxOff = dialogView.findViewById(R.id.btn_sub_box_off);
        Button btnSize14 = dialogView.findViewById(R.id.btn_sub_size_14);
        Button btnSize18 = dialogView.findViewById(R.id.btn_sub_size_18);
        Button btnSize22 = dialogView.findViewById(R.id.btn_sub_size_22);
        Button btnSize26 = dialogView.findViewById(R.id.btn_sub_size_26);
        Button btnDone = dialogView.findViewById(R.id.btn_sub_styling_done);

        final String[] currentFont = { prefs.getString("sub_font_choice", "NDOT") };
        final boolean[] currentBox = { prefs.getBoolean("sub_box_choice", true) };
        final int[] currentSize = { prefs.getInt("sub_size_choice", 18) };

        Runnable refreshUI = () -> {
            btnFontNdot.setBackgroundResource("NDOT".equalsIgnoreCase(currentFont[0]) ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
            btnFontSans.setBackgroundResource("SANS".equalsIgnoreCase(currentFont[0]) ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);

            btnBoxOn.setBackgroundResource(currentBox[0] ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
            btnBoxOff.setBackgroundResource(!currentBox[0] ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);

            btnSize14.setBackgroundResource(currentSize[0] == 14 ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
            btnSize18.setBackgroundResource(currentSize[0] == 18 ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
            btnSize22.setBackgroundResource(currentSize[0] == 22 ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
            btnSize26.setBackgroundResource(currentSize[0] == 26 ? R.drawable.bg_chip_red_active : R.drawable.bg_chip_dark_inactive);
        };
        refreshUI.run();

        btnFontNdot.setOnClickListener(v -> { currentFont[0] = "NDOT"; refreshUI.run(); });
        btnFontSans.setOnClickListener(v -> { currentFont[0] = "SANS"; refreshUI.run(); });

        btnBoxOn.setOnClickListener(v -> { currentBox[0] = true; refreshUI.run(); });
        btnBoxOff.setOnClickListener(v -> { currentBox[0] = false; refreshUI.run(); });

        btnSize14.setOnClickListener(v -> { currentSize[0] = 14; refreshUI.run(); });
        btnSize18.setOnClickListener(v -> { currentSize[0] = 18; refreshUI.run(); });
        btnSize22.setOnClickListener(v -> { currentSize[0] = 22; refreshUI.run(); });
        btnSize26.setOnClickListener(v -> { currentSize[0] = 26; refreshUI.run(); });

        btnDone.setOnClickListener(v -> {
            prefs.edit()
                .putString("sub_font_choice", currentFont[0])
                .putBoolean("sub_box_choice", currentBox[0])
                .putInt("sub_size_choice", currentSize[0])
                .apply();
            applySubtitleStyling();
            showDotMatrixHud("SUBTITLE STYLE APPLIED");
            dialog.dismiss();
        });

        trackDialog(dialog);
        dialog.show();
    }

    private void showInVideoDialogueSearchDialog() {
        if (!isActivityAlive()) return;
        List<SubtitleParser.SubtitleCue> existingCues = (searchableSubtitleCues != null && !searchableSubtitleCues.isEmpty())
                ? searchableSubtitleCues
                : secondarySubtitleCues;

        if (existingCues != null && !existingCues.isEmpty()) {
            openDialogueSearchDialogWithCues(existingCues);
            return;
        }

        showDotMatrixHud("● LOADING SUBTITLE CUES...");
        final Uri mediaUri = resolveMediaUri();
        subtitleIoExecutor.execute(() -> {
            List<SubtitleParser.SubtitleCue> loaded = loadAvailableSrtCuesSync();
            runOnUiThread(() -> {
                if (!isActivityAlive()) return;
                if (loaded != null && !loaded.isEmpty()) {
                    searchableSubtitleCues = loaded;
                    openDialogueSearchDialogWithCues(loaded);
                } else {
                    EmbeddedSubtitleExtractor.extractSubtitlesAsync(this, mediaUri, videoPath, new EmbeddedSubtitleExtractor.ExtractionCallback() {
                        @Override
                        public void onSuccess(List<EmbeddedSubtitleExtractor.SubtitleTrackInfo> tracks) {
                            if (!isActivityAlive()) return;
                            if (tracks != null && !tracks.isEmpty() && tracks.get(0).cues != null && !tracks.get(0).cues.isEmpty()) {
                                searchableSubtitleCues = tracks.get(0).cues;
                                openDialogueSearchDialogWithCues(searchableSubtitleCues);
                            } else {
                                showEmptyDialogueSearchPrompt();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (!isActivityAlive()) return;
                            showEmptyDialogueSearchPrompt();
                        }
                    });
                }
            });
        });
    }

    private void showEmptyDialogueSearchPrompt() {
        if (!isActivityAlive()) return;
        NothingDialogHelper.showConfirmDialog(
                this,
                "NO SUBTITLE LOADED",
                "No subtitle is active for search. Would you like to select an .SRT subtitle file from storage?",
                "SELECT .SRT",
                this::showDualSubtitleDialog,
                "CANCEL",
                null
        );
    }

    private void openDialogueSearchDialogWithCues(List<SubtitleParser.SubtitleCue> activeCues) {
        if (!isActivityAlive() || activeCues == null || activeCues.isEmpty()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_dialogue, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etQuery = dialogView.findViewById(R.id.et_search_dialogue_query);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_dialogue_search);
        ListView lvResults = dialogView.findViewById(R.id.lv_dialogue_results);
        TextView tvStatus = dialogView.findViewById(R.id.tv_dialogue_search_status);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_dialogue_empty);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        final List<SubtitleParser.SubtitleCue> allCues = new ArrayList<>(activeCues);
        final List<SubtitleParser.SubtitleCue> filteredCues = new ArrayList<>(allCues);

        android.widget.BaseAdapter cueAdapter = new android.widget.BaseAdapter() {
            @Override public int getCount() { return filteredCues.size(); }
            @Override public Object getItem(int pos) { return filteredCues.get(pos); }
            @Override public long getItemId(int pos) { return pos; }
            @Override public View getView(int pos, View cv, ViewGroup p) {
                if (cv == null) {
                    cv = LayoutInflater.from(ExoVideoPlayerActivity.this).inflate(R.layout.item_dialogue_result, p, false);
                }
                SubtitleParser.SubtitleCue cue = filteredCues.get(pos);
                TextView tvTime = cv.findViewById(R.id.tv_dialogue_timestamp);
                TextView tvText = cv.findViewById(R.id.tv_dialogue_text);
                View btnDict = cv.findViewById(R.id.btn_dialogue_dict);

                long sec = cue.startTimeMs / 1000;
                long m = sec / 60;
                long s = sec % 60;
                long h = m / 60;
                String timeStr = h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m % 60, s) : String.format(java.util.Locale.US, "%02d:%02d", m, s);

                if (tvTime != null) tvTime.setText(timeStr);
                if (tvText != null) tvText.setText(cue.text != null ? cue.text.replace("\n", " ") : "");

                if (btnDict != null) {
                    btnDict.setOnClickListener(v -> {
                        if (cue.text != null && !cue.text.isEmpty()) {
                            SubtitleDictionaryManager.showWordSelectorDialog(ExoVideoPlayerActivity.this, cue.text, (word, sentence) -> {
                                showDotMatrixHud("● LOOKUP: " + word.toUpperCase(Locale.US));
                                SubtitleDictionaryManager.lookupWord(ExoVideoPlayerActivity.this, word, sentence, new SubtitleDictionaryManager.DictionaryCallback() {
                                    @Override
                                    public void onResult(SubtitleDictionaryManager.WordDefinition definition) {
                                        if (!isActivityAlive()) return;
                                        SubtitleDictionaryManager.showDictionaryDialog(ExoVideoPlayerActivity.this, definition, null);
                                    }

                                    @Override
                                    public void onError(String message) {
                                        if (!isActivityAlive()) return;
                                        Toast.makeText(ExoVideoPlayerActivity.this, "Lookup: " + message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                        }
                    });
                }
                return cv;
            }
        };

        lvResults.setAdapter(cueAdapter);

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase(Locale.US);
                filteredCues.clear();
                if (q.isEmpty()) {
                    filteredCues.addAll(allCues);
                } else {
                    for (SubtitleParser.SubtitleCue cue : allCues) {
                        if (cue.text != null && cue.text.toLowerCase(Locale.US).contains(q)) {
                            filteredCues.add(cue);
                        }
                    }
                }
                if (tvStatus != null) {
                    tvStatus.setText("FOUND " + filteredCues.size() + " DIALOGUE MATCHES");
                }
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(filteredCues.isEmpty() ? View.VISIBLE : View.GONE);
                }
                cueAdapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        lvResults.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < filteredCues.size()) {
                SubtitleParser.SubtitleCue targetCue = filteredCues.get(position);
                long seekTime = targetCue.startTimeMs;
                if (isYouTubeActive && youtubeStreamView != null) {
                    youtubeStreamView.evaluateJavascript("if (player && player.seekTo) player.seekTo(" + (seekTime / 1000f) + ", true);", null);
                } else if (isVlcActive && vlcPlayer != null) {
                    vlcPlayer.setTime(seekTime);
                } else if (exoPlayer != null) {
                    exoPlayer.seekTo(seekTime);
                }
                long sec = seekTime / 1000;
                String timeStr = String.format(java.util.Locale.US, "%02d:%02d", sec / 60, sec % 60);
                showDotMatrixHud("JUMP TO DIALOGUE ● [" + timeStr + "]");
                dialog.dismiss();
            }
        });

        trackDialog(dialog);
        dialog.show();
    }


    private void putStereoDownmixMatrix(int inChannels, float[] baseLrCoeffs, float leftGain, float rightGain) {
        if (channelMixingAudioProcessor == null || baseLrCoeffs.length != inChannels * 2) return;
        float[] coeffs = new float[inChannels * 2];
        for (int i = 0; i < inChannels; i++) {
            float l = baseLrCoeffs[i * 2];
            float r = baseLrCoeffs[i * 2 + 1];
            if (isMonoDownmixEnabled) {
                float mono = 0.5f * (l + r);
                coeffs[i * 2] = mono * leftGain;
                coeffs[i * 2 + 1] = mono * rightGain;
            } else {
                coeffs[i * 2] = l * leftGain;
                coeffs[i * 2 + 1] = r * rightGain;
            }
        }
        channelMixingAudioProcessor.putChannelMixingMatrix(new ChannelMixingMatrix(inChannels, 2, coeffs));
    }

    private void applyAudioBalance() {
        if (channelMixingAudioProcessor == null) return;
        SharedPreferences sp = (prefs != null) ? prefs : getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        int balance = sp.getInt("audio_balance_lr", 0); // -100 to +100
        float leftGain = balance <= 0 ? 1.0f : (100f - balance) / 100f;
        float rightGain = balance >= 0 ? 1.0f : (100f + balance) / 100f;
        try {
            putStereoDownmixMatrix(1, new float[]{1.0f, 1.0f}, leftGain, rightGain);
            putStereoDownmixMatrix(2, new float[]{1.0f, 0.0f, 0.0f, 1.0f}, leftGain, rightGain);
            putStereoDownmixMatrix(3, new float[]{0.58f, 0.0f, 0.0f, 0.58f, 0.41f, 0.41f}, leftGain, rightGain);
            putStereoDownmixMatrix(4, new float[]{0.58f, 0.0f, 0.0f, 0.58f, 0.41f, 0.0f, 0.0f, 0.41f}, leftGain, rightGain);
            putStereoDownmixMatrix(5, new float[]{0.50f, 0.0f, 0.0f, 0.50f, 0.35f, 0.35f, 0.35f, 0.0f, 0.0f, 0.35f}, leftGain, rightGain);
            // 5.1 Surround (FL, FR, C, LFE, SL, SR) ITU downmix with LFE weight
            putStereoDownmixMatrix(6, new float[]{
                0.58f, 0.00f,
                0.00f, 0.58f,
                0.41f, 0.41f,
                0.20f, 0.20f,
                0.41f, 0.00f,
                0.00f, 0.41f
            }, leftGain, rightGain);
            putStereoDownmixMatrix(7, new float[]{
                0.50f, 0.00f, 0.00f, 0.50f, 0.35f, 0.35f, 0.18f, 0.18f, 0.25f, 0.25f, 0.35f, 0.00f, 0.00f, 0.35f
            }, leftGain, rightGain);
            // 7.1 Surround (FL, FR, C, LFE, BL, BR, SL, SR)
            putStereoDownmixMatrix(8, new float[]{
                0.50f, 0.00f,
                0.00f, 0.50f,
                0.35f, 0.35f,
                0.18f, 0.18f,
                0.35f, 0.00f,
                0.00f, 0.35f,
                0.35f, 0.00f,
                0.00f, 0.35f
            }, leftGain, rightGain);
        } catch (Exception ignored) {}
    }

    private void applyAccentColorTheme() {
        SharedPreferences sp = (prefs != null) ? prefs : getSharedPreferences("nothing_player_prefs", MODE_PRIVATE);
        String hex = sp.getString("accent_color_hex", "#D71921");
        try {
            int color = Color.parseColor(hex);
            if (videoSeekBar != null) videoSeekBar.setActiveColor(color);
        } catch (Exception ignored) {}
    }

    private void checkAndApplyRefreshRate() {
        if (prefs == null || !prefs.getBoolean("match_display_refresh_rate", false)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && exoPlayer != null) {
            try {
                Format format = exoPlayer.getVideoFormat();
                if (format != null && format.frameRate > 10f) {
                    float fps = format.frameRate;
                    android.view.Display display = getWindowManager().getDefaultDisplay();
                    android.view.Display.Mode[] modes = display.getSupportedModes();
                    android.view.Display.Mode bestMode = null;
                    float bestDiff = Float.MAX_VALUE;
                    for (android.view.Display.Mode m : modes) {
                        float rate = m.getRefreshRate();
                        float diff = Math.abs(rate - fps);
                        float modDiff = (fps > 0) ? (rate % fps) : 999f;
                        if (diff < bestDiff || (modDiff < 0.1f && diff < bestDiff)) {
                            bestDiff = diff;
                            bestMode = m;
                        }
                    }
                    if (bestMode != null) {
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.preferredDisplayModeId = bestMode.getModeId();
                        getWindow().setAttributes(lp);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

}
