package com.nyala.core.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuthClientDto(@JsonProperty("clientId") val clientId: String,
                          @JsonProperty("clientSecret") val clientSecret: String)