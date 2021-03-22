package com.nyala.server.test.integration;

import io.restassured.http.ContentType;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileNotFoundException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@ExtendWith(VertxExtension.class)
public class StatusRestIT {

    @BeforeAll
    public void beforeAllTests(VertxTestContext vertxTestContext) throws FileNotFoundException {
        //VertxTestContext vertxTestContext = new VertxTestContext();
        //IntegrationTestHelper.configureTestSuite(vertxTestContext);
    }

    @AfterClass
    public static void tearDown() {
        IntegrationTestHelper.tearDownTestSuite();
    }

    @Test
    void status_endpoint_returns_alive_message(Vertx vertx, VertxTestContext vertxTestContext) {
        vertxTestContext.verify(() -> {
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
        });
    }
}
