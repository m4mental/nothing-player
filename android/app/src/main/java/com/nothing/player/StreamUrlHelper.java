package com.nothing.player;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamUrlHelper {

    private static final String PREF_NAME = "nothing_streams_prefs";
    private static final String KEY_RECENT = "recent_streams";

    // Regex to match URLs starting with http, https, rtsp, rtmp, mms
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?|rtsp|rtmp|mms)://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Extracts the first valid stream/web URL from any raw text,
     * cleanly trimming trailing punctuation like '.', ',', ')', etc.
     */
    public static String extractUrl(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        Matcher matcher = URL_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            String url = matcher.group(0);
            while (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith(">") || url.endsWith(";") || url.endsWith("\"") || url.endsWith("'")) {
                url = url.substring(0, url.length() - 1);
            }
            return url;
        }
        return null;
    }

    /**
     * Checks if a string is a stream URL or online media protocol.
     */
    public static boolean isOnlineStream(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("rtsp://") || lower.startsWith("rtmp://")
                || lower.startsWith("mms://");
    }

    /**
     * Extracts media URI or stream URL from any Android Intent (ACTION_SEND, ACTION_VIEW, etc.)
     */
    public static String extractMediaUriOrUrl(Intent intent) {
        if (intent == null) return null;

        // 1. Direct Intent Data (e.g. ACTION_VIEW with Uri)
        if (intent.getData() != null) {
            String dataStr = intent.getDataString();
            String extracted = extractUrl(dataStr);
            if (extracted != null) return extracted;
            return dataStr;
        }

        // 2. EXTRA_TEXT (Shared link / text via ACTION_SEND)
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            CharSequence cs = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (cs != null) {
                String extracted = extractUrl(cs.toString());
                if (extracted != null) return extracted;
                String trimmed = cs.toString().trim();
                if (isOnlineStream(trimmed)) return trimmed;
            }
        }

        // 3. ClipData
        if (intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
            for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                ClipData.Item item = intent.getClipData().getItemAt(i);
                if (item.getUri() != null) {
                    return item.getUri().toString();
                }
                if (item.getText() != null) {
                    String extracted = extractUrl(item.getText().toString());
                    if (extracted != null) return extracted;
                    String trimmed = item.getText().toString().trim();
                    if (isOnlineStream(trimmed)) return trimmed;
                }
            }
        }

        // 4. EXTRA_STREAM (Shared media file URI via ACTION_SEND)
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            try {
                Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (streamUri != null) return streamUri.toString();
            } catch (Exception ignored) {}
        }

        // 5. Custom App Extras
        String[] keys = {"video_uri", "contentUri", "path", "video_path", "url"};
        for (String key : keys) {
            if (intent.hasExtra(key)) {
                String val = intent.getStringExtra(key);
                if (val != null && !val.trim().isEmpty()) {
                    String extracted = extractUrl(val);
                    if (extracted != null) return extracted;
                    return val.trim();
                }
            }
        }

        return null;
    }

    /**
     * Extracts or creates a friendly, descriptive title for the media.
     */
    public static String extractTitle(Context context, Intent intent, String mediaUrl) {
        if (intent != null) {
            if (intent.hasExtra("title")) {
                String t = intent.getStringExtra("title");
                if (t != null && !t.trim().isEmpty()) return t.trim();
            }
            if (intent.hasExtra("video_title")) {
                String t = intent.getStringExtra("video_title");
                if (t != null && !t.trim().isEmpty()) return t.trim();
            }
            if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
                String subj = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                if (subj != null && !subj.trim().isEmpty()) return subj.trim();
            }
            if (intent.hasExtra(Intent.EXTRA_TITLE)) {
                String t = intent.getStringExtra(Intent.EXTRA_TITLE);
                if (t != null && !t.trim().isEmpty()) return t.trim();
            }
        }

        if (mediaUrl != null) {
            if (YouTubeStreamResolver.isYouTubeUrl(mediaUrl)) {
                return "YouTube Stream";
            }
            try {
                Uri uri = Uri.parse(mediaUrl);
                if ("content".equalsIgnoreCase(uri.getScheme()) && context != null) {
                    try (android.database.Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            if (idx != -1) {
                                String name = cursor.getString(idx);
                                if (name != null && !name.trim().isEmpty()) return name.trim();
                            }
                        }
                    } catch (Exception ignored) {}
                }

                String last = uri.getLastPathSegment();
                if (last != null && !last.trim().isEmpty() && !last.equals("/")) {
                    return last;
                }
                if (uri.getHost() != null) {
                    return "Online Stream (" + uri.getHost() + ")";
                }
            } catch (Exception ignored) {}
        }

        return "Online Stream";
    }

    /**
     * Automatically saves played stream to Recent Streams so it's readily accessible in Library.
     */
    public static void saveRecentStream(Context context, String url) {
        if (context == null || url == null || !isOnlineStream(url)) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String recent = prefs.getString(KEY_RECENT, "");
            String updated = url + "\n" + recent.replace(url + "\n", "");
            prefs.edit().putString(KEY_RECENT, updated).apply();
        } catch (Exception ignored) {}
    }

    public static java.util.List<String> getRecentStreams(Context context) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (context == null) return list;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String recent = prefs.getString(KEY_RECENT, "");
            if (!recent.isEmpty()) {
                String[] parts = recent.split("\n");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) {
                        list.add(p.trim());
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }
}
