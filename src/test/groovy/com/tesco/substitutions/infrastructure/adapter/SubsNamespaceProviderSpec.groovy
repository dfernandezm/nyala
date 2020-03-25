package com.tesco.substitutions.infrastructure.adapter

import com.tesco.substitutions.commons.errorhandling.ApiErrorException
import com.tesco.substitutions.domain.model.UnavailableProduct
import io.vertx.rxjava.redis.RedisClient
import org.assertj.core.util.Lists
import rx.Single
import spock.lang.Specification

import static junit.framework.Assert.assertTrue

class SubsNamespaceProviderSpec extends Specification {

    private SubsNamespaceProvider subsNamespaceProvider
    private RedisClient redisClient

    def setup(){
        redisClient = Mock(RedisClient)
        subsNamespaceProvider = new SubsNamespaceProvider(redisClient)
    }

    def 'should return list of original tpnbs prefixed with "originalTpn_cfc" if storeId is of a CFC store'(){
        given:
        def cfcStores = '1111,2222'
        def tpnbs = ['tpnb1', 'tpnb2']
        def subsDatePrefix = "18_09_2019"
        1 * redisClient.rxGet(SubsNamespaceProvider.REDIS_KEYS_CFC_STORE_IDS) >> Single.just(cfcStores)
        1 * redisClient.rxGet(SubsNamespaceProvider.REDIS_KEYS_SUBS_DATE_PREFIX) >> Single.just(subsDatePrefix)


        when: 'A CFC store and valid tpnbs are requested'
        def result = subsNamespaceProvider.getRedisNamespaceForTpnbs('1111', createUnavailableProductList(tpnbs)).toBlocking().value()

        then: 'Keys are prefixed with originalTpn_cfc_'
        assertTrue(result.every {tpnb -> tpnb.startsWith(SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE)})
    }

    def 'should return list of original tpnbs prefixed with "originalTpn_" if storeId is not of a CFC store'(){
        given:
        def cfcStores = '1111,2222'
        def tpnbs = ['tpnb1', 'tpnb2']
        def subsDatePrefix = "18_09_2019"
        1 * redisClient.rxGet(SubsNamespaceProvider.REDIS_KEYS_CFC_STORE_IDS) >> Single.just(cfcStores)
        1 * redisClient.rxGet(SubsNamespaceProvider.REDIS_KEYS_SUBS_DATE_PREFIX) >> Single.just(subsDatePrefix)

        when: 'A non CFC store and valid tpnbs are requested'
        def result = subsNamespaceProvider.getRedisNamespaceForTpnbs('9999', createUnavailableProductList(tpnbs)).toBlocking().value()

        then: 'Keys are prefixed with originalTpn_'
        assertTrue(result.every {tpnb -> tpnb.startsWith(SubsNamespaceProvider.REDIS_KEYS_SUBS_NAMESPACE)})
    }

    def 'should return list of original tpnbs prefixed with "originalTpn_" if storeId is not passed' (){
        given:
        def tpnbs = ['tpnb1', 'tpnb2']
        def subsDatePrefix = "18_09_2019"

        1 * redisClient.rxGet(SubsNamespaceProvider.REDIS_KEYS_SUBS_DATE_PREFIX) >> Single.just(subsDatePrefix)

        when: 'No storeid and valid tpnbs are requested'
        def result = subsNamespaceProvider.getRedisNamespaceForTpnbs(createUnavailableProductList(tpnbs)).toBlocking().value()

        then: 'Keys are prefixed with originalTpn_'
        assertTrue(result.every {tpnb -> tpnb.startsWith(SubsNamespaceProvider.REDIS_KEYS_SUBS_NAMESPACE)})
    }

    def 'should throw error if subs prefix is not found' (){
        given:
        def cfcStores = '1111,2222'
        def tpnbs = ['tpnb1', 'tpnb2']
        def subsDatePrefix = ""
        1 * redisClient.rxGet(SubsNamespaceProvider.REDIS_KEYS_CFC_STORE_IDS) >> Single.just(cfcStores)
        1 * redisClient.rxGet(SubsNamespaceProvider.REDIS_KEYS_SUBS_DATE_PREFIX) >> Single.just(subsDatePrefix)

        when:
        subsNamespaceProvider.getRedisNamespaceForTpnbs(null, createUnavailableProductList(tpnbs)).toBlocking().value()

        then:
        thrown(ApiErrorException)
    }

    List<UnavailableProduct> createUnavailableProductList(List<String> tpnbs){
        List<UnavailableProduct> result = Lists.newArrayList()
        tpnbs.each {tpnb -> result.add(UnavailableProduct.of(tpnb))}
        result
    }
}
