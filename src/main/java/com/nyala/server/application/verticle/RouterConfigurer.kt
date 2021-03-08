package com.nyala.server.application.verticle

import com.nyala.server.common.vertx.FailureExceptionHandler
import com.nyala.server.common.vertx.verticle.StatusVerticle
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.PermittedOptions
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.RoutingContext
import io.vertx.rxjava.ext.web.handler.BodyHandler
import io.vertx.rxjava.ext.web.handler.LoggerHandler
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler

class RouterConfigurer {
    companion object {

        fun globalRouter(vertx: Vertx): Router? {
            return jsonRouterBase(vertx)
        }

        private fun jsonRouterBase(vertx: Vertx): Router? {
            val router = Router.router(vertx)
            addEventBusDefinitions(vertx, router)

            //TODO: add logback
            router.route().handler(LoggerHandler.create(LoggerFormat.TINY))
            router.route().handler(BodyHandler.create())
            router.route().consumes("application/json")
            router.route().produces("application/json")
            router.route().handler { context: RoutingContext ->
                context.response().headers().add("Content-Type", "application/json")
                context.next()
            }
            router.route().failureHandler(FailureExceptionHandler())
            return router
        }

        private fun addEventBusDefinitions(vertx: Vertx, router: Router) {
            val bridgeOptions = BridgeOptions().setReplyTimeout(1000L)
            val address = StatusVerticle.STATUS_ADDRESS
            bridgeOptions.addInboundPermitted(PermittedOptions().setAddress(address))
            router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(bridgeOptions))
        }
    }
}