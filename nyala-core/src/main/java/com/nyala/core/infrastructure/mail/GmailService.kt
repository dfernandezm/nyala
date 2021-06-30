package com.nyala.core.infrastructure.mail

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.ModifyMessageRequest
import com.nyala.core.infrastructure.mail.command.CleanupCommand
import com.nyala.core.infrastructure.mail.command.MailAction
import com.nyala.core.infrastructure.mail.command.Timeframe
import com.nyala.core.infrastructure.oauth2.google.LocalServerGoogleOAuth2Provider
import com.nyala.core.infrastructure.oauth2.OAuth2CredentialProvider
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class GmailService(private val credentialProvider: OAuth2CredentialProvider) {

    companion object {
        private const val TIMEOUT = 15000
        private const val APPLICATION_NAME = "Email Extractor"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    private var gmail: Gmail? = null

    fun init(credential: Credential) {

        if (gmail == null) {
            // Build a new authorized API client service.
            val httpTransport = GoogleNetHttpTransport
                    .newTrustedTransport()
                    .createRequestFactory { request ->
                        request.connectTimeout = TIMEOUT
                        request.readTimeout = TIMEOUT
                    }.transport

            gmail = Gmail.Builder(httpTransport, LocalServerGoogleOAuth2Provider.JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build()
        }
    }


    private tailrec fun processMessages(
            user: String,
            label: Label,
            nextPageToken: String? = null,
            process: (Message) -> Unit
    ) {

        val messages = gmail!!.users().messages().list(user).apply {
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

    private fun findLabel(user: String, labelName: String): Label {
        val labelList = gmail!!.users().labels().list(user).execute()
        return labelList.labels
                .find { it.name == labelName } ?: error("Label `$labelName` is unknown.")
    }

    private fun metadataFromMessage(user: String, message: Message): Message {
        return gmail!!.users().messages().get(user, message.id).apply { format = "METADATA" }.execute()
    }

    private fun messageMatchesTimeframe(message: Message, timeframe: Timeframe): Boolean {
        val now = ZonedDateTime.now()
        val localDate = Instant.ofEpochMilli(message.internalDate)
        val msgDate = ZonedDateTime.ofInstant(localDate, ZoneId.systemDefault())

        if (timeframe == Timeframe.BI_WEEKLY) {
            log.info("Timeframe has been matched for message")
            return msgDate.isBefore(now.minusWeeks(2))
        }

        return false
    }

    private fun markMessageRead(user: String, message: Message) {
        try {
            log.info("Marking message ${message.id} as READ")

            message.payload.headers.find { it.name == "From" }?.let { from ->
                log.info("Message from: $from")
            }

            val modifyRequest = ModifyMessageRequest()
            modifyRequest.removeLabelIds = listOf("UNREAD")
            gmail!!.users().messages().modify(user, message.id, modifyRequest).execute()
        } catch (t: Throwable) {
            log.error("Error executing", t)
        }

    }

    private fun executeCmd(user: String, labelName: String, action: MailAction, timeframe: Timeframe) {
        log.info("Executing for user $user and label $labelName")
        val label = findLabel(user, labelName)
        processMessages(user, label) { message ->
            val msg = metadataFromMessage(user, message)
            if (messageMatchesTimeframe(msg, timeframe)) {
               if (action == MailAction.MARK_READ) {
                   markMessageRead(user, msg)
               }
            }
        }
    }

    fun performCommand() {
        log.info("Executing mail command...")

        val label = "Freelances"
        val user = "me"
        val action = MailAction.MARK_READ
        val timeframe = Timeframe.BI_WEEKLY

        val cmd = CleanupCommand(user,
                label = label,
                action = MailAction.MARK_READ,
                timeframe = timeframe
        )

        executeCmd(user, label, action, timeframe)
    }
}