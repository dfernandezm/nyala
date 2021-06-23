package com.nyala.core.application

import com.fasterxml.jackson.annotation.JsonProperty
import com.nyala.core.application.dto.OAuth2ClientDto

data class OAuth2UrlRequest(@JsonProperty("oauth2Client") val oauth2Client: OAuth2ClientDto,
                            @JsonProperty("userId") val userId: String)