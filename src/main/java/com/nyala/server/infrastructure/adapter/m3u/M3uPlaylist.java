package com.nyala.server.infrastructure.adapter.m3u;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class M3uPlaylist {

    private  M3uTag startTag;
    private final List<Map<M3uTag, M3uMediaUri>> m3uTagsWithMediaUris = new ArrayList<>();
    private  M3uTag finalTag;

    private M3uPlaylist(Builder builder) {
    }


    public boolean isEmpty() {
        return m3uTagsWithMediaUris.isEmpty();
    }


    public static class Builder {
        private M3uTag startTag;
        private List<Map<String, M3uMediaUri>> entries = new LinkedList<>();

        public Builder startTag(M3uTag startTag) {
            this.startTag = startTag;
            return this;
        }

        public Builder addMediaTag(M3uTag mediaTag, M3uMediaUri mediaUri) {
            String mediaTagString = mediaTag.toString();
            Map<String, M3uMediaUri> pair = new LinkedHashMap<>();
            pair.put(mediaTagString, mediaUri);
            entries.add(pair);
        }

        public M3uPlaylist build() {
            return new M3uPlaylist(this);
        }
    }
}
