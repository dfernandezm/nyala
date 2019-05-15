package com.fazula.server.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Channel {
    String name;
    String country;
}
