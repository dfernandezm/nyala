package com.nyala.core.test.integration;

import io.restassured.http.ContentType;
import io.vertx.junit5.VertxExtension;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@ExtendWith(VertxExtension.class)
public class StatusRestIT {

    @BeforeAll
    public static void setup() {
       IntegrationTestHelper.configureIntegrationTest();
    }

    @AfterAll
    public static void tearDown() {
        IntegrationTestHelper.tearDownIntegrationTest();
    }

    @Test
    public void statusEndpointTest() {
            given().
                    header("Accept-Encoding", "application/json").
                    log().all().
                    when().
                    get("/_status").
                    then().
                    log().all().
                    assertThat().
                    statusCode(HttpStatus.SC_OK).
                    contentType(ContentType.JSON).
                    body("message", equalTo("alive"));

    }
}
