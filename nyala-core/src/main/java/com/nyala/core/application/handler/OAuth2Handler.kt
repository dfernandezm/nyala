package com.nyala.core.application.handler

import com.nyala.core.application.verticle.HttpServerVerticle
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.Vertx

import io.vertx.rxjava.ext.web.RoutingContext
import org.slf4j.LoggerFactory

class OAuth2Handler(private val vertx: Vertx): Handler<RoutingContext> {

    private val authUrlEventBusRoute = "oauth2.authUrl";

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    /**
     * Handle:
     *
     * <pre>
     * POST /oauth2/authUrl
     * {
     *   "oauth2Client": {
     *      "clientId": "xxx",
     *      "clientSecret": "yyy"
     *   }
     * }
     * </pre>
     *
     *
     */
    override fun handle(routingContext: RoutingContext) {
        val url = routingContext.request().absoluteURI()

        if (url.contains("/authUrl")) {
            val oauth2UrlRequestJson = routingContext.bodyAsJson
            val response = routingContext.response()
            vertx.eventBus().rxSend<JsonObject>(authUrlEventBusRoute, oauth2UrlRequestJson)
                    .subscribe({ message ->
                        log.info("Sent oauth2Request")
                        response
                                .putHeader("content-type", "application/json")
                                .end(Json.encodePrettily(message.body()))
                    }, {
                        log.error("Error occurred", it)
                        response.setStatusCode(500).end(Json.encodePrettily(JsonObject(it.message)))
                    })
        }
    }

}