//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.fazula.server.common.routing;

import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import java.util.function.Consumer;

public class EndpointDsl {
    public EndpointDsl() {
    }

    public interface CanRegisterRoute {
        Router registerOn(Router var1);
    }

    public interface Endpoint extends EndpointDsl.CanRegisterRoute {
        EndpointDsl.Endpoint then(Consumer<RoutingContext> var1);
    }

    public interface EndpointWithoutMethod {
        EndpointDsl.Endpoint GET();

        EndpointDsl.Endpoint POST();
    }
}
