package com.tesco.substitutions.application.verticle;


import com.tesco.substitutions.commons.shutdown.ShutdownUtils;
import com.tesco.substitutions.commons.vertx.verticle.Channel;
import com.tesco.substitutions.infrastructure.module.IptvAggregatorConfig;
import io.micronaut.context.BeanContext;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Referenced in the config.json
 *
 */
@Slf4j
public class ChannelsVerticle extends AbstractVerticle {

    private BeanContext beanContext;

    @Override
    public void start(final Future<Void> startFuture) {
        // This should probably be in the constructor
        this.beanContext = BeanContext.run();
        this.beanContext.registerSingleton(new IptvAggregatorConfig(this.vertx), true);
        this.startHttpServer(startFuture);
    }

    private void startHttpServer(final Future<Void> startFuture) {
        final Integer port = this.config().getInteger("http.port");
        this.httpServer(port).rxListen(port).subscribe(httpServer -> {
            log.info("Substitutions HTTP server started at: {} on port {}",
                    ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT), port);
            log.info("Substitutions Verticle deployed");
            startFuture.complete();
        }, error -> {
            log.error("Server unable to start: {}", error.getMessage());
            startFuture.fail(error.getMessage());
        });
    }

    private HttpServer httpServer(Integer port) {
        log.info("Setting up HTTP server");
        final HttpServerOptions options = new HttpServerOptions();

        final Router router = this.setupRouter(port);

        ShutdownUtils.setupReadinessLivenessProbe(router, "/internal/healthCheck", (rc) ->
                rc.response()
                        .putHeader("Content-Type", "text/plain")
                        .setStatusCode(200).end("OK"));

        final HttpServer server = this.vertx.createHttpServer(options);
        ShutdownUtils.setServer(server);
        ShutdownUtils.setEventBus(this.vertx.eventBus().getDelegate());
        server.requestHandler(router::accept);
        return server;
    }

    // https://github.com/vert-x3/vertx-examples/blob/4.x/web-examples/src/main/java/io/vertx/example/web/rest/SimpleREST.java
    private Router setupRouter(Integer port) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/channels").handler(this::handleGetChannels);
        return router;
    }

    private void handleGetChannels(RoutingContext routingContext) {
        String channelId = routingContext.request().getParam("channelId");
        HttpServerResponse response = routingContext.response();
        Channel channel = Channel.builder()
                .country("ES")
                .name("Cuatro HD")
                .build();

        response.putHeader("content-type", "application/json").end(Json.encodePrettily(channel));
    }
}
