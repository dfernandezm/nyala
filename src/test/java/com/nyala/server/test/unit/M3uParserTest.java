package com.nyala.server.test.unit;

import com.nyala.server.infrastructure.adapter.m3u.M3uParser;
import com.nyala.server.infrastructure.adapter.m3u.M3uPlaylist;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

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

    @Test
    public void readPlaylistWithParserTest() throws IOException {
        File file = testHelper.readFile("testdata/samplePlaylist.m3u");
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
}
