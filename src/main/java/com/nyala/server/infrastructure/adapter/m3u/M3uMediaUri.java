package com.nyala.server.infrastructure.adapter.m3u;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class M3uMediaUri {
    String uri;
}
