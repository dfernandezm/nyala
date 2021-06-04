package com.nyala.server.infrastructure.di

import com.nyala.server.infrastructure.config.HttpServerModule
import io.vertx.rxjava.core.Vertx
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

object KoinDI {

    private var app: KoinApplication? = null

    @JvmStatic
    fun start(vertx: Vertx) {
        app = buildKoinApplication(vertx)
    }

    @JvmStatic
    fun stop() = synchronized(this) {
        app?.close()
        app = null
    }

    @JvmStatic
    fun get(): KoinApplication = app
            ?: error("Isolated KoinApplication for has not been started")

    // This is specific to one verticle, it should be populated by every verticle 'start'
    private fun buildKoinApplication(vertx: Vertx): KoinApplication {
        return koinApplication {
            modules(HttpServerModule(vertx).httpServerModule)
        }
    }
}


