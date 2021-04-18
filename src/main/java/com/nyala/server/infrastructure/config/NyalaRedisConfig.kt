package com.nyala.server.infrastructure.config

import io.vertx.redis.RedisOptions
import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.redis.RedisClient

class NyalaRedisConfig(private val vertx: Vertx) {

    private var context: Context? = vertx.orCreateContext

    fun provideRedisClient(): RedisClient? {
        val redisConfiguration = context!!.config().getJsonObject("redisConfiguration")
        val redisOptions = RedisOptions(redisConfiguration)
        return RedisClient.create(vertx, redisOptions)
    }
}