package com.nyala.server.test.unit;

import com.nyala.server.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.server.infrastructure.adapter.m3u.M3uMediaUri;
import com.nyala.server.infrastructure.adapter.m3u.M3uPlaylist;
import com.nyala.server.infrastructure.adapter.m3u.M3uPlaylistEntry;
import com.nyala.server.infrastructure.adapter.m3u.MediaSegmentDuration;
import com.nyala.server.infrastructure.adapter.m3u.parser.M3uParser;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class M3uParserTest {

    private TestHelper testHelper = new TestHelper();

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
    public void parsePlaylistWithParseparsePlaylistWithParserTestrTest() throws IOException {
        String m3uPlaylistString = testHelper.readFileToString("testdata/samplePlaylist.m3u");
        Integer expectedEntriesSize = 4;

        M3uParser m3uParser = new M3uParser();
        M3uPlaylist m3uPlaylist = m3uParser.parse(m3uPlaylistString);

        assertThat(m3uPlaylist.isEmpty(), is(false));
        assertThat(m3uPlaylist.entries().size(), is(expectedEntriesSize));
    }

    @Test
    public void parseValidPlaylist() {
        String m3uPlaylistString = testHelper.readFileToString("testdata/samplePlaylist.m3u");

        M3uParser m3uParser = new M3uParser();
        M3uPlaylist m3uPlaylist = m3uParser.parse(m3uPlaylistString);

        assertThat(m3uPlaylist.isEmpty(), is(false));
        assertEntriesMatch(m3uPlaylist);
    }

    private void assertEntriesMatch(M3uPlaylist m3uPlaylist) {
        List<List<String>> entryOneTag = List.of(List.of("-1", "MOVISTAR+ MARVEL 1", "SPANISH"));
        List<String> entryOneMedia = List.of("http://u3mPAlw3Gdc6imjH0r.c2e3n4te5r6.me/live/cra4.php?z=997&ui=116740&s=CT4FQCVW5E&id=26325&h=4cf5c6dd7136ef5f736ab45ba93c6dffc726e4ab&n=1614243531");

        List<M3uPlaylistEntry> m3uPlaylistEntries = m3uPlaylist.entries();
        IntStream.range(0, m3uPlaylistEntries.size()).forEach( i -> {
            M3uPlaylistEntry entry = m3uPlaylistEntries.get(i);
            List<String> entryValues = entryOneTag.get(i);
            String expectedDurationStr = entryValues.get(0);
            MediaSegmentDuration expectedDuration = MediaSegmentDuration.builder()
                    .duration(expectedDurationStr)
                    .build();

            String expectedTvgName = entryValues.get(1);
            String expectedGroupTitle = entryValues.get(2);

            M3uMediaTag m3uMediaTag = entry.mediaTag();
            M3uMediaUri m3uMediaUri = entry.mediaUri();
            assertThat(m3uMediaUri, equalTo(entryOneMedia.get(i)));
            assertThat(m3uMediaTag.duration(), equalTo(expectedDuration));
            assertThat(m3uMediaTag.tvgData().groupTitle(), equalTo(expectedGroupTitle));
            assertThat(m3uMediaTag.tvgData().tvgName(), equalTo(expectedTvgName));

        });
    }
}
