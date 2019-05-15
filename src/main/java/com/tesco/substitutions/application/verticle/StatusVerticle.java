package com.tesco.substitutions.application.verticle;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.tesco.personalisation.commons.shutdown.ShutdownUtils;
import com.tesco.substitutions.infrastructure.module.InfrastructureBinder;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.redis.RedisClient;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Completable;
import rx.Single;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

public class StatusVerticle extends AbstractVerticle {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Inject
    @Named("redisConfiguration")
    private JsonObject redisConfiguration;

    private RedisClient redisClient;

    @Override
    public void start(final Future<Void> startFuture) {
        Guice.createInjector(new InfrastructureBinder(this.vertx)).injectMembers(this);

        final RedisOptions redisOptions = new RedisOptions(this.redisConfiguration);
        this.checkForRedisConnection(redisOptions);

        this.vertx.eventBus().<JsonObject>consumer("status").handler(message -> {
            final MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();
            multiMap.add("content-type", "application/json");

            // Wrap in Observable so errors bubble up properly
            Single.just(new JsonObject())
                    .flatMapCompletable(r -> this.verifyRedisIsUp())
                    .subscribe(() -> message.reply(new JsonObject().put("message", "alive"), new DeliveryOptions().setHeaders(multiMap))
                            , error -> {
                                this.LOGGER.error("Error in status endpoint", error);
                                message.fail(502, error.getMessage());
                            });
        });

        this.LOGGER.info("Status Verticle deployed");
        startFuture.complete();
    }

    private Completable verifyRedisIsUp() {
        return this.redisClient.rxPing()
                .timeout(5000, TimeUnit.MILLISECONDS)
                .flatMapCompletable(result -> {
                    this.LOGGER.debug("Redis is up: " + result);
                    return Completable.complete();
                })
                .doOnError(error -> {
                    throw new RuntimeException("Bad Gateway:cache");
                });
    }

    private void checkForRedisConnection(final RedisOptions redisConfig) {
        this.LOGGER.info("Attempting Redis connection");
        this.pingRedisContinuously(redisConfig);
    }

    private void pingRedisContinuously(final RedisOptions redisConfig) {
        Single.just(new JsonObject())
                .observeOn(Schedulers.io())
                .map(r -> this.redisClient = RedisClient.create(this.vertx, redisConfig))
                .observeOn(Schedulers
                        .io()) // In case immediate downstream operation blocks (pinging redis), we move it out of eventloop to dedicated one
                .flatMap(r -> this.redisClient.rxPing())
                .timeout(2000, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(this::logAndPropagateError)
                .retryWhen(errors -> errors.delay(3, TimeUnit.SECONDS))
                .subscribe(result -> this.LOGGER.info("Successfully established Redis connection"),
                        error -> this.LOGGER.info("Error pinging Redis", error));
    }

    private Single<String> logAndPropagateError(final Throwable err) {
        if (err instanceof TimeoutException) {
            this.LOGGER.info("Timeout connecting to Redis");
        } else {
            this.LOGGER.info("Failed to connect to Redis", err);
        }
        throw Exceptions.propagate(err);
    }

    @Override
    public void stop(final Future<Void> stopFuture) {
        ShutdownUtils.stopVerticle(this.vertx, stopFuture, this.config(), () -> {
            this.LOGGER.info("Stopping StatusVerticle");
            this.redisClient.rxClose().subscribe();
            stopFuture.complete();
        });
    }
}
