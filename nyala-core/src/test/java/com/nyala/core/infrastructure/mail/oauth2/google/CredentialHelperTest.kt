package com.nyala.core.infrastructure.mail.oauth2.google

import com.google.api.client.auth.oauth2.*
import com.nyala.core.infrastructure.oauth2.google.GoogleCredentialHelper
import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.domain.model.oauth2.OAuth2Credential
import com.nyala.core.domain.model.oauth2.OAuth2Provider
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.*

import java.lang.RuntimeException


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

        // Given: a valid oauth2Client
        // And: an existing credential
        val (scopes, oauth2Client) = anOauth2Client()
        val defaultTime = 3600L
        val oauth2Tokens = OAuth2Credential(
                accessToken = "token",
                refreshToken = "refreshToken",
                expirationTimeSeconds = defaultTime,
                scope = scopes.joinToString(" "),
                provider = OAuth2Provider.GOOGLE)
        val userId = "user"

        val credentialHelper = GoogleCredentialHelper()
        val credentialHelperSpy = spy(credentialHelper)
        val (tokenResponseCaptor, oauth2ClientIdCaptor, userIdCaptor) =
                mockCredentialHelpers(credentialHelperSpy)

        // When: asking to regenerate the Oauth2 credential
        credentialHelperSpy.regenerateCredentialFrom(oauth2Client, oauth2Tokens, userId)

        // Then: gets flow from cache
        // And: stores credential for the user
        // And: token data is the returned one from the original response
        verify(credentialHelperSpy, times(1)).getCodeFlow(oauth2Client.clientId)
        verify(credentialHelperSpy, times(1)).storeCredential(
                        oauth2ClientIdCaptor.capture(), userIdCaptor.capture(),
                        tokenResponseCaptor.capture())

        val capturedTokenResponse = tokenResponseCaptor.lastValue
        assertThat(capturedTokenResponse.accessToken, equalTo(oauth2Tokens.accessToken))
        assertThat(capturedTokenResponse.refreshToken, equalTo(oauth2Tokens.refreshToken))
        assertThat(capturedTokenResponse.scope, equalTo(oauth2Tokens.scope))
        assertThat(capturedTokenResponse.expiresInSeconds, equalTo(defaultTime))
    }

    private fun mockCredentialHelpers(credentialHelperSpy: GoogleCredentialHelper): Triple<KArgumentCaptor<TokenResponse>, KArgumentCaptor<String>, KArgumentCaptor<String>> {
        val tokenResponseCaptor = argumentCaptor<TokenResponse>()
        val oauth2ClientIdCaptor = argumentCaptor<String>()
        val userIdCaptor = argumentCaptor<String>()

        val authorizationCodeFlow = Mockito.mock(AuthorizationCodeFlow::class.java)
        val credential = Mockito.mock(Credential::class.java)
        doReturn(credential).whenever(authorizationCodeFlow).createAndStoreCredential(any(), any())
        doReturn(authorizationCodeFlow).whenever(credentialHelperSpy).getCodeFlow(any())
        return Triple(tokenResponseCaptor, oauth2ClientIdCaptor, userIdCaptor)
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

        // Given: initial authorization flow has been completed (for code)
        // And: invalid token request happened
        whenever(authCodeFlow.newTokenRequest(any())).thenThrow(RuntimeException())

        // cannot use whenever(...) as it is an spy, doReturn needs to go first
        // https://stackoverflow.com/questions/11620103/mockito-trying-to-spy-on-method-is-calling-the-original-method
        doReturn(authCodeFlow).whenever(credentialHelperSpy).getCodeFlow(any())


        // When: validating the auth code
        val exception = assertThrows<RuntimeException> {
            credentialHelperSpy.validateAuthorizationCode(oauth2Client.clientId,
                    "aRedirectUri","code")
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
        credentialHelper.generateCodeFlow(oauth2Client)

        val credentialHelperSpy = spy(credentialHelper)

        val tokenResponse = Mockito.mock(TokenResponse::class.java)
        whenever(tokenResponse.accessToken).thenReturn("anAccessToken")
        whenever(tokenResponse.refreshToken).thenReturn("aRefreshToken")

        // When
        credentialHelperSpy.storeCredential(oauth2Client.clientId, anUserId, tokenResponse)

        // Then: credential exists
        val credential = credentialHelperSpy.getStoredCredential(oauth2Client.clientId, anUserId)
        assertNotNull(credential)
        assertThat(credential?.accessToken, equalTo("anAccessToken"))
        assertThat(credential?.refreshToken, equalTo("aRefreshToken"))
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