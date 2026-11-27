package com.nothing.player;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class NothingPlayerWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_WIDGET_PLAY_PAUSE = "com.nothing.player.WIDGET_PLAY_PAUSE";
    public static final String ACTION_WIDGET_NEXT = "com.nothing.player.WIDGET_NEXT";
    public static final String ACTION_WIDGET_PREV = "com.nothing.player.WIDGET_PREV";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        if (ACTION_WIDGET_PLAY_PAUSE.equals(action)) {
            if (BackgroundPlaybackManager.getInstance().hasActiveSession()) {
                BackgroundPlaybackManager.getInstance().togglePlayPause();
            } else if (MusicPlaybackService.instance != null) {
                if (MusicPlaybackService.instance.isPlaying()) {
                    MusicPlaybackService.instance.pause();
                } else {
                    MusicPlaybackService.instance.resume();
                }
            }
            updateAllWidgets(context);
        } else if (ACTION_WIDGET_NEXT.equals(action)) {
            if (BackgroundPlaybackManager.getInstance().hasActiveSession()) {
                BackgroundPlaybackManager.getInstance().playNext(context);
            } else {
                dispatchMusicServiceAction(context, MusicPlaybackService.ACTION_NEXT);
            }
            updateAllWidgets(context);
        } else if (ACTION_WIDGET_PREV.equals(action)) {
            if (BackgroundPlaybackManager.getInstance().hasActiveSession()) {
                BackgroundPlaybackManager.getInstance().playPrevious(context);
            } else {
                dispatchMusicServiceAction(context, MusicPlaybackService.ACTION_PREV);
            }
            updateAllWidgets(context);
        }
    }

    private static void dispatchMusicServiceAction(Context context, String serviceAction) {
        if (context == null || MusicPlaybackService.instance == null) return;
        Intent serviceIntent = new Intent(context, MusicPlaybackService.class).setAction(serviceAction);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MusicPlaybackService.instance.onStartCommand(serviceIntent, 0, 0);
        } else {
            context.startService(serviceIntent);
        }
    }

    public static void updateAllWidgets(Context context) {
        if (context == null) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, NothingPlayerWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(widget);
        for (int id : ids) {
            updateAppWidget(context, manager, id);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_nothing_player);

        String title = "NOTHING PLAYER";
        String artist = "TAP TO OPEN";
        boolean isPlaying = false;

        if (BackgroundPlaybackManager.getInstance().hasActiveSession()) {
            title = BackgroundPlaybackManager.getInstance().getCurrentTitle();
            artist = "VIDEO PLAYBACK";
            isPlaying = BackgroundPlaybackManager.getInstance().isPlaying();
        } else if (MusicPlaybackService.instance != null) {
            String mTitle = MusicPlaybackService.instance.getCurrentTitle();
            String mArtist = MusicPlaybackService.instance.getCurrentArtist();
            if (mTitle != null && !mTitle.isEmpty()) {
                title = mTitle;
            }
            if (mArtist != null && !mArtist.isEmpty()) {
                artist = mArtist;
            }
            isPlaying = MusicPlaybackService.instance.isPlaying();
        }

        views.setTextViewText(R.id.widget_tv_title, title != null && !title.isEmpty() ? title : "NOTHING PLAYER");
        views.setTextViewText(R.id.widget_tv_artist, artist);
        views.setImageViewResource(R.id.widget_btn_play_pause, isPlaying ? R.drawable.ic_pause_dotmatrix : R.drawable.ic_play_dotmatrix);

        // Intent to open App on container click
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPending = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_container, openAppPending);

        // Control pending intents
        Intent playPauseIntent = new Intent(context, NothingPlayerWidgetProvider.class);
        playPauseIntent.setAction(ACTION_WIDGET_PLAY_PAUSE);
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, PendingIntent.getBroadcast(
                context, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));

        Intent nextIntent = new Intent(context, NothingPlayerWidgetProvider.class);
        nextIntent.setAction(ACTION_WIDGET_NEXT);
        views.setOnClickPendingIntent(R.id.widget_btn_next, PendingIntent.getBroadcast(
                context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));

        Intent prevIntent = new Intent(context, NothingPlayerWidgetProvider.class);
        prevIntent.setAction(ACTION_WIDGET_PREV);
        views.setOnClickPendingIntent(R.id.widget_btn_prev, PendingIntent.getBroadcast(
                context, 3, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}

