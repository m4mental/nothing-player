package com.nothing.player;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;

public class StreamDownloadHelper {

    public static void downloadMedia(Context context, String mediaUrl, String title) {
        if (context == null || mediaUrl == null || mediaUrl.isEmpty()) {
            return;
        }

        try {
            Uri uri = Uri.parse(mediaUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            String fileName = (title != null && !title.isEmpty()) ? title : "video_" + System.currentTimeMillis();
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            if (!fileName.toLowerCase().endsWith(".mp4") && !fileName.toLowerCase().endsWith(".mkv")) {
                fileName += ".mp4";
            }

            request.setTitle("Nothing Player: " + (title != null ? title : fileName));
            request.setDescription("Downloading video for offline playback...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "NothingPlayer/" + fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

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
}

