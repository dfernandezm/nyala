package com.nyala.core.application.handler;

import com.nyala.core.application.verticle.StatusVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
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
                    log.error("Status endpoint failure", error);
                    routingContext
                            .response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE)
                            .end(new JsonObject().put("message", "Status endpoint failure").encodePrettily());
                });
    }
}
