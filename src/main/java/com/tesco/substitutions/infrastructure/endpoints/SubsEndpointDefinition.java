package com.tesco.substitutions.infrastructure.endpoints;

import com.google.inject.Inject;
import com.tesco.substitutions.application.handler.SubsHandler;
import com.tesco.substitutions.commons.routing.EndpointBuilder;
import com.tesco.substitutions.commons.routing.EndpointDefinition;
import com.tesco.substitutions.commons.routing.EndpointDsl;

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
                .POST()
                .then(this.subsHandler::substitutions);
    }
}
