package com.nyala.server.infrastructure.mail.oauth2.google

class OAuth2Client (val clientId: String,
                         val clientSecret: String,
                         val redirectUri: String?,
                         val scopes: Set<String>,
                         val applicationName: String? = null)