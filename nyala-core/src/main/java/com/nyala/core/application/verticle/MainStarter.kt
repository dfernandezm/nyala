package com.nyala.core.application.verticle

import com.nyala.common.vertx.MultiDeploySetupVerticle
import io.vertx.core.Future
import org.slf4j.LoggerFactory

class MainStarter : MultiDeploySetupVerticle() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun start(startFuture: Future<Void>) {
        log.info("Starting deployment of verticles")
        super.start(startFuture)
    }
    override fun stop(stopFuture: Future<Void>) {
        super.stop(stopFuture, config())
    }
}