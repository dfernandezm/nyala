package com.nyala.core.application.verticle

import com.nyala.core.application.Oauth2UrlRequest
import com.nyala.core.domain.model.Channel
import com.nyala.core.infrastructure.di.IsolatedKoinVerticle
import io.vertx.core.Future
import io.vertx.core.json.Json
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
        return "Oauth2Verticle"
    }

    override fun start(startFuture: Future<Void>?) {
        vertx.eventBus().consumer<JsonObject>("oauth2.authUrl") { message ->
            val oauth2UrlRequest = message.body().mapTo(Oauth2UrlRequest::class.java)
            log.info("Received - {}", oauth2UrlRequest)
            val resp = JsonObject().put("authUrl", "https://google.com")
            message.rxReply<JsonObject>(resp).subscribe ({
                log.info("Responding...")
            }, {
                log.error("Error occurred", it)
            })
        }
    }
}