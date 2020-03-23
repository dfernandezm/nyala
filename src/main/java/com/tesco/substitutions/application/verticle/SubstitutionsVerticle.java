package com.tesco.substitutions.application.verticle;

import com.tesco.personalisation.commons.logging.LoggerHandler;
import com.tesco.personalisation.commons.routing.RouterFactory;
import com.tesco.personalisation.commons.shutdown.ShutdownUtils;
import com.tesco.substitutions.infrastructure.module.SubstitutionConfig;
import io.micronaut.context.BeanContext;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class SubstitutionsVerticle extends AbstractVerticle {

    private BeanContext beanContext;

    @Override
    public void start(final Future<Void> startFuture) {
        //TODO: This should probably be in the constructor
        this.beanContext = BeanContext.run();
        this.beanContext.registerSingleton(new SubstitutionConfig(this.vertx), true);
        this.startHttpServer(startFuture);
    }

    private void startHttpServer(final Future<Void> startFuture) {
        final Integer port = this.config().getInteger("http.port");
        this.httpServer().rxListen(port).subscribe(httpServer -> {
            log.info("Substitutions HTTP server started at: {} on port {}", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT), port);
            log.info("Substitutions Verticle deployed");
            startFuture.complete();
        }, error -> {
            log.error("Server unable to start: {}", error.getMessage());
            startFuture.fail(error.getMessage());
        });
    }

    private HttpServer httpServer() {
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
        // Inline injection of routerFactory and endpoints setup
        RouterFactory routerFactory = beanContext.getBean(RouterFactory.class);
        routerFactory.useLoggerHandler(new LoggerHandler());
        return routerFactory.globalRouter(this.vertx);
    }
}
