package com.tesco.substitutions.domain.service;

import com.tesco.substitutions.domain.model.BulkSubstitution;
import com.tesco.substitutions.domain.model.Substitution;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import java.util.List;
import rx.Single;

public interface SubstitutionsService {

    Single<List<Substitution>> getSubstitutionsFor(UnavailableProduct unavailableProduct);

    Single<List<BulkSubstitution>> getBulkSubstitutionsFor(List<UnavailableProduct> unavailableProducts);
}
