package com.nyala.server.infrastructure.mail

import com.google.api.client.auth.oauth2.*
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.nyala.server.infrastructure.mail.google.CredentialHelper
import com.nyala.server.infrastructure.mail.oauth2.OAuth2Client
import com.nyala.server.infrastructure.mail.oauth2.OAuth2Credential
import com.nyala.server.infrastructure.mail.oauth2.OAuth2Provider
import com.nyala.server.infrastructure.mail.old.OAuthFlowCredentialProvider
import java.io.File

class GoogleOAuth2CredentialProvider(val credentialHelper: CredentialHelper): OAuth2CredentialProvider {

    companion object {
        public const val OAUTH2_PROVIDER_NAME = "GOOGLE"
    }

    override fun generateAuthUrl(client: OAuth2Client): String {
        val flow: AuthorizationCodeFlow = credentialHelper.generateCodeFlow(client)
        val redirectUri = client.redirectUri ?: generateServerUrl()
        return flow.newAuthorizationUrl().setRedirectUri(redirectUri).build()
    }

    override fun validateAuthorizationCode(userId: String?, oauth2ClientId: String, authorizationCode: String): OAuth2Credential {
        if (credentialHelper.findByClientId(oauth2ClientId) != null) {
            val flow = credentialHelper.findByClientId(oauth2ClientId)

            // TODO: error handling
            val response = flow?.newTokenRequest(authorizationCode)?.execute()

            val credential = flow?.createAndStoreCredential(response, userId)

            return OAuth2Credential(
                    accessToken = response?.accessToken!!,
                    refreshToken = response.refreshToken!!,
                    scope = response.scope,
                    expirationTimeSeconds = response.expiresInSeconds,
                    provider = OAuth2Provider.GOOGLE)
        }
        error("Authorization code is not known $authorizationCode")
    }

    private fun fromTokens(clientId: String, oAuth2Tokens: OAuth2Credential): Credential {
        val tokenResponse = TokenResponse()
        tokenResponse.accessToken = oAuth2Tokens.accessToken
        tokenResponse.refreshToken = oAuth2Tokens.refreshToken
        tokenResponse.expiresInSeconds = oAuth2Tokens.expirationTimeSeconds
        tokenResponse.scope = oAuth2Tokens.scope

        val flow = credentialHelper.findByClientId(clientId)
        return flow?.createAndStoreCredential(tokenResponse, null)!!
    }

    private fun generateServerUrl(): String {
        return ""
    }
}