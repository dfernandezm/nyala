package com.nyala.core.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuth2ClientInput(@JsonProperty("clientId") val clientId: String,
                             @JsonProperty("clientSecret") val clientSecret: String,
                             @JsonProperty("redirectUri") val redirectUri: String? = null,
                             @JsonProperty("scopes") val scopes: Set<String>? = null)