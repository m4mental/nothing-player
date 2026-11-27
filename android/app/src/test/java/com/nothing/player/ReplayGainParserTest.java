package com.nothing.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ReplayGainParserTest {

    @Test
    public void parseGainDb_extractsId3ReplayGainTag() {
        byte[] fakeId3Header = "ID3\u0003\u0000TXXX\u0000REPLAYGAIN_TRACK_GAIN\u0000-6.50 dB\u0000"
                .getBytes(StandardCharsets.ISO_8859_1);
        Float gain = ReplayGainParser.parseGainDbFromBytes(fakeId3Header, fakeId3Header.length);
        assertNotNull(gain);
        assertEquals(-6.50f, gain, 0.01f);

        float linear = ReplayGainParser.dbToLinearAttenuation(gain);
        assertTrue(linear > 0.45f && linear < 0.50f);
    }

    @Test
    public void parseGainDb_extractsR128OpusTag() {
        // -512 in Q7.8 = -2.0 dB, plus +5.0 dB ReplayGain reference shift = +3.0 dB
        byte[] vorbisTag = "R128_TRACK_GAIN=-512".getBytes(StandardCharsets.ISO_8859_1);
        Float gain = ReplayGainParser.parseGainDbFromBytes(vorbisTag, vorbisTag.length);
        assertNotNull(gain);
        assertEquals(3.0f, gain, 0.01f);
        assertEquals(1.0f, ReplayGainParser.dbToLinearAttenuation(gain), 0.001f);
    }

    @Test
    public void parseGainDb_returnsNullWhenAbsent() {
        byte[] emptyAudio = "fLaC\u0000\u0000\u0001\u0002\u0003".getBytes(StandardCharsets.ISO_8859_1);
        assertNull(ReplayGainParser.parseGainDbFromBytes(emptyAudio, emptyAudio.length));
    }

    @Test
    public void parseTimestampToMs_handlesHoursMinutesSeconds() {
        assertEquals(4530000L, ExoVideoPlayerActivity.parseTimestampToMs("01:15:30"));
        assertEquals(90000L, ExoVideoPlayerActivity.parseTimestampToMs("1:30"));
        assertEquals(45000L, ExoVideoPlayerActivity.parseTimestampToMs("45"));
        assertEquals(-1L, ExoVideoPlayerActivity.parseTimestampToMs("abc"));
    }
}
