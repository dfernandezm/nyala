package com.nyala.server.common.vertx.redis;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Completable;
import rx.Single;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RedisConnectionChecker {

    private static final int DEFAULT_TIMEOUT_PING_REDIS_MILLISECONDS = 2000;
    private static final int DEFAULT_RETRY_TIME_REDIS_SECONDS = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConnectionChecker.class);

    public static Completable verifyRedisIsUp(RedisClient redisClient) {
        return redisClient.rxPing()
                .timeout(5000L, TimeUnit.MILLISECONDS)
                .flatMapCompletable((result) -> {
                    LOGGER.debug("Redis is up: " + result);
                    return Completable.complete();
                }).doOnError((error) -> {
                    throw new RuntimeException("Bad Gateway:cache");
                });
    }

    public static void checkForRedisConnection(RedisClient redisClient) {
        LOGGER.info("Attempting Redis connection");
        pingRedisContinuously(redisClient);
    }

    private static void pingRedisContinuously(RedisClient redisClient) {
        Single.just(new JsonObject())
                .observeOn(Schedulers.io())
                .map(r -> redisClient)
                .observeOn(Schedulers.io())
                .flatMap((r) -> redisClient.rxPing())
                .timeout(DEFAULT_TIMEOUT_PING_REDIS_MILLISECONDS, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(RedisConnectionChecker::logAndPropagateError)
                .retryWhen( errors -> errors.delay(DEFAULT_RETRY_TIME_REDIS_SECONDS, TimeUnit.SECONDS))
                .subscribe( result -> LOGGER.info("Successfully established Redis connection"),
                            error -> LOGGER.info("Error pinging Redis", error)
                );
    }

    private static Single<String> logAndPropagateError(Throwable err) {
        if (err instanceof TimeoutException) {
            LOGGER.info("Timeout connecting to Redis");
        } else {
            LOGGER.info("Failed to connect to Redis", err);
        }

        throw Exceptions.propagate(err);
    }
}
