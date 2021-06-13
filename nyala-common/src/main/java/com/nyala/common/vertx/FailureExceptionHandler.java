package com.nyala.common.vertx;

import com.nyala.common.errorhandling.ApiErrorException;
import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureExceptionHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FailureExceptionHandler.class.getName());

    public FailureExceptionHandler() {
    }

    public void handle(RoutingContext context) {
        Throwable throwable = context.failure();

        int statusCode = 500;
        if (throwable instanceof DecodeException) {
            statusCode = 400;
        }

        if (throwable instanceof ApiErrorException) {
            statusCode = 500;
        }

        String payload = Json.encode(throwable.getMessage());
        context.response().setStatusCode(statusCode);
        context.response().end(payload);
    }
}
