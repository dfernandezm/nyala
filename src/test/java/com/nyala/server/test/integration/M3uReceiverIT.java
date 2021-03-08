package com.nyala.server.test.integration;

import io.restassured.http.ContentType;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

// https://vertx.io/docs/vertx-junit5/java/#_a_test_context_for_asynchronous_executions
@RunWith(VertxUnitRunner.class)
public class M3uReceiverIT {

    @BeforeClass
    public static void beforeAllTests(final TestContext context) throws FileNotFoundException {
        IntegrationTestHelper.configureTestSuite(context);
    }


    @AfterClass
    public static void tearDown() {
        IntegrationTestHelper.tearDownTestSuite();
    }

    @Test
    public void status_endpoint_returns_alive_message() {
        given().
                header("Accept-Encoding", "application/json").
                log().all().
                when().
                post("/m3u").
                then().
                log().all().
                assertThat().
                statusCode(HttpStatus.SC_OK).
                contentType(ContentType.JSON).
                body("message", equalTo("alive"));
    }

    @Test
    public void shouldUploadMultipart() {
        File m3uFile = readFile("testdata/samplePlaylist.m3u");
        given()
                .multiPart("file", m3uFile)
                .expect().statusCode(201)
                .when()
                .post("/m3u");
    }

    private File readFile(String testFile) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File file = new File(classLoader.getResource(testFile).getFile());
        assertThat(file.exists(), is(true));
        return file;
    }
}
