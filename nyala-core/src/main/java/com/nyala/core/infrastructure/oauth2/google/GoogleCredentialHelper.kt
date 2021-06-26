package com.nyala.core.infrastructure.oauth2.google

import com.google.api.client.auth.oauth2.*
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.common.annotations.VisibleForTesting
import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.domain.model.oauth2.OAuth2Credential
import com.nyala.core.infrastructure.hash.HashUtils
import org.apache.commons.codec.digest.Sha2Crypt
import org.slf4j.LoggerFactory

import java.io.File
import java.lang.RuntimeException

class GoogleCredentialHelper {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    // should use sharedCache
    private val oAuth2ClientCache: MutableMap<String, AuthorizationCodeFlow> = HashMap()
    private val validationResponseCache: MutableMap<String, TokenResponse> = HashMap()
    private val credentialMap: MutableMap<String, Credential> = HashMap()

    fun validateAuthorizationCode(oAuth2ClientId: String, redirectUri: String, authorizationCode: String): TokenResponse {
        val flow = getCodeFlow(oAuth2ClientId)
                ?: throw RuntimeException("Unknown authorization flow for " +
                        "clientId $oAuth2ClientId, regenerate code again")

        try {
            val response = flow.newTokenRequest(authorizationCode)?.setRedirectUri(redirectUri)?.execute()
            validationResponseCache[oAuth2ClientId] = response!!
            return response
        } catch (e: Exception) {
            log.error("Error validating code", e)
            throw RuntimeException("Error validating code for clientId $oAuth2ClientId", e)
        }
    }

    @VisibleForTesting
    fun getCodeFlow(oAuth2ClientId: String): AuthorizationCodeFlow? {
        return oAuth2ClientCache[oAuth2ClientId]
    }

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
                .setDataStoreFactory(FileDataStoreFactory(File("/tmp/tokens")))
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
        storeCredential(oAuth2Client.clientId, userId, tokenResponse)
        return getStoredCredential(oAuth2Client.clientId, userId)
    }

    fun storeCredential(oauth2ClientId: String, userId: String?, tokenResponse: TokenResponse) {
        val key = getKey(oauth2ClientId, userId)
        val flow = getCodeFlow(oauth2ClientId)
        if (flow != null) {
            credentialMap[key] = flow.createAndStoreCredential(tokenResponse, userId)
        } else {
            throw RuntimeException("Unknown flow for clientId $oauth2ClientId, " +
                    "credential cannot be generated - please start flow again")
        }
    }

    private fun getKey(oAuth2ClientId: String, userId: String?): String {
        return if (userId == null) oAuth2ClientId else oAuth2ClientId + "_" + userId
    }
}