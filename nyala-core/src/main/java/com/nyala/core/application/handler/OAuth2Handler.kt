package com.nyala.core.application.handler

import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.eventbus.Message
import io.vertx.rxjava.core.http.HttpServerResponse

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
     */
    override fun handle(routingContext: RoutingContext) {
        val url = routingContext.request().absoluteURI()
        if (url.contains("/authUrl")) {
            val oauth2UrlRequestJson = routingContext.bodyAsJson
            val response = routingContext.response()

            // RxSend passes internally a replyHandler, so that needs to be replied to,
            // otherwise the call hangs even after the response was sent
            vertx.eventBus().rxSend<JsonObject>(authUrlEventBusRoute, oauth2UrlRequestJson)
                    .subscribe ({ reply ->
                        replyToClose(reply)
                        writeResponseAsJson(response, reply)
                    }, { error ->
                        log.error("Error occurred", error)
                        sendErrorCode(response, 500, error)
                    })
        }
    }

    private fun sendErrorCode(response: HttpServerResponse, statusCode: Int, error: Throwable) {
        response.setStatusCode(statusCode).end(Json.encodePrettily(JsonObject(error.message)))
    }

    private fun writeResponseAsJson(response: HttpServerResponse, reply: Message<JsonObject>) {
        response
                .putHeader("content-type", "application/json")
                .end(Json.encodePrettily(reply.body()))
    }

    private fun replyToClose(reply: Message<JsonObject>) {
        reply.rxReply<String>("Success").subscribe()
    }

}