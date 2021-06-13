package com.nyala.core.infrastructure.adapter.m3u;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(fluent = true)
public class M3uPlaylistEntry {

    private M3uMediaTag mediaTag;
    private M3uMediaUri mediaUri;

    public M3uPlaylistEntry(M3uMediaTag m3uMediaTag, M3uMediaUri mediaUri) {
        this.mediaTag = m3uMediaTag;
        this.mediaUri = mediaUri;
    }
}
