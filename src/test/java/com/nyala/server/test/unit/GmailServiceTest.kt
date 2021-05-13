package com.nyala.server.test.unit

import com.nyala.server.infrastructure.mail.old.OAuthFlowCredentialProvider
import com.nyala.server.infrastructure.mail.GmailService
import com.nyala.server.infrastructure.mail.oauth2.google.OAuth2Client
import org.junit.jupiter.api.Test

class GmailServiceTest {

    @Test
    fun testModify() {

        val clientId = "835923206105-8ttgvcsogntl0iigdl3esl30t941ctbs.apps.googleusercontent.com"
        val secret = "iC9POuSKYTsMEp_EFVtzzcyb"

        // TODO: separate building gmail from message processing
        // TODO: batchModify, batchDelete
        //  https://stackoverflow.com/questions/27610601/with-the-gmail-api-is-it-possible-to-batch-my-requests
        //TODO: Add timeframe
        //TODO: add category rule
        //TODO: credentials, how to check for token expiry / use refreshToken -- see Credential Javadoc
        //TODO: run periodically
//        val credentialProvider = OAuthFlowCredentialProvider()
//        val oauth2Client = OAuth2Client(clientId, secret, OAuthFlowCredentialProvider.SCOPES)
//        val gmailService = GmailService(credentialProvider)
//        val userId = "morenza@gmail.com"
//
//        //TODO: avoid having to call init - use DI
//        gmailService.init(userId, oauth2Client)
//        gmailService.performCommand()
    }
}