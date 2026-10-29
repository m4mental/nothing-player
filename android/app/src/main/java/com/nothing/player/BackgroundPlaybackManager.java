                                                                                                                            package com.nothing.player;

import android.content.Context;
import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.io.File;
import java.util.ArrayList;

/**
 * Singleton manager to keep video audio playing in the background
 * across playlist queue (supporting both ExoPlayer HW+ and LibVLC engines),
 * while showing an active mini player bar in MainActivity.
 */
public class BackgroundPlaybackManager {

    public interface Callback {
        void onTrackChanged(String title, String path, int index);
        void onPlaybackStateChanged(boolean isPlaying);
        void onPlaybackEnded();
    }

    private static BackgroundPlaybackManager instance;

    private ExoPlayer activePlayer;
    private org.videolan.libvlc.MediaPlayer activeVlcPlayer;
    private LibVLC libVLC;
    private boolean isVlcActive = false;

    private boolean isBackgroundPlayActive = false;
    private boolean isSessionActive = false;

    private ArrayList<String> playlistPaths = new ArrayList<>();
    private ArrayList<String> playlistTitles = new ArrayList<>();
    private ArrayList<String> playlistUris = new ArrayList<>();
    private int playlistIndex = 0;

    private String currentTitle = "";
    private String currentPath = "";
    private String currentUriStr = "";

    private Callback callback;
    private Context appContext;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                if (isBackgroundPlayActive && hasNext()) {
                    playNext(appContext);
                } else {
                    if (callback != null) {
                        callback.onPlaybackEnded();
                    }
                }
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
        if (event.type == org.videolan.libvlc.MediaPlayer.Event.EndReached) {
            if (isBackgroundPlayActive && hasNext()) {
                playNext(appContext);
            } else {
                if (callback != null) {
                    callback.onPlaybackEnded();
                }
            }
        } else if (event.type == org.videolan.libvlc.MediaPlayer.Event.Playing) {
            if (isVlcActive && callback != null) {
                callback.onPlaybackStateChanged(true);
            }
        } else if (event.type == org.videolan.libvlc.MediaPlayer.Event.Paused) {
            if (isVlcActive && callback != null) {
                callback.onPlaybackStateChanged(false);
            }
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

        if (this.activePlayer != null && this.activePlayer != exo) {
            try {
                this.activePlayer.removeListener(playerListener);
            } catch (Exception ignored) {}
        }

        this.activePlayer = exo;
        this.activeVlcPlayer = vlc;

        this.playlistPaths = paths != null ? new ArrayList<>(paths) : new ArrayList<>();
        this.playlistTitles = titles != null ? new ArrayList<>(titles) : new ArrayList<>();
        this.playlistUris = uris != null ? new ArrayList<>(uris) : new ArrayList<>();
        this.playlistIndex = index;
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

    public void updateCurrentIndex(int index, String title, String path, String uriStr) {
        this.playlistIndex = index;
        if (title != null) this.currentTitle = title;
        if (path != null) this.currentPath = path;
        if (uriStr != null) this.currentUriStr = uriStr;
        if (callback != null) {
            callback.onTrackChanged(this.currentTitle, this.currentPath, this.playlistIndex);
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
            playlistIndex++;
            playCurrentIndex(context);
        } else {
            if (callback != null) callback.onPlaybackEnded();
        }
    }

    public void playPrevious(Context context) {
        if (hasPrevious()) {
            playlistIndex--;
            playCurrentIndex(context);
        }
    }

    private void playCurrentIndex(Context context) {
        if (playlistPaths == null || playlistIndex >= playlistPaths.size()) return;
        currentPath = playlistPaths.get(playlistIndex);
        currentTitle = playlistTitles.size() > playlistIndex ? playlistTitles.get(playlistIndex) : new File(currentPath).getName();
        currentUriStr = playlistUris.size() > playlistIndex ? playlistUris.get(playlistIndex) : currentPath;

        if (isVlcActive && activeVlcPlayer != null && libVLC != null) {
            try {
                Media m;
                if (currentUriStr != null && !currentUriStr.isEmpty()) {
                    m = new Media(libVLC, Uri.parse(currentUriStr));
                } else {
                    m = new Media(libVLC, currentPath);
                }
                m.setHWDecoderEnabled(true, true);
                activeVlcPlayer.setMedia(m);
                m.release();
                activeVlcPlayer.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (activePlayer != null) {
            Uri targetUri;
            if (currentUriStr != null && !currentUriStr.isEmpty()) {
                targetUri = Uri.parse(currentUriStr);
            } else if (currentPath != null && currentPath.startsWith("content://")) {
                targetUri = Uri.parse(currentPath);
            } else {
                targetUri = Uri.fromFile(new File(currentPath));
            }

            MediaItem mediaItem = MediaItem.fromUri(targetUri);
            activePlayer.setMediaItem(mediaItem);
            activePlayer.prepare();
            activePlayer.play();
        }

        if (callback != null) {
            callback.onTrackChanged(currentTitle, currentPath, playlistIndex);
            callback.onPlaybackStateChanged(true);
        }
        NothingPlayerWidgetProvider.updateAllWidgets(this.appContext);
    }

    public void togglePlayPause() {
        if (isVlcActive && activeVlcPlayer != null) {
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
        if (activePlayer != null) {
            try {
                activePlayer.removeListener(playerListener);
                activePlayer.stop();
                activePlayer.release();
            } catch (Exception ignored) {}
            activePlayer = null;
        }
        if (activeVlcPlayer != null) {
            try {
                activeVlcPlayer.setEventListener(null);
                activeVlcPlayer.stop();
                activeVlcPlayer.release();
            } catch (Exception ignored) {}
            activeVlcPlayer = null;
        }
        isSessionActive = false;
        isBackgroundPlayActive = false;
        isVlcActive = false;
        if (callback != null) {
            callback.onPlaybackEnded();
        }
        NothingPlayerWidgetProvider.updateAllWidgets(this.appContext);
    }

    public boolean isPlaying() {
        if (isVlcActive && activeVlcPlayer != null) {
            return activeVlcPlayer.isPlaying();
        }
        return activePlayer != null && activePlayer.isPlaying();
    }

    public long getCurrentPosition() {
        if (isVlcActive && activeVlcPlayer != null) {
            return activeVlcPlayer.getTime();
        }
        return activePlayer != null ? activePlayer.getCurrentPosition() : 0;
    }

    public long getDuration() {
        if (isVlcActive && activeVlcPlayer != null) {
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

    public ArrayList<String> getPlaylistUris() {
        return playlistUris;
    }
}
