package com.nyala.core.infrastructure.mail.oauth2.google

import com.google.api.client.auth.oauth2.*
import com.nyala.core.infrastructure.oauth2.google.GoogleOAuth2CredentialProvider
import com.nyala.core.infrastructure.mail.ServerInfo
import com.nyala.core.infrastructure.oauth2.google.GoogleCredentialHelper
import com.nyala.core.domain.model.oauth2.OAuth2Client
import com.nyala.core.infrastructure.oauth2.OAuth2Cache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito
import org.mockito.kotlin.*
import java.lang.RuntimeException


class GoogleOAuth2CredentialProviderTest {

    /**
     * For Parameterized tests to work it's needed a junit-jupiter-engine in the classpath and a recent version
     * of Gradle otherwise it fails with strange errors in IDEA and Gradle
     *
     * Given a valid OAuth2 Client exists with clientId and secret
     * And a redirect Uri is not provided / provided explicitly
     * When generating the OAuth2 url
     * Then the redirect Uri will be the server url / the custom Url provided
     */
    @ParameterizedTest
    @CsvSource("null,https://myserver.url", "https://customUri.url,https://customUri.url")
    fun generatesAuthUrlFromValidOAuthClient(uriProvided: String, expectedRedirectUri:String) {
        val redirectUri = if (uriProvided == "null") null else uriProvided
        val (_, validOauth2Client) = aValidOauth2Client(redirectUri)

        val mockedServerInfo = Mockito.mock(ServerInfo::class.java)
        whenever(mockedServerInfo.currentUri()).thenReturn(expectedRedirectUri)

        val credentialHelper = GoogleCredentialHelper()
        val oauth2Cache = OAuth2Cache()
        val googleOAuth2CredentialProvider = GoogleOAuth2CredentialProvider(credentialHelper, oauth2Cache, mockedServerInfo)

        val authUrl = googleOAuth2CredentialProvider.generateAuthUrl(validOauth2Client)

        // https://accounts.google.com/o/oauth2/auth?client_id=anId&redirect_uri=http://uri&response_type=code&scope=scope1%20scope2
        assertThat(authUrl).isNotNull()
        assertIsHttps(authUrl)
        assertClientIdIs(authUrl, validOauth2Client.clientId)
        assertRedirectUri(authUrl, expectedRedirectUri)
        assertScopesAre(authUrl, validOauth2Client.scopes)
        assertThat(authUrl).contains("prompt=consent")
        assertThat(authUrl).contains("access_type=offline")
    }

    /**
     * Given a valid oauth2 client is provided
     * And an auth code has been requested
     * When validating the returned code
     * Then validation passes
     * And accessToken is issued
     * And refreshToken is issued
     */
    @Test
    fun validateAuthorizationCodeIsSuccessFul() {
        val (_, validOauth2Client) = aValidOauth2Client(null)
        val aValidAuthorizationCode = "authCode"
        val aUserId = "aUserId";
        val aCurrentUri = "http://anUri"

        val testTokenData = tokenData()
        val aValidResponse = mockTokenResponseData(testTokenData)

        val authorizationCodeTokenRequest = Mockito.mock(AuthorizationCodeTokenRequest::class.java)
        whenever(authorizationCodeTokenRequest.setRedirectUri(any())).thenReturn(authorizationCodeTokenRequest)
        whenever(authorizationCodeTokenRequest.execute()).thenReturn(aValidResponse)

        val authorizationCodeFlow = Mockito.mock(AuthorizationCodeFlow::class.java)
        whenever(authorizationCodeFlow.newTokenRequest(any())).thenReturn(authorizationCodeTokenRequest)

        val credential = Mockito.mock(Credential::class.java)
        whenever(authorizationCodeFlow.createAndStoreCredential(any(), any())).thenReturn(credential)

        val credentialHelper = GoogleCredentialHelper()
        val credentialHelperSpy = spy(credentialHelper)
        doReturn(authorizationCodeFlow).whenever(credentialHelperSpy).getCodeFlow(any())

        val mockedServerInfo = Mockito.mock(ServerInfo::class.java)
        whenever(mockedServerInfo.currentUri()).thenReturn(aCurrentUri)

        val oauth2Cache = mockOAuth2Cache(validOauth2Client.clientId, aCurrentUri)

        val googleOAuth2CredentialProvider = GoogleOAuth2CredentialProvider(credentialHelperSpy, oauth2Cache, mockedServerInfo)
        val oauth2Credential = googleOAuth2CredentialProvider.validateCode(aUserId, aValidAuthorizationCode)

        assertThat(oauth2Credential).isNotNull
        assertThat(oauth2Credential.accessToken).isEqualTo(testTokenData.accessToken)
        assertThat(oauth2Credential.refreshToken).isEqualTo(testTokenData.refreshToken)
    }

