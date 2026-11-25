package com.nothing.player;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

public class StreamDownloadHelper {

    public static void downloadMedia(Context context, String mediaUrl, String title) {
        if (context == null || mediaUrl == null || mediaUrl.trim().isEmpty()) {
            return;
        }

        String trimmed = mediaUrl.trim();
        String lower = trimmed.toLowerCase(Locale.US);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            Toast.makeText(context, "Only direct HTTP/HTTPS URLs can be downloaded", Toast.LENGTH_SHORT).show();
            return;
        }
        if (YouTubeStreamResolver.isYouTubeUrl(trimmed) || lower.contains(".m3u8") || lower.contains(".mpd")) {
            Toast.makeText(context, "Adaptive streams / YouTube links require a direct media file URL", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Uri uri = Uri.parse(trimmed);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            String fileName = (title != null && !title.trim().isEmpty()) ? title.trim() : "video_" + System.currentTimeMillis();
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            if (fileName.equals(".") || fileName.equals("..")) {
                fileName = "video_" + System.currentTimeMillis();
            }
            if (fileName.length() > 100) {
                fileName = fileName.substring(0, 100);
            }

            String ext = extractMediaExtension(uri.getLastPathSegment());
            if (!hasSupportedMediaExtension(fileName)) {
                fileName += (ext != null ? ext : ".mp4");
            }

            request.setTitle("Nothing Player: " + (title != null ? title : fileName));
            request.setDescription("Downloading media for offline playback...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            boolean isAudio = fileName.toLowerCase(Locale.US).matches(".*\\.(mp3|m4a|aac|flac|ogg|opus|wav)$");
            String publicDir = isAudio ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES;
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    File targetDir = new File(Environment.getExternalStoragePublicDirectory(publicDir), "NothingPlayer");
                    if (!targetDir.exists()) targetDir.mkdirs();
                }
                request.setDestinationInExternalPublicDir(publicDir, "NothingPlayer/" + fileName);
            } catch (Exception destEx) {
                request.setDestinationInExternalFilesDir(context, publicDir, fileName);
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(context, "DOWNLOAD STARTED: " + fileName, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Download service unavailable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean hasSupportedMediaExtension(String name) {
        return name.toLowerCase(Locale.US).matches(".*\\.(mp4|mkv|webm|mov|avi|mp3|m4a|aac|flac|ogg|opus|wav)$");
    }

    private static String extractMediaExtension(String lastSegment) {
        if (lastSegment == null) return null;
        String lower = lastSegment.toLowerCase(Locale.US);
        int dot = lower.lastIndexOf('.');
        if (dot >= 0 && dot < lower.length() - 1) {
            String ext = lower.substring(dot);
            if (hasSupportedMediaExtension("a" + ext)) {
                return ext;
            }
        }
        return null;
    }
}

