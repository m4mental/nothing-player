package com.nothing.player;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Media Organizer & Classification Engine for NOTHING-PLAYER.
 * Automatically parses local file names, directories, and durations to classify
 * videos into Web Series (with Season & Episode grouping), Movies, and Clips.
 */
public class MediaOrganizerEngine {

    // Regex patterns for episodic series detection
    private static final Pattern PATTERN_S_E = Pattern.compile("(?i)^(.+?)[ ._\\-]+[sS](\\d{1,2})[ ._eE\\-]*[eE](\\d{1,3})");
    private static final Pattern PATTERN_X = Pattern.compile("(?i)^(.+?)[ ._\\-]+(\\d{1,2})x(\\d{1,3})");
    private static final Pattern PATTERN_SEASON_EPISODE = Pattern.compile("(?i)^(.+?)[ ._\\-]+season[ ._\\-]*(\\d{1,2})[ ._\\-]+(?:episode|ep)[ ._\\-]*(\\d{1,3})");
    private static final Pattern PATTERN_EP_ONLY = Pattern.compile("(?i)^(.+?)[ ._\\-]+(?:episode|ep)[ ._\\-]*(\\d{1,3})");
    private static final Pattern PATTERN_E_STANDALONE = Pattern.compile("(?i)^(.+?)[ ._\\-]+[eE](\\d{1,3})(?:[ ._\\-]|$)");
    private static final Pattern PATTERN_PART = Pattern.compile("(?i)^(.+?)[ ._\\-]+(?:part|pt)[ ._\\-]*(\\d{1,3})");
    private static final Pattern PATTERN_YEAR = Pattern.compile("(?i)[ (._\\[](19\\d\\d|20\\d\\d)[ )._\\]]");

    // Release groups, audio/video codecs, and scene tag cleaners
    private static final Pattern CLEANER_TAGS = Pattern.compile(
        "(?i)\\b(1080p|720p|480p|2160p|4k|uhd|fhd|hd|hevc|x264|x265|h264|h265|10bit|bluray|blu-ray|" +
        "web-dl|webrip|web|hdtv|dvdrip|brrip|bdrip|aac|ddp5\\.1|ddp|ac3|dts|dolby|atmos|sub|esub|esubs|" +
        "dual[ ._-]audio|multi[ ._-]audio|hindi|english|tamil|telugu|kannada|malayalam|bengali|clean|" +
        "uncut|remux|proper|repack|yify|galaxytv|psa|pahe|yts|rarbg|eztv|tg|telegram)\\b"
    );

    private static final Pattern CLEANER_BRACKETS = Pattern.compile("[\\[\\(].*?[\\]\\)]");
    private static final Pattern CLEANER_SYMBOLS = Pattern.compile("[._\\-]+");

    public static class ClassificationResult {
        public String category; // "series", "movie", "clip"
        public String seriesName;
        public int season = 1;
        public int episode = -1;
        public String cleanTitle;
        public String releaseYear;
    }

