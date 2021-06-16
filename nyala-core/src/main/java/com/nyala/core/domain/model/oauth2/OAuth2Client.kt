package com.nyala.core.domain.model.oauth2

class OAuth2Client (val clientId: String,
                         val clientSecret: String,
                         val redirectUri: String?,
                         val scopes: Set<String>,
                         val applicationName: String? = null)