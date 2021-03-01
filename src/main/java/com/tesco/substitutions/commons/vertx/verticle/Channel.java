package com.tesco.substitutions.commons.vertx.verticle;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Channel {
    String name;
    String country;
}
