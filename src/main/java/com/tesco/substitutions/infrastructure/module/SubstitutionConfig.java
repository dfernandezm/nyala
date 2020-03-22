package com.tesco.substitutions.infrastructure.module;

import com.google.common.collect.ImmutableList;
import com.tesco.personalisation.commons.routing.EndpointDefinition;
import com.tesco.personalisation.commons.routing.RouterFactory;
import com.tesco.personalisation.commons.routing.RoutesDefinition;
import com.tesco.substitutions.application.handler.StatusEndpointHandler;
import com.tesco.substitutions.infrastructure.endpoints.StatusEndpointDefinition;
import com.tesco.substitutions.infrastructure.endpoints.SubstitutionsRoutes;
import io.micronaut.context.annotation.Bean;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;

import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SubstitutionConfig {

    private final Vertx vertx;
    private final Context context;

    public SubstitutionConfig(Vertx vertx) {
        this.vertx = vertx;
        this.context = vertx.getOrCreateContext();
    }

//    public SubstitutionConfig() {
//        this.vertx = Vertx.vertx();
//        this.context = vertx.getOrCreateContext();
//    }

    @Bean
    public RedisClient redisClient() {
        final JsonObject redisConfiguration = this.context.config().getJsonObject("redisConfiguration");
        final RedisOptions redisOptions = new RedisOptions(redisConfiguration);
        return RedisClient.create(this.vertx, redisOptions);
    }

    //final SubsEndpointDefinition subsEndpointDefinition
    @Bean
    public List<EndpointDefinition> endpointDefinitions(final StatusEndpointDefinition statusEndpointDefinition) {
        return ImmutableList.of(statusEndpointDefinition);
    }

    @Bean
    public Set<RoutesDefinition> routesDefinitions(List<EndpointDefinition> endpointDefinitions) {
        Set<RoutesDefinition> routes = new HashSet<>();
        routes.add(new SubstitutionsRoutes(endpointDefinitions));
       return routes;
    }

    @Bean
    @Named("routerFactoryMine")
    public RouterFactory routerFactory(StatusEndpointHandler statusEndpointHandler /*SubsHandler subsHandler*/) {
        StatusEndpointDefinition statusEndpointDefinition = new StatusEndpointDefinition(statusEndpointHandler);
        //SubsEndpointDefinition subsEndpointDefinition = new SubsEndpointDefinition(subsHandler);
        List<EndpointDefinition> endpointDefinitions = ImmutableList.of(statusEndpointDefinition);
        Set<RoutesDefinition> routesDefinitions = new HashSet<>();
        routesDefinitions.add(new SubstitutionsRoutes(endpointDefinitions));
        return new RouterFactory(routesDefinitions, new HashSet<>());
    }
}
