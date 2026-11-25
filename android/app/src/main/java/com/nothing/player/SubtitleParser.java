package com.nothing.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SubtitleParser {

    public static class SubtitleCue {
        public long startTimeMs;
        public long endTimeMs;
        public String text;

        public SubtitleCue(long startTimeMs, long endTimeMs, String text) {
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.text = text;
        }
    }

    public static List<SubtitleCue> parseSrt(File file) {
        return parseSrt(file, "UTF-8");
    }

    public static List<SubtitleCue> parseSrt(File file, String charset) {
        if (file == null || !file.exists()) return new ArrayList<>();
        try (InputStream is = new FileInputStream(file)) {
            return parseSrt(is, charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<SubtitleCue> parseSrt(InputStream is) {
        return parseSrt(is, "UTF-8");
    }

    public static List<SubtitleCue> parseSrt(InputStream is, String charset) {
        List<SubtitleCue> cues = new ArrayList<>();
        if (is == null) return cues;
        String enc = (charset != null && !charset.trim().isEmpty()) ? charset.trim() : "UTF-8";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, enc));
            return parseSrt(reader);
        } catch (Exception e) {
            try {
                BufferedReader fallbackReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                return parseSrt(fallbackReader);
            } catch (Exception ignored) {}
        }
        return cues;
    }

    public static List<SubtitleCue> parseSrt(BufferedReader reader) {
        List<SubtitleCue> cues = new ArrayList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.matches("\\d+")) {
                    continue;
                }
                if (line.contains("-->")) {
                    String[] parts = line.split("-->");
                    if (parts.length == 2) {
                        long start = parseTimestamp(parts[0].trim());
                        long end = parseTimestamp(parts[1].trim());

                        StringBuilder textBuilder = new StringBuilder();
                        String textLine;
                        while ((textLine = reader.readLine()) != null) {
                            textLine = textLine.trim();
                            if (textLine.isEmpty()) break;
                            if (textBuilder.length() > 0) textBuilder.append("\n");
                            textBuilder.append(textLine.replaceAll("<[^>]*>", ""));
                        }
                        if (end > start && textBuilder.length() > 0) {
                            cues.add(new SubtitleCue(start, end, textBuilder.toString()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cues;
    }

    private static long parseTimestamp(String timeStr) {
        try {
            timeStr = timeStr.replace(',', '.');
            String[] parts = timeStr.split(":");
            if (parts.length == 3) {
                long h = Long.parseLong(parts[0]);
                long m = Long.parseLong(parts[1]);
                String[] secMillis = parts[2].split("\\.");
                long s = Long.parseLong(secMillis[0]);
                long ms = secMillis.length > 1 ? Long.parseLong(secMillis[1]) : 0;
                return (h * 3600 + m * 60 + s) * 1000 + ms;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static String getCueTextAt(List<SubtitleCue> cues, long timeMs) {
        if (cues == null || cues.isEmpty()) return null;
        int low = 0;
        int high = cues.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            SubtitleCue cue = cues.get(mid);
            if (timeMs < cue.startTimeMs) {
                high = mid - 1;
            } else if (timeMs > cue.endTimeMs) {
                low = mid + 1;
            } else {
                return cue.text;
            }
        }
        return null;
    }

    public static String cleanSdh(String text) {
        if (text == null) return "";
        return text
            .replaceAll("\\[.*?\\]", "")
            .replaceAll("\\(.*?\\)", "")
            .replaceAll("\\{.*?\\}", "")
            .replaceAll("[♪♫#♩♬]+", "")
            .replaceAll("(?m)^[A-Z0-9 _-]+:\\s*", "")
            .replaceAll("(?m)^\\s*$\\n", "")
            .trim();
    }

    public static String formatTimestamp(long timeMs) {
        long h = timeMs / 3600000;
        long m = (timeMs % 3600000) / 60000;
        long s = (timeMs % 60000) / 1000;
        long ms = timeMs % 1000;
        return String.format(java.util.Locale.US, "%02d:%02d:%02d,%03d", h, m, s, ms);
    }

    public static String exportToSrt(List<SubtitleCue> cues) {
        if (cues == null || cues.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cues.size(); i++) {
            SubtitleCue cue = cues.get(i);
            sb.append(i + 1).append("\r\n");
            sb.append(formatTimestamp(cue.startTimeMs))
              .append(" --> ")
              .append(formatTimestamp(cue.endTimeMs))
              .append("\r\n");
            sb.append(cue.text != null ? cue.text : "").append("\r\n\r\n");
        }
        return sb.toString();
    }

    public static boolean writeSrtFile(File file, List<SubtitleCue> cues) {
        if (file == null || cues == null) return false;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                 java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {
                osw.write(exportToSrt(cues));
                osw.flush();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

