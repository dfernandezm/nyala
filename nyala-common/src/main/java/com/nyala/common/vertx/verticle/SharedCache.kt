package com.nyala.common.vertx.verticle

import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.shareddata.LocalMap
import io.vertx.rxjava.core.shareddata.SharedData

/**
 * This class is used to share data across verticles/eventloops. It serves as a
 * thread-safe local in-memory cache
 *
 * See: https://dzone.com/articles/how-to-share-data-between-threads-in-vertx
 */
object SharedCache {

    private val DEFAULT_LOCAL_MAP_NAME = "VERTICLE_CACHE"
    val CURRENT_SERVER_URI_KEY_NAME = "currentUri"

    fun putData(vertx: Vertx, key: String, value: String) {
        val sd: SharedData = vertx.sharedData()
        val sharedData: LocalMap<String, String> = sd.getLocalMap(DEFAULT_LOCAL_MAP_NAME)
        sharedData.put(key, value)
    }

    fun getData(vertx:Vertx, key: String): String {
        val sd: SharedData = vertx.sharedData()
        val sharedData: LocalMap<String, String> = sd.getLocalMap(DEFAULT_LOCAL_MAP_NAME)
        return sharedData.get(key)
    }
}