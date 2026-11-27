package com.nothing.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class LrcParserTest {

    @Test
    public void parseLrcFile_and_getActiveLineIndex() throws Exception {
        File tmp = File.createTempFile("sample_track", ".lrc");
        tmp.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write("[00:05.50]First Line\n");
            fw.write("[00:10.125][00:20.00]Chorus\n");
        }

        List<LrcParser.LrcLine> lines = LrcParser.parseLrcFile(tmp);
        assertEquals(3, lines.size());
        assertEquals(5500L, lines.get(0).timeMs);
        assertEquals("First Line", lines.get(0).text);
        assertEquals(10125L, lines.get(1).timeMs);
        assertEquals(20000L, lines.get(2).timeMs);

        assertEquals(-1, LrcParser.getActiveLineIndex(lines, 1000L));
        assertEquals(0, LrcParser.getActiveLineIndex(lines, 6000L));
        assertEquals(1, LrcParser.getActiveLineIndex(lines, 15000L));
        assertEquals(2, LrcParser.getActiveLineIndex(lines, 25000L));
    }

    @Test
    public void findAndParseLrc_handlesExtensionlessFilenameSafely() throws Exception {
        File tmp = File.createTempFile("noext_audio", "");
        tmp.deleteOnExit();
        List<LrcParser.LrcLine> lines = LrcParser.findAndParseLrc(tmp.getAbsolutePath());
        assertTrue(lines.isEmpty());
    }
}
