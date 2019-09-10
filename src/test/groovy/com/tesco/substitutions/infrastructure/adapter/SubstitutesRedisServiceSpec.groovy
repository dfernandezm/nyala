package com.tesco.substitutions.infrastructure.adapter

import com.google.common.collect.Lists
import com.tesco.substitutions.domain.model.Substitution
import com.tesco.substitutions.domain.model.UnavailableProduct
import groovy.json.JsonSlurper
import io.vertx.core.json.JsonArray
import io.vertx.rxjava.redis.RedisClient
import rx.Single
import spock.lang.Specification

import java.util.concurrent.TimeoutException

class SubstitutesRedisServiceSpec extends Specification {


    private SubstitutesRedisService substitutesRedisService
    private SubsNamespaceProvider subsNamespaceProvider
    private RedisResponseMapper redisResponseMapper
    private RedisClient redisClient

    def setup() {
        redisClient = Mock(RedisClient)
        redisResponseMapper = Mock(RedisResponseMapper)
        subsNamespaceProvider = Mock(SubsNamespaceProvider)
        substitutesRedisService = new SubstitutesRedisService(subsNamespaceProvider, redisClient, redisResponseMapper)
    }

    def 'substitutions are returned for a tpnb of an unavailable product that has substitutions stored in redis with a matching storeId'() {
        def tpnbs = ['58396818']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + it }
        def redisResponse = createRedisResponseFromTpnbs(tpnbs as String[])

        def storeId = '1111'
        given: 'Redis has tpnbs as keys, it provides substitutions per each tpnb'
        def redisAsyncResponse = Single.just(redisResponse)
        def responseMapperResponse = new JsonArray(redisResponse.toString())
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(storeId, unavailableProducts) >> Single.just(unavailableTpnbs)
        1 * redisClient.rxMgetMany(unavailableTpnbs) >> redisAsyncResponse
        1 * redisResponseMapper.mapSubstitutionsResponse(redisResponse) >> responseMapperResponse

        when: 'We request a substitution for a tpnb to SubstitutesRedisService'
        def result = substitutesRedisService.getSubstitutionsFor(storeId, unavailableProducts).toBlocking().value()

