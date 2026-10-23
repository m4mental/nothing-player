package com.nothing.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Online Metadata & Poster Fetching Engine for NOTHING-PLAYER.
 * Automatically queries open, free media databases (TVMaze for Web Series,
 * iTunes Movie Registry for Movies) to fetch official high-res posters,
 * release years, IMDb/ratings, and synopsis without requiring API keys.
 * Caches all responses locally for instant 0ms offline retrieval.
 */
public class OnlineMetadataManager {
    private static final String TAG = "OnlineMetadataManager";
    private static final String CACHE_FILE = "media_metadata_cache.json";

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final Map<String, MetadataInfo> MEMORY_CACHE = new HashMap<>();
    private static boolean cacheLoaded = false;

    public static class MetadataInfo {
        public String queryKey;
        public String title;
        public String posterUrl;
        public String releaseYear;
        public String rating;
        public String synopsis;

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("queryKey", queryKey);
                obj.put("title", title);
                obj.put("posterUrl", posterUrl);
                obj.put("releaseYear", releaseYear);
                obj.put("rating", rating);
                obj.put("synopsis", synopsis);
                return obj;
            } catch (Exception e) {
                return new JSONObject();
            }
        }

        public static MetadataInfo fromJson(JSONObject obj) {
            if (obj == null) return null;
            MetadataInfo info = new MetadataInfo();
            info.queryKey = obj.optString("queryKey");
            info.title = obj.optString("title");
            info.posterUrl = obj.optString("posterUrl");
            info.releaseYear = obj.optString("releaseYear");
            info.rating = obj.optString("rating");
            info.synopsis = obj.optString("synopsis");
            return info;
        }
    }

    public interface MetadataCallback {
        void onMetadataLoaded(MetadataInfo info);
    }

    public static synchronized void initCache(Context context) {
        if (cacheLoaded || context == null) return;
        cacheLoaded = true;
        EXECUTOR.execute(() -> loadCacheFromDisk(context.getApplicationContext()));
    }

    public static MetadataInfo getCached(Context context, String queryKey) {
        if (queryKey == null || queryKey.trim().isEmpty()) return null;
        String key = queryKey.trim().toLowerCase();
        synchronized (MEMORY_CACHE) {
            return MEMORY_CACHE.get(key);
        }
    }

    /**
     * Asynchronously enriches MediaItem with poster and details if available.
     */
    public static void enrichItem(Context context, MediaItem item, MetadataCallback callback) {
        if (item == null || context == null) return;
        initCache(context);

        String query = ("series".equals(item.mediaCategory) && item.seriesName != null) ?
                item.seriesName : item.cleanTitle;

        if (query == null || query.trim().isEmpty()) {
            query = item.title;
        }
        if (query == null || query.trim().isEmpty()) return;

        final String finalKey = query.trim().toLowerCase();
        MetadataInfo cached = getCached(context, finalKey);
        if (cached != null) {
            applyMetadataToItem(item, cached);
            if (callback != null) {
                MAIN_HANDLER.post(() -> callback.onMetadataLoaded(cached));
            }
            return;
        }

        EXECUTOR.execute(() -> {
            MetadataInfo fetched = null;
            try {
                if ("series".equals(item.mediaCategory)) {
                    fetched = fetchSeriesFromTvMaze(finalKey);
                } else if ("movie".equals(item.mediaCategory)) {
                    fetched = fetchMovieFromItunes(finalKey);
                    if (fetched == null) {
                        fetched = fetchSeriesFromTvMaze(finalKey); // Fallback to TVMaze in case it's a TV special/show
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed online metadata query for: " + finalKey, e);
            }

            if (fetched != null) {
                fetched.queryKey = finalKey;
                synchronized (MEMORY_CACHE) {
                    MEMORY_CACHE.put(finalKey, fetched);
                }
                saveCacheToDisk(context.getApplicationContext());

                final MetadataInfo finalFetched = fetched;
                applyMetadataToItem(item, finalFetched);

                if (callback != null) {
                    MAIN_HANDLER.post(() -> callback.onMetadataLoaded(finalFetched));
                }
            }
        });
    }

    /**
     * Asynchronously enriches Web Series with poster and details if available.
     */
    public static void enrichSeries(Context context, String seriesName, MetadataCallback callback) {
        if (seriesName == null || seriesName.trim().isEmpty() || context == null) return;
        initCache(context);

        final String finalKey = seriesName.trim().toLowerCase();
        MetadataInfo cached = getCached(context, finalKey);
        if (cached != null) {
            if (callback != null) {
                MAIN_HANDLER.post(() -> callback.onMetadataLoaded(cached));
            }
            return;
        }

        EXECUTOR.execute(() -> {
            MetadataInfo fetched = null;
            try {
                fetched = fetchSeriesFromTvMaze(finalKey);
            } catch (Exception e) {
                Log.w(TAG, "Failed online series query for: " + finalKey, e);
            }

            if (fetched != null) {
                fetched.queryKey = finalKey;
                synchronized (MEMORY_CACHE) {
                    MEMORY_CACHE.put(finalKey, fetched);
                }
                saveCacheToDisk(context.getApplicationContext());

                final MetadataInfo finalFetched = fetched;
                if (callback != null) {
                    MAIN_HANDLER.post(() -> callback.onMetadataLoaded(finalFetched));
                }
            }
        });
    }

    /**
     * Explicit user search to match a video with an online show or movie.
     */
    public static void searchAndApply(Context context, MediaItem item, String customQuery, String category, MetadataCallback callback) {
        if (item == null || customQuery == null || customQuery.trim().isEmpty() || context == null) return;
        initCache(context);

        final String finalKey = customQuery.trim().toLowerCase();
        EXECUTOR.execute(() -> {
            MetadataInfo fetched = null;
            try {
                if ("series".equalsIgnoreCase(category)) {
                    fetched = fetchSeriesFromTvMaze(finalKey);
                    if (fetched == null) fetched = fetchMovieFromItunes(finalKey);
                } else {
                    fetched = fetchMovieFromItunes(finalKey);
                    if (fetched == null) fetched = fetchSeriesFromTvMaze(finalKey);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed manual query for: " + finalKey, e);
            }

            if (fetched != null) {
                fetched.queryKey = finalKey;
                synchronized (MEMORY_CACHE) {
                    MEMORY_CACHE.put(finalKey, fetched);
                }
                saveCacheToDisk(context.getApplicationContext());

                final MetadataInfo finalFetched = fetched;
                item.mediaCategory = category != null ? category.toLowerCase() : "movie";
                if ("series".equalsIgnoreCase(item.mediaCategory)) {
                    item.seriesName = finalFetched.title != null ? finalFetched.title : customQuery;
                } else {
                    item.cleanTitle = finalFetched.title != null ? finalFetched.title : customQuery;
                }
                applyMetadataToItem(item, finalFetched);

                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onMetadataLoaded(finalFetched);
                });
            } else {
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onMetadataLoaded(null);
                });
            }
        });
    }

    private static void applyMetadataToItem(MediaItem item, MetadataInfo info) {
        if (item == null || info == null) return;
        if (info.posterUrl != null && !info.posterUrl.isEmpty()) {
            item.posterUrl = info.posterUrl;
        }
        if (info.releaseYear != null && !info.releaseYear.isEmpty()) {
            item.releaseYear = info.releaseYear;
        }
        if (info.rating != null && !info.rating.isEmpty()) {
            item.rating = info.rating;
        }
        if (info.synopsis != null && !info.synopsis.isEmpty()) {
            item.synopsis = info.synopsis;
        }
    }

    private static MetadataInfo fetchSeriesFromTvMaze(String seriesName) {
        try {
            String encoded = URLEncoder.encode(seriesName, "UTF-8");
            String urlStr = "https://api.tvmaze.com/singlesearch/shows?q=" + encoded;
            String jsonStr = httpGet(urlStr);
            if (jsonStr == null || jsonStr.isEmpty()) return null;

            JSONObject obj = new JSONObject(jsonStr);
            MetadataInfo info = new MetadataInfo();
            info.title = obj.optString("name");

            JSONObject imgObj = obj.optJSONObject("image");
            if (imgObj != null) {
                info.posterUrl = imgObj.optString("original", imgObj.optString("medium"));
            }

            String premiered = obj.optString("premiered");
            if (premiered != null && premiered.length() >= 4) {
                info.releaseYear = premiered.substring(0, 4);
            }

            JSONObject ratingObj = obj.optJSONObject("rating");
            if (ratingObj != null && !ratingObj.isNull("average")) {
                info.rating = String.valueOf(ratingObj.optDouble("average"));
            }

            String summary = obj.optString("summary");
            if (summary != null) {
                // Strip HTML tags from TVMaze synopsis
                info.synopsis = summary.replaceAll("<[^>]*>", "").trim();
            }

            return info;
        } catch (Exception e) {
            return null;
        }
    }

    private static MetadataInfo fetchMovieFromItunes(String movieTitle) {
        try {
            String encoded = URLEncoder.encode(movieTitle, "UTF-8");
            String urlStr = "https://itunes.apple.com/search?term=" + encoded + "&media=movie&limit=1";
            String jsonStr = httpGet(urlStr);
            if (jsonStr == null || jsonStr.isEmpty()) return null;

            JSONObject root = new JSONObject(jsonStr);
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) return null;

            JSONObject first = results.getJSONObject(0);
            MetadataInfo info = new MetadataInfo();
            info.title = first.optString("trackName");

            String artwork = first.optString("artworkUrl100");
            if (artwork != null && !artwork.isEmpty()) {
                // Scale iTunes artwork from 100x100 to 600x600 for sharp OLED presentation
                info.posterUrl = artwork.replace("100x100bb.jpg", "600x600bb.jpg");
            }

            String releaseDate = first.optString("releaseDate");
            if (releaseDate != null && releaseDate.length() >= 4) {
                info.releaseYear = releaseDate.substring(0, 4);
            }

            info.synopsis = first.optString("longDescription", first.optString("shortDescription"));
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Nothing-Player/2.0 (Android; Phone 2a)");

            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static void loadCacheFromDisk(Context context) {
        try {
            File f = new File(context.getFilesDir(), CACHE_FILE);
            if (!f.exists()) return;

            try (FileInputStream fis = new FileInputStream(f);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject root = new JSONObject(sb.toString());
                JSONArray arr = root.optJSONArray("items");
                if (arr != null) {
                    synchronized (MEMORY_CACHE) {
                        for (int i = 0; i < arr.length(); i++) {
                            MetadataInfo info = MetadataInfo.fromJson(arr.optJSONObject(i));
                            if (info != null && info.queryKey != null) {
                                MEMORY_CACHE.put(info.queryKey, info);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed reading metadata cache", e);
        }
    }

    private static void saveCacheToDisk(Context context) {
        try {
            File f = new File(context.getFilesDir(), CACHE_FILE);
            JSONArray arr = new JSONArray();
            synchronized (MEMORY_CACHE) {
                for (MetadataInfo info : MEMORY_CACHE.values()) {
                    arr.put(info.toJson());
                }
            }
            JSONObject root = new JSONObject();
            root.put("items", arr);

            try (FileOutputStream fos = new FileOutputStream(f);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                osw.write(root.toString());
                osw.flush();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed saving metadata cache", e);
        }
    }
}
