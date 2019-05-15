package com.fazula.server.common.jsonutils;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class JsonUtils {
    public JsonUtils() {
    }

    public static String encodeResult(Object result) {
        if (result == null) {
            return null;
        } else {
            return result instanceof List ? (new JsonArray(Json.encode(result))).encodePrettily() : Json.encode(result);
        }
    }

    public static String getAuthorizationHeader(JsonObject message) {
        return message.getString("authorization").replace("Bearer", "").trim();
    }

    public static Stream<JsonObject> iterateJsonArray(JsonArray jsonArray) {
        IntStream var10000 = IntStream.range(0, jsonArray.size());
        jsonArray.getClass();
        return var10000.mapToObj(jsonArray::getJsonObject);
    }
}