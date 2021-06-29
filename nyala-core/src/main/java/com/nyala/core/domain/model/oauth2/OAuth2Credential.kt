package com.nyala.core.domain.model.oauth2

class OAuth2Credential( val accessToken: String,
                        val refreshToken: String,
                        val scope: String,
                        val expirationTimeSeconds: Long,
                        val provider: OAuth2Provider)