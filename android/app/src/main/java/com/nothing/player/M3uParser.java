package com.nothing.player;

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

    private static final Pattern GROUP_PATTERN = Pattern.compile("group-title=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOGO_PATTERN = Pattern.compile("tvg-logo=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN = Pattern.compile(",(.*)$");

    public static void loadM3u(String pathOrUrl, ParseCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            try {
                InputStream is;
                if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                    HttpURLConnection conn = (HttpURLConnection) new URL(pathOrUrl).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    is = conn.getInputStream();
                } else {
                    is = new FileInputStream(new File(pathOrUrl));
                }

                List<M3uChannel> channels = parseStream(is);
                mainHandler.post(() -> callback.onParsed(channels));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
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
                    if (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("rtsp://") || line.startsWith("rtmp://")) {
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

