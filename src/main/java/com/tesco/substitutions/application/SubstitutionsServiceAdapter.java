package com.tesco.substitutions.application;

import com.google.inject.Inject;
import com.tesco.substitutions.domain.model.BulkSubstitution;
import com.tesco.substitutions.domain.model.Substitution;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;


public class SubstitutionsServiceAdapter {

    private final static Logger LOGGER = LoggerFactory.getLogger(SubstitutionsServiceAdapter.class);
    private SubstitutionsService substitutionsService;

    @Inject
    public SubstitutionsServiceAdapter(final SubstitutionsService substitutionsService) {
        this.substitutionsService = substitutionsService;
    }

    public Single<List<Substitution>> obtainSubstitutionsFor(final String unavailableTpnb) {
        LOGGER.info("Requesting substitutions for {} to the substitution service adapter", unavailableTpnb);
        return substitutionsService.getSubstitutionsFor(UnavailableProduct.of(unavailableTpnb));
    }

    public Single<List<BulkSubstitution>> obtainBulkSubstitutionsFor(final List<String> unavailableTpnbs) {
        LOGGER.info("Requesting substitutions for {} to the substitution service adapter", unavailableTpnbs);
        return substitutionsService.getBulkSubstitutionsFor(
                unavailableTpnbs.stream()
                .map(UnavailableProduct::of)
                .collect(Collectors.toList()));
    }
}
