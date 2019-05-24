package com.tesco.substitutions.application;

import com.google.inject.Inject;
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

    @Inject
    public SubstitutionsApplicationService(final SubstitutionsService substitutionsService) {
        this.substitutionsService = substitutionsService;
    }

    public Single<List<SubstitutionCandidate>> obtainCandidateSubstitutionsFor(final Long unavailableTpnb) {
        LOGGER.info("Requesting substitutions for {} to the substitution service adapter", unavailableTpnb);
        return substitutionsService.substitutionsFor(UnavailableProduct.of(unavailableTpnb));
    }

}
