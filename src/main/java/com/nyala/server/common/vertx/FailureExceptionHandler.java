package com.nyala.server.common.vertx;

import com.nyala.server.common.errorhandling.ApiErrorException;
import io.vavr.API;
import io.vavr.Predicates;
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
        int httpStatusCode = (Integer) API.Match(throwable).of(new API.Match.Case[]{API.Case(API.$(Predicates.instanceOf(DecodeException.class)), () -> {
            return 400;
        }), API.Case(API.$(Predicates.instanceOf(ApiErrorException.class)), ApiErrorException::getStatusCode), API.Case(API.$(), () -> {
            LOGGER.error("UnexpectedFailure", throwable);
            return 500;
        })});
        String payload = Json.encode(throwable.getMessage());
        context.response().setStatusCode(httpStatusCode);
        context.response().end(payload);
    }
}
