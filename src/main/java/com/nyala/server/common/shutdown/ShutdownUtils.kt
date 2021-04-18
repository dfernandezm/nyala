package com.nyala.server.common.shutdown

import com.nyala.server.common.logging.LoggerHandler
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.http.HttpServer
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import rx.Completable
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

class ShutdownUtils {

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ShutdownUtils::class.java)
        private const val FAILURE_THRESHOLD = 2
        private const val PERIOD_SECONDS = 2
        private var READINESS_PROBE_DELAY: Long? = null
        private var SERVER_CLOSING_DELAY: Long? = null
        private var SHUTDOWN_TIMEOUT: Long? = null

        @Volatile
        private var isShuttingDown = false

        @Volatile
        private var isBeingHandled = false

        @Volatile
        private var gracefulDoneLatch: CountDownLatch? = null
        private var httpServer: HttpServer? = null
        private var eventBus: EventBus? = null

        private fun waitForLatch(gracefulDoneLatch: CountDownLatch?, completionHandler: Runnable) {
            Observable.fromCallable {
                try {
                    gracefulDoneLatch!!.await(SHUTDOWN_TIMEOUT as Long, TimeUnit.MILLISECONDS)
                } catch (exception: InterruptedException) {
                    exception.printStackTrace()
                }
                null
            }.subscribeOn(Schedulers.io()).subscribe { completionHandler.run() }
        }

        @Synchronized
        fun handleShutdown(vertx: Vertx, completionHandler: Runnable) {
            isShuttingDown = true
            if (!isBeingHandled) {
                isBeingHandled = true
                Observable.fromCallable {
                    LOGGER.info("Starting graceful shutdown")
                    gracefulShutdown(vertx, gracefulDoneLatch)
                    null
                }.subscribeOn(Schedulers.io()).subscribe()
            } else {
                LOGGER.info("Shutting down in progress")
            }
            waitForLatch(gracefulDoneLatch, completionHandler)
        }

        fun setupReadinessLivenessProbe(router: Router, endpoint: String, unhealthyResourcesMessage: String?, probeCompletable: Completable) {
            buildProbeEndpoint(endpoint, router)
            { rc: RoutingContext, json: JsonObject ->
                probeCompletable.subscribe({
                    json.put("health", "OK")
                    rc.response().setStatusCode(200).end(json.encodePrettily())
                }) {
                    json.put("health", "NOT OK")
                    LOGGER.info(unhealthyResourcesMessage)
                    rc.response().setStatusCode(500).end(json.encodePrettily())
                }
            }
        }

        fun setupReadinessLivenessProbe(router: Router, endpoint: String, probeHandler: (RoutingContext) -> Unit) {
            buildProbeEndpoint(endpoint, router) {
                rc: RoutingContext, _: JsonObject ->
                probeHandler(rc)
            }
        }

        private fun buildProbeEndpoint(endpoint: String, router: Router, handlerConsumer: (RoutingContext, JsonObject) -> Unit) {
            router[endpoint].handler { rc: RoutingContext ->
                val json = JsonObject()
                if (isShuttingDown) {
                    LOGGER.info("Server is shutting down -- sending failure for readiness probe")
                    json.put("health", "NOT OK")
                    rc.response().setStatusCode(500).end(json.encodePrettily())
                } else {
                    handlerConsumer(rc, json)
                }
            }
        }

        private fun gracefulShutdown(vertx: Vertx, gracefulDoneLatch: CountDownLatch?) {
            LOGGER.info("Waiting for readiness probe to fail...")
            LOGGER.info("Pending requests is: {}", LoggerHandler.getPendingRequestsCount())
            LoggerHandler.logPendingRequests()
            vertx.setTimer(READINESS_PROBE_DELAY as Long) {
                LOGGER.info("Stopping HTTP server and EventBus...")
                LoggerHandler.logPendingRequests()
                closeHttpServer()
                closeEventBus()
                vertx.setTimer(SERVER_CLOSING_DELAY as Long) {
                    LOGGER.info("Triggering cleanup after waiting...")
                    performCleanup(gracefulDoneLatch)
                }
            }
        }

        private fun closeHttpServer() {
            if (httpServer != null) {
                httpServer!!.close { ar: AsyncResult<Void?> ->
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server closed")
                    } else {
                        LOGGER.error("Failed to close HTTP server", ar.cause())
                    }
                }
            }
        }

        private fun closeEventBus() {
            if (eventBus != null) {
                eventBus!!.close { ar: AsyncResult<Void?> ->
                    if (ar.succeeded()) {
                        LOGGER.info("Event bus closed")
                    } else {
                        LOGGER.error("Failed to close event bus", ar.cause())
                    }
                }
            }
        }

        private fun performCleanup(countDownLatch: CountDownLatch?) {
            LOGGER.info("Cleaning up resources and finalizing shutdown...")
            countDownLatch!!.countDown()
        }

        @JvmStatic
        fun stopVerticle(vertx: Vertx, stopFuture: Future<Void?>, config: JsonObject, completionHandler: Runnable) {
            if (isGracefulShutdownEnabled(config)) {
                handleShutdown(vertx, completionHandler)
            } else {
                stopFuture.complete()
            }
        }

        private fun isGracefulShutdownEnabled(config: JsonObject): Boolean {
            return if (config.getBoolean("gracefulShutdownEnabled") == null) false else config.getBoolean("gracefulShutdownEnabled")
        }

        fun setServer(server: HttpServer?) {
            httpServer = server
        }

        @JvmStatic
        fun setEventBus(coreEventBus: EventBus?) {
            eventBus = coreEventBus
        }

        init {
            READINESS_PROBE_DELAY = (2 + FAILURE_THRESHOLD * PERIOD_SECONDS) * 1000L
            SERVER_CLOSING_DELAY = 5000
            SHUTDOWN_TIMEOUT = 20000
            isShuttingDown = false
            isBeingHandled = false
            gracefulDoneLatch = CountDownLatch(1)
        }
    }

    fun registerShutdownHook(vertx: Vertx) {
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { unDeployVerticles(vertx, CountDownLatch(1)) }))
    }

    private fun unDeployVerticles(vertx: Vertx, countDownLatch: CountDownLatch) {
        val verticleIds = vertx.deploymentIDs().stream()
        Observable.from(verticleIds.map { deploymentID: String? -> vertx.rxUndeploy(deploymentID) }.toArray()).doOnCompleted {
            LOGGER.info("Undeploy has been issued for all verticles")
            countDownLatch.countDown()
        }.subscribe()
        try {
            countDownLatch.await(15000L, TimeUnit.MILLISECONDS)
        } catch (ie: InterruptedException) {
            ie.printStackTrace()
        }
    }
}