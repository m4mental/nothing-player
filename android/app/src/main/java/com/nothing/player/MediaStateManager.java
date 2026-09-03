package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediaStateManager {
    private static final String PREF_NAME = "nothing_media_state";
    private static final String KEY_OPENED_VIDEOS = "opened_video_paths";

    public static boolean isVideoNew(Context context, String path) {
        if (path == null || context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> opened = prefs.getStringSet(KEY_OPENED_VIDEOS, null);
        if (opened == null) {
            return true;
        }
        return !opened.contains(path);
    }

    public static void markVideoOpened(Context context, String path) {
        if (path == null || context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> existing = prefs.getStringSet(KEY_OPENED_VIDEOS, null);
        Set<String> opened = existing != null ? new HashSet<>(existing) : new HashSet<>();
        if (opened.add(path)) {
            prefs.edit().putStringSet(KEY_OPENED_VIDEOS, opened).apply();
        }
    }

    public static boolean folderHasNewVideos(Context context, String folderName, List<MediaItem> allVideos) {
        if (context == null || folderName == null || allVideos == null) return false;
        for (MediaItem v : allVideos) {
            if (folderName.equalsIgnoreCase(v.folder)) {
                String path = v.path != null ? v.path : v.contentUri;
                if (isVideoNew(context, path)) {
                    return true;
                }
            }
        }
        return false;
    }
}
