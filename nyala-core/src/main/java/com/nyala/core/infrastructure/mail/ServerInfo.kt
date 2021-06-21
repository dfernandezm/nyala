package com.nyala.core.infrastructure.mail

import com.nyala.common.vertx.verticle.SharedCache
import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx

class ServerInfo(private val vertx: Vertx, private val context: Context) {
    fun currentUri(): String {
        return SharedCache.getData(vertx, "currentUri")
    }
}
