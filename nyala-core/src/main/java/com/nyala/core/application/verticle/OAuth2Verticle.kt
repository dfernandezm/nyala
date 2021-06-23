package com.nyala.core.application.verticle

import com.nyala.core.application.OAuth2UrlRequest
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
        vertx.eventBus().consumer<JsonObject>("oauth2.authUrl")
                .toObservable()
                .doOnNext { message ->

                    val oauth2UrlRequest = message.body().mapTo(OAuth2UrlRequest::class.java)
                    log.info("Received - {}", oauth2UrlRequest)
                    val oauth2ClientDto = oauth2UrlRequest.oauth2Client
                    val scopes = oauth2ClientDto.scopes ?: setOf()

                    val oAuth2Client = OAuth2Client(
                            clientId = oauth2ClientDto.clientId,
                            clientSecret = oauth2ClientDto.clientSecret,
                            redirectUri = oauth2ClientDto.redirectUri,
                            scopes = scopes,
                            applicationName = "nyala"
                    )

                    val authUrl = oAuth2CredentialProvider.generateAuthUrl(oAuth2Client)
                    val resp = JsonObject().put("authUrl", authUrl)

                    message.rxReply<JsonObject>(resp)
                            .subscribe(
                            { a -> a.reply(resp)},
                            { err -> log.error("Error", err)})

                    //TODO: respond with error
                }.subscribe()

        vertx.eventBus().consumer<JsonObject>("oauth2.validateCode")
                .toObservable()
                .doOnNext { message ->
                    log.info("Message: {}", message)
                    //oAuth2CredentialProvider.validateAuthorizationCode()
                    message.rxReply<JsonObject>(JsonObject().put("message", "success"))
                }.subscribe()

    }
}