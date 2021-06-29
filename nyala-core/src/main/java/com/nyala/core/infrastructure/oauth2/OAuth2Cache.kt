package com.nyala.core.infrastructure.oauth2

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.TokenResponse

class OAuth2Cache {

    private val userIdToOAuth2ClientId = HashMap<String, String>()
    private val userIdRedirectUri = HashMap<String, String>()

    fun saveRedirectUri(state: String, redirectUri: String) {
        userIdRedirectUri[state] = redirectUri
    }

    fun saveClientId(state: String, clientId: String) {
        userIdToOAuth2ClientId[state] = clientId
    }

    fun findRedirectUri(state: String): String? {
        return userIdRedirectUri[state]
    }

    fun findClientId(state: String): String? {
        return userIdToOAuth2ClientId[state]
    }
}