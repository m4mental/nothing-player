package com.nothing.player;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
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

    private static DeleteCallback pendingCallback;
    private static final Set<String> pendingPromptPaths = new HashSet<>();
    private static final Set<String> pendingImmediateDeletedPaths = new HashSet<>();

    public interface DeleteCallback {
        void onCompleted(int deletedCount, Set<String> deletedPaths);
    }

    public static void onActivityResult(Context context, boolean approved) {
        DeleteCallback cb;
        Set<String> promptPaths;
        Set<String> immediatePaths;
        synchronized (FileDeleteHelper.class) {
            cb = pendingCallback;
            promptPaths = new HashSet<>(pendingPromptPaths);
            immediatePaths = new HashSet<>(pendingImmediateDeletedPaths);
            pendingCallback = null;
            pendingPromptPaths.clear();
            pendingImmediateDeletedPaths.clear();
        }

        Set<String> allDeleted = new HashSet<>(immediatePaths);
        if (approved && !promptPaths.isEmpty()) {
            allDeleted.addAll(promptPaths);
            if (context != null) {
                Context appCtx = context.getApplicationContext();
                MediaScannerConnection.scanFile(appCtx, promptPaths.toArray(new String[0]), null, null);
                MediaRepository.removeItemsFromCache(appCtx, promptPaths);
            }
        }
        if (cb != null) {
            cb.onCompleted(allDeleted.size(), allDeleted);
        }
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
            final boolean[] handled = {false};
            Dialog dialog = NothingDialogHelper.showConfirmDialog(
                activity,
                "DIRECT FILE DELETE PERMISSION",
                "Allow 'All files access' once in system settings so Nothing Player can delete files instantly without asking system confirmation every time.",
                "ALLOW IN SETTINGS",
                () -> {
                    handled[0] = true;
                    requestAllFilesAccess(activity);
                    if (callback != null) callback.onCompleted(0, new HashSet<>());
                },
                "USE SYSTEM POPUP",
                () -> {
                    handled[0] = true;
                    performDeletion(activity, items, deleteLauncher, callback);
                }
            );
            if (dialog != null) {
                dialog.setOnCancelListener(d -> {
                    if (!handled[0] && callback != null) {
                        callback.onCompleted(0, new HashSet<>());
                    }
                });
            } else if (callback != null) {
                callback.onCompleted(0, new HashSet<>());
            }
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
            Set<String> permissionNeededPaths = new HashSet<>();
            int attemptedCount = 0;

            for (MediaItem item : items) {
                if (item == null) continue;
                attemptedCount++;
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

                // 2. ContentResolver delete (only for content:// URIs — avoids IllegalArgumentException on file://)
                Uri resolvedContentUri = resolveMediaStoreUri(resolver, item);
                if (resolvedContentUri != null) {
                    try {
                        int rows = resolver.delete(resolvedContentUri, null, null);
                        if (rows > 0) {
                            success = true;
                        }
                    } catch (Exception ignored) {}
                }

                if (success) {
                    if (item.path != null && !item.path.isEmpty()) deletedPaths.add(item.path);
                    if (resolvedContentUri != null) deletedPaths.add(resolvedContentUri.toString());
                    if (item.contentUri != null && !item.contentUri.isEmpty()) deletedPaths.add(item.contentUri);
                } else if (resolvedContentUri != null) {
                    permissionNeededUris.add(resolvedContentUri);
                    if (item.path != null && !item.path.isEmpty()) {
                        permissionNeededPaths.add(item.path);
                    } else {
                        permissionNeededPaths.add(resolvedContentUri.toString());
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !permissionNeededUris.isEmpty() && deleteLauncher != null && attemptedCount > 0) {
                synchronized (FileDeleteHelper.class) {
                    pendingCallback = callback;
                    pendingPromptPaths.clear();
                    pendingPromptPaths.addAll(permissionNeededPaths);
                    pendingImmediateDeletedPaths.clear();
                    pendingImmediateDeletedPaths.addAll(deletedPaths);
                }
                activity.runOnUiThread(() -> {
                    try {
                        IntentSender is = MediaStore.createDeleteRequest(resolver, permissionNeededUris).getIntentSender();
                        IntentSenderRequest request = new IntentSenderRequest.Builder(is).build();
                        deleteLauncher.launch(request);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to launch MediaStore.createDeleteRequest", e);
                        onActivityResult(activity, false);
                    }
                });
                return;
            }

            activity.runOnUiThread(() -> {
                if (callback != null) callback.onCompleted(deletedPaths.size(), deletedPaths);
            });
        }).start();
    }

    private static Uri resolveMediaStoreUri(ContentResolver resolver, MediaItem item) {
        if (item == null || resolver == null) return null;
        if (item.contentUri != null && item.contentUri.startsWith("content://")) {
            return Uri.parse(item.contentUri);
        }
        if (item.path == null || item.path.isEmpty()) return null;

        Uri baseUri = "audio".equalsIgnoreCase(item.type)
                ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.MediaColumns._ID};
        String selection = MediaStore.MediaColumns.DATA + "=?";
        try (Cursor cursor = resolver.query(baseUri, projection, selection, new String[]{item.path}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return ContentUris.withAppendedId(baseUri, id);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Copies src to dst across mount boundaries and removes src cleanly (Scoped Storage safe).
     * Deletes dst on failure so unencrypted/untrashed originals are never left duplicated.
     */
    public static boolean copyAndDeleteFile(Context context, File src, File dst, String contentUri) {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            if (dst.exists()) dst.delete();
            return false;
        }

        boolean deleted = src.delete();
        if (!deleted && context != null && contentUri != null && contentUri.startsWith("content://")) {
            try {
                int rows = context.getContentResolver().delete(Uri.parse(contentUri), null, null);
                deleted = rows > 0 || !src.exists();
            } catch (Exception ignored) {}
        }
        if (!deleted && src.exists()) {
            if (dst.exists()) dst.delete();
            return false;
        }
        return true;
    }

    /**
     * Restores a vault/trash video via MediaStore on API 29+ when direct path writes are denied.
     */
    public static boolean restoreVideoViaMediaStore(Context context, File srcFile, String displayName) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NothingPlayer");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);

        Uri inserted = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (inserted == null) return false;
        try {
            try (FileInputStream in = new FileInputStream(srcFile);
                 OutputStream out = resolver.openOutputStream(inserted)) {
                if (out == null) {
                    resolver.delete(inserted, null, null);
                    return false;
                }
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }
            values.clear();
            values.put(MediaStore.Video.Media.IS_PENDING, 0);
            resolver.update(inserted, values, null, null);
            return srcFile.delete() || !srcFile.exists();
        } catch (Exception e) {
            try { resolver.delete(inserted, null, null); } catch (Exception ignored) {}
            return false;
        }
    }
}
