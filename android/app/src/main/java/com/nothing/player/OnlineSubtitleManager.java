package com.nothing.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * Open, Keyless Online Subtitle Fetching Engine for NOTHING-PLAYER.
 * Automatically queries open subtitle repositories for matching movie/series
 * SRT subtitles in English, Hindi, and regional languages.
 */
public class OnlineSubtitleManager {
    private static final String TAG = "OnlineSubtitleManager";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static class SubtitleItem {
        public String fileName;
        public String language;
        public String languageCode;
        public String downloadUrl;
        public String rating;
        public int downloadCount;
    }

    public interface SubtitleSearchCallback {
        void onResults(List<SubtitleItem> results);
        void onError(String message);
    }

    public interface SubtitleDownloadCallback {
        void onDownloaded(File srtFile);
        void onError(String message);
    }

    public static void searchSubtitles(Context context, String query, String languageCode, SubtitleSearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            if (callback != null) callback.onError("Empty search query");
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                String cleanQuery = query.replaceAll("(?i)\\b(1080p|720p|480p|2160p|4k|x264|x265|hevc|bluray|webrip|web-dl|mkv|mp4)\\b", " ")
                        .replaceAll("[\\[\\]\\(\\)\\._\\-]+", " ")
                        .trim();

                String encoded = URLEncoder.encode(cleanQuery, "UTF-8");
                String langParam = (languageCode != null && !languageCode.equalsIgnoreCase("all"))
                        ? languageCode : "eng,hin";

                String urlStr = "https://rest.opensubtitles.org/search/query-" + encoded + "/sublanguageid-" + langParam;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "TemporaryUserAgent");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    MAIN_HANDLER.post(() -> {
                        if (callback != null) callback.onError("Server returned code " + code);
                    });
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                } finally {
                    conn.disconnect();
                }

                JSONArray arr = new JSONArray(sb.toString());
                List<SubtitleItem> items = new ArrayList<>();
                for (int i = 0; i < arr.length() && i < 25; i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    SubtitleItem item = new SubtitleItem();
                    item.fileName = obj.optString("SubFileName", "Subtitle " + (i + 1));
                    item.language = obj.optString("LanguageName", "English");
                    item.languageCode = obj.optString("SubLanguageID", "eng");
                    item.downloadUrl = obj.optString("SubDownloadLink");
                    item.rating = obj.optString("SubRating", "0.0");
                    item.downloadCount = obj.optInt("SubDownloadsCnt", 0);

                    if (item.downloadUrl != null && !item.downloadUrl.isEmpty()) {
                        items.add(item);
                    }
                }

                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onResults(items);
                });

            } catch (Exception e) {
                Log.w(TAG, "Subtitle search error", e);
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onError("Failed to fetch subtitles: " + e.getMessage());
                });
            }
        });
    }

    public static void downloadSubtitle(Context context, SubtitleItem item, SubtitleDownloadCallback callback) {
        if (item == null || item.downloadUrl == null) {
            if (callback != null) callback.onError("Invalid download URL");
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(item.downloadUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "TemporaryUserAgent");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    MAIN_HANDLER.post(() -> {
                        if (callback != null) callback.onError("Download server code " + code);
                    });
                    return;
                }

                File subDir = new File(context.getCacheDir(), "subtitles");
                if (!subDir.exists()) subDir.mkdirs();

                String safeName = (item.fileName != null ? item.fileName : "downloaded_sub")
                        .replaceAll("[^a-zA-Z0-9._-]", "_");
                if (!safeName.toLowerCase().endsWith(".srt")) {
                    safeName = safeName + ".srt";
                }
                File targetSrt = new File(subDir, safeName);

                InputStream rawStream = conn.getInputStream();
                InputStream inStream;
                try {
                    inStream = new GZIPInputStream(rawStream);
                } catch (Exception notGzip) {
                    // Fallback to plain stream if not gzipped
                    conn.disconnect();
                    conn = (HttpURLConnection) new URL(item.downloadUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "TemporaryUserAgent");
                    inStream = conn.getInputStream();
                }

                try (InputStream is = inStream;
                     FileOutputStream fos = new FileOutputStream(targetSrt)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                } finally {
                    conn.disconnect();
                }

                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onDownloaded(targetSrt);
                });

            } catch (Exception e) {
                Log.w(TAG, "Download error", e);
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onError("Download error: " + e.getMessage());
                });
            }
        });
    }
}
