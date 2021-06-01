package com.nyala.server.infrastructure.di

import com.nyala.server.infrastructure.config.HttpServerModule
import io.vertx.rxjava.core.Vertx

import org.koin.core.context.startKoin

class DependencyInjection {
    companion object {
        private var koinStarted = false
        fun startKoinDi(vertx: Vertx) {
            if (!koinStarted) {
                startKoin {
                    // use Koin logger
                    printLogger()
                    // declare modules
                    modules(HttpServerModule(vertx).httpServerModule)
                }
                koinStarted = true
            }
        }

        fun startKoinOnly() {
            if (!koinStarted) {
                startKoin {
                    // use Koin logger
                    printLogger()
                }
                koinStarted = true
            }
        }
    }
}