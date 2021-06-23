package com.nyala.core.test.integration

import com.nyala.core.application.OAuth2UrlRequest
import com.nyala.core.application.dto.OAuth2ClientDto
import com.nyala.core.domain.model.oauth2.OAuth2Scopes
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.builder.ResponseSpecBuilder
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification
import io.vertx.junit5.VertxExtension
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Oauth2IT {

    companion object {
        @BeforeAll
        @JvmStatic
        internal fun setup() {
            IntegrationTestHelper.configureIntegrationTest()
        }

        @AfterAll
        @JvmStatic
        internal fun tearDown() {
            IntegrationTestHelper.tearDownIntegrationTest()
        }
    }

    @Test
    fun oauth2UrlSmokeTestWithoutScopes() {
        val oauth2ClientDto = OAuth2ClientDto(
                clientId =  "withoutScopes",
                clientSecret = "aSecret")
        val oauth2UrlRequest = OAuth2UrlRequest(oauth2ClientDto,
                userId = "test1")

        given()
            .spec(oauth2Request(oauth2UrlRequest))
            .log().all()
        .`when`()
            .post("/oauth2/authUrl")
        .then().log().all()
            .assertThat()
            .spec(okResponse())
            .body("authUrl", startsWith("https://accounts.google.com/o/oauth2/auth"))
    }

    @Test
    fun oauth2UrlSmokeTestWithScopes() {
        val oauth2ClientDto = OAuth2ClientDto(
                clientId =  "withScopes",
                clientSecret = "anotherSecret",
                scopes = OAuth2Scopes.forFullGmailAccess())
        val oauth2UrlRequest = OAuth2UrlRequest(oauth2ClientDto,
                userId = "test2")

        given()
            .spec(oauth2Request(oauth2UrlRequest))
            .log().all()
        .`when`()
            .post("/oauth2/authUrl")
        .then()
            .assertThat()
            .spec(okResponse())
        .and()
            .body("authUrl", startsWith("https://accounts.google.com/o/oauth2/auth"))
            .body("authUrl", containsString("scope="))
        //TODO: redirect uri check

    }

    private fun okResponse(): ResponseSpecification {
        return ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build()
    }

    private fun oauth2Request(oauth2UrlRequest: OAuth2UrlRequest): RequestSpecification {
        return RequestSpecBuilder()
                .addHeader("Accept-Encoding", "application/json")
                .addHeader("Content-Type", "application/json")
                .setBody(oauth2UrlRequest)
                .build()
    }
}