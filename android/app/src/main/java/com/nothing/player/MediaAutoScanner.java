package com.nothing.player;

import android.content.Context;
import android.database.ContentObserver;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Real-time Media Auto-Scanner for Nothing Player.
 * Automatically detects newly downloaded media files (videos, music) in the background
 * via MediaStore ContentObservers and Download directory FileObservers.
 * Eliminates the need for manual scanning or refreshing.
 */
public class MediaAutoScanner {
    private static final String TAG = "MediaAutoScanner";
    private static MediaAutoScanner instance;

    public interface OnMediaChangeListener {
        void onMediaChanged();
    }

    private final List<OnMediaChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context appContext;
    private boolean isWatching = false;

    private ContentObserver videoObserver;
    private ContentObserver audioObserver;
    private ContentObserver downloadsObserver;
    private final List<FileObserver> fileObservers = new ArrayList<>();

    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY_MS = 800;

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts", "m4v", "wmv",
            "mp3", "m4a", "wav", "flac", "aac", "ogg", "opus", "wma"
    ));

    public static synchronized MediaAutoScanner getInstance() {
        if (instance == null) {
            instance = new MediaAutoScanner();
        }
        return instance;
    }

    private MediaAutoScanner() {}

    /**
     * Start background monitoring of MediaStore and Download directories.
     */
    public synchronized void startWatching(Context context) {
        if (isWatching || context == null) return;
        this.appContext = context.getApplicationContext();
        this.isWatching = true;

        registerContentObservers();
        registerFileObservers();

        Log.d(TAG, "MediaAutoScanner started watching for real-time media changes.");
    }

    public synchronized void stopWatching() {
        if (!isWatching || appContext == null) return;

        try {
            if (videoObserver != null) appContext.getContentResolver().unregisterContentObserver(videoObserver);
            if (audioObserver != null) appContext.getContentResolver().unregisterContentObserver(audioObserver);
            if (downloadsObserver != null) appContext.getContentResolver().unregisterContentObserver(downloadsObserver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering ContentObservers", e);
        }

        for (FileObserver fo : fileObservers) {
            try {
                fo.stopWatching();
            } catch (Exception ignored) {}
        }
        fileObservers.clear();
        isWatching = false;
        Log.d(TAG, "MediaAutoScanner stopped watching.");
    }

    public void addListener(OnMediaChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnMediaChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Trigger an immediate rescan on user request (e.g. from 3-dot overflow menu).
     */
    public void triggerImmediateScan(Context context) {
        if (context == null && appContext == null) return;
        Context ctx = context != null ? context.getApplicationContext() : appContext;

        MediaRepository.scanMedia(ctx, (videos, audios) -> {
            notifyListeners();
        });
    }

    private void registerContentObservers() {
        videoObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                Log.d(TAG, "MediaStore video change detected: " + uri);
                scheduleDebouncedScan();
            }
        };

        audioObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                Log.d(TAG, "MediaStore audio change detected: " + uri);
                scheduleDebouncedScan();
            }
        };

        try {
            appContext.getContentResolver().registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    videoObserver
            );
            appContext.getContentResolver().registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    audioObserver
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadsObserver = new ContentObserver(mainHandler) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        super.onChange(selfChange, uri);
                        Log.d(TAG, "MediaStore downloads change detected: " + uri);
                        scheduleDebouncedScan();
                    }
                };
                appContext.getContentResolver().registerContentObserver(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        true,
                        downloadsObserver
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed registering ContentObservers", e);
        }
    }

    private void registerFileObservers() {
        List<File> watchDirs = new ArrayList<>();

        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads != null && downloads.exists()) {
                watchDirs.add(downloads);
                // Also watch common sub-folders if they exist
                File tg = new File(downloads, "Telegram");
                if (tg.exists()) watchDirs.add(tg);
            }

            File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            if (movies != null && movies.exists()) watchDirs.add(movies);

            File music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (music != null && music.exists()) watchDirs.add(music);

            File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            if (dcim != null && dcim.exists()) {
                File camera = new File(dcim, "Camera");
                if (camera.exists()) watchDirs.add(camera);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error gathering watch directories", e);
        }

        int mask = FileObserver.CREATE | FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO;

        for (File dir : watchDirs) {
            try {
                FileObserver observer;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    observer = new FileObserver(dir, mask) {
                        @Override
                        public void onEvent(int event, String path) {
                            handleFileEvent(dir, path);
                        }
                    };
                } else {
                    final String dirPath = dir.getAbsolutePath();
                    observer = new FileObserver(dirPath, mask) {
                        @Override
                        public void onEvent(int event, String path) {
                            handleFileEvent(dir, path);
                        }
                    };
                }
                observer.startWatching();
                fileObservers.add(observer);
                Log.d(TAG, "Started FileObserver on: " + dir.getAbsolutePath());
            } catch (Exception e) {
                Log.w(TAG, "Could not start FileObserver on " + dir, e);
            }
        }
    }

    private void handleFileEvent(File parentDir, String fileName) {
        if (fileName == null) return;
        String ext = getFileExtension(fileName);
        if (SUPPORTED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            File fullFile = new File(parentDir, fileName);
            Log.d(TAG, "Instant download/creation detected: " + fullFile.getAbsolutePath());

            // Scan file into MediaStore so content resolver has it indexed
            if (appContext != null && fullFile.exists()) {
                MediaScannerConnection.scanFile(
                        appContext,
                        new String[]{fullFile.getAbsolutePath()},
                        null,
                        (path, uri) -> scheduleDebouncedScan()
                );
            } else {
                scheduleDebouncedScan();
            }
        }
    }

    private synchronized void scheduleDebouncedScan() {
        if (debounceRunnable != null) {
            mainHandler.removeCallbacks(debounceRunnable);
        }
        debounceRunnable = () -> {
            if (appContext == null) return;
            Log.d(TAG, "Running debounced media rescan...");
            MediaRepository.scanMedia(appContext, (videos, audios) -> {
                notifyListeners();
            });
        };
        mainHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            Log.d(TAG, "Notifying " + listeners.size() + " listeners of media updates.");
            for (OnMediaChangeListener listener : listeners) {
                try {
                    listener.onMediaChanged();
                } catch (Exception e) {
                    Log.e(TAG, "Error in OnMediaChangeListener", e);
                }
            }
        });
    }

    private String getFileExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot + 1);
        }
        return "";
    }
}
