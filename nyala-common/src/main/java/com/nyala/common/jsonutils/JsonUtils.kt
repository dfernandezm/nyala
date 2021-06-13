package com.nyala.common.jsonutils

import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.stream.IntStream
import java.util.stream.Stream

object JsonUtils {
    @JvmStatic
    fun encodeResult(result: Any?): String? {
        return if (result == null) {
            null
        } else {
            if (result is List<*>) JsonArray(Json.encode(result)).encodePrettily() else Json.encode(result)
        }
    }

    fun getAuthorizationHeader(message: JsonObject): String {
        return message.getString("authorization").replace("Bearer", "").trim { it <= ' ' }
    }

    fun iterateJsonArray(jsonArray: JsonArray): Stream<JsonObject> {
        val list = IntStream.range(0, jsonArray.size())
        jsonArray.javaClass
        return list.mapToObj { pos: Int -> jsonArray.getJsonObject(pos) }
    }
}