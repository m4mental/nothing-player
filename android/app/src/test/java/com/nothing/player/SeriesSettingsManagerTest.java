package com.nothing.player;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SeriesSettingsManagerTest {

    @Test
    public void deriveSeriesKey_prefersMeaningfulFolder() {
        String key = SeriesSettingsManager.deriveSeriesKey(
                "Lupin Season 1",
                "/storage/emulated/0/Lupin Season 1/Lupin.S01E02.1080p.mkv",
                "Lupin S01E02"
        );
        assertEquals("folder:lupin season 1", key);
    }

    @Test
    public void deriveSeriesKey_stripsEpisodeSuffixForGenericDownloadsFolder() {
        String key = SeriesSettingsManager.deriveSeriesKey(
                "Downloads",
                "/storage/emulated/0/Downloads/Severance.S02E04.1080p.WEB-DL.mkv",
                "Severance.S02E04.1080p.WEB-DL.mkv"
        );
        assertEquals("series:severance", key);
    }
}
