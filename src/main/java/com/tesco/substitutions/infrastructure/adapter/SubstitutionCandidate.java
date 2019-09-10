package com.tesco.substitutions.infrastructure.adapter;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SubstitutionCandidate {
    private String subTpn;
    private String subRank;
    private List<String> storeIds;
}
