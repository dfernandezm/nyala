package com.nyala.server.infrastructure.adapter.m3u;

import java.util.LinkedHashMap;
import java.util.Map;

public class M3uPlaylist {
    private M3uTag startTag;
    private Map<M3uTag, M3uMediaUri> mediaTagsAndEntries = new LinkedHashMap<>();
    private M3uTag finalTag;



    public boolean isEmpty() {
        return mediaTagsAndEntries.isEmpty();
    }
}
