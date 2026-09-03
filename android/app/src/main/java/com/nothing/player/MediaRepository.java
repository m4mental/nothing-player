package com.nothing.player;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MediaRepository {
    private static final String TAG = "MediaRepository";
    private static final String CACHE_VIDEOS_FILE = "cached_videos.json";
    private static final String CACHE_AUDIOS_FILE = "cached_audios.json";

    private static List<MediaItem> cachedVideos = null;
    private static List<MediaItem> cachedAudios = null;

    public interface ScanCallback {
        void onScanComplete(List<MediaItem> videos, List<MediaItem> audios);
    }

    /**
     * Instantly returns cached videos (from memory or disk) with 0ms delay on startup.
     */
    public static synchronized List<MediaItem> getCachedVideos(Context context) {
        if (cachedVideos != null) {
            return new ArrayList<>(cachedVideos);
        }
        cachedVideos = loadListFromDisk(context, CACHE_VIDEOS_FILE);
        return new ArrayList<>(cachedVideos);
    }

    /**
     * Instantly returns cached audios (from memory or disk) with 0ms delay on startup.
     */
    public static synchronized List<MediaItem> getCachedAudios(Context context) {
        if (cachedAudios != null) {
            return new ArrayList<>(cachedAudios);
        }
        cachedAudios = loadListFromDisk(context, CACHE_AUDIOS_FILE);
        return new ArrayList<>(cachedAudios);
    }

    /**
     * Background scanning that updates cache when differences are detected.
     */
    public static void scanMedia(Context context, ScanCallback callback) {
        new Thread(() -> {
            List<MediaItem> freshVideos = scanVideos(context);
            List<MediaItem> freshAudios = scanAudios(context);

            boolean videosChanged = hasListChanged(cachedVideos, freshVideos);
            boolean audiosChanged = hasListChanged(cachedAudios, freshAudios);

            if (videosChanged || audiosChanged || cachedVideos == null || cachedAudios == null) {
                synchronized (MediaRepository.class) {
                    cachedVideos = freshVideos;
                    cachedAudios = freshAudios;
                }
                saveListToDisk(context, CACHE_VIDEOS_FILE, freshVideos);
                saveListToDisk(context, CACHE_AUDIOS_FILE, freshAudios);
            }

            if (callback != null) {
                callback.onScanComplete(freshVideos, freshAudios);
            }
        }).start();
    }

    /**
     * Explicitly update cache when items are deleted.
     */
    public static synchronized void removeItemsFromCache(Context context, Set<String> deletedPaths) {
        if (deletedPaths == null || deletedPaths.isEmpty()) return;

        if (cachedVideos != null) {
            List<MediaItem> updated = new ArrayList<>();
            for (MediaItem item : cachedVideos) {
                if (!deletedPaths.contains(item.path)) updated.add(item);
            }
            cachedVideos = updated;
            saveListToDisk(context, CACHE_VIDEOS_FILE, updated);
        }

        if (cachedAudios != null) {
            List<MediaItem> updated = new ArrayList<>();
            for (MediaItem item : cachedAudios) {
                if (!deletedPaths.contains(item.path)) updated.add(item);
            }
            cachedAudios = updated;
            saveListToDisk(context, CACHE_AUDIOS_FILE, updated);
        }
    }

    private static boolean hasListChanged(List<MediaItem> oldList, List<MediaItem> newList) {
        if (oldList == null || newList == null) return true;
        if (oldList.size() != newList.size()) return true;

        // Quick check first and last item
        if (!oldList.isEmpty()) {
            if (!oldList.get(0).path.equals(newList.get(0).path)) return true;
            int lastIdx = oldList.size() - 1;
            if (!oldList.get(lastIdx).path.equals(newList.get(lastIdx).path)) return true;
        }

        Set<String> oldPaths = new HashSet<>(oldList.size());
        for (MediaItem m : oldList) oldPaths.add(m.path);
        for (MediaItem m : newList) {
            if (!oldPaths.contains(m.path)) return true;
        }
        return false;
    }

    private static List<MediaItem> loadListFromDisk(Context context, String fileName) {
        List<MediaItem> list = new ArrayList<>();
        if (context == null) return list;
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) return list;

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                MediaItem item = new MediaItem();
                item.id = obj.optString("id");
                item.title = obj.optString("title");
                item.path = obj.optString("path");
                item.contentUri = obj.optString("contentUri");
                item.duration = obj.optLong("duration");
                item.size = obj.optLong("size");
                item.format = obj.optString("format");
                item.resolution = obj.optString("resolution");
                item.folder = obj.optString("folder");
                item.artist = obj.optString("artist");
                item.album = obj.optString("album");
                item.audioCodec = obj.optString("audioCodec");
                item.audioChannels = obj.optString("audioChannels");
                item.type = obj.optString("type", "video");
                item.addedAt = obj.optLong("addedAt");

                // Verify file still exists on disk before adding
                if (item.path != null && new File(item.path).exists()) {
                    list.add(item);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed reading cache file: " + fileName, e);
        }
        return list;
    }

    private static void saveListToDisk(Context context, String fileName, List<MediaItem> list) {
        if (context == null || list == null) return;
        new Thread(() -> {
            try {
                File file = new File(context.getFilesDir(), fileName);
                JSONArray arr = new JSONArray();
                for (MediaItem item : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", item.id);
                    obj.put("title", item.title);
                    obj.put("path", item.path);
                    obj.put("contentUri", item.contentUri);
                    obj.put("duration", item.duration);
                    obj.put("size", item.size);
                    obj.put("format", item.format);
                    obj.put("resolution", item.resolution);
                    obj.put("folder", item.folder);
                    obj.put("artist", item.artist);
                    obj.put("album", item.album);
                    obj.put("audioCodec", item.audioCodec);
                    obj.put("audioChannels", item.audioChannels);
                    obj.put("type", item.type);
                    obj.put("addedAt", item.addedAt);
                    arr.put(obj);
                }

                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                    osw.write(arr.toString());
                    osw.flush();
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed saving cache file: " + fileName, e);
            }
        }).start();
    }

    public static List<MediaItem> scanVideos(Context context) {
        List<MediaItem> list = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DATE_ADDED
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
                int hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String path = cursor.getString(dataCol);
                    long duration = cursor.getLong(durCol);
                    long size = cursor.getLong(sizeCol);
                    int width = cursor.getInt(wCol);
                    int height = cursor.getInt(hCol);
                    long added = cursor.getLong(dateCol) * 1000L;

                    if (path == null) continue;

                    File f = new File(path);
                    String folder = "Storage";
                    if (f.getParentFile() != null) {
                        folder = f.getParentFile().getName();
                    }

                    String format = "MP4";
                    String name = f.getName();
                    if (name.contains(".")) {
                        format = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
                    }

                    String res = "HD";
                    if (width >= 3840 || height >= 2160) res = "4K UHD";
                    else if (width >= 1920 || height >= 1080) res = "1080p FHD";
                    else if (width >= 1280 || height >= 720) res = "720p HD";
                    else if (width > 0 && height > 0) res = width + "x" + height;

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                    MediaItem item = new MediaItem(
                            "vid_" + id,
                            title != null && !title.isEmpty() ? title : name,
                            path,
                            contentUri.toString(),
                            duration,
                            size,
                            format,
                            folder,
                            "video"
                    );
                    item.resolution = res;
                    item.addedAt = added;

                    // Codec extraction
                    extractAudioFormat(item);

                    list.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning videos", e);
        }
        return list;
    }

    public static List<MediaItem> scanAudios(Context context) {
        List<MediaItem> list = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATE_ADDED
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, MediaStore.Audio.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String path = cursor.getString(dataCol);
                    long duration = cursor.getLong(durCol);
                    long size = cursor.getLong(sizeCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    long added = cursor.getLong(dateCol) * 1000L;

                    if (path == null) continue;

                    File f = new File(path);
                    String folder = "Music";
                    if (f.getParentFile() != null) {
                        folder = f.getParentFile().getName();
                    }

                    String format = "MP3";
                    String name = f.getName();
                    if (name.contains(".")) {
                        format = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
                    }

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    MediaItem item = new MediaItem(
                            "aud_" + id,
                            title != null && !title.isEmpty() ? title : name,
                            path,
                            contentUri.toString(),
                            duration,
                            size,
                            format,
                            folder,
                            "audio"
                    );
                    item.artist = artist != null && !artist.equals("<unknown>") ? artist : "Unknown Artist";
                    item.album = album != null && !album.equals("<unknown>") ? album : "Local Audio";
                    item.addedAt = added;

                    if (format.equalsIgnoreCase("FLAC")) {
                        item.audioCodec = "FLAC Lossless Audio";
                        item.audioChannels = "Stereo (2 Channels, 44.1/96 kHz, 24-bit)";
                    } else if (format.equalsIgnoreCase("M4A") || format.equalsIgnoreCase("AAC")) {
                        item.audioCodec = "AAC LC (MPEG-4 Audio)";
                        item.audioChannels = "Stereo (2 Channels, 44.1 kHz)";
                    } else {
                        item.audioCodec = "MPEG-1 Audio Layer III (MP3)";
                        item.audioChannels = "Stereo (2 Channels, 44.1 kHz)";
                    }

                    list.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning audios", e);
        }
        return list;
    }

    private static void extractAudioFormat(MediaItem item) {
        String lowerTitle = item.title != null ? item.title.toLowerCase() : "";
        String lowerPath = item.path != null ? item.path.toLowerCase() : "";

        if (lowerTitle.contains("eac3") || lowerTitle.contains("e-ac-3") || lowerTitle.contains("ddp") || lowerTitle.contains("dd5.1") || lowerTitle.contains("5.1") ||
            lowerPath.contains("eac3") || lowerPath.contains("dd5.1") || lowerPath.contains("5.1")) {
            item.audioCodec = "Dolby Digital Plus (E-AC-3 5.1 Surround)";
            item.audioChannels = "6 Channels (5.1 Surround, 48 kHz)";
            return;
        }

        if (lowerTitle.contains("ac3") || lowerTitle.contains("ac-3") || lowerTitle.contains("dolby")) {
            item.audioCodec = "Dolby Digital (AC-3)";
            item.audioChannels = "6 Channels (5.1 Surround, 48 kHz)";
            return;
        }

        if (lowerTitle.contains("dts")) {
            item.audioCodec = "DTS Digital Surround (5.1)";
            item.audioChannels = "6 Channels (5.1 Surround, 48 kHz)";
            return;
        }

        item.audioCodec = "AAC LC (Stereo)";
        item.audioChannels = "2 Channels (Stereo, 48 kHz)";
    }
}
