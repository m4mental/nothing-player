package com.nothing.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight header scanner extracting ReplayGain / EBU R128 track gain (in dB)
 * from ID3v2 (MP3), Vorbis comments (FLAC/OGG/OPUS), or M4A freeform atoms
 * without pulling heavy native tagging libraries.
 */
public final class ReplayGainParser {
    private static final int HEADER_READ_LIMIT = 96 * 1024;
    private static final Pattern RG_PATTERN = Pattern.compile(
            "(?i)REPLAYGAIN_TRACK_GAIN[^\\d+-]{0,8}([+-]?\\d+(?:\\.\\d+)?)\\s*(?:dB)?"
    );
    private static final Pattern R128_PATTERN = Pattern.compile(
            "(?i)R128_TRACK_GAIN[^\\d+-]{0,8}([+-]?\\d+)"
    );

    private ReplayGainParser() {}

    public static Float parseGainDbFromFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) return null;
        File f = new File(filePath);
        if (!f.exists() || !f.isFile()) return null;
        try (InputStream is = new FileInputStream(f)) {
            return parseGainDbFromStream(is);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Float parseGainDbFromStream(InputStream is) {
        if (is == null) return null;
        try {
            byte[] buf = new byte[HEADER_READ_LIMIT];
            int total = 0;
            int n;
            while (total < buf.length && (n = is.read(buf, total, buf.length - total)) > 0) {
                total += n;
            }
            if (total <= 0) return null;
            return parseGainDbFromBytes(buf, total);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Float parseGainDbFromBytes(byte[] data, int length) {
        if (data == null || length <= 0) return null;
        int len = Math.min(data.length, length);
        // Strip NUL bytes cleanly so UTF-16LE / mixed ID3v2 frames match ASCII regex patterns
        StringBuilder cleaned = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int b = data[i] & 0xFF;
            if (b >= 32 && b <= 126) {
                cleaned.append((char) b);
            } else if (b == 0) {
                // Skip UTF-16 padding zeroes inside ASCII tags
            } else {
                cleaned.append(' ');
            }
        }
        String ascii = cleaned.toString();

        Matcher rg = RG_PATTERN.matcher(ascii);
        if (rg.find()) {
            try {
                float db = Float.parseFloat(rg.group(1));
                if (db >= -30f && db <= 24f) return db;
            } catch (NumberFormatException ignored) {}
        }

        Matcher r128 = R128_PATTERN.matcher(ascii);
        if (r128.find()) {
            try {
                // EBU R128 gain is stored as Q7.8 fixed-point dB + 5 dB LUFS offset vs ReplayGain -18 LUFS
                float q78 = Float.parseFloat(r128.group(1)) / 256.0f;
                float db = q78 + 5.0f;
                if (db >= -30f && db <= 24f) return db;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Converts ReplayGain dB into a [0.05f .. 1.0f] linear attenuation scalar for MediaPlayer.setVolume
     * plus optional positive preamp millibels when LoudnessEnhancer is available.
     */
    public static float dbToLinearAttenuation(float gainDb) {
        if (gainDb >= 0f) return 1.0f;
        float linear = (float) Math.pow(10.0, gainDb / 20.0);
        return Math.max(0.08f, Math.min(1.0f, linear));
    }

    public static String formatGainBadge(Float gainDb) {
        if (gainDb == null) return "RG: UNITY";
        return String.format(Locale.US, "RG: %+.1F DB", gainDb);
    }
}
