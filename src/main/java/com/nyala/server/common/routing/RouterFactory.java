package com.nyala.server.common.routing;


import com.nyala.server.common.vertx.FailureExceptionHandler;
import io.vavr.collection.Array;
import io.vertx.core.Handler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;

import javax.inject.Inject;
import java.util.Set;

public class RouterFactory {
    private final Set<RoutesDefinition> routesDefinitions;
    private final Set<EventBusDefinition> eventBusDefinitions;
    private static final String APPLICATION_JSON = "application/json";
    private Handler<RoutingContext> loggerHandler;

    @Inject
    public RouterFactory(Set<RoutesDefinition> routesDefinitions, Set<EventBusDefinition> eventBusDefinitions) {
        this.routesDefinitions = routesDefinitions;
        this.eventBusDefinitions = eventBusDefinitions;
    }

    public Router globalRouter(Vertx vertx) {
        return (Router)Array.ofAll(this.routesDefinitions).foldLeft(this.jsonRouterBase(vertx), (router, routesDefinition) -> {
            return router.mountSubRouter(routesDefinition.mountPoint(), routesDefinition.setupRoutes(vertx));
        });
    }

    public void useLoggerHandler(Handler<RoutingContext> loggerHandler) {
        this.loggerHandler = loggerHandler;
    }

    private Router jsonRouterBase(Vertx vertx) {
        Router router = Router.router(vertx);
        this.addEventBusDefinitions(vertx, router);
        if (this.loggerHandler == null) {
            router.route().handler(LoggerHandler.create(LoggerFormat.TINY));
        } else {
            router.route().handler(this.loggerHandler);
        }

        router.route().handler(BodyHandler.create());
        router.route().consumes("application/json");
        router.route().produces("application/json");
        router.route().handler((context) -> {
            context.response().headers().add("Content-Type", "application/json");
            context.next();
        });
        router.route().failureHandler(new FailureExceptionHandler());
        return router;
    }

    private void addEventBusDefinitions(Vertx vertx, Router router) {
        if (this.eventBusDefinitions.size() > 0) {
            BridgeOptions bridgeOptions = (new BridgeOptions()).setReplyTimeout(1000L);
            this.eventBusDefinitions.forEach((eventBusDefinition) -> {
                bridgeOptions.addInboundPermitted((new PermittedOptions()).setAddress(eventBusDefinition.prepareConsumer().getAddress()));
            });
            router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(bridgeOptions));
            this.eventBusDefinitions.forEach((eventBusDefinition) -> {
                eventBusDefinition.prepareConsumer().setUpConsumer(vertx);
            });
        }

    }
}
