package com.nyala.core.domain.model.oauth2

class OAuth2Scopes {

    companion object {

        private const val GMAIL_READ_METADATA = "https://www.googleapis.com/auth/gmail.metadata"
        private const val GMAIL_FULL_ACCESS = "https://mail.google.com/"

        fun forFullGmailAccess(): Set<String> {
            return setOf(GMAIL_FULL_ACCESS)
        }

        fun forGmailMetadata(): Set<String> {
            return setOf(GMAIL_READ_METADATA)
        }
    }
}