    public static ClassificationResult classify(MediaItem item) {
        ClassificationResult res = new ClassificationResult();
        if (item == null) {
            res.category = "clip";
            return res;
        }

        String rawName = item.title != null ? item.title : "";
        if (rawName.isEmpty() && item.path != null) {
            File f = new File(item.path);
            rawName = f.getName();
            int dotIdx = rawName.lastIndexOf('.');
            if (dotIdx > 0) rawName = rawName.substring(0, dotIdx);
        }

        // 1. Try matching Episodic Web Series patterns
        Matcher m = PATTERN_S_E.matcher(rawName);
        if (m.find()) {
            res.category = "series";
            res.seriesName = cleanShowName(m.group(1));
            res.season = parseSafeInt(m.group(2), 1);
            res.episode = parseSafeInt(m.group(3), 1);
            res.cleanTitle = String.format(Locale.US, "EPISODE %02d", res.episode);
            return res;
        }

        m = PATTERN_X.matcher(rawName);
        if (m.find()) {
            res.category = "series";
            res.seriesName = cleanShowName(m.group(1));
            res.season = parseSafeInt(m.group(2), 1);
            res.episode = parseSafeInt(m.group(3), 1);
            res.cleanTitle = String.format(Locale.US, "EPISODE %02d", res.episode);
            return res;
        }

        m = PATTERN_SEASON_EPISODE.matcher(rawName);
        if (m.find()) {
            res.category = "series";
            res.seriesName = cleanShowName(m.group(1));
            res.season = parseSafeInt(m.group(2), 1);
            res.episode = parseSafeInt(m.group(3), 1);
            res.cleanTitle = String.format(Locale.US, "EPISODE %02d", res.episode);
            return res;
        }

        m = PATTERN_EP_ONLY.matcher(rawName);
        if (m.find()) {
            res.category = "series";
            res.seriesName = cleanShowName(m.group(1));
            res.season = checkFolderForSeason(item.path);
            res.episode = parseSafeInt(m.group(2), 1);
            res.cleanTitle = String.format(Locale.US, "EPISODE %02d", res.episode);
            return res;
        }

        m = PATTERN_E_STANDALONE.matcher(rawName);
        if (m.find()) {
            res.category = "series";
            res.seriesName = cleanShowName(m.group(1));
            res.season = checkFolderForSeason(item.path);
            res.episode = parseSafeInt(m.group(2), 1);
            res.cleanTitle = String.format(Locale.US, "EPISODE %02d", res.episode);
            return res;
        }

        m = PATTERN_PART.matcher(rawName);
        if (m.find()) {
            res.category = "series";
            res.seriesName = cleanShowName(m.group(1));
            res.season = checkFolderForSeason(item.path);
            res.episode = parseSafeInt(m.group(2), 1);
            res.cleanTitle = String.format(Locale.US, "PART %02d", res.episode);
            return res;
        }

        // 2. Check if parent folder indicates a series (e.g. /Downloads/Mirzapur/ep01.mp4 or /Series/Loki S02/...)
        if (item.path != null) {
            File file = new File(item.path);
            File parent = file.getParentFile();
            if (parent != null) {
                String parentName = parent.getName();
                Matcher folderMatcher = PATTERN_S_E.matcher(parentName);
                if (folderMatcher.find()) {
                    res.category = "series";
                    res.seriesName = cleanShowName(folderMatcher.group(1));
                    res.season = parseSafeInt(folderMatcher.group(2), 1);
                    res.episode = extractNumericEpisode(rawName);
                    res.cleanTitle = res.episode > 0 ? String.format(Locale.US, "EPISODE %02d", res.episode) : cleanMediaTitle(rawName);
                    return res;
                }
            }
        }

        // 3. Movie vs Clip Detection
        Matcher yearMatcher = PATTERN_YEAR.matcher(rawName);
        if (yearMatcher.find()) {
            res.releaseYear = yearMatcher.group(1);
        }

        // If video duration is 40+ minutes OR contains release year, classify as Movie
        long durationMs = item.duration;
        if (durationMs >= 40 * 60 * 1000L || (res.releaseYear != null && durationMs >= 20 * 60 * 1000L)) {
            res.category = "movie";
            res.cleanTitle = cleanMediaTitle(rawName);
            return res;
        }

        // 4. Default to Clip for shorter clips / recordings
        res.category = "clip";
        res.cleanTitle = cleanMediaTitle(rawName);
        return res;
    }

    public static void processItem(MediaItem item) {
        if (item == null) return;
        ClassificationResult res = classify(item);
        item.mediaCategory = res.category;
        item.seriesName = res.seriesName;
        item.seasonNumber = res.season;
        item.episodeNumber = res.episode;
        item.cleanTitle = res.cleanTitle != null ? res.cleanTitle : item.title;
        if (res.releaseYear != null) item.releaseYear = res.releaseYear;
    }

    public static void processAll(Iterable<MediaItem> items) {
        if (items == null) return;
        for (MediaItem item : items) {
            processItem(item);
        }
    }

    private static String cleanShowName(String raw) {
        if (raw == null) return "Unknown Series";
        String cleaned = CLEANER_BRACKETS.matcher(raw).replaceAll(" ");
        cleaned = CLEANER_TAGS.matcher(cleaned).replaceAll(" ");
        cleaned = CLEANER_SYMBOLS.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("@\\S+", " "); // Remove telegram usernames like @ChannelName
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return "Series";
        return toTitleCase(cleaned);
    }

    private static String cleanMediaTitle(String raw) {
        if (raw == null) return "";
        String cleaned = CLEANER_BRACKETS.matcher(raw).replaceAll(" ");
        cleaned = CLEANER_TAGS.matcher(cleaned).replaceAll(" ");
        cleaned = CLEANER_SYMBOLS.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("@\\S+", " ");
        cleaned = cleaned.replaceAll("(?i)\\b(19\\d\\d|20\\d\\d)\\b", " ");
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return raw;
        return toTitleCase(cleaned);
    }

    private static int checkFolderForSeason(String path) {
        if (path == null) return 1;
        try {
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null) {
                String pName = parent.getName().toLowerCase();
                if (pName.contains("season")) {
                    Matcher m = Pattern.compile("season\\s*(\\d+)").matcher(pName);
                    if (m.find()) return Integer.parseInt(m.group(1));
                } else if (pName.matches(".*s(\\d{1,2}).*")) {
                    Matcher m = Pattern.compile("s(\\d{1,2})").matcher(pName);
                    if (m.find()) return Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private static int extractNumericEpisode(String name) {
        if (name == null) return -1;
        Matcher m = Pattern.compile("(?i)(?:ep|e|episode|part)[ ._\\-]*(\\d{1,3})").matcher(name);
        if (m.find()) {
            return parseSafeInt(m.group(1), -1);
        }
        // Check for isolated digits e.g. "01", "02"
        Matcher mNum = Pattern.compile("\\b(\\d{1,3})\\b").matcher(name);
        if (mNum.find()) {
            return parseSafeInt(mNum.group(1), -1);
        }
        return -1;
    }

    private static int parseSafeInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) {
                sb.append(w.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
