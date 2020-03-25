package com.tesco.substitutions.commons.routing;

import com.tesco.personalisation.commons.routing.EndpointDsl.Endpoint;
import com.tesco.personalisation.commons.routing.EndpointDsl.EndpointWithoutMethod;
import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import java.util.function.Consumer;

public class EndpointBuilder implements EndpointWithoutMethod, Endpoint {
    private final String path;
    private Consumer<RoutingContext> handler = null;
    private EndpointBuilder.Method method;

    private EndpointBuilder(String path) {
        this.path = path;
    }

    public static EndpointWithoutMethod forPath(String path) {
        return new EndpointBuilder(path);
    }

    public Endpoint GET() {
        this.method = (router) -> {
            return router.get(this.path);
        };
        return this;
    }

    public Endpoint POST() {
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

    public Endpoint then(Consumer<RoutingContext> handler) {
        this.handler = handler;
        return this;
    }

    private interface Method {
        Route on(Router var1);
    }
}
