package com.nyala.server.infrastructure.adapter.m3u;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class M3uPlaylist {

    private M3uTag startTag;
    private List<Map<M3uTag, M3uMediaUri>> mediaTagsAndEntries = new ArrayList<>();
    private M3uTag finalTag;

    public boolean isEmpty() {
        return mediaTagsAndEntries.isEmpty();
    }

    public void addEntry(M3uTag tag, M3uMediaUri mediaUri) {

    }
}
