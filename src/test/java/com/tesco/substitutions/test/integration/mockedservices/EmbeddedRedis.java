package com.tesco.substitutions.test.integration.mockedservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import java.io.IOException;
import redis.embedded.RedisServer;

public class EmbeddedRedis extends AbstractVerticle {

    private static final String PORT_JSON_KEY = "port";
    private static RedisServer server;

    public static void stopRedis() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Override
    public void start(final Future<Void> future) {
        try {
            server = new RedisServer(this.config().getInteger(PORT_JSON_KEY));
            server.start(); // seems to be blocking
            future.complete();
        } catch (final IOException ioe) {
            future.fail(ioe);
        }
    }

    @Override
    public void stop(final Future<Void> future) {
        stopRedis();
        future.complete();
    }

}