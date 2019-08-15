package com.tesco.substitutions.domain.model;

import java.util.List;
import lombok.Value;

@Value(staticConstructor = "of")
public class BulkSubstitution {
    private String tpnb;
    private List<String> substitutes;
}
