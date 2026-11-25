package com.nothing.player;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class M3uParserTest {

    @Test
    public void parseStream_parsesChannelsGroupsAndLogos() {
        String m3u = "#EXTM3U\n"
                + "#EXTINF:-1 group-title=\"NEWS\" tvg-logo=\"https://img/logo.png\",Channel One\n"
                + "https://stream.example.com/one.m3u8\n"
                + "#EXTINF:-1,Channel Two\n"
                + "rtsp://stream.example.com/two\n"
                + "#COMMENT\n"
                + "invalid_local_path.mp4\n";

        List<M3uParser.M3uChannel> channels = M3uParser.parseStream(
                new ByteArrayInputStream(m3u.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, channels.size());
        assertEquals("Channel One", channels.get(0).name);
        assertEquals("NEWS", channels.get(0).group);
        assertEquals("https://img/logo.png", channels.get(0).logoUrl);
        assertEquals("https://stream.example.com/one.m3u8", channels.get(0).streamUrl);
        assertEquals("GENERAL", channels.get(1).group);
        assertEquals("rtsp://stream.example.com/two", channels.get(1).streamUrl);
    }
}
