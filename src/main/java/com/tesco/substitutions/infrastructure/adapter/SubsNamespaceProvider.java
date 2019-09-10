package com.tesco.substitutions.infrastructure.adapter;

import static com.tesco.substitutions.infrastructure.adapter.SubstitutesRedisService.REDIS_FIND_TIMEOUT;

import com.tesco.substitutions.domain.model.UnavailableProduct;
import io.vertx.rxjava.redis.RedisClient;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import rx.Single;

@Slf4j
public class SubsNamespaceProvider {

    public static final String REDIS_KEYS_SUBS_NAMESPACE = "originalTpn_";
    // CFC is a Customer Fulfillment Centre
    public static final String REDIS_KEYS_SUBS_IN_CFC_NAMESPACE = REDIS_KEYS_SUBS_NAMESPACE + "cfc_";
    public static final String REDIS_KEYS_CFC_STORE_IDS = "cfcStoreIds";

    private final RedisClient redisClient;

    @Inject
    public SubsNamespaceProvider(@Named("redisClient") final RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    private static String concatNonCfcNamespaceToTpnb(final UnavailableProduct unavailableProduct) {
        log.debug("adding non-CFC namespace to unavailable product {}", unavailableProduct);
        return REDIS_KEYS_SUBS_NAMESPACE + unavailableProduct.tpnb();
    }

    private static List<String> concatNonCfcNamespaceToTpnbs(final List<UnavailableProduct> unavailableProducts) {
        return unavailableProducts.stream().map(SubsNamespaceProvider::concatNonCfcNamespaceToTpnb).collect(Collectors.toList());
    }

    private static String concatNamespaceToTpnb(final boolean isCfcStoreId, final UnavailableProduct unavailableProduct) {
        if (isCfcStoreId) {
            log.debug("adding CFC namespace to unavailable product {}", unavailableProduct);
            return REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + unavailableProduct.tpnb();
        }
        return SubsNamespaceProvider.concatNonCfcNamespaceToTpnb(unavailableProduct);
    }

    private static List<String> concatNamespaceToTpnbs(final boolean isCfcStore, final List<UnavailableProduct> unavailableProducts) {
        return unavailableProducts.stream().map(unavailableProduct -> SubsNamespaceProvider
                .concatNamespaceToTpnb(isCfcStore, unavailableProduct))
                .collect(Collectors.toList());
    }

    protected Single<List<String>> getRedisNamespaceForTpnbs(final List<UnavailableProduct> unavailableProducts) {
        log.info("Adding namespace to unavailable products: {} with no store id", unavailableProducts);
        return Single.just(SubsNamespaceProvider.concatNonCfcNamespaceToTpnbs(unavailableProducts));
    }

    protected Single<List<String>> getRedisNamespaceForTpnbs(final String storeId, final List<UnavailableProduct> unavailableProducts) {
        log.info("Adding namespace to unavailable products: {} with store id {}", unavailableProducts, storeId);
        return this.isCfcStoreId(storeId).map(isCfcStore -> SubsNamespaceProvider.concatNamespaceToTpnbs(isCfcStore, unavailableProducts));
    }

    private Single<Boolean> isCfcStoreId(final String storeId) {
        return this.redisClient.rxGet(REDIS_KEYS_CFC_STORE_IDS).timeout(REDIS_FIND_TIMEOUT, TimeUnit.MILLISECONDS)
                .map(response -> response != null && storeId != null && response.contains(storeId));
    }

}
