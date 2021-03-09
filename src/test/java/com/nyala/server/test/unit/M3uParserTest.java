package com.nyala.server.test.unit;

import com.nyala.server.infrastructure.adapter.m3u.*;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        M3uPlaylist m3uPlaylist = new M3uPlaylist();
        M3uTag m3uTag = parseExtInfTag(EXTINF_INTEGER_DURATION);
        assertThat(m3uTag.duration().asSeconds(), is(-1));
        assertThat(m3uTag.name(), is(M3uTag.EXTINF_TAG_NAME));
    }

    @Test
    public void getsTvgDataFromExtInfTitle() {
        TvgData tvgData = parseTvgData(EXTINF_WITH_TVG_DATA);
        assertThat(tvgData.groupTitle(), is("SPANISH"));
        assertThat(tvgData.tvgName(), is("MOVISTAR+ MARVEL 1"));
    }

    private M3uTag parseExtInfTag(String extInfTag) {
        Pattern extInfTagPattern = Pattern.compile(M3uParser.EXTINF_TAG_REGEX);
        Matcher extInfTagMatcher = extInfTagPattern.matcher(extInfTag);

        if (extInfTagMatcher.matches()) {
            String duration = extInfTagMatcher.group(1);
            MediaSegmentDuration mediaSegmentDuration = MediaSegmentDuration.builder().duration(duration).build();
            return M3uTag.builder()
                    .name(M3uTag.EXTINF_TAG_NAME)
                    .duration(mediaSegmentDuration)
                    .build();
        } else {
            throw new RuntimeException("EXTINF tag expression is incorrect -- " + extInfTag);
        }
    }

    private TvgData parseTvgData(String extInfWithTvgData) {
        Pattern extInfTagPattern = Pattern.compile(M3uParser.EXTINF_TAG_REGEX);
        Matcher extInfTagMatcher = extInfTagPattern.matcher(extInfWithTvgData);

        if (extInfTagMatcher.matches()) {
            String tvgData = extInfTagMatcher.group(3);

            Pattern tvgDataPattern = Pattern.compile(M3uParser.TVG_DATA_REGEX);
            Matcher tvgDataMatcher = tvgDataPattern.matcher(tvgData.trim());

            TvgData.TvgDataBuilder tvgDataBuilder = TvgData.builder();
            while (tvgDataMatcher.find()) {
                String tvgAttrName = tvgDataMatcher.group(1);
                String tvgAttrValue = tvgDataMatcher.group(2);
                buildTvgAttribute(tvgDataBuilder, tvgAttrName, tvgAttrValue);
            }

            return tvgDataBuilder.build();
        }

        throw new RuntimeException("EXTINF tag expression is incorrect -- " + extInfWithTvgData);
    }

    private TvgData.TvgDataBuilder buildTvgAttribute(TvgData.TvgDataBuilder tvgDataBuilder,
                                                     String tvgAttributeName, String tvgAttributeValue) {
        if (tvgAttributeName.equals(TvgAttributes.TVG_ID.attrName)) {
            tvgDataBuilder.tvgId(tvgAttributeValue);
        }

        if (tvgAttributeName.equals(TvgAttributes.TVG_NAME.attrName)) {
            tvgDataBuilder.tvgName(tvgAttributeValue);
        }

        if (tvgAttributeName.equals(TvgAttributes.TVG_LOGO.attrName)) {
            tvgDataBuilder.tvgLogo(tvgAttributeValue);
        }

        if (tvgAttributeName.equals(TvgAttributes.GROUP_TITLE.attrName)) {
            tvgDataBuilder.groupTitle(tvgAttributeValue);
        }

        return tvgDataBuilder;
    }
}
