package com.tesco.substitutions.application.verticle;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tesco.personalisation.commons.logging.LoggerHandler;
import com.tesco.personalisation.commons.routing.RouterFactory;
import com.tesco.personalisation.commons.shutdown.ShutdownUtils;
import com.tesco.substitutions.infrastructure.module.SubstitutionsBinder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubstitutionsVerticle extends AbstractVerticle {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private Injector injector;

    @Override
    public void start(final Future<Void> startFuture) {
        this.getOrCreateInjector().injectMembers(this);
        this.startHttpServer(startFuture);
    }

    private void startHttpServer(final Future<Void> startFuture) {
        final Integer port = this.config().getInteger("http.port");
        this.httpServer().rxListen(port).subscribe(httpServer -> {
            this.LOGGER.info("Substitutions HTTP server started at: {} on port {}", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                    port);
            this.LOGGER.info("Substitutions Verticle deployed");
            startFuture.complete();
        }, error -> {
            this.LOGGER.error("Server unable to start: {}", error.getMessage());
            startFuture.fail(error.getMessage());
        });
    }

    private Injector getOrCreateInjector() {
        if (this.injector == null) {
            this.injector = Guice.createInjector(new SubstitutionsBinder(this.vertx));
        }
        return this.injector;
    }

    private HttpServer httpServer() {
        this.LOGGER.info("Setting up HTTP server");
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
        final RouterFactory routerFactory = this.getOrCreateInjector().getInstance(RouterFactory.class);
        routerFactory.useLoggerHandler(new LoggerHandler());
        return routerFactory.globalRouter(this.vertx);
    }

}
