package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton manager to keep video audio playing in the background
 * across playlist queue (supporting both ExoPlayer HW+ and LibVLC engines),
 * while showing an active mini player bar in MainActivity.
 */
@OptIn(markerClass = UnstableApi.class)
public class BackgroundPlaybackManager {

    public interface Callback {
        void onTrackChanged(String title, String path, int index);
        void onPlaybackStateChanged(boolean isPlaying);
        void onPlaybackEnded();
    }

    private static final ExecutorService VLC_RELEASE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static BackgroundPlaybackManager instance;

    private ExoPlayer activePlayer;
    private org.videolan.libvlc.MediaPlayer activeVlcPlayer;
    private LibVLC libVLC;
    private DefaultTrackSelector activeTrackSelector;
    private ChannelMixingAudioProcessor activeChannelMixingProcessor;
    private boolean isVlcActive = false;

    private boolean isBackgroundPlayActive = false;
    private boolean isSessionActive = false;
    private boolean isForegroundUiAttached = false;

    private ArrayList<String> playlistPaths = new ArrayList<>();
    private ArrayList<String> playlistTitles = new ArrayList<>();
    private ArrayList<String> playlistUris = new ArrayList<>();
    private int playlistIndex = 0;

    private String currentTitle = "";
    private String currentPath = "";
    private String currentUriStr = "";
    private float currentPlaybackSpeed = 1.0f;

