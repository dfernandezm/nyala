package com.tesco.substitutions.infrastructure.adapter;

import io.vertx.core.json.JsonArray;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import rx.exceptions.Exceptions;

@Slf4j
public class RedisResponseMapper {

    private static final Long REDIS_FIND_TIMEOUT = 3000L;

    public Single<JsonArray> mapSingleSubstitutionsResponse(Single<String> asyncResponsePromise, long startTime){
        return setTimeoutAndErrorHandler(asyncResponsePromise)
                .map(redisResponse -> wrapResponseAsJsonArrayIfNeeded(redisResponse, r -> new JsonArray(transformRedisResponseToJsonArray(redisResponse)), startTime));
    }

    public Single<JsonArray> mapBulkSubstitutionsResponse(Single<JsonArray> asyncResponsePromise, long startTime){
        return setTimeoutAndErrorHandler(asyncResponsePromise)
                .map(redisResponse -> wrapResponseAsJsonArrayIfNeeded(redisResponse, Function.identity(), startTime));
    }

    private <T> JsonArray wrapResponseAsJsonArrayIfNeeded(T value, Function<T, JsonArray> mapper, long startTime){
        if (value == null) {
            log.info("Substitutions NOT found in {} ms", calculateTimeFrom(startTime));
            log.info("Returning empty substitutions");
            return new JsonArray();
        }
        log.info("Substitutions found in {} ms",(calculateTimeFrom(startTime)));
        return mapper.apply(value);
    }

    private <T> Single<T> setTimeoutAndErrorHandler(Single<T> single){
        return single.timeout(REDIS_FIND_TIMEOUT, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(this::logError);
    }

    private <T> Single<T> logError(Throwable error){
        if (error instanceof TimeoutException) {
            log.info("Timeout connecting to Redis");
        } else {
            log.warn("Error connecting to Redis", error);
        }
        throw Exceptions.propagate(error);
    }

    private long calculateTimeFrom(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private String transformRedisResponseToJsonArray(String redisResponse) {
        return "[" + redisResponse + "]";
    }
}
