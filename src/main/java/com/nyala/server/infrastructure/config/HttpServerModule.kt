package com.nyala.server.infrastructure.config

import com.nyala.server.application.handler.StatusEndpointHandler
import io.vertx.redis.RedisOptions
import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.redis.RedisClient
import org.koin.dsl.module

// -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory

class HttpServerModule(private val vertx: Vertx) {

    private var context: Context = vertx.orCreateContext

    val httpServerModule = module {

        single { provideRedisClient() }
        single { StatusEndpointHandler() }

    }

    private fun provideRedisClient(): RedisClient {
        val redisConfiguration = context.config().getJsonObject("redisConfiguration")
        val redisOptions = RedisOptions(redisConfiguration)
        return RedisClient.create(vertx, redisOptions)
    }
}