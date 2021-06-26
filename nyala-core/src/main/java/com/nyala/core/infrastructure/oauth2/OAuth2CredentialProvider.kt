package com.nyala.core.infrastructure.oauth2

import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.domain.model.oauth2.OAuth2Credential

interface OAuth2CredentialProvider {
   fun generateAuthUrl(client: OAuth2Client): String
   fun validateCode(state: String?, code: String): OAuth2Credential
}