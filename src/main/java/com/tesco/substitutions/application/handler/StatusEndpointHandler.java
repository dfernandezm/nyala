package com.tesco.substitutions.application.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusEndpointHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public void status(final RoutingContext routingContext) {
        routingContext.vertx().eventBus().<JsonObject>rxSend("status", new JsonObject())
                .subscribe(statusMessage -> {
                    // successful status
                    this.LOGGER.info("Successfully status returned");
                    routingContext.response().setStatusCode(HttpStatus.SC_OK).end(statusMessage.body().encodePrettily());
                }, error -> {
                    this.LOGGER.error("Status endpoint failure: ", error.getCause().getMessage());
                    routingContext.response().setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE).end(error.getCause().getMessage());
                });
    }
}
