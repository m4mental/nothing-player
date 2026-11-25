package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkBrowserHelper {

    private static final String PREFS_NAME = "nothing_network_shares";
    private static final String KEY_SHARES = "saved_shares_json";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static class NetworkShareItem {
        public String title;
        public String protocol; // SMB, WEBDAV, FTP, HTTP
        public String host;
        public int port;
        public String username;
        public String password;
        public String sharePath;

        public NetworkShareItem(String title, String protocol, String host, int port, String username, String password, String sharePath) {
            this.title = title;
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.sharePath = sharePath;
        }

        public String toStreamUrl() {
            String proto = protocol != null ? protocol.toLowerCase(Locale.US) : "smb";
            StringBuilder sb = new StringBuilder();
            sb.append(proto).append("://");
            if (username != null && !username.trim().isEmpty()) {
                sb.append(encodeCredential(username.trim()));
                if (password != null && !password.trim().isEmpty()) {
                    sb.append(":").append(encodeCredential(password.trim()));
                }
                sb.append("@");
            }
            sb.append(host != null ? host.trim() : "");
            if (port > 0) {
                sb.append(":").append(port);
            }
            if (sharePath != null && !sharePath.trim().isEmpty()) {
                String p = sharePath.trim();
                if (!p.startsWith("/")) sb.append("/");
                sb.append(p);
            }
            return sb.toString();
        }

        private static String encodeCredential(String raw) {
            try {
                return URLEncoder.encode(raw, "UTF-8").replace("+", "%20");
            } catch (Exception e) {
                return raw;
            }
        }
    }

    public interface ScanCallback {
        void onDeviceFound(String ip, String detectedService);
        void onScanComplete(List<String> activeHosts);
    }

    public static void scanLocalSubnet(String localIp, ScanCallback callback) {
        if (localIp == null || !localIp.contains(".")) {
            if (callback != null) MAIN_HANDLER.post(() -> callback.onScanComplete(new ArrayList<>()));
            return;
        }
        String prefix = localIp.substring(0, localIp.lastIndexOf(".") + 1);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<String> foundHosts = new ArrayList<>();

        for (int i = 1; i <= 254; i++) {
            final String host = prefix + i;
            if (host.equals(localIp)) continue;

            executor.execute(() -> {
                // Check SMB (445), WebDAV (8080/5005), or FTP (21)
                int[] ports = {445, 8080, 21};
                String[] names = {"SMB (WINDOWS / NAS)", "WEBDAV / HTTP", "FTP SERVER"};
                for (int pIdx = 0; pIdx < ports.length; pIdx++) {
                    int p = ports[pIdx];
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(host, p), 200);
                        synchronized (foundHosts) {
                            foundHosts.add(host);
                        }
                        if (callback != null) {
                            String serviceName = names[pIdx];
                            MAIN_HANDLER.post(() -> callback.onDeviceFound(host, serviceName));
                        }
                        break;
                    } catch (Exception ignored) {}
                }
            });
        }

        ExecutorService waiter = Executors.newSingleThreadExecutor();
        waiter.execute(() -> {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(6, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (Exception ignored) {
            } finally {
                waiter.shutdown();
            }
            if (callback != null) {
                List<String> snapshot;
                synchronized (foundHosts) {
                    snapshot = new ArrayList<>(foundHosts);
                }
                MAIN_HANDLER.post(() -> callback.onScanComplete(snapshot));
            }
        });
    }

    public static List<NetworkShareItem> getSavedShares(Context context) {
        List<NetworkShareItem> list = new ArrayList<>();
        if (context == null) return list;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_SHARES, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new NetworkShareItem(
                    obj.optString("title", "PC Share"),
                    obj.optString("protocol", "smb"),
                    obj.optString("host", ""),
                    obj.optInt("port", 0),
                    obj.optString("username", ""),
                    obj.optString("password", ""),
                    obj.optString("sharePath", "")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void saveShare(Context context, NetworkShareItem item) {
        if (context == null || item == null) return;
        List<NetworkShareItem> list = getSavedShares(context);
        list.add(0, item);
        JSONArray arr = new JSONArray();
        for (NetworkShareItem s : list) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("title", s.title);
                obj.put("protocol", s.protocol);
                obj.put("host", s.host);
                obj.put("port", s.port);
                obj.put("username", s.username);
                obj.put("password", s.password);
                obj.put("sharePath", s.sharePath);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHARES, arr.toString())
            .apply();
    }
}

