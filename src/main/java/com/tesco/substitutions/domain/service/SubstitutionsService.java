package com.tesco.substitutions.domain.service;

import com.tesco.substitutions.domain.model.SubstitutionCandidate;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import java.util.List;
import rx.Single;

public interface SubstitutionsService {

    Single<List<SubstitutionCandidate>> substitutionsFor(UnavailableProduct unavailableProduct);

}
