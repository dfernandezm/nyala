package com.tesco.substitutions.infrastructure.adapter;

import com.tesco.substitutions.domain.model.SubstitutionCandidate;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.redis.RedisClient;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

public class HttpRedisService implements SubstitutionsService {

    private static final Long REDIS_FIND_TIMEOUT = 3000L;
    public static final String REDIS_KEYS_SUBS_NAMESPACE = "originalTpn_";
    private RedisClient redisClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRedisService.class);

    @Inject
    public HttpRedisService(@Named("redisClient") final RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Single<List<SubstitutionCandidate>> substitutionsFor(UnavailableProduct unavailableProduct) {
        long startTime = System.currentTimeMillis();
        Single<JsonArray> redisResult = redisClient.rxGet(addNamespaceToTpnb(unavailableProduct.tpnb().toString()))
				.timeout(REDIS_FIND_TIMEOUT, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(this::logError)
                .observeOn(Schedulers.computation())
                .map(redisReponse -> {
                    if (redisReponse == null) {
                        LOGGER.info("Substitutions NOT found in {} ms", calculateTimeFrom(startTime));
                        LOGGER.info("Returning empty substitution candidates");
                        return new JsonArray();
                    } else {
                        LOGGER.info("Substitutions found in {} ms",(calculateTimeFrom(startTime)));
                        return new JsonArray(transformRedisResponseToJsonArray(redisReponse));
                    }
                });

        return redisResult.map(HttpRedisService::asRecommendationCandidates);
    }

    private long calculateTimeFrom(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private String addNamespaceToTpnb(String tpnb) {
        return REDIS_KEYS_SUBS_NAMESPACE + tpnb;
    }

    private String transformRedisResponseToJsonArray(String redisResponse) {
        return "[" + redisResponse + "]";
    }

    private static List<SubstitutionCandidate>  asRecommendationCandidates(
            final JsonArray jsonArray) {
        return  jsonArray.stream().map(subtpnb -> SubstitutionCandidate.of(new Long(subtpnb.toString()))).collect(Collectors.toList());

    }

    private Single<String> logError(Throwable error) {
        if (error instanceof TimeoutException) {
            LOGGER.info("Timeout connecting to Redis");
        } else {
            LOGGER.warn("Error connecting to Redis", error);
        }
        throw Exceptions.propagate(error);
    }
}
