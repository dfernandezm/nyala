package com.nyala.core.test.integration;

import com.nyala.core.test.unit.TestHelper;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static io.restassured.RestAssured.given;

// https://vertx.io/docs/vertx-junit5/java/#_a_test_context_for_asynchronous_executions
// https://vertx.io/docs/vertx-junit5/java/#_use_it_in_your_build
// https://livebook.manning.com/book/vertx-in-action/chapter-8
@ExtendWith(VertxExtension.class)
class M3uReceiverIT {

    private final TestHelper testHelper = new TestHelper();

    @BeforeAll
    public static void setup() {
        IntegrationTestHelper.configureIntegrationTest();
    }

    @AfterEach
    public void tearDown() {
        IntegrationTestHelper.tearDownIntegrationTest();
    }


    @Test
    void shouldUploadMultipart() {
            File m3uFile = testHelper.readFile("testdata/samplePlaylist.m3u");
            given()
                    .multiPart("file", m3uFile)
                    .expect().statusCode(201)
                    .when()
                    .post("/m3u");
    }
}
