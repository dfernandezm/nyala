package com.nyala.core.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuth2GenerateAuthUrlInput(@JsonProperty("oauth2Client") val oauth2Client: OAuth2ClientInput,
                                      @JsonProperty("userId") val userId: String)