package com.nyala.server.infrastructure.config;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;

import javax.inject.Singleton;

@Factory
public class FazulaConfig {

    private final Vertx vertx;
    private final Context context;

    public FazulaConfig(Vertx vertx) {
        this.vertx = vertx;
        this.context = vertx.getOrCreateContext();
    }

    @Bean
    @Singleton
    public RedisClient redisClient() {
        final JsonObject redisConfiguration = this.context.config().getJsonObject("redisConfiguration");
        final RedisOptions redisOptions = new RedisOptions(redisConfiguration);
        return RedisClient.create(this.vertx, redisOptions);
    }
}