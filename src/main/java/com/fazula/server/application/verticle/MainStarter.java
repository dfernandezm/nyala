package com.fazula.server.application.verticle;


import com.fazula.server.common.vertx.MultiDeploySetupVerticle;
import io.vertx.core.Future;

public class MainStarter extends MultiDeploySetupVerticle {

    @Override
    public void stop(final Future<Void> stopFuture) {
        this.stop(stopFuture, this.config());
    }
}
