package com.tesco.substitutions.infrastructure.adapter;


import com.tesco.substitutions.commons.logging.LoggingUtils;
import com.tesco.substitutions.domain.model.Substitution;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import rx.exceptions.Exceptions;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class SubstitutesRedisService implements SubstitutionsService {

    static final Long REDIS_FIND_TIMEOUT = 3000L;

    private final SubsNamespaceProvider subsNamespaceProvider;
    private final RedisClient redisClient;
    private final RedisResponseMapper redisResponseMapper;

    public SubstitutesRedisService(final SubsNamespaceProvider subsNamespaceProvider, final RedisClient redisClient,
                                   final RedisResponseMapper redisResponseMapper) {
        this.subsNamespaceProvider = subsNamespaceProvider;
        this.redisClient = redisClient;
        this.redisResponseMapper = redisResponseMapper;
    }

    private static void checkForMissingResponses(final JsonArray dataFromRedis, final List<UnavailableProduct> unavailableProducts) {
        if (dataFromRedis.size() != unavailableProducts.size()) {
            log.warn("The number of results from Redis does not match the requested number");
            throw new RuntimeException("Incorrect number of results from Redis");
        }
    }

    private static Stream<SubstitutionCandidate> getSubstitutionCandidates(final JsonArray redisResponseChunk) {
        return redisResponseChunk
                .stream()
                .map(obj -> Json.decodeValue(obj.toString(), SubstitutionCandidate.class));
    }

    private static boolean isSubstitutionCandidateInStore(final SubstitutionCandidate substitutionCandidate, final String storeId) {
        if (storeId != null && substitutionCandidate.getStoreIds() != null) {
            return substitutionCandidate.getStoreIds().contains(storeId);
        }
        return true;
    }

    private static <T> Single<T> logError(final Throwable error) {
        if (error instanceof TimeoutException) {
            log.warn("Timeout connecting to Redis");
        }
        throw Exceptions.propagate(error);
    }

    private static List<String> getSubstitutionTpnbList(final JsonArray redisResponseChunk) {
        return SubstitutesRedisService.getSubstitutionCandidates(redisResponseChunk)
                .map(SubstitutionCandidate::getSubTpn)
                .collect(Collectors.toList());
    }

    private static List<String> filterSubTpnbsByStoreId(final JsonArray redisResponseChunk, final String storeId) {
        return SubstitutesRedisService.getSubstitutionCandidates(redisResponseChunk)
                .filter(sc -> SubstitutesRedisService.isSubstitutionCandidateInStore(sc, storeId))
                .map(SubstitutionCandidate::getSubTpn)
                .collect(Collectors.toList());
    }

    private static Substitution toSubstitution(final UnavailableProduct unavailableProduct, final String storeId,
            final JsonArray redisResponseChunk) {
        return Substitution.of(unavailableProduct.tpnb(), SubstitutesRedisService.filterSubTpnbsByStoreId(redisResponseChunk, storeId));
    }

    private static Substitution toSubstitution(final UnavailableProduct unavailableProduct, final JsonArray redisResponseChunk) {
        return Substitution.of(unavailableProduct.tpnb(), SubstitutesRedisService.getSubstitutionTpnbList(redisResponseChunk));
    }

    private static List<Substitution> asSubstitutions(final JsonArray dataFromRedis, final List<UnavailableProduct> unavailableProducts,
            final String storeId) {
        log.info("found these substitution {} for unavailable products {} and storeId {}", dataFromRedis.encode(), unavailableProducts,
                storeId);
        SubstitutesRedisService.checkForMissingResponses(dataFromRedis, unavailableProducts);

        final List<Substitution> result = IntStream.range(0, dataFromRedis.size())
                .mapToObj(i -> SubstitutesRedisService.toSubstitution(unavailableProducts.get(i), storeId, dataFromRedis.getJsonArray(i)))
                .collect(Collectors.toList());

        log.info("Returning substitutions {} for unavailable products {} with store id {}", result, unavailableProducts, storeId);
        return result;
    }

    private static List<Substitution> asSubstitutions(final JsonArray dataFromRedis, final List<UnavailableProduct> unavailableProducts) {
        log.info("found these substitution {} for unavailable products {}", dataFromRedis.encode(), unavailableProducts);
        SubstitutesRedisService.checkForMissingResponses(dataFromRedis, unavailableProducts);

        final List<Substitution> result = IntStream.range(0, dataFromRedis.size())
                .mapToObj(i -> SubstitutesRedisService.toSubstitution(unavailableProducts.get(i), dataFromRedis.getJsonArray(i)))
                .collect(Collectors.toList());

        log.info("Returning substitutions {} for unavailable products {}", result, unavailableProducts);
        return result;
    }

    @Override
    public Single<List<Substitution>> getSubstitutionsFor(final String storeId, final List<UnavailableProduct> unavailableProducts) {
        final Single<List<Substitution>> listSingle = this.subsNamespaceProvider
                .getRedisNamespaceForTpnbs(storeId, unavailableProducts)
                .flatMap(this::getSubstitutionsFromRedis)
                .map(jsonArray -> SubstitutesRedisService.asSubstitutions(jsonArray, unavailableProducts, storeId))
                .onErrorResumeNext(SubstitutesRedisService::logError);
        return LoggingUtils
                .logTiming(listSingle, "Getting Substitution for unavailable products " + unavailableProducts + " and storeId " + storeId);
    }

    @Override
    public Single<List<Substitution>> getSubstitutionsFor(final List<UnavailableProduct> unavailableProducts) {
        final Single<List<Substitution>> listSingle = this.subsNamespaceProvider
                .getRedisNamespaceForTpnbs(unavailableProducts)
                .flatMap(this::getSubstitutionsFromRedis)
                .map(jsonArray -> SubstitutesRedisService.asSubstitutions(jsonArray, unavailableProducts))
                .onErrorResumeNext(SubstitutesRedisService::logError);
        return LoggingUtils.logTiming(listSingle, "Getting Substitution for unavailable products " + unavailableProducts);
    }

    private Single<JsonArray> getSubstitutionsFromRedis(final List<String> redisKeys) {
        log.info("Getting substitutions from Redis for {}", redisKeys);
        return this.redisClient.rxMgetMany(redisKeys)
                .timeout(REDIS_FIND_TIMEOUT, TimeUnit.MILLISECONDS)
                .map(this.redisResponseMapper::mapSubstitutionsResponse);
    }
}
