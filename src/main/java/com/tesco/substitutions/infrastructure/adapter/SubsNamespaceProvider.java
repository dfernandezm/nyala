package com.tesco.substitutions.infrastructure.adapter;


import com.tesco.substitutions.commons.errorhandling.ApiErrorException;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.rxjava.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import rx.Single;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tesco.substitutions.infrastructure.adapter.SubstitutesRedisService.REDIS_FIND_TIMEOUT;

@Slf4j
public class SubsNamespaceProvider {

    public static final String REDIS_KEYS_SUBS_NAMESPACE = "originalTpn_";
    // CFC is a Customer Fulfillment Centre
    public static final String REDIS_KEYS_SUBS_IN_CFC_NAMESPACE = REDIS_KEYS_SUBS_NAMESPACE + "cfc_";
    public static final String REDIS_KEYS_CFC_STORE_IDS = "cfcStoreIds";
    public static final String REDIS_KEYS_SUBS_DATE_PREFIX = "currentSubsDatePrefix";


    private final RedisClient redisClient;

    @Inject
    public SubsNamespaceProvider(@Named("redisClient") final RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    protected Single<List<String>> getRedisNamespaceForTpnbs(final String storeId, final List<UnavailableProduct> unavailableProducts) {
        log.info("Adding namespace to unavailable products: {} with store id {}", unavailableProducts, storeId);
        return isCfcStoreId(storeId)
                .flatMap(isCfcStore -> getSubsDatePrefix()
                        .map(subsPrefix -> concatNamespaceToTpnbs(subsPrefix, isCfcStore, unavailableProducts)));
    }

    protected Single<List<String>> getRedisNamespaceForTpnbs(final List<UnavailableProduct> unavailableProducts) {
        log.info("Adding namespace to unavailable products: {} with no store id", unavailableProducts);
        return getSubsDatePrefix().map(subsPrefix -> concatNonCfcNamespaceToTpnbs(subsPrefix, unavailableProducts));
    }

    private String concatNonCfcNamespaceToTpnb(final String subsPrefix, UnavailableProduct unavailableProduct) {
        log.debug("adding non-CFC namespace to unavailable product {}", unavailableProduct);
        return REDIS_KEYS_SUBS_NAMESPACE +  subsPrefix + "_" + unavailableProduct.tpnb();
    }

    private List<String> concatNonCfcNamespaceToTpnbs(String subsPrefix, final List<UnavailableProduct> unavailableProducts) {
        return unavailableProducts.stream().map(tpnb -> concatNonCfcNamespaceToTpnb(subsPrefix,tpnb)).collect(Collectors.toList());
    }

    private String concatNamespaceToTpnb(final String subsPrefix, final boolean isCfcStoreId, final UnavailableProduct unavailableProduct) {
        if (isCfcStoreId) {
            log.debug("adding CFC namespace to unavailable product {}", unavailableProduct);
            return REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + subsPrefix + "_" + unavailableProduct.tpnb();
        }
        return concatNonCfcNamespaceToTpnb(subsPrefix, unavailableProduct);
    }

    private List<String> concatNamespaceToTpnbs(String subsPrefix, final boolean isCfcStore, final List<UnavailableProduct> unavailableProducts) {
        return unavailableProducts.stream()
                 .map(unavailableProduct -> concatNamespaceToTpnb(subsPrefix, isCfcStore, unavailableProduct))
                 .collect(Collectors.toList());
    }
    
    private Single<String> getSubsDatePrefix() {
        return redisClient
                .rxGet(REDIS_KEYS_SUBS_DATE_PREFIX)
                .timeout(REDIS_FIND_TIMEOUT, TimeUnit.MILLISECONDS)
                .map(this::throwIfEmpty);
    }

    private String throwIfEmpty(String subsPrefix) {
        if (StringUtils.isEmpty(subsPrefix))  {
            throw new ApiErrorException(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Subs prefix key is not present in Redis: " + REDIS_KEYS_SUBS_DATE_PREFIX);
        }
        return subsPrefix;
    }

    private Single<Boolean> isCfcStoreId(final String storeId) {
        return this.redisClient.rxGet(REDIS_KEYS_CFC_STORE_IDS).timeout(REDIS_FIND_TIMEOUT, TimeUnit.MILLISECONDS)
                .map(response -> response != null && storeId != null && response.contains(storeId));
    }

}
