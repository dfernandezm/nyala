package com.nyala.server.infrastructure.adapter.m3u.parser;

import com.google.common.annotations.VisibleForTesting;
import com.nyala.server.infrastructure.adapter.m3u.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3uParser {

    public static final String TAG_MARKER = "#";
    public static final String EXTM3U = "#EXTM3U";

    //public static final String EXTINF_TAG_REGEX = "#EXTINF:(\\d+(\\.\\d+)*|-1)(?:(?:,|\\s+)([^,]*))*(?:,([\\w\\+\\s][^\\n]+))*";
    // Example: tvg-id="" tvg-name="MOVISTAR+ MARVEL 1" tvg-logo="" group-title="SPANISH"
    // it will be 1 match per pair with 2 groups each (4 matches, g1: key, g2: value)
    public static final String TVG_DATA_ATTRIBUTES_REGEX = "([\\w\\-]+)=\"([\\w\\s\\+]*)\"";

    public M3uPlaylist parse(String m3uText) {
        if (!m3uText.startsWith(EXTM3U)) {
            throw new RuntimeException("Invalid playlist -- does not start with base header");
        }

        // use builder
        M3uPlaylist.Builder playlistBuilder = M3uPlaylist.builder();

        Arrays.stream(
                m3uText.split("\n"))
                .map(String::trim)
                .map(line -> isHeader(line) ?
                        parseHeader(playlistBuilder, line) :
                        parseLocation(playlistBuilder, line)
                );

        return playlistBuilder.build();
    }

    public M3uPlaylist parseHeader(M3uPlaylist.Builder m3uPlaylistBuilder, String header) {

        // skip top header
        if (!isTopHeader(header) || !isExtInfTag(header)) {

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
        return header.startsWith(M3uMediaTagParser.EXTINF_TAG_WITH_COLON);
    }

    private boolean isHeader(String m3uLine) {
        return m3uLine.startsWith(TAG_MARKER);
    }
}
