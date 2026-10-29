package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VaultManager {
    private static final String TAG = "VaultManager";
    private static final String PREFS_NAME = "nothing_vault_prefs";
    public static final String LOCK_TYPE_PIN = "PIN";
    public static final String LOCK_TYPE_PATTERN = "PATTERN";

    private static final String KEY_LOCK_TYPE = "vault_lock_type";
    private static final String KEY_PIN_HASH = "vault_pin_hash";
    private static final String KEY_PATTERN_HASH = "vault_pattern_hash";
    private static final String KEY_BIOMETRIC_ENABLED = "vault_biometric_enabled";
    private static final String KEY_AUTO_BIOMETRIC = "vault_auto_biometric";
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
        String lockType = getLockType(context);
        if (LOCK_TYPE_PATTERN.equals(lockType)) {
            return isPatternSet(context);
        } else {
            return isPinSet(context);
        }
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

    public static boolean isBiometricEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
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
            String vaultFileName = "vault_" + System.currentTimeMillis() + ext;
            File destFile = new File(vaultDir, vaultFileName);

            boolean moved = sourceFile.renameTo(destFile);
            if (!moved) {
                moved = copyAndDelete(sourceFile, destFile);
            }

            if (moved) {
                MediaScannerConnection.scanFile(context, new String[]{sourceFile.getAbsolutePath()}, null, null);

                VaultItem vaultItem = new VaultItem();
                vaultItem.id = String.valueOf(System.currentTimeMillis());
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
            File targetFile = new File(item.originalPath);
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            boolean restored = vaultFile.renameTo(targetFile);
            if (!restored) {
                restored = copyAndDelete(vaultFile, targetFile);
            }

            if (restored) {
                MediaScannerConnection.scanFile(context, new String[]{targetFile.getAbsolutePath()}, null, null);
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

                File f = new File(item.vaultPath);
                if (f.exists()) {
                    items.add(item);
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

