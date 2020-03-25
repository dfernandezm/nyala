package com.tesco.substitutions.commons.routing;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public interface RoutesDefinition {
    String mountPoint();

    Router setupRoutes(Vertx var1);
}
