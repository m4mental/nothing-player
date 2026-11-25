package com.nothing.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubtitleParserTest {

    @Test
    public void parseSrt_and_getCueTextAt_workCorrectly() {
        String srt = "1\n00:00:01,000 --> 00:00:04,500\n<i>Hello</i> World\n\n"
                + "2\n00:00:05,000 --> 00:00:08,000\nSecond Line\n";
        List<SubtitleParser.SubtitleCue> cues = SubtitleParser.parseSrt(
                new ByteArrayInputStream(srt.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, cues.size());
        assertEquals("Hello World", cues.get(0).text);
        assertEquals("Hello World", SubtitleParser.getCueTextAt(cues, 2000));
        assertNull(SubtitleParser.getCueTextAt(cues, 4700));
        assertEquals("Second Line", SubtitleParser.getCueTextAt(cues, 6000));
    }

    @Test
    public void cleanSdh_stripsBracketsMusicAndSpeakerLabels() {
        String raw = "[DOOR SLAMS]\nJOHN: ♪ Hello (laughs) there ♪";
        String cleaned = SubtitleParser.cleanSdh(raw);
        assertEquals("Hello  there", cleaned);
    }

    @Test
    public void exportToSrt_roundTripsCues() {
        String srt = "1\n00:01:02,050 --> 00:01:05,250\nTest Cue\n\n";
        List<SubtitleParser.SubtitleCue> cues = SubtitleParser.parseSrt(
                new ByteArrayInputStream(srt.getBytes(StandardCharsets.UTF_8)));
        String exported = SubtitleParser.exportToSrt(cues);
        assertNotNull(exported);
        assertTrue(exported.contains("00:01:02,050 --> 00:01:05,250"));
        assertTrue(exported.contains("Test Cue"));
    }
}
