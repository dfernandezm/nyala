package com.nyala.server.application.verticle;


import com.nyala.server.application.handler.StatusEndpointHandler;
import com.nyala.server.common.shutdown.ShutdownUtils;
import com.nyala.server.domain.model.Channel;
import com.nyala.server.infrastructure.config.NyalaConfig;
import io.micronaut.context.BeanContext;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import rx.Single;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Referenced in the config.json
 *
 */
// https://vertx.io/docs/vertx-rx/java2/
@Slf4j
public class ChannelsVerticle extends AbstractVerticle {

    private StatusEndpointHandler statusEndpointHandler;

    @Override
    public void start(final Future<Void> startFuture) {
        BeanContext beanContext = BeanContext.run();
        populateBeanContext(beanContext);
        this.statusEndpointHandler = beanContext.getBean(StatusEndpointHandler.class);
        this.startHttpServer(startFuture);
    }

    private void populateBeanContext(BeanContext beanContext) {
        beanContext.registerSingleton(new NyalaConfig(this.vertx), true);
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
        final Router router = this.setupRouter();

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

    private Router setupRouter() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/channels").handler(this::handleGetChannels);
        router.get("/_status").handler(statusEndpointHandler::status);
        router.post("/m3u").handler(this::handleM3uUpload);
        return router;
    }

    private Single<String> readUploadedM3uPlaylist(FileUpload f) {
        return vertx.fileSystem()
                .rxReadFile(f.uploadedFileName())
                .map(Buffer::toString);
    }

    // https://github.com/vert-x3/vertx-examples/blob/4.x/web-examples/src/main/java/io/vertx/example/web/rest/SimpleREST.java
    private void handleM3uUpload(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response.putHeader("Content-Type", "text/plain");
        response.setChunked(true);

        List<FileUpload> fileUploadList = new ArrayList<>(ctx.fileUploads());
        FileUpload fileUpload = fileUploadList.get(0);

        readUploadedM3uPlaylist(fileUpload)
                .subscribe( result -> {
                    log.info("Result " + result);
                    response.setStatusCode(201).end(result);
                });

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
