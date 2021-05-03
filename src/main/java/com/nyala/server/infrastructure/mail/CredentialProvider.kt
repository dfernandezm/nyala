package com.nyala.server.infrastructure.mail

import com.google.api.client.auth.oauth2.Credential

interface CredentialProvider {
   fun provideCredential(client: OAuth2Client): Credential
}