package com.nyala.core.infrastructure.mail.oauth2.google

class OAuth2Credential(val accessToken: String,
                        val refreshToken: String,
                        val scope: String,
                        val expirationTimeSeconds: Long,
                        val provider: OAuth2Provider)