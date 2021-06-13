package com.nyala.core.infrastructure.mail.old

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
import com.google.api.services.gmail.GmailScopes
import com.google.auth.oauth2.GoogleCredentials
import com.nyala.core.infrastructure.mail.oauth2.google.OAuth2Client
import com.nyala.core.infrastructure.mail.oauth2.google.OAuth2Credential
import org.slf4j.LoggerFactory
import java.io.File

class OAuthFlowCredentialProvider {

    private val localServerUrl: String = "127.0.0.1"
    private var credential: Credential? = null
    private var oauth2Tokens: OAuth2Credential? = null

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)

        val SCOPES = setOf(
            GmailScopes.GMAIL_LABELS,
            GmailScopes.MAIL_GOOGLE_COM,
            GmailScopes.GMAIL_METADATA
        )
        private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
        const val FILE_TOKENS_PATH = "/tmp/tokens"
    }

    fun provideCredential(userId: String, client: OAuth2Client): Credential {
      if (oauth2Tokens == null) {
          credential = authorize(userId, client)!!
//          oauth2Tokens =  OAuth2Tokens(
//                  accessToken = credential!!.accessToken,
//                  refreshToken = credential!!.refreshToken)

      }

      return credential!!
    }

    //TODO: https://developers.google.com/api-client-library/java/google-api-java-client/oauth2
    //TODO: avoid using receiver, look inside .authorize(userId)
    private fun authorize(userId: String, client: OAuth2Client): Credential? {
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



        val existingCredential = flow.loadCredential(userId)


        // Flow to generate a token

        //1: put as redirectUri same server so code can be captured
        val authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(localServerUrl)
        log.info("Authorization server encoded url {}", authorizationUrl)



        //2: capture the code
        // new token, AuthorizationCodeFlow.newTokenRequest(String)).

        //3: create and store credential
        // https://googleapis.dev/java/google-oauth-client/latest/com/google/api/client/auth/oauth2/AuthorizationCodeFlow.html#createAndStoreCredential-com.google.api.client.auth.oauth2.TokenResponse-java.lang.String-


        // Flow to use a token

        //TODO: how to create a credential from the tokens (or just read from DataStore?)
        // check this: https://stackoverflow.com/questions/14948530/refresh-token-with-google-api-java-client-library



        val g: GoogleCredentials


        if (existingCredential == null) {
            log.info("Credential not found for $userId, requesting new one")
            val receiver = LocalServerReceiver.Builder().setHost(localServerUrl).build()
            return AuthorizationCodeInstalledApp(flow, receiver).authorize(userId)
        } else {
            log.info("Credential has been found for $userId")
            return existingCredential
        }
    }

    //TODO: implement, https://developers.google.com/api-client-library/java/google-oauth-java-client/oauth2#detecting_an_expired_access_token
}