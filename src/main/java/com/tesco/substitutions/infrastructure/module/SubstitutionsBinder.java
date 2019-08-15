package com.tesco.substitutions.infrastructure.module;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.tesco.personalisation.commons.routing.EndpointDefinition;
import com.tesco.personalisation.commons.routing.EventBusDefinition;
import com.tesco.personalisation.commons.routing.RoutesDefinition;
import com.tesco.substitutions.infrastructure.adapter.SubstitutesRedisService;
import com.tesco.substitutions.infrastructure.endpoints.BulkSubsEndpointDefinition;
import com.tesco.substitutions.infrastructure.endpoints.SubsEndpointDefinition;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import com.tesco.substitutions.infrastructure.endpoints.StatusEndpointDefinition;
import com.tesco.substitutions.infrastructure.endpoints.SubstitutionsRoutes;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubstitutionsBinder extends AbstractModule {

    private final Vertx vertx;
    private final Context context;

    public SubstitutionsBinder(final Vertx vertx) {
        this.vertx = vertx;
        this.context = vertx.getOrCreateContext();
    }

    @Override
    protected void configure() {
        this.bind(Vertx.class).toInstance(this.vertx);
        this.bind(SubstitutionsService.class).to(SubstitutesRedisService.class);
        this.bindRoutes();
    }

    @Provides
    @Singleton
    @Named("substitutionsEndpoints")
    static List<EndpointDefinition> endpointDefinitions(final StatusEndpointDefinition statusEndpointDefinition,
            final SubsEndpointDefinition subsEndpointDefinition, final BulkSubsEndpointDefinition bulkSubsEndpointDefinition) {
        return ImmutableList.of(statusEndpointDefinition, subsEndpointDefinition, bulkSubsEndpointDefinition);
    }

    // Empty as RouterFactory expects the eventBusDefinitions as well
    @Provides
    @Singleton
    static Set<EventBusDefinition> eventBusDefinitions() {
        return new HashSet<>();
    }



    private void bindRoutes() {
        final Multibinder<RoutesDefinition> routesBinder = Multibinder.newSetBinder(this.binder(), RoutesDefinition.class);
        routesBinder.addBinding().to(SubstitutionsRoutes.class);

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
