package com.nyala.server.infrastructure.mail

data class OAuth2Client (val clientId: String,
                         val clientSecret: String,
                         val scopes: Set<String>,
                         val applicationName: String)