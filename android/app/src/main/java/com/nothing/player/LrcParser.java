package com.nothing.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcParser {

    public static class LrcLine {
        public long timeMs;
        public String text;

        public LrcLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    private static final Pattern TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})[\\.:](\\d{2,3})\\]");

    public static List<LrcLine> findAndParseLrc(String audioPath) {
        if (audioPath == null || audioPath.trim().isEmpty()) return new ArrayList<>();
        try {
            File audioFile = new File(audioPath);
            if (!audioFile.exists()) return new ArrayList<>();

            // 1. Check exact name with .lrc extension
            int dotIdx = audioPath.lastIndexOf('.');
            if (dotIdx > 0) {
                String basePath = audioPath.substring(0, dotIdx);
                File lrcFile = new File(basePath + ".lrc");
                if (lrcFile.exists()) {
                    return parseLrcFile(lrcFile);
                }
            }

            // 2. Check same folder for case-insensitive match
            File parent = audioFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                String rawName = audioFile.getName();
                int nameDot = rawName.lastIndexOf('.');
                String audioNameOnly = (nameDot > 0 ? rawName.substring(0, nameDot) : rawName).toLowerCase();
                File[] siblings = parent.listFiles((dir, name) -> name.toLowerCase().endsWith(".lrc"));
                if (siblings != null) {
                    for (File sib : siblings) {
                        String sibRaw = sib.getName();
                        int sibDot = sibRaw.lastIndexOf('.');
                        if (sibDot <= 0) continue;
                        String sibName = sibRaw.substring(0, sibDot).toLowerCase();
                        if (sibName.equals(audioNameOnly)) {
                            return parseLrcFile(sib);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }

    public static List<LrcLine> parseLrcFile(File file) {
        List<LrcLine> lines = new ArrayList<>();
        if (file == null || !file.exists()) return lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String lineStr;
            while ((lineStr = reader.readLine()) != null) {
                lineStr = lineStr.trim();
                if (lineStr.isEmpty()) continue;

                Matcher matcher = TIME_TAG_PATTERN.matcher(lineStr);
                List<Long> timestamps = new ArrayList<>();
                int lastMatchEnd = 0;
                while (matcher.find()) {
                    long min = Long.parseLong(matcher.group(1));
                    long sec = Long.parseLong(matcher.group(2));
                    String msStr = matcher.group(3);
                    long ms = Long.parseLong(msStr);
                    if (msStr.length() == 2) ms *= 10;
                    long totalMs = min * 60000 + sec * 1000 + ms;
                    timestamps.add(totalMs);
                    lastMatchEnd = matcher.end();
                }

                if (!timestamps.isEmpty()) {
                    String lyric = lineStr.substring(lastMatchEnd).trim();
                    for (Long t : timestamps) {
                        lines.add(new LrcLine(t, lyric));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(lines, Comparator.comparingLong(o -> o.timeMs));
        return lines;
    }

    public static int getActiveLineIndex(List<LrcLine> lines, long currentMs) {
        if (lines == null || lines.isEmpty()) return -1;
        int active = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).timeMs <= currentMs) {
                active = i;
            } else {
                break;
            }
        }
        return active;
    }
}

