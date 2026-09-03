package com.nothing.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 100% Non-Root standard Android file deletion helper.
 * Uses Scoped Storage & All Files Access (MANAGE_EXTERNAL_STORAGE)
 * to delete media cleanly and silently without repetitive popups.
 */
public class FileDeleteHelper {
    private static final String TAG = "FileDeleteHelper";

    public interface DeleteCallback {
        void onCompleted(int deletedCount, Set<String> deletedPaths);
    }

    public static boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    public static void requestAllFilesAccess(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivity(intent);
            }
        }
    }

    public static void deleteMediaFiles(
            Activity activity,
            Collection<MediaItem> items,
            ActivityResultLauncher<IntentSenderRequest> deleteLauncher,
            DeleteCallback callback
    ) {
        if (activity == null || items == null || items.isEmpty()) {
            if (callback != null) callback.onCompleted(0, new HashSet<>());
            return;
        }

        // If Android 11+ and user hasn't granted All Files Access, ask them once so future deletes are 100% silent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Direct File Delete Permission")
                .setMessage("Allow 'All files access' once in system settings so Nothing Player can delete files instantly without asking system confirmation every time.")
                .setPositiveButton("ALLOW IN SETTINGS", (dialog, which) -> {
                    requestAllFilesAccess(activity);
                })
                .setNegativeButton("USE SYSTEM POPUP", (dialog, which) -> {
                    performDeletion(activity, items, deleteLauncher, callback);
                })
                .show();
            return;
        }

        performDeletion(activity, items, deleteLauncher, callback);
    }

    private static void performDeletion(
            Activity activity,
            Collection<MediaItem> items,
            ActivityResultLauncher<IntentSenderRequest> deleteLauncher,
            DeleteCallback callback
    ) {
        new Thread(() -> {
            ContentResolver resolver = activity.getContentResolver();
            Set<String> deletedPaths = new HashSet<>();
            List<Uri> permissionNeededUris = new ArrayList<>();

            for (MediaItem item : items) {
                if (item == null) continue;
                boolean success = false;

                // 1. Direct Java File deletion (Works silently when All Files Access is granted)
                if (item.path != null && !item.path.isEmpty()) {
                    try {
                        File f = new File(item.path);
                        if (f.exists() && f.delete()) {
                            success = true;
                        }
                    } catch (Exception ignored) {}
                }

                // 2. ContentResolver delete
                if (!success && item.contentUri != null && !item.contentUri.isEmpty()) {
                    try {
                        int rows = resolver.delete(Uri.parse(item.contentUri), null, null);
                        if (rows > 0) {
                            success = true;
                        }
                    } catch (Exception ignored) {}
                }

                if (success) {
                    if (item.path != null) deletedPaths.add(item.path);
                } else {
                    if (item.contentUri != null) {
                        try {
                            permissionNeededUris.add(Uri.parse(item.contentUri));
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Sync with Android MediaStore immediately so deleted files disappear from lists
            if (!deletedPaths.isEmpty()) {
                String[] pathsArray = deletedPaths.toArray(new String[0]);
                MediaScannerConnection.scanFile(activity.getApplicationContext(), pathsArray, null, null);
                MediaRepository.removeItemsFromCache(activity.getApplicationContext(), deletedPaths);
            }

            // 3. Fallback: If any file required system permission on Android 11+ (Scoped Storage)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !permissionNeededUris.isEmpty() && deleteLauncher != null && deletedPaths.size() < items.size()) {
                List<Uri> pending = new ArrayList<>();
                for (MediaItem item : items) {
                    if (item.path != null && !deletedPaths.contains(item.path) && item.contentUri != null) {
                        pending.add(Uri.parse(item.contentUri));
                    }
                }
                if (!pending.isEmpty()) {
                    activity.runOnUiThread(() -> {
                        try {
                            IntentSender is = MediaStore.createDeleteRequest(resolver, pending).getIntentSender();
                            IntentSenderRequest request = new IntentSenderRequest.Builder(is).build();
                            deleteLauncher.launch(request);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to launch MediaStore.createDeleteRequest", e);
                            if (callback != null) callback.onCompleted(deletedPaths.size(), deletedPaths);
                        }
                    });
                    return;
                }
            }

            activity.runOnUiThread(() -> {
                if (callback != null) callback.onCompleted(deletedPaths.size(), deletedPaths);
            });
        }).start();
    }
}
