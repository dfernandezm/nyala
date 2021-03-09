package com.nyala.server.infrastructure.adapter.m3u;

import java.util.Arrays;

public class M3uParser {

    public static final String TAG_MARKER = "#";
    public static final String EXTM3U = "#EXTM3U";
    public static final String EXTINF_TAG = "#EXTINF";

    // In the spec for HLS (m3u8) is:
    // #EXTINF:<duration>,[<title>]
    // but M3U playlists may use spaces instead of comma
    // Alternative regex, #EXTINF:(\d+(\.\d+)*|-1)(?:,|\s+)([^,]*),([\w\+\s][^\n]+)
    public static final String EXTINF_TAG_REGEX = "#EXTINF:(\\d+(\\.\\d+)*|-1)(?:,|\\s+)(.*)";


    public M3uPlaylist parse(String m3uText) {

        if (!m3uText.startsWith(EXTM3U)) {
            throw new RuntimeException("Invalid playlist -- does not start with base header");
        }

        //TODO: use builder
        M3uPlaylist m3uPlaylist = new M3uPlaylist();

        Arrays.stream(
                m3uText.split("\n"))
                .map(String::trim)
                .map(line -> isHeader(line) ?
                        parseHeader(m3uPlaylist, line) :
                        parseLocation(m3uPlaylist, line)
                );

        return new M3uPlaylist();
    }

    public M3uPlaylist parseHeader(M3uPlaylist m3uPlaylist, String header) {

        // skip top header
        if (!isTopHeader(header)) {

        }

        return new M3uPlaylist();
    }

    public M3uPlaylist parseLocation(M3uPlaylist m3uPlaylist, String location) {
        return new M3uPlaylist();
    }

    private boolean isTopHeader(String header) {
        return EXTM3U.equals(header.trim());
    }

    private boolean isHeader(String m3uLine) {
        return m3uLine.startsWith(TAG_MARKER);
    }
}
