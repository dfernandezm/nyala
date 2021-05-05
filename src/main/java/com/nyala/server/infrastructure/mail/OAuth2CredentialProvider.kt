package com.nyala.server.infrastructure.mail

import com.nyala.server.infrastructure.mail.oauth2.OAuth2Client
import com.nyala.server.infrastructure.mail.oauth2.OAuth2Credential

interface OAuth2CredentialProvider {
   fun generateAuthUrl(client: OAuth2Client): String
   fun validateAuthorizationCode(userId: String?, oauth2ClientId: String, authorizationCode: String): OAuth2Credential
}