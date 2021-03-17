package com.nyala.server.infrastructure.adapter.m3u.parser;

import com.nyala.server.infrastructure.adapter.m3u.M3uMediaTag;
import com.nyala.server.infrastructure.adapter.m3u.MediaSegmentDuration;
import com.nyala.server.infrastructure.adapter.m3u.TvgAttributes;
import com.nyala.server.infrastructure.adapter.m3u.TvgData;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
// https://www.baeldung.com/kotlin/builder-pattern
@Slf4j
public class M3uMediaTagParser {
    public static final String EXTINF_TAG = "#EXTINF";

    // In the spec for HLS (m3u8) is:
    // #EXTINF:<duration>,[<title>]
    // but M3U playlists may use spaces instead of comma
    // added optional groups (?:...) as the whole title part is optional. This should be split
    private static final String EXTINF_DURATION_REGEX = "(\\d+(\\.\\d+)*|-1)";
    private static final String EXTINF_COMMA_OR_SPACE_REGEX = "(?:,|\\s+)";
    private static final String EXTINF_TITLE_TVG_DATA_PART_REGEX = "([^,\n]*)";
    private static final String EXTINF_TITLE_TRACK_NAME_REGEX = ",([\\w\\+\\s][^,\\n]+)";
    private static final String OPTIONAL_NON_CAPTURING_GROUP = "(?:{value})*";

    // Example: tvg-id="" tvg-name="MOVISTAR+ MARVEL 1" tvg-logo="" group-title="SPANISH"
    // it will be 1 match per pair with 2 groups each (4 matches, g1: key, g2: value)
    public static final String TVG_DATA_ATTRIBUTES_REGEX = "([\\w\\-]+)=\"([\\w\\s\\+]*)\"";


    public M3uMediaTag parseMediaTag(String extInfTag) {
        Pattern extInfTagPattern = Pattern.compile(extInfRegex());
        Matcher extInfTagMatcher = extInfTagPattern.matcher(extInfTag);

        if (extInfTagMatcher.matches()) {

            M3uMediaTag.M3uMediaTagBuilder extInfTagBuilder = M3uMediaTag.builder()
                    .name(M3uMediaTag.EXTINF_TAG_NAME);

            if (extInfTagMatcher.groupCount() >= 1) {
                String matchedDuration = extInfTagMatcher.group(1);
                durationFrom(extInfTagBuilder, matchedDuration);
            }

            if (extInfTagMatcher.groupCount() >= 3) {
                String matchedTvgData = extInfTagMatcher.group(3);
                if (matchedTvgData == null) {
                    extInfTagBuilder.tvgData(null);
                } else {
                    tvgDataFrom(extInfTagBuilder, matchedTvgData);
                }
            }

            return extInfTagBuilder.build();
        } else {
            throw new RuntimeException("EXTINF tag expression is incorrect -- " + extInfTag);
        }
    }

    private void tvgDataFrom(M3uMediaTag.M3uMediaTagBuilder extInfTagBuilder, String matchedTvgData) {
        TvgData tvgData = buildTvgData(matchedTvgData);
        extInfTagBuilder.tvgData(tvgData);
    }

    private void durationFrom(M3uMediaTag.M3uMediaTagBuilder extInfTagBuilder, String matchedDuration) {
        MediaSegmentDuration mediaSegmentDuration = MediaSegmentDuration.builder()
                .duration(matchedDuration)
                .build();
        extInfTagBuilder.duration(mediaSegmentDuration);
    }

//    public Optional<TvgData> parseTvgData(String extInfWithTvgData) {
//
//        Pattern extInfTagPattern = Pattern.compile(extInfRegex());
//        Matcher extInfTagMatcher = extInfTagPattern.matcher(extInfWithTvgData);
//
//        if (extInfTagMatcher.matches()) {
//            //TODO: may not be present - test
//            String matchedTvgData = extInfTagMatcher.group(3);
//            TvgData tvgData = buildTvgData(matchedTvgData);
//            return Optional.of(tvgData);
//        } else {
//            //TODO: log tvgdata not present - test
//            return Optional.empty();
//        }
//    }

    private TvgData buildTvgData(String tvgData) {
        Pattern tvgDataPattern = Pattern.compile(TVG_DATA_ATTRIBUTES_REGEX);
        Matcher tvgDataMatcher = tvgDataPattern.matcher(tvgData.trim());

        TvgData.TvgDataBuilder tvgDataBuilder = TvgData.builder();
        while (tvgDataMatcher.find()) {
            //TODO: may not be present - test
            String tvgAttrName = tvgDataMatcher.group(1);
            String tvgAttrValue = tvgDataMatcher.group(2);
            buildTvgAttribute(tvgDataBuilder, tvgAttrName, tvgAttrValue);
        }
        return tvgDataBuilder.build();
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
        return EXTINF_TAG + ":" +
                EXTINF_DURATION_REGEX +
                optionalNonCapturingGroupOf(
                        EXTINF_COMMA_OR_SPACE_REGEX + EXTINF_TITLE_TVG_DATA_PART_REGEX +
                                optionalNonCapturingGroupOf(EXTINF_TITLE_TRACK_NAME_REGEX)
                );
    }

}
