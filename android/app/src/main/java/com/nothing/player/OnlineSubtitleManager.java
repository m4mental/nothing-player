package com.nothing.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * Open, Keyless Online Subtitle Fetching Engine for NOTHING-PLAYER.
 * Automatically queries open subtitle repositories for matching movie/series
 * SRT subtitles via Text Query and 64-bit Exact Movie Hash Matching.
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
        public boolean isExactHashMatch = false;
    }

    public interface SubtitleSearchCallback {
        void onResults(List<SubtitleItem> results);
        void onError(String message);
    }

    public interface SubtitleDownloadCallback {
        void onDownloaded(File srtFile);
        void onError(String message);
    }

    /**
     * Computes the official OpenSubtitles 64-bit checksum hash from a video file.
     * Reads first 64KB and last 64KB + file size in under 10ms.
     */
    public static String computeMovieHash(File file) {
        if (file == null || !file.exists() || file.length() < 131072) {
            return null;
        }
        long size = file.length();
        long checksum = size;
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel channel = fis.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate(65536);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // First 64KB
            channel.position(0);
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
            LongBuffer longBuffer = buffer.asLongBuffer();
            while (longBuffer.hasRemaining()) {
                checksum += longBuffer.get();
            }

            // Last 64KB
            channel.position(Math.max(0, size - 65536));
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
            longBuffer = buffer.asLongBuffer();
            while (longBuffer.hasRemaining()) {
                checksum += longBuffer.get();
            }

            return String.format(Locale.US, "%016x", checksum);
        } catch (Exception e) {
            Log.w(TAG, "Hash computation failed", e);
            return null;
        }
    }

    /**
     * Computes OpenSubtitles 64-bit hash from Android ContentProvider Uri.
     */
    public static String computeMovieHash(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                     FileChannel channel = fis.getChannel()) {
                    long size = channel.size();
                    if (size < 131072) return null;
                    long checksum = size;
                    ByteBuffer buffer = ByteBuffer.allocate(65536);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);

                    channel.position(0);
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    LongBuffer longBuffer = buffer.asLongBuffer();
                    while (longBuffer.hasRemaining()) {
                        checksum += longBuffer.get();
                    }

                    channel.position(Math.max(0, size - 65536));
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    longBuffer = buffer.asLongBuffer();
                    while (longBuffer.hasRemaining()) {
                        checksum += longBuffer.get();
                    }

                    return String.format(Locale.US, "%016x", checksum);
                } finally {
                    pfd.close();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Uri hash computation failed", e);
        }
        return null;
    }

    public static void searchSubtitles(Context context, String query, String languageCode, SubtitleSearchCallback callback) {
        searchSubtitles(context, query, null, 0, languageCode, callback);
    }

    public static void searchSubtitles(Context context, String query, String movieHash, long fileSize, String languageCode, SubtitleSearchCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                String langParam = (languageCode != null && !languageCode.equalsIgnoreCase("all"))
                        ? languageCode : "eng,hin";
                List<SubtitleItem> items = new ArrayList<>();
                Set<String> seenUrls = new HashSet<>();

                // 1. Exact Movie Hash Search (OpenSubtitles Protocol)
                if (movieHash != null && !movieHash.trim().isEmpty() && fileSize > 0) {
                    try {
                        String hashUrl = "https://rest.opensubtitles.org/search/moviebytesize-" + fileSize
                                + "/moviehash-" + movieHash.trim() + "/sublanguageid-" + langParam;
                        HttpURLConnection conn = (HttpURLConnection) new URL(hashUrl).openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "TemporaryUserAgent");
                        conn.setConnectTimeout(6000);
                        conn.setReadTimeout(6000);

                        if (conn.getResponseCode() == 200) {
                            StringBuilder sb = new StringBuilder();
                            try (InputStream is = conn.getInputStream();
                                 BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = r.readLine()) != null) sb.append(line);
                            }
                            JSONArray arr = new JSONArray(sb.toString());
                            for (int i = 0; i < arr.length() && i < 15; i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                String dl = obj.optString("SubDownloadLink");
                                if (dl != null && !dl.isEmpty() && !seenUrls.contains(dl)) {
                                    seenUrls.add(dl);
                                    SubtitleItem item = new SubtitleItem();
                                    item.fileName = obj.optString("SubFileName", "Exact Subtitle " + (i + 1));
                                    item.language = obj.optString("LanguageName", "English");
                                    item.languageCode = obj.optString("SubLanguageID", "eng");
                                    item.downloadUrl = dl;
                                    item.rating = obj.optString("SubRating", "5.0");
                                    item.downloadCount = obj.optInt("SubDownloadsCnt", 0);
                                    item.isExactHashMatch = true;
                                    items.add(item);
                                }
                            }
                        }
                        conn.disconnect();
                    } catch (Exception eHash) {
                        Log.w(TAG, "Hash search fallback: " + eHash.getMessage());
                    }
                }

                // 2. Text Query Fallback / Complementary Search
                if (query != null && !query.trim().isEmpty() && items.size() < 15) {
                    try {
                        String cleanQuery = query.replaceAll("(?i)\\b(1080p|720p|480p|2160p|4k|x264|x265|hevc|bluray|webrip|web-dl|mkv|mp4)\\b", " ")
                                .replaceAll("[\\[\\]\\(\\)\\._\\-]+", " ")
                                .trim();

                        String encoded = URLEncoder.encode(cleanQuery, "UTF-8");
                        String urlStr = "https://rest.opensubtitles.org/search/query-" + encoded + "/sublanguageid-" + langParam;
                        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "TemporaryUserAgent");
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(8000);

                        if (conn.getResponseCode() == 200) {
                            StringBuilder sb = new StringBuilder();
                            try (InputStream is = conn.getInputStream();
                                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) sb.append(line);
                            }
                            JSONArray arr = new JSONArray(sb.toString());
                            for (int i = 0; i < arr.length() && items.size() < 25; i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                String dl = obj.optString("SubDownloadLink");
                                if (dl != null && !dl.isEmpty() && !seenUrls.contains(dl)) {
                                    seenUrls.add(dl);
                                    SubtitleItem item = new SubtitleItem();
                                    item.fileName = obj.optString("SubFileName", "Subtitle " + (i + 1));
                                    item.language = obj.optString("LanguageName", "English");
                                    item.languageCode = obj.optString("SubLanguageID", "eng");
                                    item.downloadUrl = dl;
                                    item.rating = obj.optString("SubRating", "0.0");
                                    item.downloadCount = obj.optInt("SubDownloadsCnt", 0);
                                    item.isExactHashMatch = false;
                                    items.add(item);
                                }
                            }
                        }
                        conn.disconnect();
                    } catch (Exception eQuery) {
                        Log.w(TAG, "Query search error: " + eQuery.getMessage());
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
