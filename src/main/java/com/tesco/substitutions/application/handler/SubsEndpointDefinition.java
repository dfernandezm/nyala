package com.tesco.substitutions.application.handler;

import com.google.inject.Inject;
import com.tesco.personalisation.commons.routing.EndpointBuilder;
import com.tesco.personalisation.commons.routing.EndpointDefinition;
import com.tesco.personalisation.commons.routing.EndpointDsl;

public class SubsEndpointDefinition implements EndpointDefinition {

    private static final String PATH = "/substitutes";
    private final SubsHandler subsHandler;

    @Inject
    public SubsEndpointDefinition(final SubsHandler subsHandler) {
        this.subsHandler = subsHandler;
    }

    @Override
    public EndpointDsl.Endpoint prepare() {
        return EndpointBuilder.forPath(PATH)
                .GET()
                .then(this.subsHandler::substitutions);
    }
}
