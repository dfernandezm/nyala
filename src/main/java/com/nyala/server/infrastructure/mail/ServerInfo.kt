package com.nyala.server.infrastructure.mail

import io.vertx.rxjava.core.Context

class ServerInfo(private val context: Context) {
    fun currentUri(): String {
        return context.get("currentUri")
    }
}
