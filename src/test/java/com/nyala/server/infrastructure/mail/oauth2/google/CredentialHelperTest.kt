package com.nyala.server.infrastructure.mail.oauth2.google

import com.google.api.client.auth.oauth2.*
import com.nyala.server.infrastructure.mail.oauth2.google.google.GoogleCredentialHelper
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.*

import java.lang.RuntimeException
import kotlin.test.assertNotNull

/**
 * In order to run the test with Coverage in IntelliJ ensure the Run Configurations...
 * dialog has the right package selected. If it's wrong it means the Test class does not
 * mimic the package structure of the original class
 *
 */
class CredentialHelperTest {

    @Test
    fun testCodeFlowBuildFromOAuth2Client() {
        val (scopes, oAuth2Client) = anOauth2Client()

        val credentialHelper = GoogleCredentialHelper()

        val codeFlow = credentialHelper.generateCodeFlow(oAuth2Client)
        val clientAuth =  codeFlow.clientAuthentication as ClientParametersAuthentication
        assertThat(codeFlow.clientId, equalTo(oAuth2Client.clientId))
        assertThat(codeFlow.scopesAsString, equalTo(scopes.joinToString(" ")))
        assertThat(clientAuth.clientSecret, equalTo(oAuth2Client.clientSecret))
    }

    @Test
    fun canRegenerateCredentialThatHasNotBeenGeneratedYet() {

        // Given
        val (scopes, oauth2Client) = anOauth2Client()
        val defaultTime = 3600L
        val aOAuth2Tokens = OAuth2Credential(
                accessToken = "token",
                refreshToken = "refreshToken",
                expirationTimeSeconds = defaultTime,
                scope = scopes.joinToString(" "),
                provider = OAuth2Provider.GOOGLE)
        val userId = "user"

        val credentialHelper = GoogleCredentialHelper()
        val credentialHelperSpy = spy(credentialHelper)

        val tokenResponseCaptor = argumentCaptor<TokenResponse>()
        val oauth2ClientCaptor = argumentCaptor<OAuth2Client>()
        val userIdCaptor = argumentCaptor<String>()

        // When
        credentialHelperSpy.regenerateCredentialFrom(oauth2Client, aOAuth2Tokens, userId)

        verify(credentialHelperSpy, times(1)).generateCodeFlow(oauth2Client)
        verify(credentialHelperSpy, times(1)).storeCredential(
                        oauth2ClientCaptor.capture(), userIdCaptor.capture(),
                        tokenResponseCaptor.capture())

        val capturedTokenResponse = tokenResponseCaptor.lastValue
        assertThat(capturedTokenResponse.accessToken, equalTo(aOAuth2Tokens.accessToken))
        assertThat(capturedTokenResponse.refreshToken, equalTo(aOAuth2Tokens.refreshToken))
        assertThat(capturedTokenResponse.scope, equalTo(aOAuth2Tokens.scope))
        assertThat(capturedTokenResponse.expiresInSeconds, equalTo(defaultTime))
    }

    @Test
    fun codeFlowIsCached() {
        // Given
        val (_, oauth2Client) = anOauth2Client()

        val credentialHelper = GoogleCredentialHelper()
        val credentialHelperSpy = spy(credentialHelper)

        // When invoking code flow generation twice
        credentialHelperSpy.generateCodeFlow(oauth2Client)
        verify(credentialHelperSpy, times(1)).buildAuthorizationCodeFlow(any())

        reset(credentialHelperSpy)

        credentialHelperSpy.generateCodeFlow(oauth2Client)

        // Then: code flow won't get built again
        verify(credentialHelperSpy, never()).buildAuthorizationCodeFlow(any())
    }

    @Test
    fun cannotValidateAuthCodeWithInvalidTokenRequest() {
        val (_, oauth2Client) = anOauth2Client()

        val credentialHelper = GoogleCredentialHelper()
        val credentialHelperSpy = spy(credentialHelper)
        val authCodeFlow = Mockito.mock(AuthorizationCodeFlow::class.java)

        // Given: invalid token request
        whenever(authCodeFlow.newTokenRequest(any())).thenThrow(RuntimeException())

        // cannot use whenever(...) as it is an spy, doReturn needs to go first
        // https://stackoverflow.com/questions/11620103/mockito-trying-to-spy-on-method-is-calling-the-original-method
        doReturn(authCodeFlow).whenever(credentialHelperSpy).generateCodeFlow(any())

        // When: validating the auth code
        val exception = assertThrows<RuntimeException> {
            credentialHelper.validateAuthorizationCode(oauth2Client, "code")
        }

        // Then: An error occurs
        assertThat(exception.message, equalTo("Error validating code for clientId ${oauth2Client.clientId}"))
    }

    @Test
    fun storesCredentialGivenValidTokenResponse() {

        // Given
        val (_, oauth2Client) = anOauth2Client()
        val anUserId = "anUserId"

        val credentialHelper = GoogleCredentialHelper()
        val credentialHelperSpy = spy(credentialHelper)

        val tokenResponse = Mockito.mock(TokenResponse::class.java)
        whenever(tokenResponse.accessToken).thenReturn("anAccessToken")
        whenever(tokenResponse.refreshToken).thenReturn("aRefreshToken")

        // When
        credentialHelperSpy.storeCredential(oauth2Client, anUserId, tokenResponse)

        // Then: credential exists
        val credential = credentialHelperSpy.getStoredCredential(oauth2Client.clientId, anUserId)
        assertNotNull(credential)
        assertThat(credential.accessToken, equalTo("anAccessToken"))
        assertThat(credential.refreshToken, equalTo("aRefreshToken"))
    }

    private fun anOauth2Client(): Pair<Set<String>, OAuth2Client> {
        val scopes = setOf("scope1", "scope2")
        val oAuth2Client = OAuth2Client(
                clientId = "anId",
                clientSecret = "aSecret",
                redirectUri = "http://uri",
                scopes = scopes)
        return Pair(scopes, oAuth2Client)
    }
}