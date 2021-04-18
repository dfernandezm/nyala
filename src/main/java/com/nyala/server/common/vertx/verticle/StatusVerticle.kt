package com.nyala.server.common.vertx.verticle

import com.nyala.server.common.shutdown.ShutdownUtils.Companion.stopVerticle
import com.nyala.server.common.vertx.redis.RedisConnectionChecker
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.redis.RedisOptions
import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.core.eventbus.Message
import io.vertx.rxjava.redis.RedisClient
import org.apache.http.entity.ContentType
import org.slf4j.LoggerFactory
import rx.Single

class StatusVerticle : AbstractVerticle() {

    companion object {
        const val STATUS_ADDRESS = "status"
    }

    private val LOGGER = LoggerFactory.getLogger(StatusVerticle::class.java)
    private val ALIVE_MESSAGE = "{\"message\" : \"alive\"} "
    private val REDIS_CONFIGURATION_JSON_KEY = "redisConfiguration"
    private lateinit var redisClient: RedisClient

    override fun start(startFuture: Future<Void?>) {
        initializeRedisClient()
        RedisConnectionChecker.checkForRedisConnection(redisClient)
        addRedisStatusConsumer()
        LOGGER.info("Status Verticle deployed")
        startFuture.complete()
    }

    private fun addRedisStatusConsumer() {
        vertx.eventBus().consumer<Any?>(STATUS_ADDRESS).handler { message: Message<Any?> ->
            redisCheckHandler(message)
        }
    }

    private fun redisCheckHandler(message: Message<Any?>) {
        val multiMap = addHeaders()
        Single.just(JsonObject()).flatMapCompletable { RedisConnectionChecker.verifyRedisIsUp(redisClient) }
                .subscribe({
                            message.reply(JsonObject(ALIVE_MESSAGE), DeliveryOptions().setHeaders(multiMap))
                        },
                        { error: Throwable ->
                            LOGGER.error("Error in status endpoint", error)
                            message.fail(502, error.message)
                        })
    }

    private fun addHeaders(): MultiMap? {
        val multiMap = MultiMap.caseInsensitiveMultiMap()
        multiMap.add(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
        return multiMap
    }

    private fun initializeRedisClient() {
        val redisConfiguration = context.config().getJsonObject(REDIS_CONFIGURATION_JSON_KEY)
        val redisOptions = RedisOptions(redisConfiguration)
        redisClient = RedisClient.create(vertx, redisOptions)
    }

    override fun stop(stopFuture: Future<Void?>) {
        stopVerticle(vertx, stopFuture, config(), Runnable {
            LOGGER.info("Stopping StatusVerticle")
            waitForRedisClientClose()
            stopFuture.complete()
        })
    }

    private fun waitForRedisClientClose() {
        redisClient.rxClose().toBlocking().value()
    }
}