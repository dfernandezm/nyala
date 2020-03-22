package com.tesco.substitutions.infrastructure.endpoints;

import com.tesco.personalisation.commons.routing.EndpointDefinition;
import com.tesco.personalisation.commons.routing.RoutesDefinition;
import io.vavr.collection.Array;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

import java.util.List;

public class SubstitutionsRoutes implements RoutesDefinition {

    public static final String SUBSTITUTIONS_MODULE_BASE_PATH = "/substitution/v1";

    private final List<EndpointDefinition> endpointDefinitions;

    public SubstitutionsRoutes(final List<EndpointDefinition> endpointDefinitions) {
        this.endpointDefinitions = endpointDefinitions;
    }

    @Override
    public String mountPoint() {
        return SUBSTITUTIONS_MODULE_BASE_PATH;
    }

    @Override
    public Router setupRoutes(final Vertx vertx) {
        return Array
                .ofAll(this.endpointDefinitions)
                .flatMap(EndpointDefinition::prepareEndPoints)
                .foldLeft(Router.router(vertx),
                        (router, uriDefinition) -> uriDefinition.registerOn(router));

    }

}
