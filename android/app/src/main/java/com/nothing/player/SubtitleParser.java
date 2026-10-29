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
        if (file == null || !file.exists()) return new ArrayList<>();
        try (InputStream is = new FileInputStream(file)) {
            return parseSrt(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<SubtitleCue> parseSrt(InputStream is) {
        List<SubtitleCue> cues = new ArrayList<>();
        if (is == null) return cues;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            return parseSrt(reader);
        } catch (Exception e) {
            e.printStackTrace();
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
        for (SubtitleCue cue : cues) {
            if (timeMs >= cue.startTimeMs && timeMs <= cue.endTimeMs) {
                return cue.text;
            }
        }
        return null;
    }
}

