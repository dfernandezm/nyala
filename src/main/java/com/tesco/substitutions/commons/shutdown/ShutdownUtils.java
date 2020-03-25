package com.tesco.substitutions.commons.shutdown;

import com.tesco.personalisation.commons.logging.LoggerHandler;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Completable;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class ShutdownUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownUtils.class);
    private static final Integer FAILURE_THRESHOLD = 2;
    private static final Integer PERIOD_SECONDS = 2;
    private static final Integer READINESS_PROBE_DELAY;
    private static final Integer SERVER_CLOSING_DELAY;
    private static final Integer SHUTDOWN_TIMEOUT;
    private static volatile boolean isShuttingDown;
    private static volatile boolean isBeingHandled;
    private static volatile CountDownLatch gracefulDoneLatch;
    private static HttpServer httpServer;
    private static EventBus eventBus;

    public static void waitForLatch(CountDownLatch gracefulDoneLatch, Runnable completionHandler) {
        Observable.fromCallable(() -> {
            try {
                gracefulDoneLatch.await((long)SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }

            return null;
        }).subscribeOn(Schedulers.io()).subscribe((r) -> {
            completionHandler.run();
        });
    }

    public static synchronized void handleShutdown(Vertx vertx, Runnable completionHandler) {
        isShuttingDown = true;
        if (!isBeingHandled) {
            isBeingHandled = true;
            Observable.fromCallable(() -> {
                LOGGER.info("Starting graceful shutdown");
                gracefulShutdown(vertx, gracefulDoneLatch);
                return null;
            }).subscribeOn(Schedulers.io()).subscribe();
        } else {
            LOGGER.info("Shutting down in progress");
        }

        waitForLatch(gracefulDoneLatch, completionHandler);
    }

    public static void setupReadinessLivenessProbe(Router router, String endpoint, String unhealthyResourcesMessage, Completable probeCompletable) {
        buildProbeEndpoint(endpoint, router, (rc, json) -> {
            probeCompletable.subscribe(() -> {
                json.put("health", "OK");
                rc.response().setStatusCode(200).end(json.encodePrettily());
            }, (onError) -> {
                json.put("health", "NOT OK");
                LOGGER.info(unhealthyResourcesMessage);
                rc.response().setStatusCode(500).end(json.encodePrettily());
            });
        });
    }

    public static void setupReadinessLivenessProbe(Router router, String endpoint, Handler<RoutingContext> probeHandler) {
        buildProbeEndpoint(endpoint, router, (rc, json) -> {
            probeHandler.handle(rc);
        });
    }

    private static void buildProbeEndpoint(String endpoint, Router router, BiConsumer<RoutingContext, JsonObject> handlerConsumer) {
        router.get(endpoint).handler((rc) -> {
            JsonObject json = new JsonObject();
            if (isShuttingDown) {
                LOGGER.info("Server is shutting down -- sending failure for readiness probe");
                json.put("health", "NOT OK");
                rc.response().setStatusCode(500).end(json.encodePrettily());
            } else {
                handlerConsumer.accept(rc, json);
            }

        });
    }

    private static void gracefulShutdown(Vertx vertx, CountDownLatch gracefulDoneLatch) {
        LOGGER.info("Waiting for readiness probe to fail...");
        LOGGER.info("Pending requests is: {}", LoggerHandler.getPendingRequestsCount());
        LoggerHandler.logPendingRequests();
        vertx.setTimer((long)READINESS_PROBE_DELAY, (timerId) -> {
            LOGGER.info("Stopping HTTP server and EventBus...");
            LoggerHandler.logPendingRequests();
            closeHttpServer();
            closeEventBus();
            vertx.setTimer((long)SERVER_CLOSING_DELAY, (t) -> {
                LOGGER.info("Triggering cleanup after waiting...");
                performCleanup(gracefulDoneLatch);
            });
        });
    }

    private static void waitForPendingRequests(Vertx vertx, ShutdownUtils.CommandInterface handler) {
        if (LoggerHandler.getPendingRequestsCount() != 0) {
            vertx.setPeriodic(250L, (timerId) -> {
                LOGGER.info("Checking for pending requests...");
                if (LoggerHandler.getPendingRequestsCount() == 0) {
                    LOGGER.info("No pending requests -- continue with shutdown");
                    vertx.cancelTimer(timerId);
                    handler.accept();
                }

            });
        } else {
            LOGGER.info("No pending requests -- continue with shutdown");
            handler.accept();
        }

    }

    private static void closeHttpServer() {
        if (httpServer != null) {
            httpServer.close((ar) -> {
                if (ar.succeeded()) {
                    LOGGER.info("HTTP server closed");
                } else {
                    LOGGER.error("Failed to close HTTP server", ar.cause());
                }

            });
        }

    }

    private static void closeEventBus() {
        if (eventBus != null) {
            eventBus.close((ar) -> {
                if (ar.succeeded()) {
                    LOGGER.info("Event bus closed");
                } else {
                    LOGGER.error("Failed to close event bus", ar.cause());
                }

            });
        }

    }

    private static void performCleanup(CountDownLatch countDownLatch) {
        LOGGER.info("Cleaning up resources and finalizing shutdown...");
        countDownLatch.countDown();
    }

    public void registerShutdownHook(Vertx vertx) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.unDeployVerticles(vertx, new CountDownLatch(1));
        }));
    }

    private void unDeployVerticles(Vertx vertx, CountDownLatch countDownLatch) {
        Stream<String> verticleIds = vertx.deploymentIDs().stream();
        Observable.from(verticleIds.map(vertx::rxUndeploy).toArray()).doOnCompleted(() -> {
            LOGGER.info("Undeploy has been issued for all verticles");
            countDownLatch.countDown();
        }).subscribe();

        try {
            countDownLatch.await(15000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public static void stopVerticle(Vertx vertx, Future<Void> stopFuture, JsonObject config, Runnable completionHandler) {
        if (isGracefulShutdownEnabled(config)) {
            handleShutdown(vertx, completionHandler);
        } else {
            stopFuture.complete();
        }

    }

    public static boolean isGracefulShutdownEnabled(JsonObject config) {
        return config.getBoolean("gracefulShutdownEnabled") == null ? false : config.getBoolean("gracefulShutdownEnabled");
    }

    public static void setServer(HttpServer server) {
        httpServer = server;
    }

    public static void setEventBus(EventBus coreEventBus) {
        eventBus = coreEventBus;
    }

    static {
        READINESS_PROBE_DELAY = (2 + FAILURE_THRESHOLD * PERIOD_SECONDS) * 1000;
        SERVER_CLOSING_DELAY = 5000;
        SHUTDOWN_TIMEOUT = 20000;
        isShuttingDown = false;
        isBeingHandled = false;
        gracefulDoneLatch = new CountDownLatch(1);
    }

    @FunctionalInterface
    private interface CommandInterface {
        void accept();
    }
}
