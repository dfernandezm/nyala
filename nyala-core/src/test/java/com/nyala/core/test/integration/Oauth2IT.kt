package com.nyala.core.test.integration

import com.nyala.core.application.Oauth2UrlRequest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.junit5.VertxExtension
import org.apache.http.HttpStatus
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
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
    fun oauth2UrlTest() {
        val oauth2UrlRequest = Oauth2UrlRequest(oauth2ClientId = "", oauth2ClientSecret = "", userId = "test")
        RestAssured
                .given()
                .header("Accept-Encoding", "application/json")
                .header("Content-Type", "application/json")
                .log().all()
                .body(oauth2UrlRequest)
                .`when`()
                .post("/oauth2/authUrl")
                .then().log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("authUrl", startsWith("https://accounts.google.com/o/oauth2/auth"))
    }
}