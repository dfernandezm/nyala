package com.tesco.substitutions.infrastructure.adapter;

import com.google.common.collect.Lists;
import com.tesco.substitutions.domain.model.BulkSubstitution;
import com.tesco.substitutions.domain.model.Substitution;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.redis.RedisClient;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import rx.Single;

@Slf4j
public class SubstitutesRedisService implements SubstitutionsService {

    public static final String REDIS_KEYS_SUBS_NAMESPACE = "originalTpn_";
    private RedisClient redisClient;
    private RedisResponseMapper redisResponseMapper;

    @Inject
    public SubstitutesRedisService(@Named("redisClient") final RedisClient redisClient, RedisResponseMapper redisResponseMapper) {
        this.redisClient = redisClient;
        this.redisResponseMapper = redisResponseMapper;
    }

    @Override
    public Single<List<Substitution>> getSubstitutionsFor(UnavailableProduct unavailableProduct) {
        return redisResponseMapper.mapSingleSubstitutionsResponse(redisClient.rxGet(addNamespaceToTpnb(unavailableProduct.tpnb())), System.currentTimeMillis())
                .map(this::asSubstitutions);
    }

    @Override
    public Single<List<BulkSubstitution>> getBulkSubstitutionsFor(List<UnavailableProduct> unavailableProducts) {
        List<String> unavailableTpnbs = unavailableProducts.stream()
                .map(UnavailableProduct::tpnb)
                .map(this::addNamespaceToTpnb)
                .collect(Collectors.toList());

        return redisResponseMapper.mapBulkSubstitutionsResponse(redisClient.rxMgetMany(unavailableTpnbs), System.currentTimeMillis())
                .map(jsonArray -> asBulkSubstitutions(jsonArray, unavailableProducts));
    }

    private String addNamespaceToTpnb(String tpnb) {
        return REDIS_KEYS_SUBS_NAMESPACE + tpnb;
    }

    private List<Substitution> asSubstitutions(final JsonArray jsonArray) {
        return jsonArray
                .stream()
                .filter(Objects::nonNull)
                .map(subtpnb -> Substitution.of(subtpnb.toString()))
                .collect(Collectors.toList());
    }

    private List<BulkSubstitution> asBulkSubstitutions(final JsonArray jsonArray, List<UnavailableProduct> unavailableProducts){
        log.debug(jsonArray.encodePrettily());
        if (jsonArray.size() != unavailableProducts.size()) {
            log.info("The number of substitutions is different from that of unavailable products list, probably some substitutions were not found");
        }
        List<BulkSubstitution> bulkSubstitutions = IntStream.range(0, jsonArray.size())
                .mapToObj(i -> createBulkSubstitution(i, unavailableProducts, jsonArray))
                .collect(Collectors.toList());

        log.debug(bulkSubstitutions.toString());
        return bulkSubstitutions;
    }

    private BulkSubstitution createBulkSubstitution(int index, List<UnavailableProduct> unavailableProducts, JsonArray redisResponse){
        return BulkSubstitution.of(unavailableProducts.get(index).tpnb(), getSubTpnbsFromRedisResponse(redisResponse, index));
    }

    private List<String> getSubTpnbsFromRedisResponse(JsonArray redisResponse, int index){
        if (redisResponse.getString(index) == null) return Collections.emptyList();
        return Lists.newArrayList(redisResponse.getString(index).split(",\\s*"));
    }
}
