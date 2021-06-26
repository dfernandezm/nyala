package com.nyala.core.infrastructure.oauth2.google

import com.google.api.client.auth.oauth2.*
import com.nyala.core.infrastructure.oauth2.OAuth2CredentialProvider
import com.nyala.core.infrastructure.mail.ServerInfo
import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.domain.model.oauth2.OAuth2Credential
import com.nyala.core.domain.model.oauth2.OAuth2Provider
import java.lang.RuntimeException

/**
 * This class represents the Google implementations of the basics of OAuth2 flow,
 * {@code generateAuthUrl} and {@code validateCode}
 */
class GoogleOAuth2CredentialProvider(private val credentialHelper: GoogleCredentialHelper,
                                     private val serverInfo: ServerInfo): OAuth2CredentialProvider {

    private val defaultUserId = "nyala";
    private val userIdToOAuth2ClientId = HashMap<String, String>()
    private val userIdRedirectUri = HashMap<String, String>()

    override fun generateAuthUrl(client: OAuth2Client): String {
        val flow: AuthorizationCodeFlow = credentialHelper.generateCodeFlow(client)
        val redirectUri = client.redirectUri ?: generateServerUrl()
        val userId = defaultUserId
        userIdToOAuth2ClientId[userId] = client.clientId
        userIdRedirectUri[userId] = redirectUri
        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(userId)
                .set("prompt", "consent")
                .set("access_type", "offline")
                .build()
    }

    override fun validateCode(state: String?,
                              code: String): OAuth2Credential {
        val requesterUserId = state ?: defaultUserId
        val clientId: String = userIdToOAuth2ClientId[requesterUserId] ?:
            throw RuntimeException("Cannot match with previous auth request - clientId")
        val redirectUri = userIdRedirectUri[requesterUserId] ?:
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
