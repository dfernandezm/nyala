package com.fazula.server.common.vertx.verticle;


import com.fazula.server.common.shutdown.ShutdownUtils;
import com.fazula.server.common.vertx.redis.RedisAdapter;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.redis.RedisClient;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;

public class StatusVerticle extends AbstractVerticle {
    public static final String STATUS_ADDRESS = "status";
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusVerticle.class);
    private static final String ALIVE_MESSAGE = "{\"message\" : \"alive\"} ";
    private static final String REDIS_CONFIGURATION_JSON_KEY = "redisConfiguration";
    private RedisClient redisClient;

    public void start(Future<Void> startFuture) {
        this.initializeRedisClient();
        RedisAdapter.checkForRedisConnection(this.redisClient);
        this.vertx.eventBus().consumer(STATUS_ADDRESS).handler((message) -> {
            MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();
            multiMap.add(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            Single.just(new JsonObject()).flatMapCompletable((r) -> {
                return RedisAdapter.verifyRedisIsUp(this.redisClient);
            }).subscribe(() -> {
                message.reply(new JsonObject(ALIVE_MESSAGE), (new DeliveryOptions()).setHeaders(multiMap));
            }, (error) -> {
                LOGGER.error("Error in status endpoint", error);
                message.fail(502, error.getMessage());
            });
        });
        LOGGER.info("Status Verticle deployed");
        startFuture.complete();
    }

    private void initializeRedisClient() {
        JsonObject redisConfiguration = this.context.config().getJsonObject(REDIS_CONFIGURATION_JSON_KEY);
        RedisOptions redisOptions = new RedisOptions(redisConfiguration);
        this.redisClient = RedisClient.create(this.vertx, redisOptions);
    }

    public void stop(Future<Void> stopFuture) {
        ShutdownUtils.stopVerticle(this.vertx, stopFuture, this.config(), () -> {
            LOGGER.info("Stopping StatusVerticle");
            this.waitForRedisClientClose();
            stopFuture.complete();
        });
    }

    private void waitForRedisClientClose() {
        this.redisClient.rxClose().toBlocking().value();
    }
}
