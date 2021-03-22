package com.nyala.server.test.integration;

import com.nyala.server.test.unit.TestHelper;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileNotFoundException;

import static io.restassured.RestAssured.given;

// https://vertx.io/docs/vertx-junit5/java/#_a_test_context_for_asynchronous_executions
// https://vertx.io/docs/vertx-junit5/java/#_use_it_in_your_build
@ExtendWith(VertxExtension.class)
class M3uReceiverIT {

    private TestHelper testHelper = new TestHelper();

    @BeforeEach
    public void setup() throws FileNotFoundException {
        //IntegrationTestHelper.configureTestSuite(testContext);
    }

    @AfterEach
    public void tearDown() {
        IntegrationTestHelper.tearDownTestSuite();
    }

    // https://livebook.manning.com/book/vertx-in-action/chapter-8
    @Test
    void shouldUploadMultipart(VertxTestContext testContext) throws FileNotFoundException {
        IntegrationTestHelper.configureTestSuite(testContext, () -> {
            File m3uFile = testHelper.readFile("testdata/samplePlaylist.m3u");
            given()
                    .multiPart("file", m3uFile)
                    .expect().statusCode(201)
                    .when()
                    .post("/m3u");
            testContext.completeNow();
            return null;
        });

    }
}
