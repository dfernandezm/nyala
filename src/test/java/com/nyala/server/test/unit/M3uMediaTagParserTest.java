package com.nyala.server.test.unit;

import com.nyala.server.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.server.infrastructure.adapter.m3u.TvgData;
import com.nyala.server.infrastructure.adapter.m3u.parser.M3uMediaTagParser;
import com.nyala.server.infrastructure.adapter.m3u.parser.M3uParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class M3uMediaTagParserTest {

    private static final String EXTINF_INTEGER_DURATION = "#EXTINF:-1 tvg-id=\"\"";
    private static final String EXTINF_WITH_TVG_DATA =
            "#EXTINF:-1 tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\" tvg-logo=\"\" group-title=\"SPANISH\",MOVISTAR+ MARVEL 1";

    @Test
    public void getsCorrectDurationFromExtInfAsInt() {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        Integer negativeDuration = -1;

        M3uMediaTag m3UMediaTag = m3uMediaTagParser.parseExtInfTag(EXTINF_INTEGER_DURATION);

        assertThat(m3UMediaTag.duration().asSeconds(), is(negativeDuration));
        assertThat(m3UMediaTag.name(), is(M3uMediaTag.EXTINF_TAG_NAME));
    }

    @Test
    public void getsCorrectDurationFromExtInfAsPositiveInt() {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        Integer expectedDuration = 10;
        String extinfTag = "#EXTINF:10 tvg-id=\"\", trackName";

        M3uMediaTag m3UMediaTag = m3uMediaTagParser.parseExtInfTag(extinfTag);

        assertThat(m3UMediaTag.duration().asSeconds(), is(expectedDuration));
        assertThat(m3UMediaTag.name(), is(M3uMediaTag.EXTINF_TAG_NAME));
    }

    @Test
    public void getsTvgDataFromExtInfTitle() {
        // Given
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        String expectedTvgGroupTitle = "SPANISH";
        String expectedTvgName = "MOVISTAR+ MARVEL 1";

        // When
        Optional<TvgData> tvgData = m3uMediaTagParser.parseTvgData(EXTINF_WITH_TVG_DATA);

        // Then
        TvgData presentTvgData = tvgData.orElseGet(() -> fail("TVG data is not present"));
        assertThat(presentTvgData.groupTitle(), is(expectedTvgGroupTitle));
        assertThat(presentTvgData.tvgName(), is(expectedTvgName));
    }
}
