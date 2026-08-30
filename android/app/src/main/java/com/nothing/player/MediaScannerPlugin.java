package com.nothing.player;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Size;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;

@CapacitorPlugin(
    name = "MediaScanner",
    permissions = {
        @Permission(
            alias = "media_audio",
            strings = {
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        ),
        @Permission(
            alias = "media_video",
            strings = {
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        )
    }
)
public class MediaScannerPlugin extends Plugin {

    @PluginMethod
    public void scanAllMedia(PluginCall call) {
        if (!hasRequiredPermissions()) {
            requestAllPermissions(call, "permissionCallback");
            return;
        }

        executeScan(call);
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        executeScan(call);
    }

    private void executeScan(PluginCall call) {
        JSObject result = new JSObject();
        JSArray videos = scanVideosInternal();
        JSArray audios = scanAudiosInternal();
        
        result.put("videos", videos);
        result.put("audios", audios);
        result.put("videoCount", videos.length());
        result.put("audioCount", audios.length());
        
        call.resolve(result);
    }

    @PluginMethod
    public void playInExoPlayer(PluginCall call) {
        String path = call.getString("path");
        String uriStr = call.getString("contentUri");
        String title = call.getString("title");
        Long position = call.getLong("position", 0L);

        try {
            android.content.Intent intent = new android.content.Intent(getContext(), ExoVideoPlayerActivity.class);
            intent.putExtra("path", path);
            intent.putExtra("contentUri", uriStr);
            intent.putExtra("title", title);
            intent.putExtra("position", position != null ? position * 1000 : 0);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Could not launch ExoPlayer: " + e.getMessage());
        }
    }

    @Override
    public void load() {
        super.load();
        MusicPlaybackService.setEventListener(new MusicPlaybackService.PlaybackEventListener() {
            @Override
            public void onTrackEnded() {
                JSObject data = new JSObject();
                notifyListeners("audioTrackEnded", data);
            }

            @Override
            public void onPlayStateChanged(boolean isPlaying) {
                JSObject data = new JSObject();
                data.put("isPlaying", isPlaying);
                notifyListeners("audioPlayStateChanged", data);
            }

            @Override
            public void onNextRequested() {
                notifyListeners("audioNextRequested", new JSObject());
            }

            @Override
            public void onPrevRequested() {
                notifyListeners("audioPrevRequested", new JSObject());
            }
        });
    }

    @PluginMethod
    public void playAudio(PluginCall call) {
        String title = call.getString("title", "Nothing Track");
        String artist = call.getString("artist", "Nothing Player");
        String path = call.getString("path");
        String uriStr = call.getString("contentUri");

        try {
            android.content.Intent serviceIntent = new android.content.Intent(getContext(), MusicPlaybackService.class);
            serviceIntent.setAction(MusicPlaybackService.ACTION_PLAY);
            serviceIntent.putExtra("title", title);
            serviceIntent.putExtra("artist", artist);
            serviceIntent.putExtra("uri", uriStr);
            serviceIntent.putExtra("path", path);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }

            if (MusicPlaybackService.instance != null) {
                MusicPlaybackService.instance.playTrack(title, artist, uriStr, path);
            }

            JSObject res = new JSObject();
            res.put("isPlaying", true);
            call.resolve(res);
        } catch (Exception e) {
            e.printStackTrace();
            call.reject("Audio playback error: " + e.getMessage());
        }
    }

    @PluginMethod
    public void pauseAudio(PluginCall call) {
        try {
            if (MusicPlaybackService.instance != null) {
                MusicPlaybackService.instance.pause();
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void resumeAudio(PluginCall call) {
        try {
            if (MusicPlaybackService.instance != null) {
                MusicPlaybackService.instance.resume();
            } else {
                android.content.Intent serviceIntent = new android.content.Intent(getContext(), MusicPlaybackService.class);
                serviceIntent.setAction(MusicPlaybackService.ACTION_RESUME);
                getContext().startService(serviceIntent);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void seekAudio(PluginCall call) {
        try {
            Double position = call.getDouble("position", 0.0);
            if (MusicPlaybackService.instance != null && position != null) {
                MusicPlaybackService.instance.seekTo(position);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void getAudioStatus(PluginCall call) {
        JSObject res = new JSObject();
        if (MusicPlaybackService.instance != null) {
            try {
                res.put("isPlaying", MusicPlaybackService.instance.isPlaying());
                res.put("currentTime", MusicPlaybackService.instance.getCurrentPosition());
                res.put("duration", MusicPlaybackService.instance.getDuration());
            } catch (Exception e) {
                res.put("isPlaying", false);
                res.put("currentTime", 0);
                res.put("duration", 0);
            }
        } else {
            res.put("isPlaying", false);
            res.put("currentTime", 0);
            res.put("duration", 0);
        }
        call.resolve(res);
    }

    @PluginMethod
    public void stopAudio(PluginCall call) {
        try {
            if (MusicPlaybackService.instance != null) {
                MusicPlaybackService.instance.stop();
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void openMediaFile(PluginCall call) {
        String path = call.getString("path");
        String uriStr = call.getString("contentUri");

        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            Uri uri;
            if (uriStr != null && !uriStr.isEmpty()) {
                uri = Uri.parse(uriStr);
            } else if (path != null && !path.isEmpty()) {
                uri = Uri.fromFile(new File(path));
            } else {
                call.reject("No URI or path provided");
                return;
            }

            intent.setDataAndType(uri, "video/*");
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Could not open media: " + e.getMessage());
        }
    }

    @Override
    public boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @PluginMethod
    public void scanVideos(PluginCall call) {
        JSArray videos = scanVideosInternal();
        JSObject result = new JSObject();
        result.put("videos", videos);
        result.put("count", videos.length());
        call.resolve(result);
    }

    @PluginMethod
    public void scanAudios(PluginCall call) {
        JSArray audios = scanAudiosInternal();
        JSObject result = new JSObject();
        result.put("audios", audios);
        result.put("count", audios.length());
        call.resolve(result);
    }

    private JSArray scanVideosInternal() {
        JSArray videoList = new JSArray();
        
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        };

        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
                int widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
                int heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);

                int count = 0;
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    String title = cursor.getString(titleCol);
                    long durationMs = cursor.getLong(durCol);
                    long size = cursor.getLong(sizeCol);
                    String path = cursor.getString(dataCol);
                    String folder = cursor.getString(bucketCol);
                    long dateAdded = cursor.getLong(dateCol);
                    int width = cursor.getInt(widthCol);
                    int height = cursor.getInt(heightCol);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                    String exactTitle = (name != null && !name.isEmpty()) ? name : ((title != null && !title.isEmpty()) ? title : "Video " + id);

                    String ext = "MP4";
                    if (name != null && name.contains(".")) {
                        ext = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
                    }

                    String resolution = "HD";
                    if (width >= 3840 || height >= 2160) {
                        resolution = "4K UHD";
                    } else if (width >= 1920 || height >= 1080) {
                        resolution = "1080p FHD";
                    } else if (width >= 1280 || height >= 720) {
                        resolution = "720p HD";
                    }

                    // Extract real video thumbnail
                    String base64Thumbnail = "";
                    if (count < 25) { // generate high speed thumbnails for top items
                        base64Thumbnail = getVideoThumbnailBase64(contentUri, path);
                    }

                    JSObject videoObj = new JSObject();
                    videoObj.put("id", "device_vid_" + id);
                    videoObj.put("title", exactTitle);
                    videoObj.put("duration", durationMs > 0 ? durationMs / 1000 : 0);
                    videoObj.put("size", size);
                    videoObj.put("path", path != null ? path : "");
                    videoObj.put("contentUri", contentUri.toString());
                    videoObj.put("url", path != null ? "_capacitor_file_" + path : contentUri.toString());
                    videoObj.put("folder", (folder != null && !folder.isEmpty()) ? folder : "Camera");
                    videoObj.put("format", ext);
                    videoObj.put("resolution", resolution);
                    videoObj.put("type", "video");
                    videoObj.put("decoder", "HW");
                    videoObj.put("addedAt", dateAdded * 1000);
                    if (!base64Thumbnail.isEmpty()) {
                        videoObj.put("thumbnail", base64Thumbnail);
                    } else {
                        videoObj.put("thumbnail", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80");
                    }

                    videoList.put(videoObj);
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videoList;
    }

    private String getVideoThumbnailBase64(Uri contentUri, String path) {
        try {
            Bitmap bitmap = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    bitmap = getContext().getContentResolver().loadThumbnail(contentUri, new Size(320, 200), null);
                } catch (Exception ignored) {}
            }

            if (bitmap == null && path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
                }
            }

            if (bitmap == null && path != null && !path.isEmpty()) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(path);
                    bitmap = retriever.getFrameAtTime(2000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                } catch (Exception ignored) {
                } finally {
                    try { retriever.release(); } catch (Exception ignored) {}
                }
            }

            if (bitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                byte[] byteArray = stream.toByteArray();
                bitmap.recycle();
                return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private JSArray scanAudiosInternal() {
        JSArray audioList = new JSArray();
        
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    long durationMs = cursor.getLong(durCol);
                    long size = cursor.getLong(sizeCol);
                    String path = cursor.getString(dataCol);
                    String folder = cursor.getString(bucketCol);
                    long dateAdded = cursor.getLong(dateCol);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    String exactTitle = (title != null && !title.isEmpty()) ? title : (name != null ? name : "Audio " + id);

                    String ext = "MP3";
                    if (name != null && name.contains(".")) {
                        ext = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
                    }

                    JSObject audioObj = new JSObject();
                    audioObj.put("id", "device_audio_" + id);
                    audioObj.put("title", exactTitle);
                    audioObj.put("artist", (artist != null && !artist.equals("<unknown>")) ? artist : "Unknown Artist");
                    audioObj.put("album", (album != null && !album.equals("<unknown>")) ? album : "Music");
                    audioObj.put("duration", durationMs > 0 ? durationMs / 1000 : 0);
                    audioObj.put("size", size);
                    audioObj.put("path", path != null ? path : "");
                    audioObj.put("contentUri", contentUri.toString());
                    audioObj.put("url", path != null ? "_capacitor_file_" + path : contentUri.toString());
                    audioObj.put("folder", (folder != null && !folder.isEmpty()) ? folder : "Music");
                    audioObj.put("format", ext);
                    audioObj.put("type", "audio");
                    audioObj.put("addedAt", dateAdded * 1000);
                    audioObj.put("thumbnail", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&auto=format&fit=crop&q=80");

                    audioList.put(audioObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return audioList;
    }
}
