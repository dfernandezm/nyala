package com.nyala.server.infrastructure.mail.oauth2.google

import com.nyala.server.infrastructure.mail.GoogleOAuth2CredentialProvider
import com.nyala.server.infrastructure.mail.ServerInfo
import com.nyala.server.infrastructure.mail.oauth2.google.google.GoogleCredentialHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito
import org.mockito.kotlin.whenever


class GoogleOAuth2CredentialProviderTest {

    //TODO: Not working atm: https://www.baeldung.com/kotlin/junit-5-kotlin
    @ParameterizedTest
    @CsvSource("null,https://myserver.url", "https://anUri,https://myserver.url")
    fun generatesAuthUrlFromValidOAuthClient(clientId: String, expectedRedirectUri:String) {
        val clientIdVal = if (clientId == "null") null else clientId
        val (_, validOauth2Client) = aValidOauth2Client(clientIdVal)

        val mockedServerInfo = Mockito.mock(ServerInfo::class.java)
        whenever(mockedServerInfo.currentUri()).thenReturn(expectedRedirectUri)

        val credentialHelper = GoogleCredentialHelper()
        val googleOAuth2CredentialProvider = GoogleOAuth2CredentialProvider(credentialHelper, mockedServerInfo)

        val authUrl = googleOAuth2CredentialProvider.generateAuthUrl(validOauth2Client)

        // https://accounts.google.com/o/oauth2/auth?client_id=anId&redirect_uri=http://uri&response_type=code&scope=scope1%20scope2
        assertThat(authUrl).isNotNull()
        assertIsHttps(authUrl)
        assertClientIdIs(authUrl, validOauth2Client.clientId)
        assertRedirectUri(authUrl, expectedRedirectUri)
        assertScopesAre(authUrl, validOauth2Client.scopes)
    }

    @ParameterizedTest
    @CsvSource(
            "1, 1",
            "2, 4",
            "3, 9"
    )
    fun testSquares(input: Int, expected: Int) {
        Assertions.assertEquals(expected, input * input)
    }

    private fun assertScopesAre(authUrl: String, scopes: Set<String>) {
        val concatenatedScopes = scopes.joinToString("%20")
        assertThat(authUrl).containsOnlyOnce("scope=$concatenatedScopes")
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