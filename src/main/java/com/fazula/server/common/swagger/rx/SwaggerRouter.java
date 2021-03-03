package com.fazula.server.common.swagger.rx;

import com.fazula.server.common.shutdown.ShutdownUtils;
import com.github.phiz71.vertx.swagger.router.DefaultServiceIdResolver;
import com.github.phiz71.vertx.swagger.router.ServiceIdResolver;
import io.swagger.models.Swagger;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.Function;

public class SwaggerRouter {

    public static Router swaggerRouter(Router baseRouter, Swagger swagger, EventBus eventBus) {
        return swaggerRouter(baseRouter, swagger, eventBus, new DefaultServiceIdResolver(), (Function)null);
    }

    public static Router swaggerRouter(Router baseRouter, Swagger swagger, EventBus eventBus, ServiceIdResolver serviceIdResolver) {
        return swaggerRouter(baseRouter, swagger, eventBus, serviceIdResolver, (Function)null);
    }

    public static Router swaggerRouter(Router baseRouter, Swagger swagger, EventBus eventBus, ServiceIdResolver serviceIdResolver, Function<RoutingContext, DeliveryOptions> configureMessage) {
        io.vertx.ext.web.Router baseRouterDelegate = baseRouter.getDelegate();
        io.vertx.core.eventbus.EventBus eventBusDelegate = eventBus.getDelegate();
        ShutdownUtils.setEventBus(eventBusDelegate);
        Function<io.vertx.ext.web.RoutingContext, DeliveryOptions> configureMessageDelegate = (rc) -> {
            return (DeliveryOptions)configureMessage.apply(new RoutingContext(rc));
        };
        return new Router(com.fazula.server.common.swagger.core.SwaggerRouter.swaggerRouter(baseRouterDelegate, swagger, eventBusDelegate, serviceIdResolver, configureMessageDelegate));
    }
}
