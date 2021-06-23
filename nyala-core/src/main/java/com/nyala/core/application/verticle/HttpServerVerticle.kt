package com.nyala.core.application.verticle

import com.nyala.core.application.handler.StatusEndpointHandler
import com.nyala.common.shutdown.ShutdownUtils
import com.nyala.common.vertx.FailureExceptionHandler
import com.nyala.common.vertx.verticle.SharedCache
import com.nyala.core.application.handler.OAuth2Handler
import com.nyala.core.infrastructure.adapter.m3u.parser.M3uParser
import com.nyala.core.infrastructure.config.HttpServerModule
import com.nyala.core.infrastructure.di.IsolatedKoinVerticle
import com.nyala.core.infrastructure.di.KoinDIFactory
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.buffer.Buffer
import io.vertx.rxjava.core.http.HttpServer
import io.vertx.rxjava.core.http.HttpServerRequest
import io.vertx.rxjava.core.http.HttpServerResponse
import io.vertx.rxjava.ext.web.FileUpload
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.RoutingContext
import io.vertx.rxjava.ext.web.handler.BodyHandler
import lombok.extern.slf4j.Slf4j
import org.koin.core.component.inject
import org.slf4j.LoggerFactory.getLogger
import rx.Single
import java.util.*

/**
 * Referenced in the config.json
 *
 */
@Slf4j
class HttpServerVerticle : IsolatedKoinVerticle() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = getLogger(javaClass.enclosingClass)
    }

    private val statusEndpointHandler: StatusEndpointHandler by inject()
    private val oauth2Handler: OAuth2Handler by inject()

    private var uuid = ""

    override fun start(startFuture: Future<Void>) {
        uuid = UUID.randomUUID().toString()
        startDependencyInjection()
        startHttpServer(startFuture)
    }

    // https://stackoverflow.com/questions/63359639/vertx-webclient-shared-vs-single-across-multiple-verticles
    override fun getAppName(): String {
        val appName = "HttpServer:$uuid"
        log.info("App Name: $appName")
        return appName
    }

    private fun startDependencyInjection() {
        KoinDIFactory.startNewApp(getAppName()) {
            modules(HttpServerModule(vertx).httpServerModule)
        }
    }

    private fun startHttpServer(startFuture: Future<Void>) {
        val port = config().getInteger("http.port")
        httpServer().rxListen(port).subscribe({
            log.info("HTTP server started on port {}", port)
            log.info("Verticle deployed")
            startFuture.complete()
        }, { error: Throwable ->
            log.error("Server unable to start: {}", error.message)
            startFuture.fail(error.message)
        })
    }

    private fun httpServer(): HttpServer {
        log.info("Setting up HTTP server")
        val options = HttpServerOptions()
        val router = setupRouter()
        setupReadinessProbe(router)
        val server = vertx.createHttpServer(options)
        ShutdownUtils.setServer(server)
        ShutdownUtils.setEventBus(vertx.eventBus().delegate)
        server.requestHandler { request: HttpServerRequest? -> router.accept(request) }
        return server
    }

    private fun setupReadinessProbe(router: Router) {
        ShutdownUtils.setupReadinessLivenessProbe(router, "/internal/healthCheck") {
            rc: RoutingContext ->
                rc.response()
                        .putHeader("Content-Type", "text/plain")
                        .setStatusCode(200).end("OK")
        }
    }

    private fun setupRouter(): Router {

        log.info("Status endpoint handler ID: {}", statusEndpointHandler)

        val router = Router.router(vertx)

        router.route().consumes("application/json")
        router.route().produces("application/json")
        router.route().handler { context: RoutingContext ->
            context.response().headers().add("Content-Type", "application/json")
            val currentUri = context.request().absoluteURI()
            log.info("Setting up current URI: {}", currentUri)
            SharedCache.putData(vertx, SharedCache.CURRENT_SERVER_URI_KEY_NAME, currentUri)
            context.next()
        }

        router.route().failureHandler(FailureExceptionHandler())
        router.route().handler(BodyHandler.create())

        router.get("/channels/:channelId").handler { handleGetChannels(it) }
        router.post("/oauth2/authUrl").handler(oauth2Handler)
        // TODO: implement validation
        router.get("/oauth2/validate/code").handler(oauth2Handler)

        router["/_status"].handler {
           try {
               statusEndpointHandler.status(it)
           } catch(t: Throwable) {
               log.error("Error", t)
           }
        }

        router.post("/m3u").handler { ctx: RoutingContext -> handleM3uUpload(ctx) }
        return router
    }

    private fun readUploadedM3uPlaylist(f: FileUpload): Single<String> {
        return vertx.fileSystem()
                .rxReadFile(f.uploadedFileName())
                .map { obj: Buffer -> obj.toString() }
    }

    // https://github.com/vert-x3/vertx-examples/blob/4.x/web-examples/src/main/java/io/vertx/example/web/rest/SimpleREST.java
    private fun handleM3uUpload(ctx: RoutingContext) {
        val response = ctx.response()
        response.putHeader("Content-Type", "text/plain")
        response.isChunked = true
        val fileUploadList: List<FileUpload> = ArrayList(ctx.fileUploads())
        val fileUpload = fileUploadList[0]
        readUploadedM3uPlaylist(fileUpload)
                .subscribe { result: String ->
                    val m3uParser = M3uParser()
                    val m3uPlaylist = m3uParser.parse(result)
                    log.info("Result $result")
                    log.info("M3u8 " + m3uPlaylist.entries())
                    response.setStatusCode(201).end(result)
                }
    }

    private fun handleGetChannels(routingContext: RoutingContext) {
        val channelId = routingContext.request().getParam("channelId")
        val response = routingContext.response()
        val currentUri = routingContext.request().absoluteURI()
        vertx.orCreateContext.put("currentUri", currentUri)

        vertx.eventBus().rxSend<JsonObject>("channel", JsonObject().put("channelId", channelId))
                .subscribe({
                    log.info("Sent channelId")
                    response
                            .putHeader("content-type", "application/json")
                            .end(Json.encodePrettily(it.body()))
                }, {
                    log.error("Error occurred", it)
                    response.setStatusCode(500).end(Json.encodePrettily(JsonObject(it.message)))
                })
    }

    private fun sendErrorCode(response: HttpServerResponse, it: Throwable) {
        response.setStatusCode(500).end(Json.encodePrettily(JsonObject(it.message)))
    }
}
