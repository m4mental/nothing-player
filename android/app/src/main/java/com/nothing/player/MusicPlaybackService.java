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
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
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
    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;

    private String currentTitle = "Nothing Player";
    private String currentArtist = "Unknown Artist";
    private String currentUriStr = "";
    private String currentPath = "";
    private boolean isPlaying = false;

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
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            );
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
        }
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
                if (eventListener != null) {
                    eventListener.onNextRequested();
                }
            }

            @Override
            public void onSkipToPrevious() {
                if (eventListener != null) {
                    eventListener.onPrevRequested();
                }
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((double) pos / 1000.0);
            }

            @Override
            public void onStop() {
                stop();
            }
        });
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
                    if (eventListener != null) {
                        eventListener.onNextRequested();
                    }
                    break;
                case ACTION_PREV:
                    if (eventListener != null) {
                        eventListener.onPrevRequested();
                    }
                    break;
                case ACTION_STOP:
                    stop();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    public void playTrack(String title, String artist, String uriStr, String path) {
        try {
            this.currentTitle = (title != null && !title.isEmpty()) ? title : "Nothing Track";
            this.currentArtist = (artist != null && !artist.isEmpty()) ? artist : "Nothing Player";
            this.currentUriStr = uriStr;
            this.currentPath = path;

            initMediaPlayer();
            mediaPlayer.reset();

            Uri uri = null;
            if (uriStr != null && !uriStr.isEmpty()) {
                uri = Uri.parse(uriStr);
            } else if (path != null && !path.isEmpty()) {
                uri = Uri.fromFile(new File(path));
            }

            if (uri != null) {
                mediaPlayer.setDataSource(this, uri);
                mediaPlayer.prepare();
                mediaPlayer.start();
                this.isPlaying = true;

                updateMediaSessionMetadata();
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                startForegroundNotification();

                if (eventListener != null) {
                    eventListener.onPlayStateChanged(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            this.isPlaying = false;
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            updateNotification();
            if (eventListener != null) {
                eventListener.onPlayStateChanged(false);
            }
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            this.isPlaying = true;
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            startForegroundNotification();
            if (eventListener != null) {
                eventListener.onPlayStateChanged(true);
            }
        }
    }

    public void seekTo(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo((int) (seconds * 1000));
            updateMediaSessionPlaybackState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        this.isPlaying = false;
        if (mediaSession != null) {
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        }
        stopForeground(true);
        stopSelf();
        if (eventListener != null) {
            eventListener.onPlayStateChanged(false);
        }
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public double getCurrentPosition() {
        try {
            return (mediaPlayer != null) ? (mediaPlayer.getCurrentPosition() / 1000.0) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double getDuration() {
        try {
            return (mediaPlayer != null) ? (mediaPlayer.getDuration() / 1000.0) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void updateMediaSessionMetadata() {
        if (mediaSession == null) return;
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Nothing Music")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (long) (getDuration() * 1000));
        mediaSession.setMetadata(builder.build());
    }

    private void updateMediaSessionPlaybackState(int state) {
        if (mediaSession == null) return;
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                       PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                       PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO |
                       PlaybackStateCompat.ACTION_STOP;

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, (long) (getCurrentPosition() * 1000), 1.0f);
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
            .setSmallIcon(R.mipmap.ic_launcher)
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
        this.isPlaying = false;
        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        updateNotification();
        if (eventListener != null) {
            eventListener.onTrackEnded();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        this.isPlaying = false;
        return false;
    }

    @Override
    public void onDestroy() {
        instance = null;
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
        super.onDestroy();
    }
}
