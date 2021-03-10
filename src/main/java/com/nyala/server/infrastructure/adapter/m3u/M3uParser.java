package com.nyala.server.infrastructure.adapter.m3u;

import java.util.Arrays;

public class M3uParser {

    public static final String TAG_MARKER = "#";
    public static final String EXTM3U = "#EXTM3U";
    public static final String EXTINF_TAG_WITH_COLON = "#EXTINF";

    // In the spec for HLS (m3u8) is:
    // #EXTINF:<duration>,[<title>]
    // but M3U playlists may use spaces instead of comma
    // added optional groups (?:...) as the whole title part is optional. This should be split
    private static final String EXTINF_DURATION_REGEX = "(\\d+(\\.\\d+)*|-1)";
    private static final String EXTINF_COMMA_OR_SPACE_REGEX = "(?:,|\\s+)";
    private static final String EXTINF_TITLE_TVG_DATA_PART_REGEX = "([^,\n]*)";
    private static final String EXTINF_TITLE_TRACK_NAME_REGEX = ",([\\w\\+\\s][^,\\n]+)";
    private static final String OPTIONAL_NON_CAPTURING_GROUP = "(?:{value})*";

    //public static final String EXTINF_TAG_REGEX = "#EXTINF:(\\d+(\\.\\d+)*|-1)(?:(?:,|\\s+)([^,]*))*(?:,([\\w\\+\\s][^\\n]+))*";
    // Example: tvg-id="" tvg-name="MOVISTAR+ MARVEL 1" tvg-logo="" group-title="SPANISH"
    // it will be 1 match per pair with 2 groups each (4 matches, g1: key, g2: value)
    public static final String TVG_DATA_ATTRIBUTES_REGEX = "([\\w\\-]+)=\"([\\w\\s\\+]*)\"";

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

    public String optionalNonCapturingGroupOf(String regex) {
        String withOptionalNonCapturing = OPTIONAL_NON_CAPTURING_GROUP.replace("{value}", regex);
        return withOptionalNonCapturing;
    }

    public String extInfRegex() {
        return EXTINF_TAG_WITH_COLON + ":" +
                EXTINF_DURATION_REGEX +
                optionalNonCapturingGroupOf(
                        EXTINF_COMMA_OR_SPACE_REGEX + EXTINF_TITLE_TVG_DATA_PART_REGEX +
                                optionalNonCapturingGroupOf(EXTINF_TITLE_TRACK_NAME_REGEX)
                );
    }
}
