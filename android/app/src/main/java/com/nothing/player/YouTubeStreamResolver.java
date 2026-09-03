package com.nothing.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube Stream Resolver for Nothing Player.
 * Extracts direct HLS / MP4 stream URLs so they play directly in the default ExoVideoPlayerActivity.
 */
public class YouTubeStreamResolver {

    public interface ResolverCallback {
        void onResolved(String streamUrl, String title);
        void onError(String message);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String[] STREAM_ENDPOINTS = {
            "https://invidious.nerdvpn.de/api/v1/videos/",
            "https://invidious.f5.si/api/v1/videos/",
            "https://yt.chocolatemoo53.com/api/v1/videos/",
            "https://invidious.tiekoetter.com/api/v1/videos/",
            "https://inv.nadeko.net/api/v1/videos/"
    };

    public static boolean isYouTubeUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase().trim();
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    public static String extractVideoId(String url) {
        if (url == null) return null;
        String trimmed = url.trim();

        // Match youtu.be/<id>, watch?v=<id>, shorts/<id>, embed/<id>, live/<id>
        Pattern pattern = Pattern.compile(
                "(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=|shorts\\/|live\\/))([\\w-]{11})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Check query parameter ?v=
        try {
            Uri uri = Uri.parse(trimmed);
            String v = uri.getQueryParameter("v");
            if (v != null && v.length() == 11) {
                return v;
            }
        } catch (Exception ignored) {}

        return null;
    }

    public static String extractPlaylistId(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        Pattern pattern = Pattern.compile("[?&]list=([\\w-]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        try {
            Uri uri = Uri.parse(trimmed);
            return uri.getQueryParameter("list");
        } catch (Exception ignored) {}
        return null;
    }

    public static void resolve(Context context, String url, ResolverCallback callback) {
        String videoId = extractVideoId(url);
        if (videoId == null || videoId.isEmpty()) {
            mainHandler.post(() -> callback.onError("Invalid YouTube URL"));
            return;
        }

        executor.execute(() -> {
            String foundStreamUrl = null;
            String foundTitle = "YouTube Video";

            // Try resolving direct HLS or MP4 stream
            for (String instance : STREAM_ENDPOINTS) {
                try {
                    URL apiUrl = new URL(instance + videoId);
                    HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3500);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Nothing Player)");
                    conn.setRequestProperty("Accept", "application/json");

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());
                        if (json.has("title")) {
                            foundTitle = json.getString("title");
                        }

                        // 1. Check for adaptive HLS stream
                        if (json.has("hlsUrl") && !json.isNull("hlsUrl")) {
                            String hls = json.getString("hlsUrl");
                            if (hls.startsWith("http")) {
                                foundStreamUrl = hls;
                                break;
                            }
                        }

                        // 2. Check for combined MP4 video streams
                        if (json.has("formatStreams")) {
                            JSONArray formats = json.getJSONArray("formatStreams");
                            for (int i = 0; i < formats.length(); i++) {
                                JSONObject f = formats.getJSONObject(i);
                                if (f.has("url")) {
                                    String u = f.getString("url");
                                    if (u.startsWith("http")) {
                                        foundStreamUrl = u;
                                        break;
                                    }
                                }
                            }
                            if (foundStreamUrl != null) break;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next endpoint
                }
            }

            final String finalStream = (foundStreamUrl != null && !foundStreamUrl.isEmpty()) ? foundStreamUrl : url;
            final String finalTitle = foundTitle;

            mainHandler.post(() -> callback.onResolved(finalStream, finalTitle));
        });
    }
}
