package com.nyala.core.application.verticle

import com.nyala.core.application.dto.OAuth2GeneralAuthUrlInput
import com.nyala.core.application.dto.OAuth2ValidateCodeInput
import com.nyala.core.application.handler.OAuth2Handler
import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.infrastructure.config.Oauth2Module
import com.nyala.core.infrastructure.di.IsolatedKoinVerticle
import com.nyala.core.infrastructure.di.KoinDIFactory
import com.nyala.core.infrastructure.oauth2.OAuth2CredentialProvider
import io.vertx.core.Future

import io.vertx.core.json.JsonObject
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class OAuth2Verticle: IsolatedKoinVerticle() {

    private val oAuth2CredentialProvider by inject<OAuth2CredentialProvider>()
    private val defaultApplicationName = "nyala"

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun getAppName(): String {
        return "Oauth2Verticle"
    }

    private fun startDependencyInjection() {
        KoinDIFactory.startNewApp(getAppName()) {
            modules(Oauth2Module(vertx).oauth2Module)
        }
    }

    override fun start(startFuture: Future<Void>?) {
        startDependencyInjection()
        vertx.eventBus().consumer<JsonObject>(OAuth2Handler.oauth2AuthUrlGenerationAddress)
                .toObservable()
                .doOnNext { message ->

                    val oauth2UrlRequest = message.body().mapTo(OAuth2GeneralAuthUrlInput::class.java)
                    log.info("Received - {}", oauth2UrlRequest)

                    val oauth2ClientDto = oauth2UrlRequest.oauth2Client
                    val scopes = oauth2ClientDto.scopes ?: setOf()

                    val oAuth2Client = OAuth2Client(
                            clientId = oauth2ClientDto.clientId,
                            clientSecret = oauth2ClientDto.clientSecret,
                            redirectUri = oauth2ClientDto.redirectUri,
                            scopes = scopes,
                            applicationName = defaultApplicationName
                    )

                    try {
                        val authUrl = oAuth2CredentialProvider.generateAuthUrl(oAuth2Client)
                        val authUrlReply = JsonObject().put("authUrl", authUrl)
                        message.rxReply<JsonObject>(authUrlReply)
                                .subscribe(
                                        { replyToReply -> replyToReply.reply(authUrlReply)},
                                        { err -> log.error("Error on the reply", err)}
                                )
                    } catch (e: Exception) {
                        log.error("Error generating authUrl: ", e)
                        message.fail(500, "Error generating authUrl: " + e.message)
                    }
                }.subscribe()

        vertx.eventBus().consumer<JsonObject>(OAuth2Handler.oauth2ValidateCodeAddress)
                .toObservable()
                .doOnNext { message ->
                    log.info("Message: {}", message)
                    try {
                        val oauth2ValidateCodeRequest = message.body().mapTo(OAuth2ValidateCodeInput::class.java)
                        log.info("Validation Request: {}", oauth2ValidateCodeRequest)
                        oAuth2CredentialProvider.validateCode(
                                oauth2ValidateCodeRequest.state,
                                oauth2ValidateCodeRequest.code)
                        message.reply(JsonObject().put("message", "success"))
                    } catch (e: Exception) {
                        log.error("Error decoding message", e)
                        message.fail(500, "Error validating code: " + e.message)
                    }
                }.subscribe()
    }
}