package com.nyala.core.application

import com.fasterxml.jackson.annotation.JsonProperty
import com.nyala.core.application.dto.OAuthClientDto

data class Oauth2UrlRequest(@JsonProperty("oauth2Client") val oauth2Client: OAuthClientDto,
                            @JsonProperty("userId") val userId: String)