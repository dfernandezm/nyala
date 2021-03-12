package com.nyala.server.test.unit;

import com.nyala.server.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.server.infrastructure.adapter.m3u.TvgData;
import com.nyala.server.infrastructure.adapter.m3u.parser.M3uMediaTagParser;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class M3uMediaTagParserTest {

    private static final String EXTINF_INTEGER_DURATION = "#EXTINF:-1 tvg-id=\"\"";
    private static final String EXTINF_WITH_COMPLETE_TVG_DATA =
            "#EXTINF:-1 tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\" tvg-logo=\"\" group-title=\"SPANISH\"";
    private static final String EXTINF_ZERO_DURATION =
            "#EXTINF:0 tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\" tvg-logo=\"\" group-title=\"SPANISH\"";
    private static final String EXTINF_ZERO_DURATION_TVG_GROUP_TITLE =
            "#EXTINF:0 group-title=\"SPANISH\"";
    private static final String EXTINF_MINUS_ONE_DURATION_GROUP_TITLE =
            "#EXTINF:-1,group-title=\"SPANISH\"";
    private static final String EXINF_MINUS_ONE_UNORDERED_TVG_IGNORED_TRACK_NAME =
            "#EXTINF:-1,group-title=\"SPANISH\" tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\",other_track_name";

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
        Optional<TvgData> tvgData = m3uMediaTagParser.parseTvgData(EXTINF_WITH_COMPLETE_TVG_DATA);

        // Then
        TvgData presentTvgData = tvgData.orElseGet(() -> fail("TVG data is not present"));
        assertThat(presentTvgData.groupTitle(), is(expectedTvgGroupTitle));
        assertThat(presentTvgData.tvgName(), is(expectedTvgName));
    }

    @ParameterizedTest
    @ValueSource(strings={
            EXTINF_WITH_COMPLETE_TVG_DATA,
            EXTINF_MINUS_ONE_DURATION_GROUP_TITLE,
            EXINF_MINUS_ONE_UNORDERED_TVG_IGNORED_TRACK_NAME
    })
    public void parseExtInfTagCorrectly(String validExtInfTag) {

        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();

        M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseExtInfTag(validExtInfTag);

        assertThat(m3uMediaTag.duration().asSeconds(), is(-1));
        assertThat(m3uMediaTag.tvgData().groupTitle(), is("SPANISH"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void isBlank_ShouldReturnTrueForNullOrBlankStrings(String input) {
        assertTrue(Strings.isNullOrEmpty(input));
    }
}
