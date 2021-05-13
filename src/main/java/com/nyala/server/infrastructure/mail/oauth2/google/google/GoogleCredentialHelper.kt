package com.nyala.server.infrastructure.mail.oauth2.google.google

import com.google.api.client.auth.oauth2.*
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.common.annotations.VisibleForTesting
import com.nyala.server.infrastructure.mail.oauth2.google.OAuth2Client
import com.nyala.server.infrastructure.mail.oauth2.google.OAuth2Credential
import com.nyala.server.infrastructure.mail.old.OAuthFlowCredentialProvider
import java.io.File
import java.lang.RuntimeException

class GoogleCredentialHelper {

    // This needs to be shared between the generation phase (generateAuth, validateCode)
    // and the retrieval (getGoogleCredential)
    private val oAuth2ClientCache: MutableMap<String, AuthorizationCodeFlow> = HashMap()
    private val validationResponseCache: MutableMap<String, TokenResponse> = HashMap()
    private val credentialMap: MutableMap<String, Credential> = HashMap()

    fun validateAuthorizationCode(oAuth2Client: OAuth2Client, authorizationCode: String): TokenResponse {
        val flow = generateCodeFlow(oAuth2Client)
        try {
            val response = flow.newTokenRequest(authorizationCode)?.execute()
            validationResponseCache[oAuth2Client.clientId] = response!!
            return response
        } catch (e: Exception) {
            //TODO: Add login
            // log.error("Error validating code")
            throw RuntimeException("Error validating code for clientId ${oAuth2Client.clientId}", e)
        }
    }

    // This needs to be shared between the generation phase (generateAuth, validateCode)
    // and the retrieval (getGoogleCredential)
    fun generateCodeFlow(oAuth2Client: OAuth2Client): AuthorizationCodeFlow {
        val flow: AuthorizationCodeFlow
        if (oAuth2ClientCache.containsKey(oAuth2Client.clientId)) {
            flow = oAuth2ClientCache[oAuth2Client.clientId]!!
        } else {
            flow = buildAuthorizationCodeFlow(oAuth2Client)
            oAuth2ClientCache[oAuth2Client.clientId] = flow
        }
        return flow
    }

    fun buildAuthorizationCodeFlow(oAuth2Client: OAuth2Client): AuthorizationCodeFlow {
        return AuthorizationCodeFlow.Builder(
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
    }

    @VisibleForTesting
    fun getStoredCredential(oauth2ClientId: String, userId: String?): Credential? {
        val key = getKey(oauth2ClientId, userId)
        val credential = credentialMap[key]
        if (credential == null) {
            throw RuntimeException("Credential has not been generated stored/yet for user $userId")
        } else {
            return credential
        }
    }

    fun regenerateCredentialFrom(oAuth2Client: OAuth2Client, oAuth2Tokens: OAuth2Credential, userId: String?): Credential? {
        val tokenResponse = TokenResponse()
        tokenResponse.accessToken = oAuth2Tokens.accessToken
        tokenResponse.refreshToken = oAuth2Tokens.refreshToken
        tokenResponse.expiresInSeconds = oAuth2Tokens.expirationTimeSeconds
        tokenResponse.scope = oAuth2Tokens.scope
        storeCredential(oAuth2Client, userId, tokenResponse)
        return getStoredCredential(oAuth2Client.clientId, userId)
    }

    fun storeCredential(oauth2Client: OAuth2Client, userId: String?, tokenResponse: TokenResponse) {
        val key = getKey(oauth2Client.clientId, userId)
        val flow = generateCodeFlow(oauth2Client)
        credentialMap[key] = flow.createAndStoreCredential(tokenResponse, userId)
    }

    private fun getKey(oAuth2ClientId: String, userId: String?): String {
        return if (userId == null) oAuth2ClientId else oAuth2ClientId + "_" + userId
    }
}