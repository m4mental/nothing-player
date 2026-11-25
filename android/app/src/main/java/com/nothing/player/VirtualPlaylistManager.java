package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages virtual smart playlists without modifying files or directory structures on disk.
 */
public class VirtualPlaylistManager {
    private static final String PREF_NAME = "nothing_virtual_playlists";
    private static final String KEY_PLAYLISTS = "playlists_json";

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toUpperCase(java.util.Locale.US);
    }

    private static String resolveExistingKey(JSONObject obj, String name) {
        if (obj == null || name == null) return "";
        if (obj.has(name)) return name;
        String normalized = normalizeName(name);
        if (obj.has(normalized)) return normalized;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            if (k.equalsIgnoreCase(normalized)) return k;
        }
        return normalized;
    }

    public static synchronized List<String> getPlaylistNames(Context context) {
        List<String> list = new ArrayList<>();
        if (context == null) return list;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(raw);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                if (!list.contains(k)) list.add(k);
            }
        } catch (Exception ignored) {}
        if (!list.contains("FAVORITES")) list.add(0, "FAVORITES");
        if (!list.contains("WATCH LATER")) list.add("WATCH LATER");
        return list;
    }

    public static synchronized List<String> getPlaylistItems(Context context, String name) {
        List<String> items = new ArrayList<>();
        if (context == null || name == null) return items;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(raw);
            String key = resolveExistingKey(obj, name);
            if (obj.has(key)) {
                JSONArray arr = obj.getJSONArray(key);
                for (int i = 0; i < arr.length(); i++) {
                    items.add(arr.getString(i));
                }
            }
        } catch (Exception ignored) {}
        return items;
    }

    public static synchronized void addToPlaylist(Context context, String name, String path) {
        if (context == null || name == null || path == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(raw);
            String key = resolveExistingKey(obj, name);
            JSONArray arr = obj.optJSONArray(key);
            if (arr == null) arr = new JSONArray();
            boolean exists = false;
            for (int i = 0; i < arr.length(); i++) {
                if (path.equals(arr.getString(i))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                arr.put(path);
                obj.put(key, arr);
                prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void removeFromPlaylist(Context context, String name, String path) {
        if (context == null || name == null || path == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(raw);
            String key = resolveExistingKey(obj, name);
            JSONArray arr = obj.optJSONArray(key);
            if (arr != null) {
                JSONArray newArr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    if (!path.equals(arr.getString(i))) {
                        newArr.put(arr.getString(i));
                    }
                }
                obj.put(key, newArr);
                prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void createPlaylist(Context context, String name) {
        if (context == null || name == null || name.trim().isEmpty()) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(raw);
            String upper = normalizeName(name);
            String existingKey = resolveExistingKey(obj, upper);
            if (!obj.has(existingKey)) {
                obj.put(upper, new JSONArray());
                prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void deletePlaylist(Context context, String name) {
        if (context == null || name == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(raw);
            String key = resolveExistingKey(obj, name);
            if (obj.has(key)) {
                obj.remove(key);
                prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }
}

