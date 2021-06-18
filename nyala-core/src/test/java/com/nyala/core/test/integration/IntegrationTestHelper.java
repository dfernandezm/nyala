package com.nyala.core.test.integration;

import com.nyala.core.application.verticle.MainStarter;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;
import rx.Single;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
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
        int port = getRandomPort();
        int redisPort = getRandomPort();

        vertx = Vertx.vertx();

        vertx.fileSystem().readFile(CONFIG_JSON_FILE, result -> {
            if (result.succeeded()) {
                final JsonObject config = result.result().toJsonObject();

                // Multideploy class expects this structure

                // Set HTTP server port
                JsonArray verticles =  config.getJsonObject("config")
                        .getJsonArray("verticles");

                JsonObject httpServer = verticles
                        .getJsonObject(2);

                httpServer.getJsonObject("options")
                        .getJsonObject("config")
                        .put("http.port",  port);

                // Set Redis port
                config.getJsonObject("config")
                        .getJsonObject("redisConfiguration")
                        .put("port", redisPort);

                JsonObject embeddedRedis = verticles
                        .getJsonObject(4);

//                config.getJsonObject("config")
//                        .getJsonArray("verticles")
//                        .getJsonObject(3)
                embeddedRedis.getJsonObject("options")
                        .getJsonObject("config")
                        .put("port", redisPort);

                config.put("verticles", config.getJsonObject("config").getJsonArray("verticles"));
                config.put("http.port", port);

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

    private static void setEmbeddedRedisPort(int redisPort, JsonObject config) {
        config.getJsonObject("config")
                .getJsonArray("verticles")
                .getJsonObject(4)
                .getJsonObject("options")
                .getJsonObject("config")
                .put("port", redisPort);

//        JsonArray verticles = config.getJsonObject("config").getJsonArray("verticles");
//        int numberOfVerticles = verticles.size();
//
//        IntStream.range(0, numberOfVerticles)
//                .mapToObj(verticles::getJsonObject)
//                .filter(verticle -> verticle.getString("main").contains("EmbeddedRedis"))
//                .map(httpServerVerticle -> httpServerVerticle.put("port", redisPort))
//                .close();
    }

    private static void setRedisPort(int redisPort, JsonObject config) {
        config.getJsonObject("config")
                .getJsonObject("redisConfiguration")
                .put("port", redisPort);
    }

    private static void setHttpServerPort(int port, JsonObject config) {
        config.getJsonObject("config")
                .getJsonArray("verticles")
                .getJsonObject(2)
                .getJsonObject("options")
                .getJsonObject("config")
                .put("http.port",  port);

//        JsonArray verticles = config.getJsonObject("config").getJsonArray("verticles");
//        int numberOfVerticles = verticles.size();
//
//        IntStream.range(0, numberOfVerticles)
//                .mapToObj(verticles::getJsonObject)
//                .filter(verticle -> verticle.getString("main").contains("HttpServerVerticle"))
//                .map(httpServerVerticle -> httpServerVerticle.put("http.port", port))
//                .close();
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
        EmbeddedRedis.stopRedis();
    }

    private static void undeployVerticles() {
        if (deploymentIDs != null) {
            deploymentIDs.forEach(id -> vertx.rxUndeploy(id).toBlocking().value());
        }
    }

    private static void waitUntilVertxContextIsClosed() {
        final Single<Void> result = vertx.rxClose();
        result.toBlocking().value();
    }

    private static int getRandomPort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
           throw new RuntimeException("Error generating port for tests", e);
        }
    }
}
