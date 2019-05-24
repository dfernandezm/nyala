package com.tesco.substitutions.infrastructure.adapter

import com.tesco.substitutions.domain.model.SubstitutionCandidate
import com.tesco.substitutions.domain.model.UnavailableProduct
import io.vertx.rxjava.redis.RedisClient
import rx.Single
import spock.lang.Specification

class HttpRedisServiceSpec extends Specification{


    HttpRedisService httpRedisService;

    def setup() {}

    def "substitutions are returned for a tpnb of an unavailable product that has substitutions stored in redis, an empty list is returned if there are no substitutions stored in redis" () {

        given : "Redis has tpnbs as keys, it provides substitutes candidates per each tpnb"
        RedisClient redisMock = Mock();
        httpRedisService = new HttpRedisService(redisMock);
        1 * redisMock.rxGet(tpnb.toString()) >> Single.just(redisSubstitutesResponse);

        when: "We request a substitution for a tpnb to HttpRedisService"
        def result = httpRedisService.substitutionsFor(UnavailableProduct.of(tpnb)).toBlocking().value();

        then: "we should receive the expected result"
        result.equals(expectedSubstitutionCandidatesList);

        where: "if Redis has substitutes, that list of tpnbs substitutes should be returned. If no substitutions were found an empty list is returned"

        tpnb                        ||      redisSubstitutesResponse     ||     expectedSubstitutionCandidatesList
        58396818                    ||         "11111111,22222222"       ||     new ArrayList<SubstitutionCandidate>(Arrays.asList(SubstitutionCandidate.of(11111111),SubstitutionCandidate.of(22222222)))
        98396818                    ||          null                     ||     new ArrayList<SubstitutionCandidate>()
    }
}
