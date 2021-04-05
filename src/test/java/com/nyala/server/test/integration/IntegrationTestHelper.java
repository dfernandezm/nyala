package com.nyala.server.test.integration;

import com.nyala.server.application.verticle.MainStarter;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;
import rx.Single;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IntegrationTestHelper {

    private static final String RESTASSURED_LOG_FILENAME = "restassured.log";
    private static final String CONFIG_JSON_FILE = "config/config.json";
    private static final int NUMBER_INSTANCES = 1;
    private static final String HTTP_LOCALHOST_BASE_URI = "http://localhost";
    private static final String HTTP_PORT_KEY = "http.port";
    private static Set<String> deploymentIDs;
    private static final String CONFIG_JSON_KEY = "config";
    private static final String REDIS_CONFIGURATION_KEY = "redisConfiguration";
    private static Vertx vertx;

    public static void configureIntegrationTest() {
        VertxTestContext vertxTestContext = new VertxTestContext();
        deployVerticlesWithHttpServer(vertxTestContext);
        try {
            vertxTestContext.awaitCompletion(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            throw new RuntimeException("Interrupted while waiting for integration test setup", ie);
        }
    }

    private static void deployVerticlesWithHttpServer(VertxTestContext context) {
        configureRestAssuredLog();
        Checkpoint serverStarted = context.checkpoint();

        vertx = Vertx.vertx();

        vertx.fileSystem().readFile(CONFIG_JSON_FILE, result -> {
            if (result.succeeded()) {
                final JsonObject config = result.result().toJsonObject();

                // Multideploy class expects this structure
                config.put("verticles", config.getJsonObject("config").getJsonArray("verticles"));

                final DeploymentOptions options = new DeploymentOptions().setInstances(
                        NUMBER_INSTANCES).setConfig(config);

                configureRestAssured(options);
                vertx.deployVerticle(MainStarter.class.getName(), options,
                        ar -> {
                            if (ar.succeeded()) {
                                saveDeploymentIds(vertx.deploymentIDs());
                                context.completeNow();
                            } else {
                                context.failNow(ar.cause());
                            }
                        });
            } else {
                context.failNow(result.cause());
            }
        });
    }

    private static void configureRestAssured(final DeploymentOptions options) {
        RestAssured.baseURI = HTTP_LOCALHOST_BASE_URI;
        RestAssured.basePath = "";
        RestAssured.port = options.getConfig().getInteger(HTTP_PORT_KEY);
    }

    private static void configureRestAssuredLog() {
        try  {
            final PrintStream fileOutPutStream = new PrintStream(new File(RESTASSURED_LOG_FILENAME));
            RestAssured.config = RestAssuredConfig.config()
                    .logConfig(new LogConfig().defaultStream(fileOutPutStream));
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException("Failed RestAssured Config", fnfe);
        }
    }

    private static void saveDeploymentIds(final Set<String> deploymentIDs) {
        IntegrationTestHelper.deploymentIDs = deploymentIDs;
    }

    private static RedisClient getRedisClient(Vertx vertx) {
        JsonObject config = (vertx.fileSystem().readFileBlocking(CONFIG_JSON_FILE).toJsonObject());
        RedisOptions redisOptions = new RedisOptions(config.getJsonObject(CONFIG_JSON_KEY).getJsonObject(REDIS_CONFIGURATION_KEY));
        return RedisClient.create(vertx, redisOptions);
    }

    public static void tearDownIntegrationTest() {
        RestAssured.reset();
        undeployVerticles();
        waitUntilVertxContextIsClosed();
    }

    private static void undeployVerticles() {
        deploymentIDs.forEach(id -> vertx.rxUndeploy(id).toBlocking().value());
    }

    private static void waitUntilVertxContextIsClosed() {
        final Single<Void> result = vertx.rxClose();
        result.toBlocking().value();
    }
}