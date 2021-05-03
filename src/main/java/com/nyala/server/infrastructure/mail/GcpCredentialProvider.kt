package com.nyala.server.infrastructure.mail

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import java.io.File

class GcpCredentialProvider: CredentialProvider {

    private val localServerUrl: String = "127.0.0.1"
    private var credential: Credential? = null
    private var oauth2Tokens: OAuth2Tokens? = null
    private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
    private val FILE_TOKENS_PATH = "/tmp/tokens"


    override fun provideCredential(client: OAuth2Client): Credential {
      if (oauth2Tokens == null) {
          credential = authorize(client)!!
          oauth2Tokens =  OAuth2Tokens(
                  accessToken = credential!!.accessToken,
                  refreshToken = credential!!.refreshToken)

      }

      return credential!!
    }

    private fun authorize(client: OAuth2Client): Credential? {
        val flow: AuthorizationCodeFlow = AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                NetHttpTransport(),
                JSON_FACTORY,
                GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
                ClientParametersAuthentication(client.clientId, client.clientSecret),
                client.clientId,
                GoogleOAuthConstants.AUTHORIZATION_SERVER_URL)
                .setScopes(client.scopes)
                .setDataStoreFactory(FileDataStoreFactory(File(FILE_TOKENS_PATH)))
                .build()

        // authorize
        val receiver = LocalServerReceiver.Builder().setHost(localServerUrl).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    // implement, https://developers.google.com/api-client-library/java/google-oauth-java-client/oauth2#detecting_an_expired_access_token

}