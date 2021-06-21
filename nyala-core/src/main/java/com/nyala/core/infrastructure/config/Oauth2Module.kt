package com.nyala.core.infrastructure.config

import com.nyala.core.application.handler.StatusEndpointHandler
import com.nyala.core.infrastructure.mail.ServerInfo
import com.nyala.core.infrastructure.oauth2.OAuth2CredentialProvider
import com.nyala.core.infrastructure.oauth2.google.GoogleCredentialHelper
import com.nyala.core.infrastructure.oauth2.google.GoogleOAuth2CredentialProvider
import io.vertx.redis.RedisOptions
import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.redis.RedisClient
import org.koin.dsl.module

class Oauth2Module(private val vertx: Vertx) {

    private val context: Context = vertx.orCreateContext

    val oauth2Module = module {
        single { GoogleOAuth2CredentialProvider(credentialHelper = get(), serverInfo = get()) as OAuth2CredentialProvider}
        single { ServerInfo(vertx, context) }
        single { GoogleCredentialHelper() }
    }
}