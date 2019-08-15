package com.tesco.substitutions.test.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import com.tesco.substitutions.application.verticle.MainStarter;
import com.tesco.substitutions.infrastructure.endpoints.SubstitutionsRoutes;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Single;

@RunWith(VertxUnitRunner.class)
public class SubstitutionsStatusRestIT {

    private static final String RESTASSURED_LOG_FILENAME = "restassured.log";
    private static final String CONFIG_JSON_FILE = "config/config.json";
    private static final int NUMBER_INSTANCES = 1;
    private static final String HTTP_LOCALHOST_BASE_URI = "http://localhost";
    private static final String HTTP_PORT_KEY = "http.port";
    private static Vertx vertx;
    private static Set<String> deploymentIDs;

    //TODO refactor once we do the refactor of the tests code. Duplicate code.
    @BeforeClass
    public static void beforeAllTests(final TestContext context) throws FileNotFoundException {

        ConfigureRestAssuredLog();

        final Async async = context.async();
        vertx = Vertx.vertx();

        vertx.fileSystem().readFile(CONFIG_JSON_FILE, result -> {
            if (result.succeeded()) {
                final JsonObject config = result.result().toJsonObject();

                //Multideploy class expects this structure
                config.put("verticles", config.getJsonObject("config").getJsonArray("verticles"));

                final DeploymentOptions options = new DeploymentOptions().setInstances(
                        NUMBER_INSTANCES).setConfig(config);

                configureRestAssured(options);
                vertx.deployVerticle(MainStarter.class.getName(), options,
                        ar -> {
                            if (ar.succeeded()) {
                                async.complete();
                                saveDeplomentIds(vertx.deploymentIDs());

                            } else {
                                context.fail(ar.cause());
                            }
                        });

            } else {
                context.fail(result.cause());
            }
        });

    }

    private static void saveDeplomentIds(final Set<String> deploymentIDs) {
        SubstitutionsStatusRestIT.deploymentIDs = deploymentIDs;
    }

    private static void ConfigureRestAssuredLog() throws FileNotFoundException {
        final PrintStream fileOutPutStream = new PrintStream(new File(RESTASSURED_LOG_FILENAME));
        RestAssured.config = RestAssuredConfig.config()
                .logConfig(new LogConfig().defaultStream(fileOutPutStream));
    }

    private static void configureRestAssured(final DeploymentOptions options) {
        RestAssured.baseURI = HTTP_LOCALHOST_BASE_URI;
        RestAssured.basePath = SubstitutionsRoutes.SUBSTITUTIONS_MODULE_BASE_PATH;
        RestAssured.port = options.getConfig().getInteger(HTTP_PORT_KEY);
    }

// TODO add a test for redis down?

    @AfterClass
    public static void tearDown() {
        RestAssured.reset();
        undeployVerticles();
        waitUntilVertxContextIsClosed();
    }

    private static void undeployVerticles() {
        deploymentIDs.forEach(id -> {
            vertx.rxUndeploy(id).toBlocking().value();
        });
    }

    private static void waitUntilVertxContextIsClosed() {
        final Single<Void> result = vertx.rxClose();
        result.toBlocking().value();
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
