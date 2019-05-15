package com.tesco.substitutions.infrastructure.endpoints;

import com.tesco.personalisation.commons.routing.EndpointDefinition;
import com.tesco.personalisation.commons.routing.RoutesDefinition;
import io.vavr.collection.Array;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

public class SubstitutionsRoutes implements RoutesDefinition {

    public static final String SUBSTITUTIONS_MOUNT_POINT = "/substitutions/";
    private final List<EndpointDefinition> endpointDefinitions;

    @Inject
    public SubstitutionsRoutes(@Named("substitutionsEndpoints") final List<EndpointDefinition> endpointDefinitions) {
        this.endpointDefinitions = endpointDefinitions;
    }

    @Override
    public String mountPoint() {
        return SUBSTITUTIONS_MOUNT_POINT;
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
