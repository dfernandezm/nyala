package com.nyala.server.infrastructure.adapter.m3u;

import com.nyala.server.infrastructure.adapter.m3u.parser.M3uParser;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Getter
@Accessors(fluent=true)
public class M3uPlaylist {

    private String startTag;
    private List<M3uPlaylistEntry> entries = new LinkedList<>();

    private M3uPlaylist() {
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Iterator<M3uMediaUri> mediaUriIterator() {
        return entries.stream().map(M3uPlaylistEntry::mediaUri).iterator();
    }

    public Iterator<M3uMediaUri> entriesIterator() {
        return null;
    }

    public static class Builder {
        private String startTag;
        private List<M3uPlaylistEntry> entries = new LinkedList<>();

        public Builder withStart() {
            this.startTag = M3uParser.EXTM3U;
            return this;
        }

        public Builder addMediaEntry(M3uMediaTag mediaTag, M3uMediaUri mediaUri) {
            M3uPlaylistEntry m3uPlaylistEntry = M3uPlaylistEntry.builder()
                                                .mediaTag(mediaTag)
                                                .mediaUri(mediaUri)
                                                .build();
            entries.add(m3uPlaylistEntry);
            return this;
        }

        public M3uPlaylist build() {
            M3uPlaylist m3uPlaylist = new M3uPlaylist();

            if (startTag == null) {
                throw new RuntimeException("Start tag is mandatory");
            }

            m3uPlaylist.startTag = startTag;
            m3uPlaylist.entries = entries;
            return m3uPlaylist;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
