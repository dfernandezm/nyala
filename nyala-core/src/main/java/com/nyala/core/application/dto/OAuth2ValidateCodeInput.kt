package com.nyala.core.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * This class represents the request data passed in from the client.
 *
 * Note it requires public members for Jackson to do proper Json serialization
 *
 */
data class OAuth2ValidateCodeInput(@JsonProperty("code") val code: String,
                                   @JsonProperty("scope") val scope: String,
                                   @JsonProperty("state") val state: String?)