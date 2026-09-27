package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class PlaybackHistoryManager {
    private static final String PREF_NAME = "nothing_playback_history";
    private static final String KEY_HISTORY_ARRAY = "history_entries_json";
    private static final int MAX_HISTORY_ENTRIES = 100;

    public static class HistoryItem implements Serializable {
        public String pathOrUri;
        public String title;
        public long positionMs;
        public long durationMs;
        public long timestamp; // epoch millis

        public HistoryItem() {}

        public HistoryItem(String pathOrUri, String title, long positionMs, long durationMs, long timestamp) {
            this.pathOrUri = pathOrUri;
            this.title = title;
            this.positionMs = positionMs;
            this.durationMs = durationMs;
            this.timestamp = timestamp;
        }

        public int getProgressPercent() {
            if (durationMs <= 0 || positionMs <= 0) return 0;
            return (int) Math.min(100, Math.max(0, (positionMs * 100) / durationMs));
        }

        public String getFormattedPosition() {
            return formatTime(positionMs);
        }

        public String getFormattedDuration() {
            return formatTime(durationMs);
        }

        public String getFormattedDate() {
            return formatRelativeDate(timestamp);
        }

        public String getExactDateTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        private static String formatTime(long ms) {
            long totalSec = Math.max(0, ms / 1000);
            long h = totalSec / 3600;
            long m = (totalSec % 3600) / 60;
            long s = totalSec % 60;
            if (h > 0) {
                return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
            }
            return String.format(Locale.getDefault(), "%02d:%02d", m, s);
        }

        private static String formatRelativeDate(long time) {
            long diff = System.currentTimeMillis() - time;
            if (diff < 60 * 1000) return "JUST NOW";
            if (diff < 60 * 60 * 1000) return (diff / (60 * 1000)) + "M AGO";
            if (diff < 24 * 60 * 60 * 1000) return (diff / (60 * 60 * 1000)) + "H AGO";
            if (diff < 48 * 60 * 60 * 1000) return "YESTERDAY";
            if (diff < 7 * 24 * 60 * 60 * 1000) return (diff / (24 * 60 * 60 * 1000)) + "D AGO";
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            return sdf.format(new Date(time));
        }
    }

    public static synchronized void saveProgress(Context context, String pathOrUri, String title, long positionMs, long durationMs) {
        if (context == null || pathOrUri == null || pathOrUri.isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<HistoryItem> items = getHistoryList(context);

        // If completed or near end (last 3 seconds), reset position to 0 so next open starts over
        long effectivePos = positionMs;
        if (durationMs > 0 && positionMs >= durationMs - 3000) {
            effectivePos = 0;
        }

        // Remove existing entry with same path
        Iterator<HistoryItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            HistoryItem existing = iterator.next();
            if (pathOrUri.equals(existing.pathOrUri)) {
                iterator.remove();
                break;
            }
        }

        // Add new entry at top
        HistoryItem newItem = new HistoryItem(pathOrUri, title, effectivePos, durationMs, System.currentTimeMillis());
        items.add(0, newItem);

        // Trim to max limit
        while (items.size() > MAX_HISTORY_ENTRIES) {
            items.remove(items.size() - 1);
        }

        persistHistory(prefs, items);
    }

    public static synchronized HistoryItem getProgress(Context context, String pathOrUri) {
        if (context == null || pathOrUri == null || pathOrUri.isEmpty()) return null;
        List<HistoryItem> items = getHistoryList(context);
        for (HistoryItem item : items) {
            if (pathOrUri.equals(item.pathOrUri)) {
                return item;
            }
        }
        return null;
    }

    public static synchronized List<HistoryItem> getHistoryList(Context context) {
        List<HistoryItem> list = new ArrayList<>();
        if (context == null) return list;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY_ARRAY, null);
        if (json == null || json.isEmpty()) return list;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                HistoryItem item = new HistoryItem();
                item.pathOrUri = obj.optString("pathOrUri");
                item.title = obj.optString("title");
                item.positionMs = obj.optLong("positionMs");
                item.durationMs = obj.optLong("durationMs");
                item.timestamp = obj.optLong("timestamp");
                if (item.pathOrUri != null && !item.pathOrUri.isEmpty()) {
                    list.add(item);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized void deleteEntry(Context context, String pathOrUri) {
        if (context == null || pathOrUri == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<HistoryItem> items = getHistoryList(context);
        Iterator<HistoryItem> iterator = items.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            if (pathOrUri.equals(iterator.next().pathOrUri)) {
                iterator.remove();
                removed = true;
                break;
            }
        }
        if (removed) {
            persistHistory(prefs, items);
        }
    }

    public static synchronized void clearHistory(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HISTORY_ARRAY).apply();
    }

    private static void persistHistory(SharedPreferences prefs, List<HistoryItem> items) {
        try {
            JSONArray arr = new JSONArray();
            for (HistoryItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("pathOrUri", item.pathOrUri);
                obj.put("title", item.title);
                obj.put("positionMs", item.positionMs);
                obj.put("durationMs", item.durationMs);
                obj.put("timestamp", item.timestamp);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_HISTORY_ARRAY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
