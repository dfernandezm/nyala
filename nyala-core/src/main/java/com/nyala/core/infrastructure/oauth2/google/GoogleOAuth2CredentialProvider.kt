package com.nyala.core.infrastructure.oauth2.google

import com.google.api.client.auth.oauth2.*
import com.nyala.core.infrastructure.oauth2.OAuth2CredentialProvider
import com.nyala.core.infrastructure.mail.ServerInfo
import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.domain.model.oauth2.OAuth2Credential
import com.nyala.core.domain.model.oauth2.OAuth2Provider

/**
 * This class represents the Google implementations of the basics of OAuth2 flow,
 * {@code generateAuthUrl} and {@code validateAuthorizationCode}
 */
class GoogleOAuth2CredentialProvider(private val credentialHelper: GoogleCredentialHelper,
                                     private val serverInfo: ServerInfo): OAuth2CredentialProvider {

    override fun generateAuthUrl(client: OAuth2Client): String {
        val flow: AuthorizationCodeFlow = credentialHelper.generateCodeFlow(client)
        val redirectUri = client.redirectUri ?: generateServerUrl()
        return flow.newAuthorizationUrl().setRedirectUri(redirectUri).build()
    }

    override fun validateAuthorizationCode(userId: String?, oauth2Client: OAuth2Client,
                                           authorizationCode: String): OAuth2Credential {
        val response = credentialHelper.validateAuthorizationCode(oauth2Client, authorizationCode)
        credentialHelper.storeCredential(oauth2Client, userId, response)
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
