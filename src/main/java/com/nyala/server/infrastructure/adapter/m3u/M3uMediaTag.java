package com.nyala.server.infrastructure.adapter.m3u;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true)
public class M3uMediaTag {

    public static String EXTINF_TAG_NAME = "EXTINF";
    private String name;
    private MediaSegmentDuration duration;
    private TvgData tvgData;
}
