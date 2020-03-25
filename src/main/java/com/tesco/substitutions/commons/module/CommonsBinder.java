package com.tesco.substitutions.commons.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;

public class CommonsBinder extends AbstractModule {
    private Context context;

    public CommonsBinder(Vertx vertx) {
        this.context = vertx.getOrCreateContext();
    }

    protected void configure() {
    }

    @Provides
    @Singleton
    @Named("redisConfiguration")
    public JsonObject redisConfiguration() {
        return this.context.config().getJsonObject("redisConfiguration");
    }
}