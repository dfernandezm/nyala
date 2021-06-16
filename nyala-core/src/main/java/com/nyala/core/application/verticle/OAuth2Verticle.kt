package com.nyala.core.application.verticle

import com.nyala.core.domain.model.Channel
import com.nyala.core.infrastructure.di.IsolatedKoinVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.AbstractVerticle


import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory

class OAuth2Verticle: IsolatedKoinVerticle() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun getAppName(): String {
        return "Channels"
    }

    override fun start(startFuture: Future<Void>?) {
       //TODO: EventBus consumers for OAuth2 Commands
    }

    private fun handleGetChannel(channelId: String): JsonObject {
        log.info("Channel ID requested {}", channelId)
        val channel = Channel(name = "Cuatro HD", country = "ES")
        return JsonObject().put("channel", JsonObject.mapFrom(channel))
    }
}