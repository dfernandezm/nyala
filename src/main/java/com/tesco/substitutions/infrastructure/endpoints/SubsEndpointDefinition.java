package com.tesco.substitutions.infrastructure.endpoints;

import com.google.inject.Inject;
import com.tesco.personalisation.commons.routing.EndpointBuilder;
import com.tesco.personalisation.commons.routing.EndpointDefinition;
import com.tesco.personalisation.commons.routing.EndpointDsl;
import com.tesco.substitutions.application.handler.SubsHandler;

public class SubsEndpointDefinition implements EndpointDefinition {

    public  static final String SUBSTITUTES_PATH = "/substitutes";
    private final SubsHandler subsHandler;

    @Inject
    public SubsEndpointDefinition(final SubsHandler subsHandler) {
        this.subsHandler = subsHandler;
    }

    @Override
    public EndpointDsl.Endpoint prepare() {
        return EndpointBuilder.forPath(SUBSTITUTES_PATH)
                .GET()
                .then(this.subsHandler::substitutions);
    }
}
