package com.tesco.substitutions.application.handler;

import com.tesco.personalisation.commons.vertx.verticle.StatusVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class StatusEndpointHandler {

    public void status(final RoutingContext routingContext) {
        routingContext.vertx().eventBus().<JsonObject>rxSend(StatusVerticle.STATUS_ADDRESS, new JsonObject())
                .subscribe(statusMessage -> {
                    log.info("Successfully status returned");
                    routingContext.response().setStatusCode(HttpStatus.SC_OK).end(statusMessage.body().encodePrettily());
                }, error -> {
                    log.error("Status endpoint failure: {}", error.getCause().getMessage());
                    routingContext.response().setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE).end(error.getCause().getMessage());
                });
    }
}
