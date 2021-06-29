package com.nyala.core.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuth2CredentialOutput(@JsonProperty("accessToken") val accessToken: String,
                                  @JsonProperty("refreshToken") val refreshToken: String,
                                  @JsonProperty("expirationTimeSeconds") val expirationTimeSeconds: Long)