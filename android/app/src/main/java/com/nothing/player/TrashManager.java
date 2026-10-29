package com.nothing.player;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrashManager {
    private static final String TAG = "TrashManager";
    private static final String TRASH_DIR = ".trash";
    private static final String META_FILE = "trash_metadata.json";
    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    public static class TrashItem {
        public String id;
        public String originalPath;
        public String trashedPath;
        public String title;
        public long trashedAt;
        public long size;
        public long duration;

        public int getDaysRemaining() {
            long elapsed = System.currentTimeMillis() - trashedAt;
            long remainingMs = Math.max(0, THIRTY_DAYS_MS - elapsed);
            return (int) Math.ceil(remainingMs / (24.0 * 60 * 60 * 1000));
        }
    }

    public static File getTrashDir(Context context) {
        File dir = new File(context.getFilesDir(), TRASH_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File nomedia = new File(dir, ".nomedia");
        if (!nomedia.exists()) {
            try { nomedia.createNewFile(); } catch (Exception ignored) {}
        }
        return dir;
    }

    public static synchronized boolean moveToTrash(Context context, MediaItem item) {
        if (context == null || item == null || item.path == null) return false;
        File sourceFile = new File(item.path);
        if (!sourceFile.exists()) return false;

        try {
            File trashDir = getTrashDir(context);
            String ext = "";
            int dotIdx = sourceFile.getName().lastIndexOf('.');
            if (dotIdx != -1) {
                ext = sourceFile.getName().substring(dotIdx);
            }
            String trashFileName = "trash_" + System.currentTimeMillis() + ext;
            File destFile = new File(trashDir, trashFileName);

            boolean moved = sourceFile.renameTo(destFile);
            if (!moved) {
                // If rename across mounts fails, copy and delete
                moved = copyAndDelete(sourceFile, destFile);
            }

            if (moved) {
                MediaScannerConnection.scanFile(context, new String[]{sourceFile.getAbsolutePath()}, null, null);

                TrashItem trashItem = new TrashItem();
                trashItem.id = String.valueOf(System.currentTimeMillis());
                trashItem.originalPath = sourceFile.getAbsolutePath();
                trashItem.trashedPath = destFile.getAbsolutePath();
                trashItem.title = item.title != null ? item.title : sourceFile.getName();
                trashItem.trashedAt = System.currentTimeMillis();
                trashItem.size = item.size > 0 ? item.size : destFile.length();
                trashItem.duration = item.duration;

                saveItemToMeta(context, trashItem);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error moving to trash: " + item.path, e);
        }
        return false;
    }

    public static synchronized boolean restoreItem(Context context, TrashItem item) {
        if (context == null || item == null || item.trashedPath == null) return false;
        File trashedFile = new File(item.trashedPath);
        if (!trashedFile.exists()) {
            removeItemFromMeta(context, item.id);
            return false;
        }

        try {
            File targetFile = new File(item.originalPath);
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            boolean restored = trashedFile.renameTo(targetFile);
            if (!restored) {
                restored = copyAndDelete(trashedFile, targetFile);
            }

            if (restored) {
                MediaScannerConnection.scanFile(context, new String[]{targetFile.getAbsolutePath()}, null, null);
                removeItemFromMeta(context, item.id);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring trash item: " + item.originalPath, e);
        }
        return false;
    }

    public static synchronized boolean deletePermanently(Context context, TrashItem item) {
        if (item == null) return false;
        try {
            if (item.trashedPath != null) {
                File f = new File(item.trashedPath);
                if (f.exists()) f.delete();
            }
            removeItemFromMeta(context, item.id);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error permanently deleting", e);
        }
        return false;
    }

    public static synchronized void emptyTrash(Context context) {
        List<TrashItem> items = getTrashItems(context);
        for (TrashItem item : items) {
            deletePermanently(context, item);
        }
    }

    public static synchronized List<TrashItem> getTrashItems(Context context) {
        List<TrashItem> items = new ArrayList<>();
        if (context == null) return items;
        File metaFile = new File(context.getFilesDir(), META_FILE);
        if (!metaFile.exists()) return items;

        try (FileInputStream fis = new FileInputStream(metaFile)) {
            byte[] data = new byte[(int) metaFile.length()];
            fis.read(data);
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            long now = System.currentTimeMillis();
            List<TrashItem> validItems = new ArrayList<>();
            boolean needsClean = false;

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                TrashItem item = new TrashItem();
                item.id = obj.optString("id");
                item.originalPath = obj.optString("originalPath");
                item.trashedPath = obj.optString("trashedPath");
                item.title = obj.optString("title");
                item.trashedAt = obj.optLong("trashedAt");
                item.size = obj.optLong("size");
                item.duration = obj.optLong("duration");

                File f = new File(item.trashedPath);
                if (!f.exists() || (now - item.trashedAt > THIRTY_DAYS_MS)) {
                    // Expired or missing
                    if (f.exists()) f.delete();
                    needsClean = true;
                } else {
                    validItems.add(item);
                }
            }

            if (needsClean) {
                saveAllToMeta(context, validItems);
            }
            return validItems;
        } catch (Exception e) {
            Log.e(TAG, "Error reading trash metadata", e);
        }
        return items;
    }

    private static void saveItemToMeta(Context context, TrashItem item) {
        List<TrashItem> all = getTrashItems(context);
        all.add(0, item);
        saveAllToMeta(context, all);
    }

    private static void removeItemFromMeta(Context context, String id) {
        List<TrashItem> all = getTrashItems(context);
        Iterator<TrashItem> it = all.iterator();
        while (it.hasNext()) {
            if (it.next().id.equals(id)) {
                it.remove();
                break;
            }
        }
        saveAllToMeta(context, all);
    }

    private static void saveAllToMeta(Context context, List<TrashItem> items) {
        try {
            JSONArray array = new JSONArray();
            for (TrashItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("originalPath", item.originalPath);
                obj.put("trashedPath", item.trashedPath);
                obj.put("title", item.title);
                obj.put("trashedAt", item.trashedAt);
                obj.put("size", item.size);
                obj.put("duration", item.duration);
                array.put(obj);
            }
            File metaFile = new File(context.getFilesDir(), META_FILE);
            try (FileOutputStream fos = new FileOutputStream(metaFile)) {
                fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing trash metadata", e);
        }
    }

    private static boolean copyAndDelete(File src, File dst) {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            src.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

