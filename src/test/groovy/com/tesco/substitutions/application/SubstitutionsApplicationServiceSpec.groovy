package com.tesco.substitutions.application

import com.tesco.substitutions.domain.model.UnavailableProduct
import spock.lang.Shared
import spock.lang.Specification

class SubstitutionsApplicationServiceSpec extends Specification {

    private static final Long  TEST_TPNB = 58396818;
    @Shared
    SubstitutionsApplicationService substitutionsApplicationService

    @Shared
    UnavailableProduct unavailableProduct = UnavailableProduct.of(TEST_TPNB);

}
