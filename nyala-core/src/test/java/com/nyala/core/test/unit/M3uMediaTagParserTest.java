package com.nyala.core.test.unit;

import com.nyala.core.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.core.infrastructure.adapter.m3u.parser.M3uMediaTagParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static final String EXTINF_MINUS_ONE_UNORDERED_TVG_IGNORED_TRACK_NAME =
            "#EXTINF:-1,group-title=\"SPANISH\" tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\",other_track_name";

    private static final String EXTINF_DURATION_ONLY =
            "#EXTINF:0,,other_track_name";

    private static final String EXTINF_FLOAT_DURATION =
            "#EXTINF:10.117";

    private static final String EXTINF_NO_TVG =
            "#EXTINF:0";

    private static final String EXTINF_NO_DURATION =
            "#EXTINF: group-title=\"SPANISH\" tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\",other_track_name";

    private static final String EXTINF_WITH_NAME_COMPLETE =
            "#EXTINF:0 tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\" tvg-logo=\"\" group-title=\"SPANISH\",MOVISTAR";

    private static final String EXTINF_WITH_NAME_SPACE =
            "#EXTINF:-1 MOVISTAR";
    private static final String EXTINF_WITH_NAME_COMMA =
            "#EXTINF:-1,MOVISTAR";


    @Test
    public void getsCorrectDurationFromExtInfAsInt() {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        Integer negativeDuration = -1;

        M3uMediaTag m3UMediaTag = m3uMediaTagParser.parseMediaTag(EXTINF_INTEGER_DURATION);

        assertThat(m3UMediaTag.duration().asIntegerSeconds(), is(negativeDuration));
        assertThat(m3UMediaTag.name(), is(M3uMediaTag.EXTINF_TAG_NAME));
    }

    @Test
    public void getsCorrectDurationFromExtInfAsPositiveInt() {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        Integer expectedDuration = 10;
        String extinfTag = "#EXTINF:10 tvg-id=\"\", trackName";

        M3uMediaTag m3UMediaTag = m3uMediaTagParser.parseMediaTag(extinfTag);

        assertThat(m3UMediaTag.duration().asIntegerSeconds(), is(expectedDuration));
        assertThat(m3UMediaTag.name(), is(M3uMediaTag.EXTINF_TAG_NAME));
    }

    private static Stream<Arguments> provideExtInfHeaders() {
        // Generator of input -> output for EXTINF headers
        return Stream.of(
                Arguments.of(EXTINF_WITH_COMPLETE_TVG_DATA, List.of("-1", "SPANISH", "int"))
//                Arguments.of(EXTINF_ZERO_DURATION_TVG_GROUP_TITLE, List.of("0", "SPANISH", "int")),
//                Arguments.of(EXTINF_ZERO_DURATION, List.of("0", "SPANISH", "int")),
//                Arguments.of(EXTINF_MINUS_ONE_DURATION_GROUP_TITLE, List.of("-1", "SPANISH", "int")),
//                Arguments.of(EXTINF_MINUS_ONE_UNORDERED_TVG_IGNORED_TRACK_NAME, List.of("-1", "SPANISH", "int")),
//                Arguments.of(EXTINF_DURATION_ONLY, List.of("0", "null", "int")),
//                Arguments.of(EXTINF_FLOAT_DURATION, List.of("10.117", "null", "float"))
        );
    }

    @Test
    public void failsIfDurationIsntPresent() {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            m3uMediaTagParser.parseMediaTag(EXTINF_NO_DURATION);
        });

        assertThat(thrown.getMessage(), containsString("EXTINF tag expression is incorrect"));
    }

    @Test
    public void testTvgDataNotPresent() {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseMediaTag(EXTINF_NO_TVG);
        assertThat(m3uMediaTag.tvgData(), is(nullValue()));
        assertThat(m3uMediaTag.duration().asIntegerSeconds(), is(0));
    }

    @Test
    public void testTrackNameIsPresent() {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseMediaTag(EXTINF_WITH_NAME_COMPLETE);
        assertThat(m3uMediaTag.duration().asIntegerSeconds(), is(0));
        assertThat(m3uMediaTag.trackName(), is("MOVISTAR"));
    }

    @ParameterizedTest
    @ValueSource(strings={EXTINF_WITH_NAME_COMMA, EXTINF_WITH_NAME_SPACE})
    public void testTrackNameWithoutTvgData(String extInfValue) {
        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();
        String expectedTrackName = "MOVISTAR";
        M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseMediaTag(extInfValue);
        assertThat(m3uMediaTag.duration().asIntegerSeconds(), is(-1));
        assertThat(m3uMediaTag.trackName(), is("MOVISTAR"));
    }

    @ParameterizedTest
    @MethodSource("provideExtInfHeaders")
    public void parseExtInfTagCorrectly(String validExtInfTag, List<String> expected) {

        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();

        String groupTitle = expected.get(1);
        String type = expected.get(2);

        int expectedIntegerDuration = 0;
        double expectedDoubleDuration =  0D;

        if ("int".equals(type)) {
            expectedIntegerDuration = Integer.parseInt(expected.get(0));
        } else {
            expectedDoubleDuration = Double.parseDouble(expected.get(0));
        }

        M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseMediaTag(validExtInfTag);

        if ("int".equals(type)) {
            assertThat(m3uMediaTag.duration().asIntegerSeconds(), is(expectedIntegerDuration));
        } else {
            assertThat(m3uMediaTag.duration().asSeconds(), is(expectedDoubleDuration));
        }


        assertThat(m3uMediaTag.tvgData().groupTitle(), is("null".equals(groupTitle) ? null : groupTitle));
    }
}
