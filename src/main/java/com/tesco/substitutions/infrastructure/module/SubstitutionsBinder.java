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
import com.tesco.substitutions.application.handler.SubsEndpointDefinition;
import com.tesco.substitutions.infrastructure.endpoints.StatusEndpointDefinition;
import com.tesco.substitutions.infrastructure.endpoints.SubstitutionsRoutes;
import io.vertx.rxjava.core.Vertx;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubstitutionsBinder extends AbstractModule {

    private final Vertx vertx;

    public SubstitutionsBinder(final Vertx vertx) {
        this.vertx = vertx;
    }

    @Provides
    @Singleton
    @Named("substitutionsEndpoints")
    static List<EndpointDefinition> endpointDefinitions(final StatusEndpointDefinition statusEndpointDefinition,
            final SubsEndpointDefinition subsEndpointDefinition) {
        return ImmutableList.of(statusEndpointDefinition, subsEndpointDefinition);
    }

    // Empty as RouterFactory expects the eventBusDefinitions as well
    @Provides
    @Singleton
    static Set<EventBusDefinition> eventBusDefinitions() {
        return new HashSet<>();
    }

    @Override
    protected void configure() {
        this.bind(Vertx.class).toInstance(this.vertx);
        this.bindRoutes();
    }

    private void bindRoutes() {
        final Multibinder<RoutesDefinition> routesBinder = Multibinder.newSetBinder(this.binder(), RoutesDefinition.class);
        routesBinder.addBinding().to(SubstitutionsRoutes.class);

    }
}
