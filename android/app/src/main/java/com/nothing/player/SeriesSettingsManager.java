package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remembers playback preferences (preferred audio language, subtitle language/delay,
 * playback speed, aspect mode, and custom intro skip length) per series or parent folder.
 */
public final class SeriesSettingsManager {
    public static final String PREF_NAME = "nothing_series_settings";
    private static final Pattern EPISODE_MARKER_PATTERN = Pattern.compile(
            "(?i)(?:[\\s._-]+(?:s\\d{1,2}e\\d{1,3}|s\\d{1,2}|e\\d{1,3}|ep\\.?\\s*\\d{1,3}|episode\\.?\\s*\\d{1,3}|part\\.?\\s*\\d+|\\(\\d{4}\\)|\\[.*?\\]|2160p|1080p|720p|480p|x264|x265|hevc|web-?dl|bluray).*)$"
    );

    public static class SeriesSettings {
        public String audioLang = "";
        public String subtitleLang = "";
        public long subtitleDelayMs = 0L;
        public float playbackSpeed = 1.0f;
        public int aspectMode = -1; // -1 = default global
        public int introSkipSec = -1; // -1 = default global
    }

    private SeriesSettingsManager() {}

    /**
     * Derives a stable key representing the series/show/folder for a video.
     */
    public static String deriveSeriesKey(String folderName, String videoPath, String title) {
        if (folderName != null) {
            String trimmedFolder = folderName.trim();
            if (!trimmedFolder.isEmpty()
                    && !"downloads".equalsIgnoreCase(trimmedFolder)
                    && !"movies".equalsIgnoreCase(trimmedFolder)
                    && !"videos".equalsIgnoreCase(trimmedFolder)
                    && !"camera".equalsIgnoreCase(trimmedFolder)
                    && !"0".equals(trimmedFolder)) {
                return "folder:" + trimmedFolder.toLowerCase(Locale.US);
            }
        }
        if (videoPath != null && !videoPath.startsWith("http") && !videoPath.startsWith("content://")) {
            File f = new File(videoPath);
            File parent = f.getParentFile();
            if (parent != null) {
                String pName = parent.getName().trim();
                if (!pName.isEmpty()
                        && !"downloads".equalsIgnoreCase(pName)
                        && !"movies".equalsIgnoreCase(pName)
                        && !"videos".equalsIgnoreCase(pName)
                        && !"camera".equalsIgnoreCase(pName)
                        && !"0".equals(pName)) {
                    return "folder:" + pName.toLowerCase(Locale.US);
                }
            }
        }
        String candidate = title != null ? title.trim() : "";
        if (candidate.isEmpty() && videoPath != null) {
            int slash = videoPath.lastIndexOf('/');
            candidate = slash >= 0 ? videoPath.substring(slash + 1) : videoPath;
        }
        int dot = candidate.lastIndexOf('.');
        if (dot > 0) candidate = candidate.substring(0, dot);

        Matcher m = EPISODE_MARKER_PATTERN.matcher(candidate);
        if (m.find()) {
            String stripped = candidate.substring(0, m.start()).trim();
            if (!stripped.isEmpty()) candidate = stripped;
        }
        candidate = candidate.replaceAll("[._-]+", " ").trim().toLowerCase(Locale.US);
        return candidate.isEmpty() ? "" : ("series:" + candidate);
    }

    public static SeriesSettings load(Context context, String seriesKey) {
        SeriesSettings settings = new SeriesSettings();
        if (context == null || seriesKey == null || seriesKey.isEmpty()) return settings;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(seriesKey, null);
            if (raw == null || raw.isEmpty()) return settings;
            JSONObject obj = new JSONObject(raw);
            settings.audioLang = obj.optString("audioLang", "");
            settings.subtitleLang = obj.optString("subtitleLang", "");
            settings.subtitleDelayMs = obj.optLong("subtitleDelayMs", 0L);
            settings.playbackSpeed = (float) obj.optDouble("playbackSpeed", 1.0d);
            settings.aspectMode = obj.optInt("aspectMode", -1);
            settings.introSkipSec = obj.optInt("introSkipSec", -1);
        } catch (Exception ignored) {}
        return settings;
    }

    public static void save(Context context, String seriesKey, SeriesSettings settings) {
        if (context == null || seriesKey == null || seriesKey.isEmpty() || settings == null) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("audioLang", settings.audioLang != null ? settings.audioLang : "");
            obj.put("subtitleLang", settings.subtitleLang != null ? settings.subtitleLang : "");
            obj.put("subtitleDelayMs", settings.subtitleDelayMs);
            obj.put("playbackSpeed", settings.playbackSpeed);
            obj.put("aspectMode", settings.aspectMode);
            obj.put("introSkipSec", settings.introSkipSec);
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(seriesKey, obj.toString())
                    .apply();
        } catch (Exception ignored) {}
    }
}
