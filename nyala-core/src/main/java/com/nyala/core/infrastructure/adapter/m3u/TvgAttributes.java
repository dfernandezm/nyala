package com.nyala.core.infrastructure.adapter.m3u;

public enum TvgAttributes {

    TVG_ID("tvg-id"),
    TVG_NAME("tvg-name"),
    TVG_LOGO("tvg-logo"),
    GROUP_TITLE("group-title");

    public final String attrName;

    TvgAttributes(String attrName) {
        this.attrName = attrName;
    }
}
