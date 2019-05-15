package com.tesco.substitutions.infrastructure.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.tesco.personalisation.commons.module.CommonsBinder;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;

public class InfrastructureBinder extends AbstractModule {

    private final Vertx vertx;
    private final Context context;

    public InfrastructureBinder(final Vertx vertx) {
        this.vertx = vertx;
        this.context = vertx.getOrCreateContext();
    }

    @Override
    protected void configure() {
        this.install(new CommonsBinder(this.vertx));
    }

    @Provides
    @Singleton
    @Named("redisClient")
    public RedisClient redisClient() {
        final JsonObject redisConfiguration = this.context.config().getJsonObject("redisConfiguration");
        final RedisOptions redisOptions = new RedisOptions(redisConfiguration);
        return RedisClient.create(this.vertx, redisOptions);
    }
}
