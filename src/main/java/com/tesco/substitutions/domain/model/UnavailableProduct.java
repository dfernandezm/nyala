package com.tesco.substitutions.domain.model;

import lombok.Value;
import lombok.experimental.Accessors;

@Value(staticConstructor = "of")
@Accessors(fluent = true)
public class UnavailableProduct {

    private String uuid;
}
