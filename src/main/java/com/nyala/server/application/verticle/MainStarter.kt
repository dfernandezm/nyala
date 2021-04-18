package com.nyala.server.application.verticle

import com.nyala.server.common.vertx.MultiDeploySetupVerticle
import io.vertx.core.Future

class MainStarter : MultiDeploySetupVerticle() {
    override fun stop(stopFuture: Future<Void>) {
        super.stop(stopFuture, config())
    }
}