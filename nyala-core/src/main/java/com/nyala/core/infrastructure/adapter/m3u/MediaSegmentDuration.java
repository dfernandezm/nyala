package com.nyala.core.infrastructure.adapter.m3u;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class MediaSegmentDuration {

    private static final String MINUS_ONE = "-1";

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Integer durationAsInt;
    private double durationSeconds;

    @Builder
    public MediaSegmentDuration(String duration) {

        try {
            this.durationSeconds = Double.parseDouble(duration);
            this.durationAsInt = (int) Math.floor(durationSeconds);
        } catch (NumberFormatException nbe) {
            throw new RuntimeException("Invalid duration provided", nbe);
        }

//        if (MINUS_ONE.equals(duration)) {
//            this.durationAsInt = -1;
//            this.duration = null;
//        } else {
//            String durationPattern = "PT" + duration + "S";
//            this.duration = Duration.parse(durationPattern);
//        }
    }

    public double asSeconds() {
//        if (durationAsInt != null) {
//            return durationAsInt;
//        } else {
//            return (int) duration.getSeconds();
//        }
        return durationSeconds;
    }

    public int asIntegerSeconds() {
//        if (durationAsInt != null) {
//            return durationAsInt;
//        } else {
//            return (int) duration.getSeconds();
//        }
        return durationAsInt;
    }
}
