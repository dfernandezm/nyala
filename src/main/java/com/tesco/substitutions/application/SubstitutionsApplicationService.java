package com.tesco.substitutions.application;

import com.tesco.substitutions.domain.model.SubstitutionCandidate;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;


public class SubstitutionsApplicationService {

    private final static Logger LOGGER = LoggerFactory.getLogger(SubstitutionsApplicationService.class);
    private SubstitutionsService substitutionsService;


    public SubstitutionsApplicationService() {

    }

    public Single<List<SubstitutionCandidate>> obtainCandidateSubstitutionsFor(final String unavailableTpnb) {
        return this.substitutionsService.substitutionsFor(UnavailableProduct.of(unavailableTpnb));
    }

}
