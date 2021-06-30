package com.nyala.core.infrastructure.oauth2.google

import com.google.api.client.auth.oauth2.*
import com.nyala.core.infrastructure.oauth2.OAuth2CredentialProvider
import com.nyala.core.infrastructure.mail.ServerInfo
import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.domain.model.oauth2.OAuth2Credential
import com.nyala.core.domain.model.oauth2.OAuth2Provider
import com.nyala.core.infrastructure.oauth2.OAuth2Cache
import java.lang.RuntimeException

/**
 * This class represents the Google implementations for the basics of OAuth2 flow,
 * {@code generateAuthUrl} and {@code validateCode}
 */
class GoogleOAuth2CredentialProvider(private val credentialHelper: GoogleCredentialHelper,
                                     private val oauth2Cache: OAuth2Cache,
                                     private val serverInfo: ServerInfo): OAuth2CredentialProvider {

    private val defaultUserId = "nyala";

    override fun generateAuthUrl(client: OAuth2Client): String {
        val flow: AuthorizationCodeFlow = credentialHelper.generateCodeFlow(client)
        val redirectUri = client.redirectUri ?: generateServerUrl()
        val state = defaultUserId
        oauth2Cache.saveClientId(state, client.clientId)
        oauth2Cache.saveRedirectUri(state, redirectUri)

        // should be in credentialHelper?
        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(state)
                .set("prompt", "consent")
                .set("access_type", "offline")
                .build()
    }

    override fun validateCode(state: String?,
                              code: String): OAuth2Credential {
        val requesterUserId = state ?: defaultUserId
        val clientId: String = oauth2Cache.findClientId(requesterUserId) ?:
            throw RuntimeException("Cannot match with previous auth request - clientId")
        val redirectUri = oauth2Cache.findRedirectUri(requesterUserId) ?:
            throw RuntimeException("Cannot match with previous auth request - redirectUri")
        val response = credentialHelper.validateAuthorizationCode(clientId, redirectUri, code)
        credentialHelper.storeCredential(clientId, requesterUserId, response)

        return OAuth2Credential(
                accessToken = response.accessToken!!,
                refreshToken = response.refreshToken!!,
                scope = response.scope,
                expirationTimeSeconds = response.expiresInSeconds,
                provider = OAuth2Provider.GOOGLE)
    }

    private fun generateServerUrl(): String {
        return serverInfo.currentUri()
    }
}
