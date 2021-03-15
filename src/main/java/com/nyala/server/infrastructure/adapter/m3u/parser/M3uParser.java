package com.nyala.server.infrastructure.adapter.m3u.parser;

import com.nyala.server.infrastructure.adapter.m3u.*;
import java.util.Arrays;

public class M3uParser {

    public static final String TAG_MARKER = "#";
    public static final String EXTM3U = "#EXTM3U";
    private final M3uMediaTagParser m3uMediaTagParser = new M3uMediaTagParser();

    public M3uPlaylist parse(String m3uText) {
        if (!m3uText.startsWith(EXTM3U)) {
            throw new RuntimeException("Invalid playlist -- does not start with base header");
        }

        // use builder
        M3uPlaylist.Builder playlistBuilder = M3uPlaylist.builder();
        playlistBuilder.withStart();
        Arrays.stream(
                m3uText.split("\n"))
                .map(String::trim)
                .map(line -> isHeader(line) ?
                        parseHeader(playlistBuilder, line) :
                        parseLocation(playlistBuilder, line)
                );

        return playlistBuilder.build();
    }


    //TODO: https://stackoverflow.com/questions/34086461/java-stream-is-there-a-way-to-iterate-taking-two-elements-a-time-instead-of-one
    public M3uPlaylist parseHeader(M3uPlaylist.Builder m3uPlaylistBuilder, String header) {

        // skip top header
        if (!isTopHeader(header)) {
            if (isExtInfTag(header)) {
                M3uMediaTag m3uMediaTag = m3uMediaTagParser.parseExtInfTag(header);
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

    private boolean isExtInfTag(String header) {
        return header.startsWith(M3uMediaTagParser.EXTINF_TAG);
    }

    private boolean isHeader(String m3uLine) {
        return m3uLine.startsWith(TAG_MARKER);
    }
}
