package com.nothing.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3uParser {

    public static class M3uChannel {
        public String name;
        public String streamUrl;
        public String group;
        public String logoUrl;

        public M3uChannel(String name, String streamUrl, String group, String logoUrl) {
            this.name = name;
            this.streamUrl = streamUrl;
            this.group = group != null && !group.isEmpty() ? group : "GENERAL";
            this.logoUrl = logoUrl;
        }
    }

    public interface ParseCallback {
        void onParsed(List<M3uChannel> channels);
        void onError(String error);
    }

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static Handler mainHandler;

    private static synchronized Handler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }

    private static final Pattern GROUP_PATTERN = Pattern.compile("group-title=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOGO_PATTERN = Pattern.compile("tvg-logo=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN = Pattern.compile(",(.*)$");

    public static void loadM3u(String pathOrUrl, ParseCallback callback) {
        loadM3u(null, pathOrUrl, callback);
    }

    public static void loadM3u(Context context, String pathOrUrl, ParseCallback callback) {
        if (callback == null) return;
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            getMainHandler().post(() -> callback.onError("Empty playlist path"));
            return;
        }
        final Context appCtx = context != null ? context.getApplicationContext() : null;
        final String target = pathOrUrl.trim();
        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            InputStream is = null;
            try {
                String lower = target.toLowerCase(Locale.US);
                if (lower.startsWith("http://") || lower.startsWith("https://")) {
                    conn = (HttpURLConnection) new URL(target).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    int code = conn.getResponseCode();
                    if (code < 200 || code >= 300) {
                        getMainHandler().post(() -> callback.onError("HTTP " + code));
                        return;
                    }
                    is = conn.getInputStream();
                } else if (lower.startsWith("content://") && appCtx != null) {
                    is = appCtx.getContentResolver().openInputStream(Uri.parse(target));
                } else {
                    is = new FileInputStream(new File(target));
                }

                List<M3uChannel> channels = parseStream(is);
                getMainHandler().post(() -> callback.onParsed(channels));
            } catch (Exception e) {
                getMainHandler().post(() -> callback.onError(e.getMessage()));
            } finally {
                if (is != null) {
                    try { is.close(); } catch (Exception ignored) {}
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    public static List<M3uChannel> parseStream(InputStream is) {
        List<M3uChannel> channels = new ArrayList<>();
        if (is == null) return channels;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            String currentName = null;
            String currentGroup = "GENERAL";
            String currentLogo = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("#EXTINF:")) {
                    Matcher groupMatcher = GROUP_PATTERN.matcher(line);
                    if (groupMatcher.find()) {
                        currentGroup = groupMatcher.group(1);
                    } else {
                        currentGroup = "GENERAL";
                    }

                    Matcher logoMatcher = LOGO_PATTERN.matcher(line);
                    if (logoMatcher.find()) {
                        currentLogo = logoMatcher.group(1);
                    } else {
                        currentLogo = null;
                    }

                    Matcher nameMatcher = NAME_PATTERN.matcher(line);
                    if (nameMatcher.find()) {
                        currentName = nameMatcher.group(1).trim();
                    } else {
                        currentName = "Channel " + (channels.size() + 1);
                    }
                } else if (!line.startsWith("#")) {
                    String lowerLine = line.toLowerCase(Locale.US);
                    if (lowerLine.startsWith("http://") || lowerLine.startsWith("https://")
                            || lowerLine.startsWith("rtsp://") || lowerLine.startsWith("rtmp://")
                            || lowerLine.startsWith("mms://")) {
                        String name = currentName != null ? currentName : ("Channel " + (channels.size() + 1));
                        channels.add(new M3uChannel(name, line, currentGroup, currentLogo));
                    }
                    currentName = null;
                    currentGroup = "GENERAL";
                    currentLogo = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return channels;
    }
}

