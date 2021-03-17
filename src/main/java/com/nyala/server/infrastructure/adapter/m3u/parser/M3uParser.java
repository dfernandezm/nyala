package com.nyala.server.infrastructure.adapter.m3u.parser;

import com.nyala.server.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.server.infrastructure.adapter.m3u.M3uMediaUri;
import com.nyala.server.infrastructure.adapter.m3u.M3uPlaylist;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Iterator;

@Slf4j
public class M3uParser {

    public static final String TAG_MARKER = "#";
    public static final String EXTM3U = "#EXTM3U";
    private final M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();

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
                line = m3uLinesIterator.next();
            }

            if (isMediaTag(line)) {
                M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseMediaTag(line);
                if (m3uLinesIterator.hasNext()) {
                    String mediaUri = m3uLinesIterator.next();

                    if (isMediaUri(mediaUri)) {
                        M3uMediaUri m3uMediaUri = M3uMediaUri.builder()
                                .uri(mediaUri)
                                .build();
                        playlistBuilder.addMediaEntry(m3uMediaTag, m3uMediaUri);
                    } else {
                        throw new RuntimeException("Invalid M3U playlist: media tag not followed by media uri");
                    }
                } else {
                    throw new RuntimeException("Invalid M3U playlist: media tag not followed by media uri");
                }
            }
        }

        return playlistBuilder.build();
    }


    //TODO: https://stackoverflow.com/questions/34086461/java-stream-is-there-a-way-to-iterate-taking-two-elements-a-time-instead-of-one
    public M3uPlaylist parseHeader(M3uPlaylist.Builder m3uPlaylistBuilder, String header) {

        // skip top header
        if (!isTopHeader(header)) {
            if (isMediaTag(header)) {
                M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseMediaTag(header);
                m3uPlaylistBuilder.addMediaEntry(m3uMediaTag, null);
            }
        }

        return m3uPlaylistBuilder.build();
    }

    public M3uPlaylist parseLocation(M3uPlaylist.Builder m3uPlaylistBuilder, String location) {
        return m3uPlaylistBuilder.build();
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

    private boolean isHeader(String m3uLine) {
        return m3uLine.startsWith(TAG_MARKER);
    }
}
