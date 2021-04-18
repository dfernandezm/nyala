package com.nyala.server.application.verticle

import com.nyala.server.domain.model.Channel
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory

class ChannelsVerticle: io.vertx.reactivex.core.AbstractVerticle(), KoinComponent {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }


    override fun start(startFuture: Future<Void?>?) {
        vertx.eventBus().consumer<JsonObject>("channel")
        { m ->
            val channelId = m.body().getString("channelId")
            log.info("Received channel {}", channelId)
            m.reply(handleGetChannels(channelId))
            //TODO: message.fail on error
        }
    }

    private fun handleGetChannels(channelId: String): JsonObject {
        log.info("ChannelId", channelId)
        val channel = Channel.builder()
                .country("ES")
                .name("Cuatro HD")
                .build()
        return JsonObject().put("channel", JsonObject.mapFrom(channel))
    }
}