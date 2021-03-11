package com.nyala.server.test.unit;

import com.nyala.server.infrastructure.adapter.m3u.M3uParser;
import com.nyala.server.infrastructure.adapter.m3u.M3uPlaylist;
import com.nyala.server.infrastructure.adapter.m3u.M3uTag;
import com.nyala.server.infrastructure.adapter.m3u.TvgData;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class M3uParserTest {

    private TestHelper testHelper = new TestHelper();
    private static final String EXTINF_INTEGER_DURATION = "#EXTINF:-1 tvg-id=\"\"";
    private static final String EXTINF_WITH_TVG_DATA = "#EXTINF:-1 tvg-id=\"\" tvg-name=\"MOVISTAR+ MARVEL 1\" tvg-logo=\"\" group-title=\"SPANISH\",MOVISTAR+ MARVEL 1";

    @Test
    public void emptyPlaylistTest() throws IOException {
        File file = testHelper.readFile("testdata/emptyPlaylist.m3u");
        String m3uString = IOUtils.toString(new FileInputStream(file), Charset.defaultCharset());

        M3uParser m3uParser = new M3uParser();
        M3uPlaylist m3uPlaylist = m3uParser.parse(m3uString);
        assertThat(m3uPlaylist.isEmpty(), is(true));
    }

    @Disabled("Cannot use m3u8 parser with m3u")
    @Test
    public void readPlaylistWithParserTest() throws IOException {
        File file = testHelper.readFile("testdata/samplePlaylist.m3u");

        // --- transform to conforming list ---

        MediaPlaylistParser parser = new MediaPlaylistParser();

        // Parse playlist
        MediaPlaylist playlist = parser.readPlaylist(file.toPath());

        // Update playlist version
        MediaPlaylist updated = MediaPlaylist.builder()
                .from(playlist)
                .version(2)
                .build();

        // Write playlist to standard out
        System.out.println(parser.writePlaylistAsString(updated));
    }

    @Test
    public void getsCorrectDurationFromExtInfAsInt() {
        M3uParser m3uParser = new M3uParser();
        Integer negativeDuration = -1;

        M3uTag m3uTag = m3uParser.parseExtInfTag(EXTINF_INTEGER_DURATION);

        assertThat(m3uTag.duration().asSeconds(), is(negativeDuration));
        assertThat(m3uTag.name(), is(M3uTag.EXTINF_TAG_NAME));
    }

    @Test
    public void getsCorrectDurationFromExtInfAsPositiveInt() {
        M3uParser m3uParser = new M3uParser();
        Integer expectedDuration = 10;
        String extinfTag = "#EXTINF:10 tvg-id=\"\", trackName";

        M3uTag m3uTag = m3uParser.parseExtInfTag(extinfTag);

        assertThat(m3uTag.duration().asSeconds(), is(expectedDuration));
        assertThat(m3uTag.name(), is(M3uTag.EXTINF_TAG_NAME));
    }

    @Test
    public void getsTvgDataFromExtInfTitle() {
        // Given
        M3uParser m3uParser = new M3uParser();
        String expectedTvgGroupTitle = "SPANISH";
        String expectedTvgName = "MOVISTAR+ MARVEL 1";

        // When
        Optional<TvgData> tvgData = m3uParser.parseTvgData(EXTINF_WITH_TVG_DATA);

        // Then
        TvgData presentTvgData = tvgData.orElseGet(() -> fail("TVG data is not present"));
        assertThat(presentTvgData.groupTitle(), is(expectedTvgGroupTitle));
        assertThat(presentTvgData.tvgName(), is(expectedTvgName));
    }


    @Test
    public void parsePlaylistWithParserTest() throws IOException {
        String m3uPlaylistString = testHelper.readFileToString("testdata/samplePlaylist.m3u");

        M3uParser m3uParser = new M3uParser();
        M3uPlaylist m3uPlaylist = m3uParser.parse(m3uPlaylistString);

        assertThat(m3uPlaylist.isEmpty(), is(false));
        //assertThat(m3uPlaylist.);

    }
}
