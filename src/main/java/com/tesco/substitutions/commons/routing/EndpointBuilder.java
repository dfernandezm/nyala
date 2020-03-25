package com.tesco.substitutions.commons.routing;


import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.Consumer;

public class EndpointBuilder implements EndpointDsl.EndpointWithoutMethod, EndpointDsl.Endpoint {
    private final String path;
    private Consumer<RoutingContext> handler = null;
    private EndpointBuilder.Method method;

    private EndpointBuilder(String path) {
        this.path = path;
    }

    public static EndpointDsl.EndpointWithoutMethod forPath(String path) {
        return new EndpointBuilder(path);
    }

    public EndpointDsl.Endpoint GET() {
        this.method = (router) -> {
            return router.get(this.path);
        };
        return this;
    }

    public EndpointDsl.Endpoint POST() {
        this.method = (router) -> {
            return router.post(this.path);
        };
        return this;
    }

    public Router registerOn(Router router) {
        Route var10000 = this.method.on(router);
        Consumer var10001 = this.handler;
        var10000.handler(var10001::accept);
        return router;
    }

    public EndpointDsl.Endpoint then(Consumer<RoutingContext> handler) {
        this.handler = handler;
        return this;
    }

    private interface Method {
        Route on(Router var1);
    }
}