    @Test
    fun validationIsNotSuccessfulWhenErrorsHappenedDuringTokenRequest() {

        val (_, oauth2Client) = aValidOauth2Client(null)
        val clientId = oauth2Client.clientId
        val redirectUri = "https://redirectUri"

        // Given: auth Url has been generated and invoked
        // And: an error is thrown when getting a token
        val oauth2CacheMock = mockOAuth2Cache(clientId, redirectUri)

        val authCodeFlow = Mockito.mock(AuthorizationCodeFlow::class.java)
        val tokenRequest = Mockito.mock(AuthorizationCodeTokenRequest::class.java)
        whenever(tokenRequest.execute()).thenThrow(TokenResponseException::class.java)
        whenever(authCodeFlow.newTokenRequest(any())).thenReturn(tokenRequest)

        val spiedCredentialHelper = spy(GoogleCredentialHelper())
        doReturn(authCodeFlow).whenever(spiedCredentialHelper).getCodeFlow(oauth2Client.clientId)

        val mockedServerInfo = Mockito.mock(ServerInfo::class.java)
        whenever(mockedServerInfo.currentUri()).thenReturn("uri")

        val authCode = "authCode"
        val aUserId = "aUserId";

        // When: validating a returned auth code
        val googleOAuth2CredentialProvider = GoogleOAuth2CredentialProvider(spiedCredentialHelper, oauth2CacheMock, mockedServerInfo)
        val exception = assertThrows<RuntimeException> {
            googleOAuth2CredentialProvider.validateCode(aUserId, authCode)
        }

        // Then: an error occurs for the passed clientId
        assertThat(exception.message).contains("Error validating code for clientId")
    }

    private fun mockOAuth2Cache(clientId: String, redirectUri: String): OAuth2Cache {
        val oauth2CacheMock = Mockito.mock(OAuth2Cache::class.java)
        whenever(oauth2CacheMock.findClientId(any())).thenReturn(clientId)
        whenever(oauth2CacheMock.findRedirectUri(any())).thenReturn(redirectUri)
        return oauth2CacheMock
    }

    private fun assertScopesAre(authUrl: String, scopes: Set<String>) {
        val concatenatedScopes = scopes.joinToString("%20")
        assertThat(authUrl).containsOnlyOnce("scope=$concatenatedScopes")
    }


    private fun mockCredentialHelper(oauth2Client: OAuth2Client,
                                     authCode: String,
                                     userIdCaptor: KArgumentCaptor<String>,
                                     tokenData: TokenData): GoogleCredentialHelper {

        val mockedCredentialHelper = Mockito.mock(GoogleCredentialHelper::class.java)
        val aValidResponse = mockTokenResponseData(tokenData)

        val redirectUri = "http://redirectUri"

        whenever(mockedCredentialHelper
                .validateAuthorizationCode(oauth2Client.clientId, redirectUri, authCode))
                .thenReturn(aValidResponse)

        val tokenResponseCaptor = argumentCaptor<TokenResponse>()
        val oauth2ClientCaptor = argumentCaptor<String>()

        doNothing().whenever(mockedCredentialHelper)
                .storeCredential(oauth2ClientCaptor.capture(),
                        userIdCaptor.capture(),
                        tokenResponseCaptor.capture())

       return mockedCredentialHelper
    }

    private data class TokenData(val accessToken: String, val refreshToken: String, val scopes: String)

    private fun mockTokenResponseData(testTokenData: TokenData): TokenResponse? {
        val aValidResponse = Mockito.mock(TokenResponse::class.java)
        whenever(aValidResponse.accessToken).thenReturn(testTokenData.accessToken)
        whenever(aValidResponse.refreshToken).thenReturn(testTokenData.refreshToken)
        whenever(aValidResponse.scope).thenReturn(testTokenData.scopes)
        return aValidResponse
    }

    private fun tokenData(): TokenData {
        val anAccessToken = "anAccessToken"
        val aRefreshToken = "aRefreshToken"
        val aResponseScope = "scope1"
        return TokenData(
                accessToken = anAccessToken,
                refreshToken = aRefreshToken,
                scopes = aResponseScope)
    }

    private fun assertRedirectUri(authUrl: String, aServerUrl: String) {
        assertThat(authUrl).containsOnlyOnce("redirect_uri=$aServerUrl")
    }

    private fun assertClientIdIs(authUrl: String, clientId: String) {
        assertThat(authUrl).containsOnlyOnce("client_id=$clientId")
    }

    private fun assertIsHttps(authUrl: String) {
        assertThat(authUrl).startsWith("https")
    }

    private fun aValidOauth2Client(redirectUri: String?): Pair<Set<String>, OAuth2Client> {
        val scopes = setOf("scope1", "scope2")
        val oAuth2Client = OAuth2Client(
                clientId = "anId",
                clientSecret = "aSecret",
                redirectUri = redirectUri,
                scopes = scopes)
        return Pair(scopes, oAuth2Client)
    }
}