package com.nyala.common.vertx;


import com.nyala.common.shutdown.ShutdownUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.CountDownLatch;

public class MultiDeploySetupVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiDeploySetupVerticle.class);

    public void start(Future<Void> startFuture) throws Exception {
        this.deployVerticles(startFuture);
    }

    protected void deployVerticles(Future<Void> startFuture) {
        JsonArray verticlesConfig = this.config().getJsonArray("verticles");
        int numberOfVerticles = verticlesConfig.size();
        Observable.from(verticlesConfig).flatMapSingle((verticleConfig) -> {
            LOGGER.debug(this.config().encodePrettily());
            JsonObject verticleConfigJson = new JsonObject(Json.encode(verticleConfig));
            JsonObject commonConfig = this.config().getJsonObject("config");
            JsonObject config = verticleConfigJson.getJsonObject("options").getJsonObject("config").mergeIn(commonConfig);
            LOGGER.info("Config: " + config);
            LOGGER.info("VerticleConfig: " + verticleConfigJson);
            return this.vertx.rxDeployVerticle(verticleConfigJson.getString("main"), new DeploymentOptions(verticleConfigJson.getJsonObject("options").put("config", config)));
        }).doOnNext((deploymentId) -> {
            LOGGER.info("Verticle has been deployed --- {}", deploymentId);
        }).doOnCompleted(() -> {
            LOGGER.info("All verticles deployed");
            this.vertx.getOrCreateContext().put("verticlesLatch", new CountDownLatch(numberOfVerticles));
            startFuture.complete();
        }).doOnError((error) -> {
            LOGGER.error("Error deploying verticle ", error);
            startFuture.fail(error);
        }).subscribe();
    }

    protected void stop(Future<Void> stopFuture, JsonObject config) {
        ShutdownUtils.stopVerticle(this.vertx, stopFuture, config, () -> {
            LOGGER.info("Stopping Starter Verticle");
            stopFuture.complete();
        });
    }
}
