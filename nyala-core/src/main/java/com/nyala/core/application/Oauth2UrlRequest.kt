package com.nyala.core.application

import com.fasterxml.jackson.annotation.JsonProperty

data class Oauth2UrlRequest(@JsonProperty("oauth2ClientId") val oauth2ClientId: String,
                            @JsonProperty("oauth2ClientSecret") val oauth2ClientSecret: String,
                            @JsonProperty("userId") val userId: String)