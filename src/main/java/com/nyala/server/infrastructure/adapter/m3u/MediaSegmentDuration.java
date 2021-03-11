package com.nyala.server.infrastructure.adapter.m3u;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;

@Accessors(fluent = true)
public class MediaSegmentDuration {

    private static final String MINUS_ONE = "-1";

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Integer durationAsInt;
    private Duration duration;

    @Builder
    public MediaSegmentDuration(String duration) {
        if (MINUS_ONE.equals(duration)) {
            this.durationAsInt = -1;
            this.duration = null;
        } else {
            String durationPattern = "PT" + duration + "S";
            this.duration = Duration.parse(durationPattern);
        }
    }

    public int asSeconds() {
        if (durationAsInt != null) {
            return durationAsInt;
        } else {
            return (int) duration.getSeconds();
        }
    }
}
