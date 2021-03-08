//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.nyala.server.test.integration;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import redis.embedded.RedisServer;

import java.io.IOException;

public class EmbeddedRedis extends AbstractVerticle {
    private static RedisServer server;

    public EmbeddedRedis() {
    }

    public void start(Future<Void> future) {
        try {
            server = new RedisServer(this.config().getInteger("port"));
            server.start();
            future.complete();
        } catch (IOException var3) {
            future.fail(var3);
        }

    }

    public void stop(Future<Void> future) {
        stopRedis();
        future.complete();
    }

    public static void stopRedis() {
        if (server != null) {
            server.stop();
            server = null;
        }

    }
}
