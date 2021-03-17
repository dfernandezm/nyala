package com.nyala.server.test.unit;

import com.nyala.server.infrastructure.adapter.m3u.*;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class M3uPlaylistBuilderTest {

    @Test
    public  void shouldBuildEntryPairMediaTagMediaUri() {
        // Given
        M3uPlaylist.Builder builder = M3uPlaylist.builder();
        String duration = "-1";
        String tvgId = "";
        String tvgLogo = "logo";
        String tvgName = "aName";
        String groupTitle = "group";
        String mediaUri = "http://anUrl";

        MediaSegmentDuration mediaSegmentDuration = MediaSegmentDuration.builder()
                .duration(duration)
                .build();

        TvgData tvgData = TvgData.builder()
                .tvgId(tvgId)
                .tvgLogo(tvgLogo)
                .tvgName(tvgName)
                .groupTitle(groupTitle)
                .build();

        M3uMediaTag m3uMediaTag = M3uMediaTag.builder()
                 .duration(mediaSegmentDuration)
                 .tvgData(tvgData)
                 .build();

        M3uMediaUri m3uMediaUri = M3uMediaUri.builder().uri(mediaUri).build();

        // When
        builder.withStart()
                .addMediaEntry(m3uMediaTag, m3uMediaUri);

        M3uPlaylist playlist = builder.build();

        // Then
        assertThat(playlist.isEmpty(), is(false));
        assertThat(playlist.entries().size(), is(1));

        // And
        M3uPlaylistEntry m3uPlaylistEntry = playlist.entries().get(0);
        M3uMediaTag tag = m3uPlaylistEntry.mediaTag();
        M3uMediaUri entryMediaUri = m3uPlaylistEntry.mediaUri();

        assertThat(tag.duration().asIntegerSeconds(), is(Integer.parseInt(duration)));
        assertThat(entryMediaUri.getUri(), is(mediaUri));
        assertThat(tag.tvgData().groupTitle(), is(groupTitle));
        assertThat(tag.tvgData().tvgId(), is(tvgId));
        assertThat(tag.tvgData().tvgLogo(), is(tvgLogo));
        assertThat(tag.tvgData().tvgName(), is(tvgName));
    }
}
