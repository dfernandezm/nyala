package com.tesco.substitutions.infrastructure.adapter

import com.tesco.substitutions.domain.model.BulkSubstitution
import com.tesco.substitutions.domain.model.Substitution
import com.tesco.substitutions.domain.model.UnavailableProduct
import io.vertx.core.json.JsonArray
import io.vertx.rxjava.redis.RedisClient
import org.assertj.core.util.Lists
import rx.Single
import spock.lang.Specification

import java.util.stream.Collectors

class SubstitutesRedisServiceSpec extends Specification{


    private SubstitutesRedisService substitutesRedisService
    private RedisResponseMapper redisResponseMapper
    private RedisClient redisClient

    def setup() {
        redisClient = Mock(RedisClient)
        redisResponseMapper = Mock(RedisResponseMapper)
        substitutesRedisService = new SubstitutesRedisService(redisClient, redisResponseMapper)
    }

    def 'substitutions are returned for a tpnb of an unavailable product that has substitutions stored in redis, an empty list is returned if there are no substitutions stored in redis' () {

        given : 'Redis has tpnbs as keys, it provides substitutions per each tpnb'
        def redisAsyncResponse = Single.just(redisSubstitutesResponse)
        1 * redisClient.rxGet(SubstitutesRedisService.REDIS_KEYS_SUBS_NAMESPACE + tpnb.toString()) >> redisAsyncResponse
        1 * redisResponseMapper.mapSingleSubstitutionsResponse(redisAsyncResponse, _) >> Single.just(new JsonArray("[$redisSubstitutesResponse]"))

        when: 'We request a substitution for a tpnb to SubstitutesRedisService'
        def result = substitutesRedisService.getSubstitutionsFor(UnavailableProduct.of(tpnb)).toBlocking().value()

        then: 'we should receive the expected result'
        result.equals(expectedSubstitutionsList)

        where: 'if Redis has substitutes, that list of tpnbs substitutes should be returned. If no substitutions were found an empty list is returned'

        tpnb                          ||      redisSubstitutesResponse     ||     expectedSubstitutionsList
        '58396818'                    ||         '11111111,22222222'       ||     Lists.newArrayList(Substitution.of('11111111'),Substitution.of('22222222'))
        '98396818'                    ||          null                     ||     Collections.emptyList()
        '98396819'                    ||          ''                       ||     Collections.emptyList()
    }

    def 'substitutions for multiple unavailable products are returned if stored in redis, an empty list is returned otherwise' (){
        given: 'Redis has tpnbs as keys'
        def redisAsyncResponse = Single.just(redisSubstitutesResponse)
        1 * redisClient.rxMgetMany(Arrays.stream(tpnbs.split(',')).map({ tpnb -> 'originalTpn_' + tpnb }).collect(Collectors.toList())) >> redisAsyncResponse
        1 * redisResponseMapper.mapBulkSubstitutionsResponse(redisAsyncResponse, _) >> Single.just(redisSubstitutesResponse)

        when: 'We request a substitutions for multiple tpnbs'
        def result = substitutesRedisService.getBulkSubstitutionsFor(Arrays.stream(tpnbs.split(',')).map({ tpnb -> UnavailableProduct.of(tpnb) }).collect(Collectors.toList())).toBlocking().value()

        then: 'we should receive the expected result'
        result.equals(expectedSubstitutionsList)

        where: 'if Redis has substitutes, that list of lists of tpnb substitutes should be returned. If no substitutions were found an empty list is returned'

        tpnbs                || redisSubstitutesResponse                                             || expectedSubstitutionsList
        '64522828,80644752'  || new JsonArray('[\"56906502,71166006\",\"78470769,80167412\"]')  || Lists.newArrayList(BulkSubstitution.of('64522828', Lists.newArrayList('56906502', '71166006')), BulkSubstitution.of('80644752', Lists.newArrayList('78470769', '80167412')))
        '79731926,84802277'  || new JsonArray(Lists.newArrayList(null, null))              || Lists.newArrayList(BulkSubstitution.of('79731926', Collections.emptyList()), BulkSubstitution.of('84802277', Collections.emptyList()))
    }
}
