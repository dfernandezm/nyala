package com.nyala.core.infrastructure.adapter.m3u.parser;

import com.nyala.core.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.core.infrastructure.adapter.m3u.M3uPlaylist;
import com.nyala.core.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.core.infrastructure.adapter.m3u.M3uMediaUri;
import com.nyala.core.infrastructure.adapter.m3u.M3uPlaylist;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Iterator;

@Slf4j
public class M3uParser {

    public static final String TAG_MARKER = "#";
    public static final String EXTM3U = "#EXTM3U";

    public M3uPlaylist parse(String m3uText) {
        if (!m3uText.startsWith(EXTM3U)) {
            throw new RuntimeException("Invalid playlist -- does not start with base header");
        }

        M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();

        M3uPlaylist.Builder playlistBuilder = M3uPlaylist.builder();
        playlistBuilder.withStart();

        Iterator<String> m3uLinesIterator = Arrays.stream(
                m3uText.split("\n"))
                .map(String::trim).iterator();


        while (m3uLinesIterator.hasNext()) {

            String line = m3uLinesIterator.next();

            if (isTopHeader(line)) {
                log.debug("Skipping starting tag");
                if (m3uLinesIterator.hasNext()) {
                    line = m3uLinesIterator.next();
                } else {
                    // returns empty playlist
                    return playlistBuilder.build();
                }
            }

            if (isMediaTag(line)) {
                M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseMediaTag(line);
                if (m3uLinesIterator.hasNext()) {
                    String mediaUri = m3uLinesIterator.next();
                    M3uMediaUri m3uMediaUri = parseMediaUriIfPresent(mediaUri);
                    playlistBuilder.addMediaEntry(m3uMediaTag, m3uMediaUri);
                } else {
                    throw new RuntimeException("Invalid M3U playlist: media tag not followed by media uri");
                }
            }
        }

        return playlistBuilder.build();
    }

    private M3uMediaUri parseMediaUriIfPresent(String mediaUri) {
        if (isMediaUri(mediaUri)) {
            return M3uMediaUri.builder()
                    .uri(mediaUri)
                    .build();
        } else {
            throw new RuntimeException("Invalid M3U playlist: media tag not followed by media uri");
        }
    }

    private boolean isTopHeader(String header) {
        return header.startsWith(EXTM3U);
    }

    private boolean isMediaTag(String mediaTagLine) {
        return mediaTagLine.startsWith(M3uMediaTagParser.EXTINF_TAG);
    }

    private boolean isMediaUri(String mediaUriLine) {
        return !mediaUriLine.startsWith(TAG_MARKER);
    }
}
