package com.tesco.substitutions.domain.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class SubstitutionCandidate {

    private Long tpnb;
}
