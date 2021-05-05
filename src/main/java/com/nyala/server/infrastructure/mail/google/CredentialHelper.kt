package com.nyala.server.infrastructure.mail.google

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.auth.oauth2.ClientId
import com.nyala.server.infrastructure.mail.oauth2.OAuth2Client
import com.nyala.server.infrastructure.mail.old.OAuthFlowCredentialProvider
import java.io.File

class CredentialHelper {

    // This needs to be shared between the generation phase (generateAuth, validateCode)
    // and the retrieval (getGoogleCredential)
    private val oAuth2ClientCache: MutableMap<String, AuthorizationCodeFlow> = HashMap()

    // This needs to be shared between the generation phase (generateAuth, validateCode)
    // and the retrieval (getGoogleCredential)
    fun generateCodeFlow(oAuth2Client: OAuth2Client): AuthorizationCodeFlow {
        val flow: AuthorizationCodeFlow
        if (oAuth2ClientCache.containsKey(oAuth2Client.clientId)) {
            flow = oAuth2ClientCache[oAuth2Client.clientId]!!
        } else {
            flow = AuthorizationCodeFlow.Builder(
                    BearerToken.authorizationHeaderAccessMethod(),
                    NetHttpTransport(),
                    JacksonFactory(),
                    GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
                    ClientParametersAuthentication(oAuth2Client.clientId, oAuth2Client.clientSecret),
                    oAuth2Client.clientId,
                    GoogleOAuthConstants.AUTHORIZATION_SERVER_URL)
                    .setScopes(oAuth2Client.scopes)
                    .setDataStoreFactory(FileDataStoreFactory(File(OAuthFlowCredentialProvider.FILE_TOKENS_PATH)))
                    .build()

            oAuth2ClientCache[oAuth2Client.clientId] = flow
        }
        return flow
    }

    fun findByClientId(clientId: String): AuthorizationCodeFlow? {
     return null
    }
}