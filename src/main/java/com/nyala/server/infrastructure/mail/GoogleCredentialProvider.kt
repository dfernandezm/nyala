package com.nyala.server.infrastructure.mail

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants.AUTHORIZATION_SERVER_URL
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants.TOKEN_SERVER_URL
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.GmailScopes
import java.io.File
import java.io.InputStreamReader


// https://plswiderski.medium.com/sending-emails-from-java-app-with-gmail-api-eac23ca0eb5
class GoogleCredentialProvider {

    companion object {
        val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
        private val serverReceiverPort = 8888
        private const val TOKENS_DIRECTORY_PATH = "tokens"
        private val SCOPES = setOf(
                GmailScopes.GMAIL_LABELS,
                GmailScopes.MAIL_GOOGLE_COM,
                GmailScopes.GMAIL_METADATA
        )
    }

    private var cachedCredential: Credential? = null

    private fun authorize(emailAddress: String, httpTransport: HttpTransport): Credential {

        if (cachedCredential == null) {
            val inputStream = File("/tmp/credentials.json").inputStream()
            val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))

            val flow = GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build()
            val receiver = LocalServerReceiver.Builder().setPort(serverReceiverPort).build()
            cachedCredential = AuthorizationCodeInstalledApp(flow, receiver).authorize(emailAddress)

        }

        return cachedCredential!!
    }

    fun authorize(clientId: String, clientSecret: String): Credential? {
        // set up authorization code flow
        val flow: AuthorizationCodeFlow = AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                NetHttpTransport(),
                JSON_FACTORY,
                GenericUrl(TOKEN_SERVER_URL),
                ClientParametersAuthentication(clientId, clientSecret),
                clientId,
                AUTHORIZATION_SERVER_URL)
                .setScopes(SCOPES)
                .setDataStoreFactory(MemoryDataStoreFactory())
                .build()
        // authorize
        val receiver = LocalServerReceiver.Builder().setHost("127.0.0.1").build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    fun getAccessToken(emailAddress: String, httpTransport: HttpTransport): String {
        return authorize(emailAddress, httpTransport).accessToken
    }

    fun getRefreshToken(emailAddress: String, httpTransport: HttpTransport): String {
        return authorize(emailAddress, httpTransport).refreshToken
    }
}

fun main(args: Array<String>) {
    val provider = GoogleCredentialProvider()
    val clientId = "835923206105-8ttgvcsogntl0iigdl3esl30t941ctbs.apps.googleusercontent.com"
    val secret = "iC9POuSKYTsMEp_EFVtzzcyb"

    val credential = provider.authorize(clientId, secret)!!
    println("Tokens ${credential.accessToken}, ${credential.refreshToken}")
}