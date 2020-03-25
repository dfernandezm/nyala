package com.tesco.substitutions.infrastructure.endpoints;

import com.google.inject.Inject;
import com.tesco.substitutions.application.handler.StatusEndpointHandler;
import com.tesco.substitutions.commons.routing.EndpointBuilder;
import com.tesco.substitutions.commons.routing.EndpointDefinition;
import com.tesco.substitutions.commons.routing.EndpointDsl;

public class StatusEndpointDefinition implements EndpointDefinition {

    public static final String STATUS_PATH = "/_status";
    private final StatusEndpointHandler statusEndpointHandler;

    @Inject
    public StatusEndpointDefinition(final StatusEndpointHandler statusEndpointHandler) {
        this.statusEndpointHandler = statusEndpointHandler;
    }

    @Override
    public EndpointDsl.Endpoint prepare() {
        return EndpointBuilder.forPath(STATUS_PATH)
                .GET()
                .then(this.statusEndpointHandler::status);
    }
}
