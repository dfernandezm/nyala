package com.nyala.server.application.verticle

import com.nyala.server.common.vertx.MultiDeploySetupVerticle
import com.nyala.server.infrastructure.di.DependencyInjection
import io.vertx.core.Future
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory

class MainStarter : MultiDeploySetupVerticle() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun start(startFuture: Future<Void>) {
        log.info("Starting Koin Application")
        DependencyInjection.startKoinOnly()
        log.info("Starting deployment of verticles")
        super.start(startFuture)
    }
    override fun stop(stopFuture: Future<Void>) {
        super.stop(stopFuture, config())
    }
}