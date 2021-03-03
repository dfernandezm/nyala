package com.fazula.server.test.integration;

import io.restassured.http.ContentType;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class StatusRestIT {

    @BeforeClass
    public static void beforeAllTests(final TestContext context) throws FileNotFoundException {
        TestHelper.configureTestSuite(context);
    }

// TODO add a test for redis down?

    @AfterClass
    public static void tearDown() {
        TestHelper.tearDownTestSuite();
    }

    @Test
    public void status_endpoint_returns_alive_message() {
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
