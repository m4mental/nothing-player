package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class VaultManager {
    private static final String TAG = "VaultManager";
    private static final String PREFS_NAME = "nothing_vault_prefs";
    public static final String LOCK_TYPE_PIN = "PIN";
    public static final String LOCK_TYPE_PATTERN = "PATTERN";

    private static final String KEY_LOCK_TYPE = "vault_lock_type";
    private static final String KEY_PIN_HASH = "vault_pin_hash";
    private static final String KEY_PATTERN_HASH = "vault_pattern_hash";
    private static final String KEY_DECOY_PIN_HASH = "vault_decoy_pin_hash";
    private static final String KEY_DECOY_PATTERN_HASH = "vault_decoy_pattern_hash";
    private static final String KEY_BIOMETRIC_ENABLED = "vault_biometric_enabled";
    private static final String KEY_AUTO_BIOMETRIC = "vault_auto_biometric";
    private static final String KEY_AUTO_LOCK_SEC = "vault_auto_lock_sec";
    private static final String KEY_LAST_BACKGROUND_MS = "vault_last_background_ms";
    private static final String VAULT_DIR = ".vault";
    private static final String META_FILE = "vault_metadata.json";

    public static class VaultItem {
        public String id;
        public String originalPath;
        public String vaultPath;
        public String title;
        public long addedAt;
        public long size;
        public long duration;
    }

    public static boolean isSecurityConfigured(Context context) {
        if (context == null) return false;
        return isPinSet(context) || isPatternSet(context) || isDecoyPinSet(context);
    }

    public static boolean canUnlockWithPin(Context context) {
        if (context == null) return false;
        return isPinSet(context) || isDecoyPinSet(context);
    }

    public static boolean canUnlockWithPattern(Context context) {
        if (context == null) return false;
        return isPatternSet(context) || isDecoyPatternSet(context);
    }

    public static String getLockType(Context context) {
        if (context == null) return LOCK_TYPE_PIN;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LOCK_TYPE, LOCK_TYPE_PIN);
    }

    public static void setLockType(Context context, String lockType) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOCK_TYPE, lockType).apply();
    }

    public static boolean isPinSet(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_PIN_HASH);
    }

    public static boolean verifyPin(Context context, String inputPin) {
        if (context == null || inputPin == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedHash = prefs.getString(KEY_PIN_HASH, "");
        return hashString(inputPin).equals(savedHash);
    }

    public static void setPin(Context context, String newPin) {
        if (context == null || newPin == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PIN_HASH, hashString(newPin)).apply();
    }

    public static boolean isPatternSet(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_PATTERN_HASH);
    }

    public static boolean verifyPattern(Context context, String pattern) {
        if (context == null || pattern == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedHash = prefs.getString(KEY_PATTERN_HASH, "");
        return hashString(pattern).equals(savedHash);
    }

    public static void setPattern(Context context, String pattern) {
        if (context == null || pattern == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PATTERN_HASH, hashString(pattern)).apply();
    }

    public static boolean isDecoyPinSet(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_DECOY_PIN_HASH);
    }

    public static boolean verifyDecoyPin(Context context, String inputPin) {
        if (context == null || inputPin == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedHash = prefs.getString(KEY_DECOY_PIN_HASH, "");
        if (savedHash.isEmpty()) return false;
        return hashString(inputPin).equals(savedHash);
    }

    public static void setDecoyPin(Context context, String newPin) {
        if (context == null || newPin == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DECOY_PIN_HASH, hashString(newPin)).apply();
    }

    public static void removeDecoyPin(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_DECOY_PIN_HASH).remove(KEY_DECOY_PATTERN_HASH).apply();
    }

    public static boolean isDecoyPatternSet(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_DECOY_PATTERN_HASH);
    }

    public static boolean verifyDecoyPattern(Context context, String pattern) {
        if (context == null || pattern == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedHash = prefs.getString(KEY_DECOY_PATTERN_HASH, "");
        if (savedHash.isEmpty()) return false;
        return hashString(pattern).equals(savedHash);
    }

    public static void setDecoyPattern(Context context, String pattern) {
        if (context == null || pattern == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DECOY_PATTERN_HASH, hashString(pattern)).apply();
    }

    public static boolean isBiometricEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public static int getAutoLockSeconds(Context context) {
        if (context == null) return 30;
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_AUTO_LOCK_SEC, 30);
    }

    public static void setAutoLockSeconds(Context context, int seconds) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_AUTO_LOCK_SEC, seconds).apply();
    }

    public static void recordBackgroundTimestamp(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_BACKGROUND_MS, System.currentTimeMillis()).apply();
    }

    public static void clearBackgroundTimestamp(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_LAST_BACKGROUND_MS).apply();
    }

    public static boolean isSessionExpired(Context context) {
        if (context == null || !isSecurityConfigured(context)) return false;
        int timeoutSec = getAutoLockSeconds(context);
        if (timeoutSec < 0) return false; // OFF
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long bgMs = prefs.getLong(KEY_LAST_BACKGROUND_MS, 0L);
        if (bgMs <= 0L) return false;
        return (System.currentTimeMillis() - bgMs) >= (timeoutSec * 1000L);
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static boolean isAutoBiometricPrompt(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_BIOMETRIC, true);
    }

    public static void setAutoBiometricPrompt(Context context, boolean auto) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_BIOMETRIC, auto).apply();
    }

    public static String hashString(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(str.hashCode());
        }
    }

    private static String hashPin(String pin) {
        return hashString(pin);
    }

    public static File getVaultDir(Context context) {
        File dir = new File(context.getFilesDir(), VAULT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File nomedia = new File(dir, ".nomedia");
        if (!nomedia.exists()) {
            try { nomedia.createNewFile(); } catch (Exception ignored) {}
        }
        return dir;
    }

    public static synchronized boolean moveToVault(Context context, MediaItem item) {
        if (context == null || item == null || item.path == null) return false;
        File sourceFile = new File(item.path);
        if (!sourceFile.exists()) return false;

        try {
            File vaultDir = getVaultDir(context);
            String ext = "";
            int dotIdx = sourceFile.getName().lastIndexOf('.');
            if (dotIdx != -1) {
                ext = sourceFile.getName().substring(dotIdx);
            }
            String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
            String vaultFileName = "vault_" + System.currentTimeMillis() + "_" + uniqueSuffix + ext;
            File destFile = new File(vaultDir, vaultFileName);

            boolean moved = sourceFile.renameTo(destFile);
            if (!moved) {
                moved = FileDeleteHelper.copyAndDeleteFile(context, sourceFile, destFile, item.contentUri);
            }

            if (moved) {
                MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{sourceFile.getAbsolutePath()}, null, null);
                Set<String> removed = new HashSet<>();
                removed.add(sourceFile.getAbsolutePath());
                if (item.contentUri != null && !item.contentUri.isEmpty()) removed.add(item.contentUri);
                MediaRepository.removeItemsFromCache(context.getApplicationContext(), removed);

                VaultItem vaultItem = new VaultItem();
                vaultItem.id = System.currentTimeMillis() + "_" + uniqueSuffix;
                vaultItem.originalPath = sourceFile.getAbsolutePath();
                vaultItem.vaultPath = destFile.getAbsolutePath();
                vaultItem.title = item.title != null ? item.title : sourceFile.getName();
                vaultItem.addedAt = System.currentTimeMillis();
                vaultItem.size = item.size > 0 ? item.size : destFile.length();
                vaultItem.duration = item.duration;

                saveItemToMeta(context, vaultItem);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error moving to vault: " + item.path, e);
        }
        return false;
    }

    public static synchronized boolean restoreItem(Context context, VaultItem item) {
        if (context == null || item == null || item.vaultPath == null) return false;
        File vaultFile = new File(item.vaultPath);
        if (!vaultFile.exists()) {
            removeItemFromMeta(context, item.id);
            return false;
        }

        try {
            String restorePath = item.originalPath;
            if (restorePath == null || restorePath.trim().isEmpty()) {
                File moviesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "NothingPlayer");
                String fallbackName = item.title != null && !item.title.trim().isEmpty() ? item.title.trim() : vaultFile.getName();
                restorePath = new File(moviesDir, fallbackName).getAbsolutePath();
            }
            File targetFile = new File(restorePath);
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            boolean restored = vaultFile.renameTo(targetFile);
            if (!restored) {
                restored = FileDeleteHelper.copyAndDeleteFile(context, vaultFile, targetFile, null);
            }
            if (!restored && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                restored = FileDeleteHelper.restoreVideoViaMediaStore(context, vaultFile, targetFile.getName());
            }

            if (restored) {
                MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{targetFile.getAbsolutePath()}, null, null);
                removeItemFromMeta(context, item.id);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring vault item", e);
        }
        return false;
    }

    public static synchronized boolean deletePermanently(Context context, VaultItem item) {
        if (item == null) return false;
        try {
            if (item.vaultPath != null) {
                File f = new File(item.vaultPath);
                if (f.exists()) f.delete();
            }
            removeItemFromMeta(context, item.id);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error permanently deleting from vault", e);
        }
        return false;
    }

    public static synchronized List<VaultItem> getVaultItems(Context context) {
        List<VaultItem> items = new ArrayList<>();
        if (context == null) return items;
        File metaFile = new File(context.getFilesDir(), META_FILE);
        if (!metaFile.exists()) return items;

        try (FileInputStream fis = new FileInputStream(metaFile)) {
            byte[] data = new byte[(int) metaFile.length()];
            fis.read(data);
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                VaultItem item = new VaultItem();
                item.id = obj.optString("id");
                item.originalPath = obj.optString("originalPath");
                item.vaultPath = obj.optString("vaultPath");
                item.title = obj.optString("title");
                item.addedAt = obj.optLong("addedAt");
                item.size = obj.optLong("size");
                item.duration = obj.optLong("duration");

                if (item.vaultPath != null && !item.vaultPath.isEmpty()) {
                    File f = new File(item.vaultPath);
                    if (f.exists()) {
                        items.add(item);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading vault metadata", e);
        }
        return items;
    }

    private static void saveItemToMeta(Context context, VaultItem item) {
        List<VaultItem> all = getVaultItems(context);
        all.add(0, item);
        saveAllToMeta(context, all);
    }

    private static void removeItemFromMeta(Context context, String id) {
        List<VaultItem> all = getVaultItems(context);
        Iterator<VaultItem> it = all.iterator();
        while (it.hasNext()) {
            if (it.next().id.equals(id)) {
                it.remove();
                break;
            }
        }
        saveAllToMeta(context, all);
    }

    private static void saveAllToMeta(Context context, List<VaultItem> items) {
        try {
            JSONArray array = new JSONArray();
            for (VaultItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("originalPath", item.originalPath);
                obj.put("vaultPath", item.vaultPath);
                obj.put("title", item.title);
                obj.put("addedAt", item.addedAt);
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
            Log.e(TAG, "Error writing vault metadata", e);
        }
    }
}