    private Callback callback;
    private org.videolan.libvlc.MediaPlayer.EventListener foregroundVlcListener;
    private Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long sleepTimerEndTimeMs = 0L;
    private boolean sleepTimerEndOfVideo = false;
    private Runnable bgSleepTimerRunnable;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY && !isVlcActive && !isForegroundUiAttached && activePlayer != null && appContext != null) {
                AudioEffectManager.getInstance().attachAudioSession(activePlayer.getAudioSessionId(), appContext);
            } else if (playbackState == Player.STATE_ENDED && !isVlcActive && !isForegroundUiAttached) {
                handleBackgroundTrackEnd();
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (!isVlcActive && callback != null) {
                callback.onPlaybackStateChanged(isPlaying);
            }
        }
    };

    private final org.videolan.libvlc.MediaPlayer.EventListener vlcListener = event -> {
        if (foregroundVlcListener != null && isForegroundUiAttached) {
            foregroundVlcListener.onEvent(event);
        }
        if (!isVlcActive) return;
        if (event.type == org.videolan.libvlc.MediaPlayer.Event.EndReached) {
            if (!isForegroundUiAttached) {
                mainHandler.post(this::handleBackgroundTrackEnd);
            }
        } else if (event.type == org.videolan.libvlc.MediaPlayer.Event.Playing) {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onPlaybackStateChanged(true);
                }
            });
        } else if (event.type == org.videolan.libvlc.MediaPlayer.Event.Paused) {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onPlaybackStateChanged(false);
                }
            });
        }
    };

    public static synchronized BackgroundPlaybackManager getInstance() {
        if (instance == null) {
            instance = new BackgroundPlaybackManager();
        }
        return instance;
    }

    private BackgroundPlaybackManager() {}

    public void initSession(
            Context context,
            ExoPlayer exo,
            org.videolan.libvlc.MediaPlayer vlc,
            LibVLC vlcInstance,
            boolean isVlc,
            ArrayList<String> paths,
            ArrayList<String> titles,
            ArrayList<String> uris,
            int index,
            String title,
            String path,
            String uriStr,
            boolean isBgPlayActive) {

        this.appContext = context != null ? context.getApplicationContext() : null;
        this.isVlcActive = isVlc;
        this.libVLC = vlcInstance;

        if (this.activePlayer != null) {
            try {
                this.activePlayer.removeListener(playerListener);
            } catch (Exception ignored) {}
        }
        if (this.activeVlcPlayer != null && this.activeVlcPlayer != vlc) {
            try {
                this.activeVlcPlayer.setEventListener(null);
            } catch (Exception ignored) {}
        }

        this.activePlayer = exo;
        this.activeVlcPlayer = vlc;
        if (exo == null) {
            this.activeTrackSelector = null;
            this.activeChannelMixingProcessor = null;
        }

        this.playlistPaths = paths != null ? new ArrayList<>(paths) : new ArrayList<>();
        this.playlistTitles = titles != null ? new ArrayList<>(titles) : new ArrayList<>();
        this.playlistUris = uris != null ? new ArrayList<>(uris) : new ArrayList<>();
        this.playlistIndex = Math.max(0, index);
        this.currentTitle = title != null ? title : "";
        this.currentPath = path != null ? path : "";
        this.currentUriStr = uriStr != null ? uriStr : "";
        this.isBackgroundPlayActive = isBgPlayActive;
        this.isSessionActive = true;

        if (this.activePlayer != null) {
            this.activePlayer.addListener(playerListener);
        }
        if (this.activeVlcPlayer != null) {
            this.activeVlcPlayer.setEventListener(vlcListener);
        }
        NothingPlayerWidgetProvider.updateAllWidgets(this.appContext);
    }

    public void setAudioComponents(DefaultTrackSelector trackSelector, ChannelMixingAudioProcessor processor) {
        this.activeTrackSelector = trackSelector;
        this.activeChannelMixingProcessor = processor;
    }

    public DefaultTrackSelector getActiveTrackSelector() {
        return activeTrackSelector;
    }

    public ChannelMixingAudioProcessor getActiveChannelMixingProcessor() {
        return activeChannelMixingProcessor;
    }

    public void setForegroundUiAttached(boolean attached) {
        this.isForegroundUiAttached = attached;
    }

    public void setForegroundVlcListener(org.videolan.libvlc.MediaPlayer.EventListener listener) {
        this.foregroundVlcListener = listener;
    }

    public void setPlaybackSpeed(float speed) {
        this.currentPlaybackSpeed = Math.max(0.25f, Math.min(8.0f, speed));
    }

    public void updateCurrentIndex(int index, String title, String path, String uriStr) {
        this.playlistIndex = Math.max(0, index);
        if (title != null) this.currentTitle = title;
        if (path != null) this.currentPath = path;
        if (uriStr != null) this.currentUriStr = uriStr;
        if (callback != null) {
            callback.onTrackChanged(this.currentTitle, this.currentPath, this.playlistIndex);
        }
        NothingPlayerWidgetProvider.updateAllWidgets(this.appContext);
    }

    private void handleBackgroundTrackEnd() {
        if (sleepTimerEndOfVideo) {
            cancelSleepTimer();
            restoreVolumeAfterFade();
            if (callback != null) {
                callback.onPlaybackEnded();
            }
            return;
        }
        if (!isBackgroundPlayActive) {
            if (callback != null) {
                callback.onPlaybackEnded();
            }
            return;
        }
        int repeatMode = 3; // default Loop All
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);
            repeatMode = prefs.getInt("repeat_mode", 3);
        }
        saveCurrentHistory();

        if (repeatMode == 1) { // LOOP ONE
            playCurrentIndex(appContext);
        } else if (repeatMode == 4) { // PLAY ONCE
            if (callback != null) callback.onPlaybackEnded();
        } else if (repeatMode == 2) { // SHUFFLE
            if (playlistPaths != null && playlistPaths.size() > 1) {
                int nextIdx = playlistIndex;
                Random rnd = new Random();
                int attempts = 0;
                while (nextIdx == playlistIndex && attempts < 10) {
                    nextIdx = rnd.nextInt(playlistPaths.size());
                    attempts++;
                }
                playlistIndex = nextIdx;
                playCurrentIndex(appContext);
            } else {
                playCurrentIndex(appContext);
            }
        } else if (repeatMode == 3) { // LOOP ALL
            if (hasNext()) {
                playlistIndex++;
                playCurrentIndex(appContext);
            } else if (playlistPaths != null && !playlistPaths.isEmpty()) {
                playlistIndex = 0;
                playCurrentIndex(appContext);
            } else if (callback != null) {
                callback.onPlaybackEnded();
            }
        } else { // ORDER (0)
            if (hasNext()) {
                playNext(appContext);
            } else if (callback != null) {
                callback.onPlaybackEnded();
            }
        }
    }

    private void saveCurrentHistory() {
        if (appContext == null) return;
        String key = (currentPath != null && !currentPath.isEmpty()) ? currentPath : currentUriStr;
        if (key != null && !key.isEmpty()) {
            long pos = getCurrentPosition();
            long dur = getDuration();
            if (pos > 0 && dur > 0) {
                PlaybackHistoryManager.saveProgress(appContext, key, currentTitle, pos, dur);
            }
        }
    }

    public void setBackgroundPlayActive(boolean active) {
        this.isBackgroundPlayActive = active;
    }

    public boolean isBackgroundPlayActive() {
        return isBackgroundPlayActive;
    }

    public boolean isVlcActive() {
        return isVlcActive;
    }

    public boolean hasActiveSession() {
        if (!isSessionActive) return false;
        if (isVlcActive) {
            return activeVlcPlayer != null;
        }
        return activePlayer != null;
    }

    public ExoPlayer getActivePlayer() {
        return activePlayer;
    }

    public org.videolan.libvlc.MediaPlayer getActiveVlcPlayer() {
        return activeVlcPlayer;
    }

    public LibVLC getLibVLC() {
        return libVLC;
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public boolean hasNext() {
        return playlistPaths != null && playlistIndex < playlistPaths.size() - 1;
    }

    public boolean hasPrevious() {
        return playlistPaths != null && playlistIndex > 0;
    }

    public void playNext(Context context) {
        if (hasNext()) {
            saveCurrentHistory();
            playlistIndex++;
            playCurrentIndex(context);
        } else {
            if (callback != null) callback.onPlaybackEnded();
        }
    }

    public void playPrevious(Context context) {
        if (hasPrevious()) {
            saveCurrentHistory();
            playlistIndex--;
            playCurrentIndex(context);
        }
    }

    private void playCurrentIndex(Context context) {
        if (playlistPaths == null || playlistIndex < 0 || playlistIndex >= playlistPaths.size()) return;
        if (context != null && this.appContext == null) {
            this.appContext = context.getApplicationContext();
        }
        String rawPath = playlistPaths.get(playlistIndex);
        currentPath = rawPath != null ? rawPath : "";
        String fallbackName = !currentPath.isEmpty() ? new File(currentPath).getName() : "Nothing Video";
        currentTitle = (playlistTitles != null && playlistTitles.size() > playlistIndex && playlistTitles.get(playlistIndex) != null)
                ? playlistTitles.get(playlistIndex)
                : fallbackName;
        currentUriStr = (playlistUris != null && playlistUris.size() > playlistIndex && playlistUris.get(playlistIndex) != null)
                ? playlistUris.get(playlistIndex)
                : currentPath;

        if (isVlcActive && activeVlcPlayer != null && !activeVlcPlayer.isReleased() && libVLC != null && !libVLC.isReleased()) {
            try {
                Media m = null;
                if (currentUriStr != null && !currentUriStr.isEmpty()) {
                    m = new Media(libVLC, Uri.parse(currentUriStr));
                } else if (!currentPath.isEmpty()) {
                    m = new Media(libVLC, currentPath);
                }
                if (m != null) {
                    m.setHWDecoderEnabled(true, false);
                    activeVlcPlayer.setMedia(m);
                    m.release();
                    activeVlcPlayer.play();
                    activeVlcPlayer.setRate(currentPlaybackSpeed);
                    AudioEffectManager.getInstance().applyVlcEqualizer(activeVlcPlayer, appContext);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (activePlayer != null) {
            Uri targetUri = null;
            if (currentUriStr != null && !currentUriStr.isEmpty()) {
                targetUri = Uri.parse(currentUriStr);
            } else if (!currentPath.isEmpty()) {
                if (currentPath.startsWith("content://") || currentPath.startsWith("http://")
                        || currentPath.startsWith("https://") || currentPath.startsWith("rtsp://")) {
                    targetUri = Uri.parse(currentPath);
                } else {
                    targetUri = Uri.fromFile(new File(currentPath));
                }
            }
            if (targetUri == null) return;

            MediaItem mediaItem = MediaItem.fromUri(targetUri);
            activePlayer.setMediaItem(mediaItem);
            activePlayer.prepare();
            activePlayer.setPlaybackParameters(new PlaybackParameters(currentPlaybackSpeed, 1.0f));
            activePlayer.play();
        }

        if (callback != null) {
            callback.onTrackChanged(currentTitle, currentPath, playlistIndex);
            callback.onPlaybackStateChanged(true);
        }
        NothingPlayerWidgetProvider.updateAllWidgets(this.appContext);
    }

    public static void releaseVlcAsync(
            final org.videolan.libvlc.MediaPlayer player,
            final LibVLC vlc,
            final ParcelFileDescriptor pfd) {
        if (player == null && vlc == null && pfd == null) return;
        VLC_RELEASE_EXECUTOR.execute(() -> {
            try {
                if (player != null && !player.isReleased()) {
                    try { player.stop(); } catch (Exception ignored) {}
                    try { player.release(); } catch (Exception ignored) {}
                }
                if (vlc != null && !vlc.isReleased()) {
                    try { vlc.release(); } catch (Exception ignored) {}
                }
                if (pfd != null) {
                    try { pfd.close(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                android.util.Log.w("BgPlayback", "Async VLC release error", e);
            }
        });
    }

    public void togglePlayPause() {
        if (isVlcActive && activeVlcPlayer != null && !activeVlcPlayer.isReleased()) {
            if (activeVlcPlayer.isPlaying()) {
                activeVlcPlayer.pause();
            } else {
                activeVlcPlayer.play();
            }
            if (callback != null) {
                callback.onPlaybackStateChanged(activeVlcPlayer.isPlaying());
            }
        } else if (activePlayer != null) {
            if (activePlayer.isPlaying()) {
                activePlayer.pause();
            } else {
                activePlayer.play();
            }
            if (callback != null) {
                callback.onPlaybackStateChanged(activePlayer.isPlaying());
            }
        }
    }

    public void stopAndRelease() {
        cancelSleepTimer();
        if (activePlayer != null) {
            try {
                activePlayer.removeListener(playerListener);
                activePlayer.stop();
                activePlayer.release();
            } catch (Exception ignored) {}
            activePlayer = null;
        }
        final org.videolan.libvlc.MediaPlayer vlcToRelease = activeVlcPlayer;
        final LibVLC libVlcToRelease = isForegroundUiAttached ? null : libVLC;
        activeVlcPlayer = null;
        if (!isForegroundUiAttached) {
            libVLC = null;
        }
        if (vlcToRelease != null) {
            try {
                vlcToRelease.setEventListener(null);
            } catch (Exception ignored) {}
            releaseVlcAsync(vlcToRelease, libVlcToRelease, null);
        }
        activeTrackSelector = null;
        activeChannelMixingProcessor = null;
        foregroundVlcListener = null;
        isForegroundUiAttached = false;
        isSessionActive = false;
        isBackgroundPlayActive = false;
        isVlcActive = false;
        if (callback != null) {
            callback.onPlaybackEnded();
        }
        NothingPlayerWidgetProvider.updateAllWidgets(this.appContext);
    }

    public boolean isPlaying() {
        if (isVlcActive && activeVlcPlayer != null && !activeVlcPlayer.isReleased()) {
            return activeVlcPlayer.isPlaying();
        }
        return activePlayer != null && activePlayer.isPlaying();
    }

    public long getCurrentPosition() {
        if (isVlcActive && activeVlcPlayer != null && !activeVlcPlayer.isReleased()) {
            return activeVlcPlayer.getTime();
        }
        return activePlayer != null ? activePlayer.getCurrentPosition() : 0;
    }

    public long getDuration() {
        if (isVlcActive && activeVlcPlayer != null && !activeVlcPlayer.isReleased()) {
            return activeVlcPlayer.getLength();
        }
        return activePlayer != null ? activePlayer.getDuration() : 0;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getCurrentUriStr() {
        return currentUriStr;
    }

    public int getPlaylistIndex() {
        return playlistIndex;
    }

    public ArrayList<String> getPlaylistPaths() {
        return playlistPaths;
    }

    public ArrayList<String> getPlaylistTitles() {
        return playlistTitles;
    }

    public void startSleepTimer(int minutes) {
        sleepTimerEndOfVideo = false;
        sleepTimerEndTimeMs = System.currentTimeMillis() + (minutes * 60L * 1000L);
        if (bgSleepTimerRunnable != null) {
            mainHandler.removeCallbacks(bgSleepTimerRunnable);
        }
        bgSleepTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (sleepTimerEndTimeMs <= 0L) return;
                long remainingMs = sleepTimerEndTimeMs - System.currentTimeMillis();
                if (!isForegroundUiAttached && isBackgroundPlayActive) {
                    if (remainingMs <= 60000 && remainingMs > 0) {
                        float fade = Math.max(0.05f, Math.min(1.0f, (float) remainingMs / 60000.0f));
                        if (activePlayer != null) {
                            try { activePlayer.setVolume(fade); } catch (Exception ignored) {}
                        }
                        if (activeVlcPlayer != null) {
                            try { activeVlcPlayer.setVolume((int) (fade * 100)); } catch (Exception ignored) {}
                        }
                    }
                    if (remainingMs <= 0) {
                        cancelSleepTimer();
                        if (activePlayer != null && activePlayer.isPlaying()) {
                            try { activePlayer.pause(); } catch (Exception ignored) {}
                        }
                        if (activeVlcPlayer != null && activeVlcPlayer.isPlaying()) {
                            try { activeVlcPlayer.pause(); } catch (Exception ignored) {}
                        }
                        restoreVolumeAfterFade();
                        if (callback != null) {
                            callback.onPlaybackStateChanged(false);
                        }
                        return;
                    }
                }
                if (remainingMs > 0) {
                    mainHandler.postDelayed(this, 1000);
                } else {
                    sleepTimerEndTimeMs = 0L;
                }
            }
        };
        mainHandler.postDelayed(bgSleepTimerRunnable, 1000);
    }

    public void setSleepTimerEndOfVideo(boolean enabled) {
        if (bgSleepTimerRunnable != null) {
            mainHandler.removeCallbacks(bgSleepTimerRunnable);
            bgSleepTimerRunnable = null;
        }
        sleepTimerEndOfVideo = enabled;
        sleepTimerEndTimeMs = 0L;
    }

    public void cancelSleepTimer() {
        sleepTimerEndOfVideo = false;
        sleepTimerEndTimeMs = 0L;
        if (bgSleepTimerRunnable != null) {
            mainHandler.removeCallbacks(bgSleepTimerRunnable);
            bgSleepTimerRunnable = null;
        }
        restoreVolumeAfterFade();
    }

    public long getSleepTimerEndTimeMs() {
        return sleepTimerEndTimeMs;
    }

    public boolean isSleepTimerEndOfVideo() {
        return sleepTimerEndOfVideo;
    }

    private void restoreVolumeAfterFade() {
        if (activePlayer != null) {
            try { activePlayer.setVolume(1.0f); } catch (Exception ignored) {}
        }
        if (activeVlcPlayer != null) {
            try { activeVlcPlayer.setVolume(100); } catch (Exception ignored) {}
        }
    }

    public ArrayList<String> getPlaylistUris() {
        return playlistUris;
    }
}