        then: 'we should receive the expected result'
        result.equals(createExpectedResult(tpnbs as String[]))
    }

    def 'substitutions are returned for a tpnb of an unavailable product that has substitutions stored in redis with no storeid'() {
        def tpnbs = ['55555555']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_NAMESPACE + it }
        def redisResponse = createRedisResponseFromTpnbs(tpnbs as String[])

        given: 'Redis has tpnbs as keys, it provides substitutions per each tpnb'
        def redisAsyncResponse = Single.just(redisResponse)
        def responseMapperResponse = new JsonArray(redisResponse.toString())
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(unavailableProducts) >> Single.just(unavailableTpnbs)
        1 * redisClient.rxMgetMany(unavailableTpnbs) >> redisAsyncResponse
        1 * redisResponseMapper.mapSubstitutionsResponse(redisResponse) >> responseMapperResponse

        when: 'We request a substitution for a tpnb to SubstitutesRedisService'
        def result = substitutesRedisService.getSubstitutionsFor(unavailableProducts).toBlocking().value()

        then: 'we should receive the expected result'
        result.equals(createExpectedResult(tpnbs as String[]))
    }

    def 'should return substitutions for tpnbs of unavailable products that have substitutions in redis with storeid'() {
        def tpnbs = ['58396818', '52575426']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + it }
        def redisResponse = createRedisResponseFromTpnbs(tpnbs as String[])

        def storeId = '1111'
        given: 'Redis has tpnbs as keys, it provides substitutions per each tpnb'
        def redisAsyncResponse = Single.just(redisResponse)
        def responseMapperResponse = new JsonArray(redisResponse.toString())
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(storeId, unavailableProducts) >> Single.just(unavailableTpnbs)

        1 * redisClient.rxMgetMany(unavailableTpnbs) >> redisAsyncResponse
        1 * redisResponseMapper.mapSubstitutionsResponse(redisResponse) >> responseMapperResponse

        when: 'We request a substitution for tpnbs to SubstitutesRedisService'
        def result = substitutesRedisService.getSubstitutionsFor(storeId, unavailableProducts).toBlocking().value()

        then: 'we should receive the expected result'
        result.equals(createExpectedResult(tpnbs as String[]))
    }

    def 'should return substitutions for tpnbs of unavailable products that have substitutions in redis with no storeid'() {
        def tpnbs = ['55555555', '66666666']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_NAMESPACE + it }
        def redisResponse = createRedisResponseFromTpnbs(tpnbs as String[])

        given: 'Redis has tpnbs as keys, it provides substitutions per each tpnb'
        def redisAsyncResponse = Single.just(redisResponse)
        def responseMapperResponse = new JsonArray(redisResponse.toString())
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(unavailableProducts) >> Single.just(unavailableTpnbs)

        1 * redisClient.rxMgetMany(unavailableTpnbs) >> redisAsyncResponse
        1 * redisResponseMapper.mapSubstitutionsResponse(redisResponse) >> responseMapperResponse

        when: 'We request a substitution for tpnbs to SubstitutesRedisService'
        def result = substitutesRedisService.getSubstitutionsFor(unavailableProducts).toBlocking().value()

        then: 'we should receive the expected result'
        result.equals(createExpectedResult(tpnbs as String[]))
    }

    def 'should return empty list if no substitutions stored in redis'() {
        def tpnbs = ['58396818', '52575426']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + it }
        def redisResponse = new JsonArray([null, null])

        def storeId = '1111'
        given: 'Redis has tpnbs as keys, it provides substitutions per each tpnb'
        def redisAsyncResponse = Single.just(redisResponse)
        def responseMapperResponse = new JsonArray("[[],[]]")
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(storeId, unavailableProducts) >> Single.just(unavailableTpnbs)
        1 * redisClient.rxMgetMany(unavailableTpnbs) >> redisAsyncResponse
        1 * redisResponseMapper.mapSubstitutionsResponse(redisResponse) >> responseMapperResponse

        when: 'We request a substitution for tpnbs to SubstitutesRedisService'
        def result = substitutesRedisService.getSubstitutionsFor(storeId, unavailableProducts).toBlocking().value()

        then: 'we should receive the expected result'
        result.size().equals(2)
        result.every { sub -> sub.getSubstitutes().equals([]) }
    }

    def 'should return empty list if no substitutions stored in redis with no storeid given'() {
        def tpnbs = ['58396818', '52575426']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_NAMESPACE + it }
        def redisResponse = new JsonArray([null, null])

        given: 'Redis has tpnbs as keys, it provides substitutions per each tpnb'
        def redisAsyncResponse = Single.just(redisResponse)
        def responseMapperResponse = new JsonArray("[[],[]]")
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(unavailableProducts) >> Single.just(unavailableTpnbs)
        1 * redisClient.rxMgetMany(unavailableTpnbs) >> redisAsyncResponse
        1 * redisResponseMapper.mapSubstitutionsResponse(redisResponse) >> responseMapperResponse

        when: 'We request a substitution for tpnbs to SubstitutesRedisService'
        def result = substitutesRedisService.getSubstitutionsFor(unavailableProducts).toBlocking().value()

        then: 'we should receive the expected result'
        result.size().equals(2)
        result.every { sub -> sub.getSubstitutes().equals([]) }
    }

    @spock.lang.Ignore
    def 'should throw exception if redis client times out'() {
        // Need to work out how to return the correct type of response to simulate a timeout in redis
        given:
        def tpnbs = ['58396818', '52575426']
        def storeId = '1111'
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + it }
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(storeId, unavailableProducts) >> Single.just(unavailableTpnbs)
        1 * redisClient.rxMgetMany(_) >> Single.error(new TimeoutException())

        when:
        substitutesRedisService.getSubstitutionsFor(storeId, unavailableProducts).toBlocking().value()

        then:
        thrown TimeoutException
    }

    def 'should throw exception if namespace mapping fails with exception'() {
        given:
        def tpnbs = ['58396818', '52575426']
        def storeId = '1111'
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(storeId, unavailableProducts) >> Single.error(new RuntimeException("test exception"))

        when:
        substitutesRedisService.getSubstitutionsFor(storeId, unavailableProducts).toBlocking().value()

        then:
        RuntimeException runtimeException = thrown()
        runtimeException.message.equals('test exception')
    }

    def 'should throw exception if namespace mapping fails with exception when no storeid provided'() {
        given:
        def tpnbs = ['58396818', '52575426']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(unavailableProducts) >> Single.error(new RuntimeException("test exception"))

        when:
        substitutesRedisService.getSubstitutionsFor(unavailableProducts).toBlocking().value()

        then:
        RuntimeException runtimeException = thrown()
        runtimeException.message.equals('test exception')
    }

    def 'should throw exception if redis client fails with exception'() {
        given:
        def tpnbs = ['58396818', '52575426']
        def storeId = '1111'
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + it }
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(storeId, unavailableProducts) >> Single.just(unavailableTpnbs)
        1 * redisClient.rxMgetMany(_) >> Single.error(new RuntimeException('another test exception'))

        when:
        substitutesRedisService.getSubstitutionsFor(storeId, unavailableProducts).toBlocking().value()

        then:
        RuntimeException runtimeException = thrown()
        runtimeException.message.equals('another test exception')
    }

    def 'should throw exception if redis client fails with exception when no storeid provided'() {
        given:
        def tpnbs = ['58396818', '52575426']
        List<UnavailableProduct> unavailableProducts = tpnbs.collect { UnavailableProduct.of(it) }
        List<String> unavailableTpnbs = tpnbs.collect { SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + it }
        1 * subsNamespaceProvider.getRedisNamespaceForTpnbs(unavailableProducts) >> Single.just(unavailableTpnbs)
        1 * redisClient.rxMgetMany(_) >> Single.error(new RuntimeException('another test exception'))

        when:
        substitutesRedisService.getSubstitutionsFor(unavailableProducts).toBlocking().value()

        then:
        RuntimeException runtimeException = thrown()
        runtimeException.message.equals('another test exception')
    }


    def createRedisResponseFromTpnbs(String[] tpnbs) {
        def testData = readRedisResponseFromFile()
        JsonArray redisResponse = new JsonArray();

        tpnbs.each {
            redisResponse.add(testData[it])
        }
        redisResponse
    }

    def readRedisResponseFromFile() {
        def slurper = new JsonSlurper()
        def data = slurper.parse(new File('src/test/resources/testData/redisResponse.json'))
        println data
        data
    }

    def createExpectedResult(String[] tpnbs) {
        tpnbs.collect {
            Substitution.of(it, Lists.newArrayList('11111111', '22222222'))
        }
    }
}
