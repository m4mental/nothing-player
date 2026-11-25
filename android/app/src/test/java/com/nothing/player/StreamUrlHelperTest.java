package com.nothing.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StreamUrlHelperTest {

    @Test
    public void extractUrl_trimsTrailingPunctuation() {
        String text = "Watch this stream (https://example.com/live/master.m3u8?token=abc), enjoy!";
        assertEquals("https://example.com/live/master.m3u8?token=abc", StreamUrlHelper.extractUrl(text));
        assertNull(StreamUrlHelper.extractUrl("not a url"));
    }

    @Test
    public void isOnlineStream_detectsSupportedProtocols() {
        assertTrue(StreamUrlHelper.isOnlineStream("https://cdn.test/video.mp4"));
        assertTrue(StreamUrlHelper.isOnlineStream("rtsp://192.168.1.5:554/stream"));
        assertTrue(StreamUrlHelper.isOnlineStream("rtmp://live.example.com/app"));
        assertFalse(StreamUrlHelper.isOnlineStream("/storage/emulated/0/Movies/test.mp4"));
    }
}
