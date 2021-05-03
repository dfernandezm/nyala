package com.nyala.server.infrastructure.mail

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.Message
import com.nyala.server.infrastructure.mail.EmailUtil.Companion.parseAddress
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.ZonedDateTime

class GmailService {

    companion object {
        private const val TIMEOUT = 15000
        private const val APPLICATION_NAME = "Email Extractor"
    }

    private var gmail: Gmail? = null

    fun getService(oauth2Client: OAuth2Client) {
            // Build a new authorized API client service.
            val httpTransport = GoogleNetHttpTransport
                    .newTrustedTransport()
                    .createRequestFactory { request ->
                        request.connectTimeout = TIMEOUT
                        request.readTimeout = TIMEOUT
                    }.transport


            val credentialProvider = GcpCredentialProvider()
            val credentials = credentialProvider.provideCredential(oauth2Client)
            gmail = Gmail.Builder(httpTransport, GoogleCredentialProvider.JSON_FACTORY, credentials)
                    .setApplicationName(APPLICATION_NAME)
                    .build()

    }

    private tailrec fun Gmail.processMessages(
            user: String,
            label: Label,
            nextPageToken: String? = null,
            process: (Message) -> Unit
    ) {

        val messages = users().messages().list(user).apply {
            labelIds = listOf(label.id)
            pageToken = nextPageToken
            includeSpamTrash = true
        }.execute()

        messages.messages.forEach { message ->
            process(message)
        }

        if (messages.nextPageToken != null) {
            processMessages(user, label, messages.nextPageToken, process)
        }
    }

    private fun processFroms(
            user: String,
            label: Label,
            process: (String) -> Unit
    ) {

            gmail!!.processMessages(user, label) {
                    fun fetchAndProcess() {
                        try {
                            val message = gmail!!.users().messages().get(user, it.id).apply { format = "METADATA" }.execute()

                            println("M: ${message.internalDate}")

                            message.payload.headers.find { it.name == "From" }?.let { from ->
                                process(parseAddress(from.value))

                            }

                            val now = ZonedDateTime.now()
                            val msgDate = ZonedDateTime.from(Instant.ofEpochMilli(message.internalDate))
                            gmail!!.users().messages().trash(user, message.id).execute()
                        } catch (e: SocketTimeoutException) {
                            // Process eventual failures.
                            // Restart request on socket timeout.
                            e.printStackTrace()
                            fetchAndProcess()
                        } catch (e: Exception) {
                            // Process eventual failures.
                            e.printStackTrace()
                        }
                    }
                    fetchAndProcess()
                }
    }


    fun fetchAndProcess(user: String, label: String, message: Message, process: (String) -> Unit) {
        try {
            val message = gmail!!.users().messages().get(user, message.id).apply { format = "METADATA" }.execute()

            println("M: ${message.internalDate}")

            message.payload.headers.find { it.name == "From" }?.let { from ->
                process(parseAddress(from.value))
            }

            val now = ZonedDateTime.now()
            val msgDate = ZonedDateTime.from(Instant.ofEpochMilli(message.internalDate))
            gmail!!.users().messages().trash(user, message.id).execute()
        } catch (e: SocketTimeoutException) {
            // Process eventual failures.
            // Restart request on socket timeout.
            e.printStackTrace()
            //fetchAndProcess()
        } catch (e: Exception) {
            // Process eventual failures.
            e.printStackTrace()
        }
    }


    fun extract(service: Gmail,labelName: String) {
        // Find the requested label
        val user = "me"
        val labelList = service.users().labels().list(user).execute()

        val label = labelList.labels
                .find { it.name == labelName } ?: error("Label `$labelName` is unknown.")

        println("Extract: $label")

        // Process all From headers.
        val senders = mutableSetOf<String>()
//        processFroms(user, label) {
//            senders += it
//        }

        senders.forEach(::println)
    }

}
