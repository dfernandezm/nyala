package com.fazula.server.application.handler;


import com.fazula.server.common.vertx.verticle.StatusVerticle;
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

                    routingContext
                            .response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(HttpStatus.SC_OK).end(statusMessage.body().encodePrettily());
                }, error -> {
                    log.error("Status endpoint failure: {}", error.getCause().getMessage());
                    routingContext
                            .response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE)
                            .end(error.getCause().getMessage());
                });
    }
}
