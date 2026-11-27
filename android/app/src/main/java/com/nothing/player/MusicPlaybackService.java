package com.nothing.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.content.SharedPreferences;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.io.File;

public class MusicPlaybackService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final String CHANNEL_ID = "nothing_music_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_PLAY = "com.nothing.player.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.nothing.player.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.nothing.player.ACTION_RESUME";
    public static final String ACTION_TOGGLE = "com.nothing.player.ACTION_TOGGLE";
    public static final String ACTION_SEEK = "com.nothing.player.ACTION_SEEK";
    public static final String ACTION_STOP = "com.nothing.player.ACTION_STOP";
    public static final String ACTION_NEXT = "com.nothing.player.ACTION_NEXT";
    public static final String ACTION_PREV = "com.nothing.player.ACTION_PREV";

    public static MusicPlaybackService instance;

    private MediaPlayer mediaPlayer;
    private MediaPlayer nextMediaPlayer;
    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private BroadcastReceiver noisyAudioReceiver;
    private BroadcastReceiver screenStateReceiver;
    private boolean isNoisyReceiverRegistered = false;
    private boolean isScreenStateReceiverRegistered = false;
    private boolean isScreenOff = false;
    private boolean isPreparing = false;
    private boolean isNextPrepared = false;
    private boolean pauseRequestedWhilePreparing = false;
    private boolean pausedByTransientFocusLoss = false;
    private final Handler crossfadeHandler = new Handler(Looper.getMainLooper());

    private String currentTitle = "Nothing Player";
    private String currentArtist = "Unknown Artist";
    private String currentUriStr = "";
    private String currentPath = "";
    private String nextTitle = "";
    private String nextArtist = "";
    private String nextUriStr = "";
    private String nextPath = "";
    private float currentReplayGainScalar = 1.0f;
    private float nextReplayGainScalar = 1.0f;
    private Bitmap currentAlbumArt = null;
    private final ExecutorService artExecutor = Executors.newSingleThreadExecutor();
    private boolean isPlaying = false;

    public interface PlaybackCallback {
        void onStateChanged(boolean playing);
        void onTrackEnded();
        default void onGaplessTrackStarted(String nextPath, String nextUriStr) {}
        default void onNextRequested() {}
        default void onPrevRequested() {}
    }

    private void notifyNextRequested() {
        if (eventListener != null) {
            eventListener.onNextRequested();
        }
        if (callback != null) {
            callback.onNextRequested();
        }
    }

    private void notifyPrevRequested() {
        if (eventListener != null) {
            eventListener.onPrevRequested();
        }
        if (callback != null) {
            callback.onPrevRequested();
        }
    }

    private PlaybackCallback callback;

    public void setCallback(PlaybackCallback cb) {
        this.callback = cb;
    }

    public interface PlaybackEventListener {
        void onTrackEnded();
        void onPlayStateChanged(boolean isPlaying);
        void onNextRequested();
        void onPrevRequested();
    }

    private static PlaybackEventListener eventListener;

    public static void setEventListener(PlaybackEventListener listener) {
        eventListener = listener;
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        initMediaSession();
        initMediaPlayer();
        initAudioSystem();
    }

    private MediaPlayer createConfiguredMediaPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        SharedPreferences prefs = getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
        boolean dacPassthrough = prefs.getBoolean("pref_dac_passthrough", false);
        AudioAttributes.Builder attrs = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA);
        if (dacPassthrough && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            attrs.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL);
        }
        mp.setAudioAttributes(attrs.build());
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
        return mp;
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = createConfiguredMediaPlayer();
        }
    }

    private void releaseNextMediaPlayer() {
        isNextPrepared = false;
        nextTitle = "";
        nextArtist = "";
        nextUriStr = "";
        nextPath = "";
        nextReplayGainScalar = 1.0f;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setNextMediaPlayer(null);
            } catch (Exception ignored) {}
        }
        if (nextMediaPlayer != null) {
            try {
                nextMediaPlayer.release();
            } catch (Exception ignored) {}
            nextMediaPlayer = null;
        }
    }

    private Uri resolveAudioUri(String uriStr, String path) {
        if (uriStr != null && !uriStr.isEmpty()) {
            return Uri.parse(uriStr);
        }
        if (path != null && !path.isEmpty()) {
            if (path.startsWith("content://")) {
                return Uri.parse(path);
            }
            File f = new File(path);
            if (f.exists()) {
                return Uri.fromFile(f);
            }
            return Uri.parse(path);
        }
        return null;
    }

    private float readReplayGainScalar(String path) {
        SharedPreferences p = getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
        if (!p.getBoolean("pref_replay_gain_leveler", true)) return 1.0f;
        Float db = ReplayGainParser.parseGainDbFromFile(path);
        return db != null ? ReplayGainParser.dbToLinearAttenuation(db) : 1.0f;
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "NothingMusicSession");
        mediaSession.setActive(true);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                notifyNextRequested();
            }

            @Override
            public void onSkipToPrevious() {
                notifyPrevRequested();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo(pos);
            }

            @Override
            public void onStop() {
                stop();
            }
        });
        setupVolumeProvider();
    }

    private VolumeProviderCompat volumeProvider;

    public void setupVolumeProvider() {
        SharedPreferences prefs = getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
        boolean isSkipEnabled = prefs.getBoolean("screen_off_volume_skip", true);
        if (mediaSession == null) return;

        if (isSkipEnabled && isScreenOff) {
            if (volumeProvider == null) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int maxVol = am != null ? am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) : 15;
                int curVol = am != null ? am.getStreamVolume(AudioManager.STREAM_MUSIC) : 7;
                volumeProvider = new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, maxVol, curVol) {
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
                            triggerHapticPulse();
                            if (direction > 0) {
                                notifyNextRequested();
                            } else {
                                notifyPrevRequested();
                            }
                        } else {
                            if (singleTapRunnable != null) handler.removeCallbacks(singleTapRunnable);
                            singleTapRunnable = () -> {
                                if (am != null) {
                                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                                    setCurrentVolume(am.getStreamVolume(AudioManager.STREAM_MUSIC));
                                }
                                consecutiveCount = 0;
                            };
                            handler.postDelayed(singleTapRunnable, 260);
                        }
                    }
                };
            }
            if (audioManager != null) {
                volumeProvider.setCurrentVolume(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
            mediaSession.setPlaybackToRemote(volumeProvider);
        } else {
            mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        }
    }

    private void triggerHapticPulse() {
        try {
            Vibrator v = getVibratorCompat();
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(new long[]{0, 35, 50, 35}, -1));
                } else {
                    vibrateLegacy(v, 50);
                }
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("deprecation")
    private Vibrator getVibratorCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @SuppressWarnings("deprecation")
    private void vibrateLegacy(Vibrator v, long ms) {
        v.vibrate(ms);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Nothing Player Music",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background Music Playback Controls");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_PLAY:
                    String title = intent.getStringExtra("title");
                    String artist = intent.getStringExtra("artist");
                    String uriStr = intent.getStringExtra("uri");
                    String path = intent.getStringExtra("path");
                    playTrack(title, artist, uriStr, path);
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_RESUME:
                    resume();
                    break;
                case ACTION_TOGGLE:
                    if (isPlaying()) {
                        pause();
                    } else {
                        resume();
                    }
                    break;
                case ACTION_NEXT:
                    notifyNextRequested();
                    break;
                case ACTION_PREV:
                    notifyPrevRequested();
                    break;
                case ACTION_SEEK:
                    long seekPos = intent.getLongExtra("position", -1L);
                    if (seekPos >= 0) {
                        seekTo(seekPos);
                    }
                    break;
                case ACTION_STOP:
                    stop();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    public void playMedia(String path, String title, String artist) {
        playTrack(title, artist, null, path);
    }

    public void playTrack(String title, String artist, String uriStr, String path) {
        try {
            this.currentTitle = (title != null && !title.isEmpty()) ? title : "Nothing Track";
            this.currentArtist = (artist != null && !artist.isEmpty()) ? artist : "Nothing Player";
            this.currentUriStr = uriStr;
            this.currentPath = path;
            this.currentReplayGainScalar = 1.0f;

            crossfadeHandler.removeCallbacksAndMessages(null);
            releaseNextMediaPlayer();
            pauseRequestedWhilePreparing = false;
            pausedByTransientFocusLoss = false;
            initMediaPlayer();
            mediaPlayer.reset();

            Uri uri = resolveAudioUri(uriStr, path);

            if (uri != null) {
                clearAlbumArt();
                loadAlbumArtAsync(uri, uriStr, path);
                if (path != null && !artExecutor.isShutdown()) {
                    final String pSnap = path;
                    artExecutor.execute(() -> {
                        float scalar = readReplayGainScalar(pSnap);
                        crossfadeHandler.post(() -> {
                            if (pSnap.equals(currentPath) && mediaPlayer != null && !isPreparing) {
                                currentReplayGainScalar = scalar;
                                applyEffectiveVolume();
                            }
                        });
                    });
                }
                mediaPlayer.setDataSource(getApplicationContext(), uri);
                isPreparing = true;
                isPlaying = false;
                startForegroundNotification();
                mediaPlayer.setOnPreparedListener(mp -> {
                    isPreparing = false;
                    if (pauseRequestedWhilePreparing) {
                        pauseRequestedWhilePreparing = false;
                        isPlaying = false;
                        updateMediaSessionMetadata();
                        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        updateNotification();
                        return;
                    }
                    try {
                        if (!requestAudioFocus()) {
                            isPlaying = false;
                            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                            updateNotification();
                            return;
                        }
                        registerNoisyReceiver();
                        mp.start();
                        isPlaying = true;
                        applyCrossfadeStart();
                        AudioEffectManager.getInstance().attachAudioSession(mp.getAudioSessionId(), MusicPlaybackService.this);
                        updateMediaSessionMetadata();
                        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                        setupVolumeProvider();
                        updateNotification();
                        if (eventListener != null) {
                            eventListener.onPlayStateChanged(true);
                        }
                        if (callback != null) {
                            callback.onStateChanged(true);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                mediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            isPreparing = false;
            e.printStackTrace();
        }
    }

    /**
     * Preloads and links the next track natively via MediaPlayer.setNextMediaPlayer
     * for true zero-gap transition between queued songs.
     */
    public void preloadNextGaplessTrack(String title, String artist, String uriStr, String path) {
        releaseNextMediaPlayer();
        Uri uri = resolveAudioUri(uriStr, path);
        if (uri == null || mediaPlayer == null) return;
        try {
            this.nextTitle = (title != null && !title.isEmpty()) ? title : "Nothing Track";
            this.nextArtist = (artist != null && !artist.isEmpty()) ? artist : "Nothing Player";
            this.nextUriStr = uriStr != null ? uriStr : "";
            this.nextPath = path != null ? path : "";
            this.nextReplayGainScalar = 1.0f;

            if (path != null && !artExecutor.isShutdown()) {
                final String nextSnap = path;
                artExecutor.execute(() -> {
                    float scalar = readReplayGainScalar(nextSnap);
                    if (nextSnap.equals(nextPath)) {
                        nextReplayGainScalar = scalar;
                    }
                });
            }

            final MediaPlayer np = createConfiguredMediaPlayer();
            nextMediaPlayer = np;
            np.setDataSource(getApplicationContext(), uri);
            np.setOnPreparedListener(mp -> {
                if (mp != nextMediaPlayer || mediaPlayer == null || isPreparing) {
                    return;
                }
                try {
                    mp.setVolume(nextReplayGainScalar, nextReplayGainScalar);
                    mediaPlayer.setNextMediaPlayer(mp);
                    isNextPrepared = true;
                } catch (Exception e) {
                    isNextPrepared = false;
                }
            });
            np.prepareAsync();
        } catch (Exception ignored) {
            releaseNextMediaPlayer();
        }
    }

    private void applyEffectiveVolume() {
        if (mediaPlayer == null) return;
        try {
            mediaPlayer.setVolume(currentReplayGainScalar, currentReplayGainScalar);
        } catch (Exception ignored) {}
    }

    private void applyCrossfadeStart() {
        if (mediaPlayer == null) return;
        crossfadeHandler.removeCallbacksAndMessages(null);
        SharedPreferences p = getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
        String crossfadePref = p.getString("pref_crossfade_sec", "OFF (0S)");
        int sec = 0;
        if (crossfadePref.contains("2")) sec = 2;
        else if (crossfadePref.contains("4")) sec = 4;
        else if (crossfadePref.contains("6")) sec = 6;
        if (sec <= 0) {
            applyEffectiveVolume();
            return;
        }
        final int durationSec = sec;
        final int steps = 20;
        final long interval = (durationSec * 1000L) / steps;
        mediaPlayer.setVolume(0.0f, 0.0f);
        for (int i = 1; i <= steps; i++) {
            final float vol = (i / (float) steps) * currentReplayGainScalar;
            crossfadeHandler.postDelayed(() -> {
                try {
                    if (mediaPlayer != null && isPlaying) {
                        mediaPlayer.setVolume(vol, vol);
                    }
                } catch (Exception ignored) {}
            }, i * interval);
        }
    }

    public void pause() {
        crossfadeHandler.removeCallbacksAndMessages(null);
        if (isPreparing) {
            pauseRequestedWhilePreparing = true;
            this.isPlaying = false;
            if (eventListener != null) eventListener.onPlayStateChanged(false);
            if (callback != null) callback.onStateChanged(false);
            updateNotification();
            return;
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            this.isPlaying = false;
            abandonAudioFocus();
            unregisterNoisyReceiver();
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            updateNotification();
            if (eventListener != null) {
                eventListener.onPlayStateChanged(false);
            }
            if (callback != null) {
                callback.onStateChanged(false);
            }
        }
    }

    public void resume() {
        if (mediaPlayer == null) return;
        pauseRequestedWhilePreparing = false;
        if (isPreparing) return;
        if (!requestAudioFocus()) return;
        registerNoisyReceiver();
        applyEffectiveVolume();
        mediaPlayer.start();
        this.isPlaying = true;
        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        startForegroundNotification();
        if (eventListener != null) {
            eventListener.onPlayStateChanged(true);
        }
        if (callback != null) {
            callback.onStateChanged(true);
        }
    }

    public void seekTo(long positionMs) {
        if (mediaPlayer != null && !isPreparing) {
            try {
                mediaPlayer.seekTo((int) positionMs);
                updateMediaSessionPlaybackState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void seekTo(double seconds) {
        seekTo((long) (seconds * 1000));
    }

    public void stop() {
        crossfadeHandler.removeCallbacksAndMessages(null);
        isPreparing = false;
        pauseRequestedWhilePreparing = false;
        pausedByTransientFocusLoss = false;
        abandonAudioFocus();
        unregisterNoisyReceiver();
        releaseNextMediaPlayer();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        this.isPlaying = false;
        clearAlbumArt();
        AudioEffectManager.getInstance().release(this);
        if (mediaSession != null) {
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        if (eventListener != null) {
            eventListener.onPlayStateChanged(false);
        }
    }

    public int getAudioSessionId() {
        return mediaPlayer != null ? mediaPlayer.getAudioSessionId() : 0;
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public long getCurrentPosition() {
        try {
            return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getDuration() {
        try {
            return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public String getCurrentArtist() {
        return currentArtist;
    }

    private void initAudioSystem() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        noisyAudioReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    pause();
                }
            }
        };
        screenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    isScreenOff = true;
                    setupVolumeProvider();
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    isScreenOff = false;
                    setupVolumeProvider();
                }
            }
        };
        try {
            IntentFilter sf = new IntentFilter();
            sf.addAction(Intent.ACTION_SCREEN_OFF);
            sf.addAction(Intent.ACTION_SCREEN_ON);
            ContextCompat.registerReceiver(this, screenStateReceiver, sf, ContextCompat.RECEIVER_EXPORTED);
            isScreenStateReceiverRegistered = true;
        } catch (Exception ignored) {}
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                pausedByTransientFocusLoss = false;
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) {
                    pausedByTransientFocusLoss = true;
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null) {
                    try { mediaPlayer.setVolume(0.2f, 0.2f); } catch (Exception ignored) {}
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (pausedByTransientFocusLoss) {
                    pausedByTransientFocusLoss = false;
                    resume();
                } else if (mediaPlayer != null) {
                    applyEffectiveVolume();
                }
                break;
        }
    };

    private boolean requestAudioFocus() {
        if (audioManager == null) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build();
            }
            return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            return requestAudioFocusLegacy();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean requestAudioFocusLegacy() {
        return audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            abandonAudioFocusLegacy();
        }
    }

    @SuppressWarnings("deprecation")
    private void abandonAudioFocusLegacy() {
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    private synchronized void registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered && noisyAudioReceiver != null) {
            try {
                ContextCompat.registerReceiver(this, noisyAudioReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_EXPORTED);
                isNoisyReceiverRegistered = true;
            } catch (Exception ignored) {}
        }
    }

    private synchronized void unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered && noisyAudioReceiver != null) {
            try {
                unregisterReceiver(noisyAudioReceiver);
                isNoisyReceiverRegistered = false;
            } catch (Exception ignored) {}
        }
    }

    private void updateMediaSessionMetadata() {
        if (mediaSession == null) return;
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Nothing Music")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());
        if (currentAlbumArt != null && !currentAlbumArt.isRecycled()) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentAlbumArt);
        }
        mediaSession.setMetadata(builder.build());
    }

    private void clearAlbumArt() {
        if (currentAlbumArt != null) {
            if (!currentAlbumArt.isRecycled()) {
                currentAlbumArt.recycle();
            }
            currentAlbumArt = null;
        }
    }

    private void loadAlbumArtAsync(Uri targetUri, String uriSnapshot, String pathSnapshot) {
        if (targetUri == null || artExecutor.isShutdown()) return;
        artExecutor.execute(() -> {
            Bitmap decoded = null;
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(getApplicationContext(), targetUri);
                byte[] artBytes = mmr.getEmbeddedPicture();
                if (artBytes != null && artBytes.length > 0) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length, opts);
                    int sample = 1;
                    while (opts.outWidth / sample > 512 || opts.outHeight / sample > 512) {
                        sample *= 2;
                    }
                    opts.inJustDecodeBounds = false;
                    opts.inSampleSize = sample;
                    decoded = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length, opts);
                }
            } catch (Exception ignored) {
            } finally {
                try { mmr.release(); } catch (Exception ignored) {}
            }
            if (decoded == null) return;
            final Bitmap finalArt = decoded;
            crossfadeHandler.post(() -> {
                boolean sameTrack = java.util.Objects.equals(uriSnapshot, currentUriStr)
                        && java.util.Objects.equals(pathSnapshot, currentPath);
                if (sameTrack && mediaPlayer != null) {
                    clearAlbumArt();
                    currentAlbumArt = finalArt;
                    updateMediaSessionMetadata();
                    updateNotification();
                } else if (!finalArt.isRecycled()) {
                    finalArt.recycle();
                }
            });
        });
    }

    private void updateMediaSessionPlaybackState(int state) {
        if (mediaSession == null) return;
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                       PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                       PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO |
                       PlaybackStateCompat.ACTION_STOP;

        float speed = (state == PlaybackStateCompat.STATE_PLAYING) ? 1.0f : 0.0f;
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, getCurrentPosition(), speed);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private Notification buildNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Previous Action
        Intent prevIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(
            this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Toggle Play/Pause Action
        Intent toggleIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_TOGGLE);
        PendingIntent togglePendingIntent = PendingIntent.getService(
            this, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Next Action
        Intent nextIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(
            this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Stop Action
        Intent stopIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String playPauseTitle = isPlaying ? "Pause" : "Play";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText("NOTHING PLAYER")
            .setSmallIcon(R.drawable.ic_audio_track)
            .setLargeIcon(currentAlbumArt != null && !currentAlbumArt.isRecycled() ? currentAlbumArt : null)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setStyle(
                new MediaStyle()
                    .setMediaSession(mediaSession != null ? mediaSession.getSessionToken() : null)
                    .setShowActionsInCompactView(0, 1, 2)
            );

        return builder.build();
    }

    private void startForegroundNotification() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification() {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        crossfadeHandler.removeCallbacksAndMessages(null);
        if (nextMediaPlayer != null && isNextPrepared && mp == mediaPlayer) {
            MediaPlayer finished = mediaPlayer;
            mediaPlayer = nextMediaPlayer;
            nextMediaPlayer = null;
            isNextPrepared = false;
            try { finished.release(); } catch (Exception ignored) {}

            currentTitle = nextTitle;
            currentArtist = nextArtist;
            currentUriStr = nextUriStr;
            currentPath = nextPath;
            currentReplayGainScalar = nextReplayGainScalar;
            nextTitle = "";
            nextArtist = "";
            nextUriStr = "";
            nextPath = "";
            nextReplayGainScalar = 1.0f;
            this.isPlaying = true;
            this.isPreparing = false;

            applyEffectiveVolume();
            AudioEffectManager.getInstance().attachAudioSession(mediaPlayer.getAudioSessionId(), MusicPlaybackService.this);
            Uri uri = resolveAudioUri(currentUriStr, currentPath);
            if (uri != null) {
                clearAlbumArt();
                loadAlbumArtAsync(uri, currentUriStr, currentPath);
            }
            updateMediaSessionMetadata();
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            updateNotification();
            if (callback != null) {
                callback.onGaplessTrackStarted(currentPath, currentUriStr);
            }
            return;
        }
        this.isPlaying = false;
        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        updateNotification();
        if (eventListener != null) {
            eventListener.onTrackEnded();
        }
        if (callback != null) {
            callback.onTrackEnded();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mp == nextMediaPlayer) {
            releaseNextMediaPlayer();
            return true;
        }
        crossfadeHandler.removeCallbacksAndMessages(null);
        isPreparing = false;
        pauseRequestedWhilePreparing = false;
        this.isPlaying = false;
        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        updateNotification();
        if (eventListener != null) {
            eventListener.onPlayStateChanged(false);
        }
        if (callback != null) {
            callback.onStateChanged(false);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        crossfadeHandler.removeCallbacksAndMessages(null);
        releaseNextMediaPlayer();
        abandonAudioFocus();
        unregisterNoisyReceiver();
        if (isScreenStateReceiverRegistered && screenStateReceiver != null) {
            try {
                unregisterReceiver(screenStateReceiver);
                isScreenStateReceiverRegistered = false;
            } catch (Exception ignored) {}
        }
        AudioEffectManager.getInstance().release(this);
        instance = null;
        eventListener = null;
        callback = null;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        try { artExecutor.shutdownNow(); } catch (Exception ignored) {}
        clearAlbumArt();
        super.onDestroy();
    }
}
