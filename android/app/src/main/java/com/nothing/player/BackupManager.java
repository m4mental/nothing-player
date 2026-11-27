package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Exports and imports Nothing Player state (watch progress history, bookmarks,
 * virtual playlists, series preferences, EQ curves, network shares, and user settings)
 * as a portable JSON backup file without touching private vault credentials.
 */
public final class BackupManager {
    private static final int BACKUP_SCHEMA_VERSION = 1;
    private static final String[] PREF_NAMES = {
            "nothing_playback_history",
            "nothing_media_state",
            "nothing_virtual_playlists",
            "nothing_player_bookmarks",
            SeriesSettingsManager.PREF_NAME,
            "nothing_network_shares",
            "nothing_streams_prefs",
            AudioEffectManager.PREFS_NAME,
            "nothing_player_prefs"
    };

    private BackupManager() {}

    public static String exportToJsonString(Context context) throws Exception {
        Map<String, Map<String, ?>> allData = new HashMap<>();
        for (String name : PREF_NAMES) {
            SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            allData.put(name, sp.getAll());
        }
        return encodeBackupMap(allData, System.currentTimeMillis());
    }

    public static String encodeBackupMap(Map<String, ? extends Map<String, ?>> allPrefs, long timestampMs) throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", BACKUP_SCHEMA_VERSION);
        root.put("exportedAt", timestampMs);
        JSONObject prefsRoot = new JSONObject();

        for (Map.Entry<String, ? extends Map<String, ?>> prefFile : allPrefs.entrySet()) {
            JSONObject entriesObj = new JSONObject();
            Map<String, ?> map = prefFile.getValue();
            if (map != null) {
                for (Map.Entry<String, ?> entry : map.entrySet()) {
                    Object val = entry.getValue();
                    if (val == null) continue;
                    JSONObject wrapped = new JSONObject();
                    if (val instanceof Boolean) {
                        wrapped.put("t", "bool").put("v", val);
                    } else if (val instanceof Integer) {
                        wrapped.put("t", "int").put("v", val);
                    } else if (val instanceof Long) {
                        wrapped.put("t", "long").put("v", val);
                    } else if (val instanceof Float) {
                        wrapped.put("t", "float").put("v", ((Float) val).doubleValue());
                    } else if (val instanceof Set<?>) {
                        JSONArray arr = new JSONArray();
                        for (Object s : (Set<?>) val) {
                            if (s != null) arr.put(String.valueOf(s));
                        }
                        wrapped.put("t", "set").put("v", arr);
                    } else {
                        wrapped.put("t", "str").put("v", String.valueOf(val));
                    }
                    entriesObj.put(entry.getKey(), wrapped);
                }
            }
            prefsRoot.put(prefFile.getKey(), entriesObj);
        }
        root.put("preferences", prefsRoot);
        return root.toString(2);
    }

    public static int exportToUri(Context context, Uri destinationUri) throws Exception {
        String json = exportToJsonString(context);
        try (OutputStream os = context.getContentResolver().openOutputStream(destinationUri, "wt")) {
            if (os == null) throw new IllegalStateException("Cannot open backup destination");
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        JSONObject root = new JSONObject(json);
        JSONObject prefs = root.optJSONObject("preferences");
        return prefs != null ? prefs.length() : 0;
    }

    public static int importFromUri(Context context, Uri sourceUri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            if (is == null) throw new IllegalStateException("Cannot read backup file");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
        }
        return importFromJsonString(context, sb.toString());
    }

    public static int importFromJsonString(Context context, String rawJson) throws Exception {
        JSONObject root = new JSONObject(rawJson);
        int version = root.optInt("version", 0);
        if (version <= 0 || !root.has("preferences")) {
            throw new IllegalArgumentException("Invalid Nothing Player backup format");
        }
        JSONObject prefsRoot = root.getJSONObject("preferences");
        Set<String> allowedNames = new HashSet<>();
        for (String name : PREF_NAMES) allowedNames.add(name);

        int restoredCount = 0;
        Iterator<String> fileKeys = prefsRoot.keys();
        while (fileKeys.hasNext()) {
            String prefName = fileKeys.next();
            if (!allowedNames.contains(prefName)) continue;
            JSONObject entries = prefsRoot.optJSONObject(prefName);
            if (entries == null) continue;

            SharedPreferences.Editor editor = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();
            Iterator<String> keys = entries.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject wrapped = entries.optJSONObject(k);
                if (wrapped == null) continue;
                String type = wrapped.optString("t", "str");
                switch (type) {
                    case "bool":
                        editor.putBoolean(k, wrapped.optBoolean("v", false));
                        break;
                    case "int":
                        editor.putInt(k, wrapped.optInt("v", 0));
                        break;
                    case "long":
                        editor.putLong(k, wrapped.optLong("v", 0L));
                        break;
                    case "float":
                        editor.putFloat(k, (float) wrapped.optDouble("v", 0.0));
                        break;
                    case "set":
                        JSONArray arr = wrapped.optJSONArray("v");
                        Set<String> set = new HashSet<>();
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                set.add(arr.optString(i));
                            }
                        }
                        editor.putStringSet(k, set);
                        break;
                    default:
                        editor.putString(k, wrapped.optString("v", ""));
                        break;
                }
                restoredCount++;
            }
            editor.apply();
        }
        return restoredCount;
    }
}
