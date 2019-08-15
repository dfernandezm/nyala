package com.tesco.substitutions.infrastructure.endpoints;

import com.google.inject.Inject;
import com.tesco.personalisation.commons.routing.EndpointBuilder;
import com.tesco.personalisation.commons.routing.EndpointDefinition;
import com.tesco.personalisation.commons.routing.EndpointDsl.Endpoint;
import com.tesco.substitutions.application.handler.SubsHandler;

public class BulkSubsEndpointDefinition implements EndpointDefinition {

    public static final String BULK_SUBSTITUTES_PATH = "/substitutes";
    private final SubsHandler subsHandler;

    @Inject
    public BulkSubsEndpointDefinition(SubsHandler subsHandler) {
        this.subsHandler = subsHandler;
    }

    @Override
    public Endpoint prepare() {
        return EndpointBuilder.forPath(BULK_SUBSTITUTES_PATH)
                .POST()
                .then(this.subsHandler::bulkSubstitutions);
    }
}
