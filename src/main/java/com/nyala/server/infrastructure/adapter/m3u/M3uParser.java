package com.nyala.server.infrastructure.adapter.m3u;

import com.google.common.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // use builder
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
        if (!isTopHeader(header) || !isExtInfTag(header)) {

        }

        return new M3uPlaylist();
    }

    public M3uPlaylist parseLocation(M3uPlaylist m3uPlaylist, String location) {
        return new M3uPlaylist();
    }

    private boolean isTopHeader(String header) {
        return header.startsWith(EXTM3U);
    }

    private boolean isExtInfTag(String header) {
        return header.startsWith(EXTINF_TAG_WITH_COLON);
    }

    private boolean isHeader(String m3uLine) {
        return m3uLine.startsWith(TAG_MARKER);
    }

    // -- tag parser --
    @VisibleForTesting
    public M3uTag parseExtInfTag(String extInfTag) {
        Pattern extInfTagPattern = Pattern.compile(extInfRegex());
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


     // -- tvg data parser --
    @VisibleForTesting
    public Optional<TvgData> parseTvgData(String extInfWithTvgData) {
        M3uParser m3uParser = new M3uParser();
        Pattern extInfTagPattern = Pattern.compile(m3uParser.extInfRegex());
        Matcher extInfTagMatcher = extInfTagPattern.matcher(extInfWithTvgData);

        if (extInfTagMatcher.matches()) {
            String tvgData = extInfTagMatcher.group(3);

            Pattern tvgDataPattern = Pattern.compile(M3uParser.TVG_DATA_ATTRIBUTES_REGEX);
            Matcher tvgDataMatcher = tvgDataPattern.matcher(tvgData.trim());

            TvgData.TvgDataBuilder tvgDataBuilder = TvgData.builder();
            while (tvgDataMatcher.find()) {
                String tvgAttrName = tvgDataMatcher.group(1);
                String tvgAttrValue = tvgDataMatcher.group(2);
                buildTvgAttribute(tvgDataBuilder, tvgAttrName, tvgAttrValue);
            }

            return Optional.of(tvgDataBuilder.build());
        } else {
            // log tvgdata not present
            return Optional.empty();
        }
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


    // ---- Regex utils for EXTINF ----
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
