package com.tesco.substitutions.application.verticle;


import com.tesco.personalisation.commons.vertx.MultiDeploySetupVerticle;
import io.vertx.core.Future;

public class MainStarter extends MultiDeploySetupVerticle {

    @Override
    public void stop(final Future<Void> stopFuture) {
        this.stop(stopFuture, this.config());
    }
}
