package com.nyala.core.application.handler

import com.nyala.core.application.dto.OAuth2ValidateCodeInput
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.MultiMap
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.eventbus.Message
import io.vertx.rxjava.core.http.HttpServerResponse

import io.vertx.rxjava.ext.web.RoutingContext
import org.slf4j.LoggerFactory

class OAuth2Handler(private val vertx: Vertx): Handler<RoutingContext> {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)

        const val oauth2AuthUrlGenerationAddress = "oauth2.authUrl"
        const val oauth2ValidateCodeAddress = "oauth2.validateCode"
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
        val response = routingContext.response()

        if (url.contains("/authUrl")) {
            val oauth2UrlRequestJson = routingContext.bodyAsJson

            // RxSend passes internally a replyHandler, so that needs to be replied to,
            // otherwise the call hangs even after the response was sent
            vertx.eventBus().rxSend<JsonObject>(oauth2AuthUrlGenerationAddress, oauth2UrlRequestJson)
                    .subscribe ({ reply ->
                        replyToClose(reply)
                        writeResponseAsJson(response, reply)
                    }, { error ->
                        log.error("Error occurred", error)
                        sendErrorCode(response, 500, error)
                    })
        }

        if (url.contains("/validate/code")) {
            val requestParams = routingContext.request().params()
            log.info("Validation request {}", requestParams)
            validateRequestParams(requestParams)
            val oauth2CodeValidationRequest = OAuth2ValidateCodeInput(
                    code = requestParams.get("code"),
                    scope = requestParams.get("scope"),
                    state = requestParams.get("state")
            )

            val oauth2CodeValidationRequestJson = JsonObject.mapFrom(oauth2CodeValidationRequest)

            vertx.eventBus().rxSend<JsonObject>(oauth2ValidateCodeAddress, oauth2CodeValidationRequestJson)
                    .subscribe ({ reply ->
                        replyToClose(reply)
                        writeResponseAsJson(response, reply)
                    }, { error ->
                        log.error("Error occurred validating code", error)
                        sendErrorCode(response, 500, error)
                    })
        }
    }

    private fun validateRequestParams(requestParams: MultiMap) {
        val code = requestParams.get("code")
        val state = requestParams.get("state")
    }

    private fun sendErrorCode(response: HttpServerResponse, statusCode: Int, error: Throwable) {
        response.setStatusCode(statusCode).end(Json.encodePrettily(JsonObject().put("error", error.message)))
    }

    private fun writeResponseAsJson(response: HttpServerResponse, reply: Message<JsonObject>) {
        response
                .putHeader("content-type", "application/json")
                .end(Json.encodePrettily(reply.body()))
    }

    /**
     *
     * This is an artifact needed for rxSend not to hang, it's not needed for
     * core send(...)
     *
     * See: https://stackoverflow.com/questions/49449257/vertx-timeout-in-message-reply
     *
     */
    private fun replyToClose(reply: Message<JsonObject>) {
        reply.rxReply<String>("Success").subscribe()
    }

}