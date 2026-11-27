package com.nothing.player;

import android.content.Context;
import android.net.Uri;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import java.io.File;

/**
 * Provides an LRU on-disk HTTP/HTTPS stream pre-buffer cache backed by Media3 SimpleCache
 * so seeking backward or replaying streamed episodes requires zero re-downloading.
 */
@androidx.media3.common.util.UnstableApi
public final class MediaCacheHelper {
    private static final long MAX_CACHE_BYTES = 200L * 1024L * 1024L; // 200 MB LRU
    private static final String CACHE_DIR_NAME = "media_stream_cache";
    private static volatile SimpleCache simpleCache;
    private static volatile StandaloneDatabaseProvider dbProvider;

    private MediaCacheHelper() {}

    public static synchronized SimpleCache getCache(Context context) {
        if (context == null) return null;
        if (simpleCache == null) {
            try {
                Context appCtx = context.getApplicationContext();
                File cacheDir = new File(appCtx.getCacheDir(), CACHE_DIR_NAME);
                if (!cacheDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    cacheDir.mkdirs();
                }
                if (!SimpleCache.isCacheFolderLocked(cacheDir)) {
                    if (dbProvider == null) {
                        dbProvider = new StandaloneDatabaseProvider(appCtx);
                    }
                    simpleCache = new SimpleCache(
                            cacheDir,
                            new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                            dbProvider
                    );
                }
            } catch (Throwable ignored) {
                simpleCache = null;
            }
        }
        return simpleCache;
    }

    public static DataSource.Factory createDataSourceFactory(Context context, Uri targetUri) {
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("NothingPlayer/1.0 (Android)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(12000)
                .setReadTimeoutMs(15000);

        String scheme = targetUri != null && targetUri.getScheme() != null
                ? targetUri.getScheme().toLowerCase(java.util.Locale.US)
                : "";

        if ("http".equals(scheme) || "https".equals(scheme)) {
            String uriStr = targetUri.toString().toLowerCase(java.util.Locale.US);
            // Cache VOD MP4/MKV/WEBM/HLS segments; bypass disk cache only for continuous live streams
            boolean isContinuousLive = uriStr.contains("live") && uriStr.endsWith(".m3u8");
            if (!isContinuousLive) {
                SimpleCache cache = getCache(context);
                if (cache != null) {
                    return new CacheDataSource.Factory()
                            .setCache(cache)
                            .setUpstreamDataSourceFactory(httpFactory)
                            .setCacheWriteDataSinkFactory(null) // use CacheDataSource default sink for cache writes
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
                }
            }
            return httpFactory;
        }
        return new DefaultDataSource.Factory(context, httpFactory);
    }

    public static long getCachedBytes(Context context) {
        SimpleCache cache = getCache(context);
        if (cache != null) {
            try {
                return cache.getCacheSpace();
            } catch (Throwable ignored) {}
        }
        return 0L;
    }

    public static synchronized void clearCache(Context context) {
        if (context == null) return;
        try {
            if (simpleCache != null) {
                simpleCache.release();
                simpleCache = null;
            }
            File cacheDir = new File(context.getApplicationContext().getCacheDir(), CACHE_DIR_NAME);
            if (dbProvider == null) {
                dbProvider = new StandaloneDatabaseProvider(context.getApplicationContext());
            }
            SimpleCache.delete(cacheDir, dbProvider);
        } catch (Throwable ignored) {}
    }
}
