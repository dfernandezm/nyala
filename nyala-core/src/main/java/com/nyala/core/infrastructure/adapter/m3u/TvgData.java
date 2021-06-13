package com.nyala.core.infrastructure.adapter.m3u;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(fluent=true)
public class TvgData {

    private String tvgId;
    private String tvgName;
    private String tvgLogo;
    private String groupTitle;
}
